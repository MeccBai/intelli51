package com.intelli51.intelli51

import com.intelli51.intelli51.loadLastFolderFromConfig
import com.intelli51.intelli51.NamedSemaphore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths

// Result object for runAction
data class RunResult(
    val json: String,
    val exitCode: Int,
    val success: Boolean,
    val failureMessage: String? = null
)

// Execute a build command using PowerShell in the format:
// builderPath ToolsPath buildTarget ProjectPath [extraCommand]
// Returns the stdout text and exit code.
suspend fun runAction(command: String = ""): RunResult {
    return runInteractiveBuild(command, { _, _ -> }, { }, null)
}

suspend fun runInteractiveBuild(
    command: String = "",
    onPhaseChange: (String, Boolean) -> Unit,
    registerTerminator: (() -> Unit) -> Unit,
    projectPathInput: String? = null
): RunResult {
    return withContext(Dispatchers.IO) {
        var builderPath = "builder.exe"
        try {
            val possiblePaths = listOf(
                Paths.get("Tools", "builder.exe"),
                Paths.get("libs", "Tools", "builder.exe"),
                Paths.get("build", "libs", "Tools", "builder.exe")
            )
            for (p in possiblePaths) {
                if (Files.exists(p)) {
                    builderPath = p.toAbsolutePath().toString()
                    break
                }
            }
        } catch (_: Exception) {}

        val projectPath = if (!projectPathInput.isNullOrBlank()) {
             projectPathInput
        } else {
             try {
                loadLastFolderFromConfig() ?: ""
            } catch (_: Exception) {
                ""
            }
        }

        if (projectPath.isBlank()) return@withContext RunResult("", -1, false)

        // determine buildTarget (first .i51 file name without extension)
        var buildTarget = ""
        try {
            val dir = Paths.get(projectPath)
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                Files.list(dir).use { stream ->
                    val opt = stream.filter { p -> p.fileName.toString().endsWith(".i51") }.findFirst()
                    if (opt.isPresent) {
                        val name = opt.get().fileName.toString()
                        buildTarget = if (name.endsWith(".i51")) name.substring(0, name.length - 4) else name
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (buildTarget.isBlank()) return@withContext RunResult("", -1, false)

        fun psEscape(s: String): String = s.replace("'", "''")

        var psCmd = "\$exe = (Get-Command '${psEscape(builderPath)}' -ErrorAction Stop | Select-Object -ExpandProperty Source -First 1); \$toolsPath = Split-Path \$exe -Parent; & \$exe \$toolsPath '${psEscape(buildTarget)}' '${psEscape(projectPath)}'"
        if (command.isNotBlank()) psCmd += " '${psEscape(command)}'"

        // Ensure PowerShell returns the exit code of the executed program
        psCmd += "; exit \$LASTEXITCODE"

        println(psCmd)

        // Create Semaphore
        val semName = "StartToDownload"
        val sem = try {
            NamedSemaphore(semName, 0, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        var downloadStarted = false

        // Notify initial Phase
        onPhaseChange("构建中...", false)

        val output = StringBuilder()
        var exit = -1
        try {
            val pb = ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", psCmd)
            pb.redirectErrorStream(true)
            val proc = pb.start()

            // Register terminator
            registerTerminator {
                try {
                    if (System.getProperty("os.name").lowercase().contains("win")) {
                        // Kill process tree on Windows
                        ProcessBuilder("taskkill", "/PID", proc.pid().toString(), "/T", "/F").start().waitFor()
                    }
                    proc.destroyForcibly()
                } catch (e: Exception) {
                    e.printStackTrace()
                    proc.destroyForcibly()
                }
            }

            // Launch semaphore monitor
            val monitorJob = launch(Dispatchers.IO) {
                if (sem != null) {
                    while (isActive && proc.isAlive) {
                        if (sem.lock(200)) { // check every 200ms
                            // Semaphore acquired!
                            downloadStarted = true
                            onPhaseChange("下载中...", true)
                            // We don't necessarily need to hold it, logic-wise strictly just "waiting for release".
                            // But usually, if we simply wait and it gets signaled, we are good.
                            // If we don't release it back, it stays 0?
                            // NamedSemaphore wrapper calls ReleaseSemaphore on unlock.
                            // If the C++ process signals it (release), count goes to 1.
                            // We validly acquired it, count goes to 0.
                            // We should probably just keep it or unlock it.
                            // If we unlock it, and loop continues, we might acquire it again.
                            // So we should break.
                            sem.unlock()
                            break
                        }
                    }
                }
            }

            val reader: BufferedReader = BufferedReader(InputStreamReader(proc.inputStream))
            reader.use { r ->
                var line = r.readLine()
                while (line != null) {
                    output.append(line).append('\n')
                    line = r.readLine()
                }
            }

            exit = proc.waitFor()
            monitorJob.cancel() // Ensure monitor stops

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext RunResult(e.message ?: "", -1, false)
        } finally {
            try {
                sem?.close()
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Parse JSON output logic added here
        val outputStr = output.toString()
        var jsonSaysSuccess = false
        try {
            // Regex to find "FinalResult" : true or false
            val match = Regex("\"FinalResult\"\\s*:\\s*(true|false)").find(outputStr)
            if (match != null) {
                jsonSaysSuccess = match.groupValues[1].toBoolean()
            }
        } catch (_: Exception) {}

        var failureMsg: String? = null
        if (downloadStarted && exit != 0) {
            failureMsg = when (exit) {
                -1 -> "串口打开失败" // -1
                -2 -> "下载错误"     // -2
                -3 -> "程序读取失败" // -3
                -4 -> "程序验证失败" // -4
                else -> "未知下载错误 (Code: $exit)"
            }
        }

        return@withContext RunResult(outputStr, exit, (exit == 0) && jsonSaysSuccess, failureMsg)
    }
}

// Lightweight parser stub (kept for compatibility)
object BuildResultParser {
    fun parse(@Suppress("UNUSED_PARAMETER") text: String): List<Any> = emptyList()
    fun processResult(@Suppress("UNUSED_PARAMETER") responses: List<Any>) {}
}