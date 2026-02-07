package com.intelli51.intelli51

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun welcomeDialog(
    dialogBg: Color,
    dialogTextColor: Color,
    primaryColor: Color,
    darkMode: Boolean,
    lastFolder: String?,
    codeFontSize: Float,
    onCloseWelcome: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Force user to accept or acknowledge */ },
        containerColor = dialogBg,
        title = { Text("欢迎使用 intelli51", color = dialogTextColor) },
        text = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "许可协议与说明",
                        style = MaterialTheme.typography.titleMedium,
                        color = dialogTextColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. 本软件为开源/免费软件，旨在辅助 .i51 项目的开发与管理。\n" +
                        "2. 请勿将本软件用于任何非法用途。\n" +
                        "3. 本软件不提供任何形式的担保，使用本软件产生的风险由用户自行承担。\n" +
                        "4. 首次使用建议前往“设置”调整界面主题与编辑器字号。\n\n" +
                        "点击“同意并继续”即代表您已阅读并接受上述条款。\n",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dialogTextColor.copy(alpha = 0.9f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCloseWelcome()
                    // Update config to mark first run as complete
                    saveSettingsToConfig(
                        primaryColor,
                        darkMode,
                        lastFolder,
                        codeFontSize,
                        isFirstRun = false
                    )
                }
            ) { Text("同意并继续") }
        }
    )
}

@Composable
fun startupDialog(
    lastFolder: String?,
    onOpenFolder: () -> Unit,
    onOpenLastFolder: () -> Unit,
    dialogBg: Color,
    dialogTextColor: Color
) {
    AlertDialog(
        onDismissRequest = { /* Mandatory choice */ },
        containerColor = dialogBg,
        title = { Text("欢迎使用 intelli51", color = dialogTextColor) },
        text = {
            Column {
                Text("请选择一个文件夹以开始。", color = dialogTextColor)
                if (!lastFolder.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "上次打开的文件夹：$lastFolder",
                        style = MaterialTheme.typography.bodySmall,
                        color = dialogTextColor.copy(alpha = 0.8f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenFolder) { Text("打开文件夹") }
        },
        dismissButton = if (!lastFolder.isNullOrBlank()) {
            {
                TextButton(onClick = onOpenLastFolder) { Text("打开上次文件夹", color = dialogTextColor) }
            }
        } else null
    )
}
