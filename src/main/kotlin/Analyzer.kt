package com.intelli51.intelli51

import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val jsonDecoder = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

fun parseProblemsFromJson(raw: String): List<Problem> {
    if (raw.isBlank()) return emptyList()
    val out = mutableListOf<Problem>()

    try {
        // Handle InitFailed root object case if the structure is flat for init errors
        if (raw.contains("\"InitFailed\"")) {
             try {
                 val root = jsonDecoder.decodeFromString<BuilderOutput>(raw)
                 if (root.Result == "InitFailed") {
                     out.add(Problem(null, null, "Init failed: ${root.InitFailedType}", "Error"))
                     return out // Return early if init failed
                 }
             } catch (_: Exception) {}
        }

        val root = try {
            jsonDecoder.decodeFromString<BuilderOutput>(raw)
        } catch (e: Exception) {
            // e.printStackTrace()
            return emptyList()
        }

        // Process Compile Info
        root.CompileInfo?.CompileTips?.forEach { tip ->
            // Warnings
            if (!tip.CompileWarningFile.isNullOrBlank()) {
                if (tip.CompileWarnings.isNullOrEmpty()) {
                    // File level warning without details
                    out.add(Problem(tip.CompileWarningFile, null, "Warning (check file)", "Warning"))
                } else {
                    tip.CompileWarnings.forEach { warn ->
                        val line = warn.CompileLine?.toIntOrNull()
                        val msg = warn.CompileWarningInfo ?: "Unknown warning"
                        val type = warn.CompileWarningType?.let { "[$it] " } ?: ""
                        out.add(Problem(tip.CompileWarningFile, line, "$type$msg", "Warning"))
                    }
                }
            }

            // Errors
            if (!tip.CompileErrorFile.isNullOrBlank()) {
                if (tip.CompileErrors.isNullOrEmpty()) {
                    out.add(Problem(tip.CompileErrorFile, null, "Error (check file)", "Error"))
                } else {
                    tip.CompileErrors.forEach { err ->
                        val line = err.CompileLine?.toIntOrNull()
                        val msg = err.CompileErrorInfo ?: "Unknown error"
                        val type = err.CompileErrorType?.let { "[$it] " } ?: ""
                        out.add(Problem(tip.CompileErrorFile, line, "$type$msg", "Error"))
                    }
                }
            }
        }

        // Process Link Info
        root.LinkInfo?.LinkTips?.forEach { linkTip ->
             if (!linkTip.`object`.isNullOrBlank() && !linkTip.symbol.isNullOrBlank()) {
                 out.add(Problem(linkTip.`object`, null, "Missing symbol: ${linkTip.symbol}", "Error"))
             }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return out
}

fun buildProblemsMap(problems: List<Problem>): Map<String, Set<Int>> {
    val map = mutableMapOf<String, MutableSet<Int>>()
    for (p in problems) {
        if (p.file != null) {
            val set = map.getOrPut(p.file) { mutableSetOf() }
            if (p.line != null) set.add(p.line)
        }
    }
    return map
}

suspend fun findFileInProject(root: Path?, fileName: String?): Path? {
    if (root == null || fileName == null) return null
    return withContext(Dispatchers.IO) {
        try {
            Files.walk(root).use { stream ->
                stream.filter { p -> p.fileName?.toString()?.equals(fileName, true) == true }
                    .findFirst().orElse(null)
            }
        } catch (e: Exception) {
            null
        }
    }
}
