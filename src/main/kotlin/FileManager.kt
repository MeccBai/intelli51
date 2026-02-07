package com.intelli51.intelli51

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.Locale.getDefault

data class FileEntry(val path: Path, val name: String, val isDirectory: Boolean, val modified: Instant?)

// Represent a highlight range within the file (start/end are character offsets)
data class HighlightRange(val start: Int, val end: Int, val colorArgb: Long)

// OpenFile is a regular class with observable content/isDirty so Compose recomposes when edited
class OpenFile(
    val id: String,
    val path: Path,
    original: String
) {
    var originalContent: String = original
        private set

    private val _content = mutableStateOf(original)
    var content: String
        get() = _content.value
        set(value) { _content.value = value }

    private val _isDirty = mutableStateOf(false)
    var isDirty: Boolean
        get() = _isDirty.value
        set(value) { _isDirty.value = value }

    var cursor: Int = 0

    // Highlighting support per open file:
    // - keywordColors: map from keyword -> ARGB long (0xAARRGGBB)
    // - highlightRanges: explicit ranges (start,end) with color
    val keywordColors = mutableStateOf<Map<String, Long>>(emptyMap())
    val highlightRanges = mutableStateListOf<HighlightRange>()

    fun markSaved() {
        originalContent = content
        isDirty = false
    }
}

data class SearchMatch(val path: Path, val lineNumber: Int, val lineContent: String, val offset: Int)

class FileManager {
    // Compose-friendly state for open files and active file id
    val openFiles = mutableStateListOf<OpenFile>()
    val activeFileId = mutableStateOf<String?>(null)
    
    // Track if there's a pending jump (offset + line) for a file
    val pendingJump = mutableStateOf<Triple<String, Int, Int>?>(null)

