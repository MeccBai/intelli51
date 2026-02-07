package com.intelli51.intelli51

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.net.URI
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.awt.Desktop


// Theme color presets
val colorPresets = listOf(
    Color(0xFF0B63D6), // blue (default)
    Color(0xFF006792), // teal
    Color(0xFF0F9D58), // green
    Color(0xFFF29D3A), // amber/orange
    Color(0xFF8A2BE2), // purple
    Color(0xFF6C757D),  // gray
    Color(0xFFFF0000),   // red
    Color(0xFF00FF00),    // green
    Color(0xFF00FFFF),
    Color(0xFFFFFF00),    // yellow
    Color(0xFF0000FF)
)

// Helper function for mixing colors
fun mixColors(c1: Color, c2: Color, ratio: Float): Color {
    val r = c1.red * (1 - ratio) + c2.red * ratio
    val g = c1.green * (1 - ratio) + c2.green * ratio
    val b = c1.blue * (1 - ratio) + c2.blue * ratio
    val a = c1.alpha * (1 - ratio) + c2.alpha * ratio
    return Color(r, g, b, a)
}

// Settings dialog component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    primaryColorState: MutableState<Color>,
    darkModeState: MutableState<Boolean>,
    // Now this controls code font size (sp)
    codeFontSizeState: MutableState<Float>,
    // project root path (used to persist project-level settings into the .i51 file)
    projectRoot: Path? = null,
    // download port setting stored per-project inside the .i51 JSON
    downloadPortState: MutableState<String> = mutableStateOf("")
) {
    if (visible) {
        val isDark = darkModeState.value
        // Compute container color: mix with white in light mode, with a dark grey in dark mode
        val baseMixColor = if (isDark) Color(0xFF1E1E1E) else Color.White
        val dialogContainer = mixColors(primaryColorState.value, baseMixColor, 0.85f)
        
        // Ensure text color is readable
        val containerLuminance = dialogContainer.red * 0.2126f + dialogContainer.green * 0.7152f + dialogContainer.blue * 0.0722f
        val containerText = if (containerLuminance > 0.5f) Color.Black else Color.White

        // Custom hex input state
        var customHex by remember(primaryColorState.value) { mutableStateOf(colorToHex(primaryColorState.value)) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("主题颜色", color = containerText) },
            text = {
                Column {
                    // Live preview of the currently selected primary color
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColorState.value),
                        contentAlignment = Alignment.Center
                    ) {
                        // Show hex in contrasting color
                        val hex = colorToHex(primaryColorState.value)
                        val luminance = primaryColorState.value.red * 0.2126f + primaryColorState.value.green * 0.7152f + primaryColorState.value.blue * 0.0722f
                        val textColor = if (luminance > 0.5f) Color.Black else Color.White
                        Text(hex, color = textColor)
                    }
                    Spacer(Modifier.height(12.dp))
                    // Dark mode toggle
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("深色模式", color = containerText)
                        Spacer(Modifier.weight(1f))
                        // Persist dark mode when toggled
                        Switch(checked = darkModeState.value, onCheckedChange = {
                            darkModeState.value = it
                            saveSettingsToConfig(primaryColorState.value, it, loadLastFolderFromConfig(), codeFontSizeState.value)
                        })
                    }
                    Spacer(Modifier.height(12.dp))

                    // Code font size control
                    Text("代码字号：${codeFontSizeState.value.toInt()}sp", color = containerText)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        // Decrease button
                        TextButton(onClick = {
                            val new = (codeFontSizeState.value - 1f).coerceAtLeast(10f)
                            codeFontSizeState.value = new
                            saveSettingsToConfig(primaryColorState.value, darkModeState.value, loadLastFolderFromConfig(), new)
                        }) { Text("-", color = containerText) }

                        // Slider
                        Slider(
                            value = codeFontSizeState.value,
                            onValueChange = { v ->
                                codeFontSizeState.value = v
                                // persist on change
                                saveSettingsToConfig(primaryColorState.value, darkModeState.value, loadLastFolderFromConfig(), v)
                            },
                            valueRange = 10f..22f,
                            steps = 12,
                            modifier = Modifier.weight(1f)
                        )

                        // Increase button
                        TextButton(onClick = {
                            val new = (codeFontSizeState.value + 1f).coerceAtMost(22f)
                            codeFontSizeState.value = new
                            saveSettingsToConfig(primaryColorState.value, darkModeState.value, loadLastFolderFromConfig(), new)
                        }) { Text("+", color = containerText) }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("选择预设主色调：", color = containerText)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.Start) {
                        colorPresets.forEach { preset ->
                            Box(modifier = Modifier
                                .size(36.dp)
                                .padding(end = 8.dp)
                                .background(preset, shape = RoundedCornerShape(6.dp))
                                .clickable {
                                    // Update theme immediately and persist both primary + current dark mode + code font size
                                    primaryColorState.value = preset
                                    saveSettingsToConfig(preset, darkModeState.value, loadLastFolderFromConfig(), codeFontSizeState.value)
                                }
                            ) {}
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("自定义颜色：", color = containerText)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = customHex,
                            onValueChange = {
                                customHex = it
                                if (it.length == 7 && it.startsWith("#")) {
                                    val c = parseHexToColor(it)
                                    if (c != null) {
                                        primaryColorState.value = c
                                        saveSettingsToConfig(c, darkModeState.value, loadLastFolderFromConfig(), codeFontSizeState.value)
                                    }
                                }
                            },
                            label = { Text("#RRGGBB", color = containerText.copy(alpha=0.6f)) },
                            singleLine = true,
                            modifier = Modifier.width(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = containerText,
                                unfocusedBorderColor = containerText.copy(alpha = 0.7f),
                                focusedTextColor = containerText,
                                unfocusedTextColor = containerText,
                                cursorColor = containerText,
                                focusedLabelColor = containerText,
                                unfocusedLabelColor = containerText.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        // Apply button for shorter hex or when user finished typing
                        Button(
                            onClick = {
                                val c = parseHexToColor(customHex)
                                if (c != null) {
                                    primaryColorState.value = c
                                    saveSettingsToConfig(c, darkModeState.value, loadLastFolderFromConfig(), codeFontSizeState.value)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerText,
                                contentColor = dialogContainer
                            )
                        ) {
                            Text("应用")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Advanced settings separator
                    HorizontalDivider(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("高级设置", color = containerText)
                    Spacer(Modifier.height(8.dp))
                    // Download serial port field
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("下载串口：", color = containerText)
                        Spacer(Modifier.width(8.dp))

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("仅在有多个可用串口时填写")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            OutlinedTextField(
                                value = downloadPortState.value,
                                onValueChange = { v ->
                                    downloadPortState.value = v
                                    // persist immediately into project's i51 file when projectRoot is provided
                                    try {
                                        if (projectRoot != null) {
                                            saveDownloadPortToProject(projectRoot, v)
                                        }
                                    } catch (_: Exception) {
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = containerText,
                                    unfocusedBorderColor = containerText.copy(alpha = 0.7f),
                                    focusedTextColor = containerText,
                                    unfocusedTextColor = containerText,
                                    cursorColor = containerText
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Register default editor button
                    Button(
                        onClick = { registerFileAssociation() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerText,
                            contentColor = dialogContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("注册为 .i51 默认编辑器")
                    }

                    Spacer(Modifier.height(12.dp))

                    HorizontalDivider(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("关于", color = containerText)
                    Spacer(Modifier.height(4.dp))
                    Text("Copyright © 2024-2026 intelli51 Team. All Rights Reserved.", style = MaterialTheme.typography.bodySmall, color = containerText.copy(alpha = 0.8f))
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        try {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                Desktop.getDesktop().browse(URI("https://gitee.com/zz51c"))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Text("作者主页 (Gitee)", color = containerText)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("关闭", color = containerText) }
                }
            },
            // Use explicit container color derived from selected primary and dark mode state
            containerColor = dialogContainer
        )
    }
}

// Configuration management
private fun getConfigDir(): Path {
    val home = System.getProperty("user.home")
    return Paths.get(home, ".intelli51")
}

private fun getConfigFile(): Path = getConfigDir().resolve("config.json")

private fun colorToHex(c: Color): String {
    val r = (c.red * 255.0f).toInt().coerceIn(0, 255)
    val g = (c.green * 255.0f).toInt().coerceIn(0, 255)
    val b = (c.blue * 255.0f).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun parseHexToColor(hexRaw: String): Color? {
    val hex = hexRaw.trim().removePrefix("#").removePrefix("0x")
    if (!hex.matches(Regex("^[0-9A-Fa-f]{6}"))) return null
    return try {
        val r = Integer.parseInt(hex.substring(0, 2), 16)
        val g = Integer.parseInt(hex.substring(2, 4), 16)
        val b = Integer.parseInt(hex.substring(4, 6), 16)
        Color(r / 255f, g / 255f, b / 255f)
    } catch (e: Exception) {
        null
    }
}

fun loadPrimaryFromConfig(): Color? {
    return try {
        val cfg = getConfigFile()
        if (!Files.exists(cfg)) return null
        val s = Files.readString(cfg, StandardCharsets.UTF_8)
        val match = Regex("\"primary\"\\s*:\\s*\"(#?[0-9A-Fa-f]{6})\"").find(s)
        val hex = match?.groups?.get(1)?.value ?: return null
        parseHexToColor(hex)
    } catch (_: Exception) {
        null
    }
}

fun loadDarkFromConfig(): Boolean? {
    return try {
        val cfg = getConfigFile()
        if (!Files.exists(cfg)) return null
        val s = Files.readString(cfg, StandardCharsets.UTF_8)
        val match = Regex("\"dark\"\\s*:\\s*(true|false)").find(s)
        val v = match?.groups?.get(1)?.value ?: return null
        v.toBoolean()
    } catch (_: Exception) {
        null
    }
}

// New: load code font size (in sp) from config
fun loadCodeFontSizeFromConfig(): Float? {
    return try {
        val cfg = getConfigFile()
        if (!Files.exists(cfg)) return null
        val s = Files.readString(cfg, StandardCharsets.UTF_8)
        val match = Regex("\"codeFontSize\"\\s*:\\s*([0-9]+(?:[.,][0-9]+)?)").find(s)
        val v = match?.groups?.get(1)?.value?.replace(",", ".") ?: return null
        v.toFloatOrNull()
    } catch (_: Exception) {
        null
    }
}

fun loadLastFolderFromConfig(): String? {
    return try {
        val cfg = getConfigFile()
        if (!Files.exists(cfg)) return null
        val s = Files.readString(cfg, StandardCharsets.UTF_8)
        val match = Regex("\"lastFolder\"\\s*:\\s*\"([^\"]+)\"").find(s)
        match?.groups?.get(1)?.value?.replace("\\\\", "\\")
    } catch (_: Exception) {
        null
    }
}

fun loadIsFirstRunFromConfig(): Boolean {
    return try {
        val cfg = getConfigFile()
        if (!Files.exists(cfg)) return true
        val s = Files.readString(cfg, StandardCharsets.UTF_8)
        val match = Regex("\"isFirstRun\"\\s*:\\s*(true|false)").find(s)
        // If key is missing, assume true if file was very old or manually edited, but usually we just want boolean
        // Default to true if not found? Or if config exists but no key -> assume false (not first run, just old config)?
        // Let's assume if config exists, default is false unless explicitly true.
        // BUT if it is a fresh install, config doesn't exist -> true.
        match?.groups?.get(1)?.value?.toBoolean() ?: false
    } catch (_: Exception) {
        true
    }
}

// Save settings: include code font size (default to 13f)
fun saveSettingsToConfig(c: Color, dark: Boolean, lastFolder: String? = null, codeFontSize: Float = 13f, isFirstRun: Boolean = false) {
    try {
        val dir = getConfigDir()
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }
        val cfg = getConfigFile()
        val currentFolder = lastFolder ?: loadLastFolderFromConfig() ?: ""
        // Escape backslashes for JSON
        val escapedFolder = currentFolder.replace("\\", "\\\\")
        val sizeStr = java.util.Locale.US.let { "%.1f".format(it, codeFontSize) }
        val json = """{"primary":"${colorToHex(c)}","dark":${dark},"lastFolder":"$escapedFolder","codeFontSize":$sizeStr,"isFirstRun":$isFirstRun}"""
        Files.writeString(cfg, json, StandardCharsets.UTF_8)
    } catch (_: Exception) {
        // Ignore write failures for now
    }
}

// Keep existing single-value helper for backward-compatibility
fun savePrimaryToConfig(c: Color) {
    // Attempt to preserve previously stored dark value when possible
    val existingDark = loadDarkFromConfig() ?: false
    val existingFont = loadCodeFontSizeFromConfig() ?: 13f
    val existingFirstRun = loadIsFirstRunFromConfig()
    saveSettingsToConfig(c, existingDark, null, existingFont, existingFirstRun)
}

// Theme creation functions
fun createDarkColorScheme(primaryColor: Color): ColorScheme {
    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = mixColors(primaryColor, Color.Black, 0.15f),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF006792),
        surface = Color(0xFF121212),
        background = Color(0xFF101010),
        surfaceVariant = Color(0xFF1E1E1E),
        outline = Color(0xFF2A2A2A)
    )
}

fun createLightColorScheme(primaryColor: Color): ColorScheme {
    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = mixColors(primaryColor, Color.White, 0.85f),
        onPrimaryContainer = Color(0xFF001A3A),
        secondary = Color(0xFF006792),
        surface = Color(0xFFFFFFFF),
        background = Color(0xFFF6F8FA),
        surfaceVariant = Color(0xFFF1F3F5),
        outline = Color(0xFFCDD6E0)
    )
}

// Project .i51 config helpers
/** Find a .i51 file directly under the given project root. Returns null if none. */
fun findI51FileInRoot(root: Path?): Path? {
    if (root == null) return null
    return try {
        val opt = Files.list(root).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".i51") }.findFirst()
        }
        opt.orElse(null)
    } catch (e: Exception) {
        null
    }
}

