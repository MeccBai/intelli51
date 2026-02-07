@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@file:Suppress("InvalidPackageDeclaration", "TooGenericExceptionCaught", "PrintStackTrace", "ReturnCount")

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
        checkForInitFailed(raw, out)
        if (out.isNotEmpty()) return out

        val root = try {
            jsonDecoder.decodeFromString<BuilderOutput>(raw)
        } catch (_: Exception) {
            return emptyList()
        }

        processCompileInfo(root.compileInfo, out)
        processLinkInfo(root.linkInfo, out)

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return out
}

private fun checkForInitFailed(raw: String, out: MutableList<Problem>) {
    // Handle InitFailed root object case if the structure is flat for init errors
    if (raw.contains("\"InitFailed\"")) {
         try {
             val root = jsonDecoder.decodeFromString<BuilderOutput>(raw)
             if (root.result == "InitFailed") {
                 out.add(Problem(null, null, "Init failed: ${root.initFailedType}", "Error"))
             }
         } catch (_: Exception) {}
    }
}

private fun processCompileInfo(compileInfo: CompileInfo?, out: MutableList<Problem>) {
    val tips = compileInfo?.compileTips ?: return

    tips.forEach { tip ->
        processTipWarnings(tip, out)
        processTipErrors(tip, out)
    }
}

private fun processTipWarnings(tip: CompileTip, out: MutableList<Problem>) {
    if (tip.compileWarningFile.isNullOrBlank()) return

    if (tip.compileWarnings.isNullOrEmpty()) {
        // File level warning without details
        out.add(Problem(tip.compileWarningFile, null, "Warning (check file)", "Warning"))
    } else {
        tip.compileWarnings.forEach { warn ->
            val line = warn.compileLine?.toIntOrNull()
            val msg = warn.compileWarningInfo ?: "Unknown warning"
            val type = warn.compileWarningType?.let { "[$it] " } ?: ""
            out.add(Problem(tip.compileWarningFile, line, "$type$msg", "Warning"))
        }
    }
}

private fun processTipErrors(tip: CompileTip, out: MutableList<Problem>) {
    if (tip.compileErrorFile.isNullOrBlank()) return

    if (tip.compileErrors.isNullOrEmpty()) {
        out.add(Problem(tip.compileErrorFile, null, "Error (check file)", "Error"))
    } else {
        tip.compileErrors.forEach { err ->
            val line = err.compileLine?.toIntOrNull()
            val msg = err.compileErrorInfo ?: "Unknown error"
            val type = err.compileErrorType?.let { "[$it] " } ?: ""
            out.add(Problem(tip.compileErrorFile, line, "$type$msg", "Error"))
        }
    }
}

private fun processLinkInfo(linkInfo: LinkInfo?, out: MutableList<Problem>) {
    linkInfo?.linkTips?.forEach { linkTip ->
         if (!linkTip.objectName.isNullOrBlank() && !linkTip.symbol.isNullOrBlank()) {
             out.add(Problem(linkTip.objectName, null, "Missing symbol: ${linkTip.symbol}", "Error"))
         }
    }
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
        } catch (_: Exception) {
            null
        }
    }
}
