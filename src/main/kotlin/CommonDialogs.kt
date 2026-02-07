package com.intelli51.intelli51

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.nio.file.Path

@Composable
fun InvalidPathDialog(
    onDismiss: () -> Unit,
    dialogBg: Color,
    dialogTextColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("无法打开文件夹", color = dialogTextColor) },
        text = { Text("不允许直接打开驱动器根目录（如 C:/），请选择子文件夹。", color = dialogTextColor) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定", color = dialogTextColor) }
        }
    )
}

@Composable
fun NameInputDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogBg: Color,
    dialogTextColor: Color,
    creatingType: String,
    fileName: String,
    onFileNameChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("创建 ${if (creatingType == "C") ".c" else ".h"} 文件", color = dialogTextColor) },
        text = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("输入文件名（不含后缀）：", color = dialogTextColor)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = onFileNameChange,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = dialogTextColor,
                            unfocusedTextColor = dialogTextColor,
                            focusedBorderColor = dialogTextColor,
                            unfocusedBorderColor = dialogTextColor.copy(alpha = 0.7f),
                            cursorColor = dialogTextColor,
                            focusedLabelColor = dialogTextColor,
                            unfocusedLabelColor = dialogTextColor.copy(alpha = 0.7f),
                        ),
                        textStyle = LocalTextStyle.current.copy(color = dialogTextColor)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("创建", color = dialogTextColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = dialogTextColor) }
        }
    )
}

@Composable
fun GenericErrorDialog(
    onDismiss: () -> Unit,
    title: String,
    content: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    fileName: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除文件") },
        text = { Text("确定要删除 '$fileName' 吗？此操作无法撤销。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun BuildProgressDialog(
    statusText: String,
    dialogBg: Color,
    dialogTextColor: Color,
    canTerminate: Boolean,
    onTerminate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = dialogBg,
        title = { Text(statusText, style = MaterialTheme.typography.headlineSmall, color = dialogTextColor) },
        confirmButton = {
            if (canTerminate) {
                Button(
                    onClick = onTerminate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("终止")
                }
            }
        }
    )
}