    // Legacy recursive scan (kept for compatibility) - currently unused
    suspend fun scan(root: Path): List<FileEntry> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileEntry>()
        if (!Files.exists(root)) return@withContext results
        Files.walk(root).use { stream ->
            stream.forEach { p ->
                try {
                    val attrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                    if (!attrs.isDirectory) {
                        val name = p.fileName?.toString() ?: p.toString()
                        val lower = name.lowercase()
                        if (lower.endsWith(".c") || lower.endsWith(".h")) {
                            val entry = FileEntry(
                                path = p,
                                name = name,
                                isDirectory = false,
                                modified = attrs.lastModifiedTime()?.toInstant()
                            )
                            results += entry
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        results.sortedWith(compareBy { it.name.lowercase(getDefault()) })
    }

    // List immediate children of a directory (non-recursive)
    suspend fun listDirectory(dir: Path): List<FileEntry> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileEntry>()
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return@withContext results
        Files.list(dir).use { stream ->
            stream.forEach { p ->
                try {
                    val attrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                    val entry = FileEntry(
                        path = p,
                        name = p.fileName?.toString() ?: p.toString(),
                        isDirectory = attrs.isDirectory,
                        modified = attrs.lastModifiedTime()?.toInstant()
                    )
                    results += entry
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
        results.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase(getDefault()) })
    }

    suspend fun read(path: Path): String = withContext(Dispatchers.IO) {
        Files.readString(path, StandardCharsets.UTF_8)
    }

    suspend fun save(path: Path, content: String) = withContext(Dispatchers.IO) {
        Files.writeString(path, content, StandardCharsets.UTF_8)
    }

    // Open or focus an existing file; create an in-memory copy (originalContent + content buffer)
    suspend fun openOrFocus(path: Path, jumpToOffset: Int? = null, jumpToLine: Int? = null) {
        val name = path.fileName?.toString() ?: return
        val lower = name.lowercase()
        
        // Restore restriction: only .c and .h
        if (!(lower.endsWith(".c") || lower.endsWith(".h"))) return

        val targetPath = path.toAbsolutePath().normalize()
        val existing = openFiles.firstOrNull { it.path.toAbsolutePath().normalize() == targetPath }
        val id = if (existing != null) {
            activeFileId.value = existing.id
            existing.id
        } else {
            val fileText = try {
                read(path)
            } catch (e: Exception) {
                return
            }
            val newId = java.util.UUID.randomUUID().toString()
            val of = OpenFile(id = newId, path = path, original = fileText)
            openFiles.add(of)
            activeFileId.value = newId
            newId
        }
        
        if (jumpToOffset != null && jumpToLine != null) {
            pendingJump.value = Triple(id, jumpToOffset, jumpToLine)
        }
    }

    fun close(id: String) {
        val idx = openFiles.indexOfFirst { it.id == id }
        if (idx >= 0) {
            openFiles.removeAt(idx)
            if (activeFileId.value == id) activeFileId.value = openFiles.firstOrNull()?.id
        }
    }

    // Update only the in-memory buffer and mark dirty
    fun updateContent(id: String, newContent: String) {
        val f = openFiles.firstOrNull { it.id == id } ?: return
        f.content = newContent
        f.isDirty = (f.content != f.originalContent)
    }

    // Save buffer to disk (overwrite) and update originalContent
    suspend fun saveOpenFile(id: String) {
        val f = openFiles.firstOrNull { it.id == id } ?: return
        save(f.path, f.content)
        f.markSaved()
    }

    /**
     * Parse a hex color string into ARGB long (0xAARRGGBB).
     * Accepts: "#RRGGBB", "RRGGBB", "#AARRGGBB", "AARRGGBB".
     * If parsing fails, returns opaque black (0xFF000000).
     */
    private fun parseColorHex(spec: String?): Long {
        if (spec == null) return 0xFF000000L
        var s = spec.trim()
        if (s.startsWith("#")) s = s.substring(1)
        // normalize length
        if (s.length == 6) s = "FF" + s
        return try {
            java.lang.Long.parseLong(s, 16) or 0x00000000L
        } catch (_: Exception) {
            0xFF000000L
        }
    }

    /**
     * Set highlighting rules for an open file (by id).
     * @param id open file id
     * @param keywordMap map of keyword -> color hex string (e.g. "#FF0000" or "FF0000")
     * @param ranges list of Triple(start, end, colorHex) where start/end are character offsets
     */
    fun setHighlightRulesForFile(id: String, keywordMap: Map<String, String>, ranges: List<Triple<Int, Int, String>>) {
        val f = openFiles.firstOrNull { it.id == id } ?: return
        val parsed = keywordMap.mapValues { (_, v) -> parseColorHex(v) }
        f.keywordColors.value = parsed
        f.highlightRanges.clear()
        ranges.forEach { (s, e, colorSpec) ->
            val c = parseColorHex(colorSpec)
            // ensure start <= end and non-negative
            val start = s.coerceAtLeast(0)
            val end = e.coerceAtLeast(start)
            f.highlightRanges.add(HighlightRange(start, end, c))
        }
    }

    /**
     * Get highlighting rules for an open file. Returns Pair(keywordMap, ranges).
     */
    fun getHighlightRulesForFile(id: String): Pair<Map<String, Long>, List<HighlightRange>> {
        val f = openFiles.firstOrNull { it.id == id } ?: return Pair(emptyMap(), emptyList())
        return Pair(f.keywordColors.value, f.highlightRanges.toList())
    }

    /**
     * Search for text in all files under the given root directory.
     * Returns a list of SearchMatch containing path, line number, and content.
     */
    suspend fun search(root: Path, query: String): List<SearchMatch> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchMatch>()
        if (query.isBlank() || !Files.exists(root)) return@withContext results
        
        try {
            Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) }.forEach { p ->
                    try {
                        val fileName = p.fileName.toString().lowercase()
                        val pathStr = p.toAbsolutePath().toString().lowercase()

                        // User requirement: Only .c in Src/src and .h in Inc/inc
                        // Check extension
                        val isC = fileName.endsWith(".c")
                        val isH = fileName.endsWith(".h")

                        if (!isC && !isH) return@forEach

                        // Check parent directory specific rules
                        // We check if the path contains path separator + "src" + path separator, or ends with path separator + "src"
                        // But simpler is to check parents.
                        // Let's look for "src" or "inc" component in the relative path from root.

                        val relative = root.relativize(p)
                        val relativeStr = relative.toString().lowercase()
                        // Normalize separators
                        val norm = relativeStr.replace("\\", "/")

                        // Strict check: must be in a folder named 'src' (for .c) or 'inc' (for .h)
                        // This allows src/foo.c or nested/src/foo.c? Usually just top level src/inc in this project type.
                        // Assuming top-level src/inc based on project creation logic.

                        val inSrc = norm.startsWith("src/") || norm.contains("/src/") || norm == "src" // norm==src is file named src
                        val inInc = norm.startsWith("inc/") || norm.contains("/inc/") || norm == "inc"

                        val matchesRule = (isC && inSrc) || (isH && inInc)

                        if (!matchesRule) return@forEach

                        // Try to read as text. If it fails (binary), we catch it.
                        // Ideally we should peek bytes, but readString might fail for non-utf8 binaries.
                        // Using MalformedInputException to skip binaries?

                        val content = try {
                            Files.readString(p, StandardCharsets.UTF_8)
                        } catch (e: Exception) {
                            // If UTF-8 fails, try ISO-8859-1 or just skip
                            try {
                                Files.readString(p, StandardCharsets.ISO_8859_1)
                            } catch (_: Exception) {
                                return@forEach
                            }
                        }

                        var index = content.indexOf(query, ignoreCase = true)
                        while (index >= 0) {
                            val lineNum = content.substring(0, index).count { it == '\n' } + 1
                            val lineStart = content.lastIndexOf('\n', index) + 1
                            var lineEnd = content.indexOf('\n', index)
                            if (lineEnd == -1) lineEnd = content.length
                            val lineContent = content.substring(lineStart, lineEnd).trim()

                            results.add(SearchMatch(p, lineNum, lineContent, index))
                            index = content.indexOf(query, index + 1, ignoreCase = true)
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }
}