/** Load downloadPort from the .i51 file (expects JSON). Returns null if not present. */
fun loadDownloadPortFromProject(root: Path?): String? {
    val f = findI51FileInRoot(root) ?: return null
    return try {
        val s = Files.readString(f, StandardCharsets.UTF_8)
        val m = Regex("\"downloadPort\"\\s*:\\s*\"([^\"]+)\"").find(s)
        m?.groupValues?.get(1)
    } catch (e: Exception) {
        null
    }
}

/** Save downloadPort into the .i51 file as a JSON object. If no .i51 exists, create one named after the folder. */
fun saveDownloadPortToProject(root: Path, port: String) {
    try {
        var f = findI51FileInRoot(root)
        if (f == null) {
            // create a default .i51 file named after the folder
            val name = root.fileName?.toString() ?: "project"
            f = root.resolve("${name}.i51")

            val newJson = buildJsonObject {
                put("downloadPort", port)
            }
            Files.writeString(f, Json { prettyPrint = true }.encodeToString(newJson), StandardCharsets.UTF_8)
            return
        }

        val content = Files.readString(f, StandardCharsets.UTF_8)
        val currentJson = try {
             if (content.isBlank()) JsonObject(emptyMap()) else Json { ignoreUnknownKeys = true; isLenient = true }.parseToJsonElement(content).jsonObject
        } catch (e: Exception) {
             JsonObject(emptyMap())
        }

        val newJson = JsonObject(currentJson.toMutableMap().apply {
            put("downloadPort", JsonPrimitive(port))
        })

        Files.writeString(f, Json { prettyPrint = true }.encodeToString(newJson), StandardCharsets.UTF_8)
    } catch (e: Exception) {
        // ignore write failures for now
        e.printStackTrace()
    }
}

fun registerFileAssociation() {
    try {
        val exePathObj = ProcessHandle.current().info().command().orElse(null)
        if (exePathObj == null) return
        val exePath = Paths.get(exePathObj).toAbsolutePath().toString()

        val progId = "Intelli51.Project"
        val ext = ".i51"
        val desc = "Intelli51 Project File"

        val commands = listOf(
            arrayOf("reg", "add", "HKCU\\Software\\Classes\\$ext", "/ve", "/d", progId, "/f"),
            arrayOf("reg", "add", "HKCU\\Software\\Classes\\$progId", "/ve", "/d", desc, "/f"),
            arrayOf("reg", "add", "HKCU\\Software\\Classes\\$progId\\DefaultIcon", "/ve", "/d", "$exePath,0", "/f"),
            arrayOf("reg", "add", "HKCU\\Software\\Classes\\$progId\\shell\\open\\command", "/ve", "/d", "\"$exePath\" \"%1\"", "/f")
        )

        for (cmd in commands) {
            Runtime.getRuntime().exec(cmd).waitFor()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
