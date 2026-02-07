package com.intelli51.intelli51

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RunOutputDialog(
    onDismiss: () -> Unit,
    dialogBg: Color,
    dialogTextColor: Color,
    runStatus: String,
    runExitCode: Int,
    runFailureMessage: String?,
    problems: List<Problem>,
    onShowProblems: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("构建结果", style = MaterialTheme.typography.headlineSmall, color = dialogTextColor) },
        text = {
            Surface(
                color = dialogBg,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Status panel with icon, color and brief summary + quick actions
                    Column(modifier = Modifier.width(640.dp)) {
                        val errorCount = problems.count { it.severity.equals("Error", true) }
                        val warnCount = problems.count { it.severity.equals("Warning", true) }
                        val (icon, tint, statusText) = when {
                            runStatus == "完美成功" -> Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, runStatus)
                            runStatus == "成功（有警告）" -> Triple(Icons.Default.Warning, MaterialTheme.colorScheme.secondary, runStatus)
                            else -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, runStatus)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(statusText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "错误: $errorCount    警告: $warnCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = {
                                        onShowProblems()
                                        onDismiss()
                                    }) { Text("查看问题", color = MaterialTheme.colorScheme.primary) }
                                }
                            }
                        }

                        if (runFailureMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                tonalElevation = 2.dp,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("下载错误详情：", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.height(4.dp))
                                    Text(runFailureMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        // Show a compact preview of problems (if any)
                        if (problems.isNotEmpty()) {
                            Text("最近的问题预览：", style = MaterialTheme.typography.labelMedium, color = dialogTextColor)
                            Spacer(Modifier.height(6.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val preview = problems.take(6)
                                for (p in preview) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isErr = p.severity.equals("Error", true)
                                        Text(
                                            p.file ?: "(global)",
                                            modifier = Modifier.width(180.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = dialogTextColor
                                        )
                                        Text(
                                            p.line?.toString() ?: "-",
                                            modifier = Modifier.width(48.dp),
                                            color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            p.message,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = dialogTextColor
                                        )
                                    }
                                    HorizontalDivider(color = dialogTextColor.copy(alpha = 0.1f))
                                }
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            if (runStatus == "错误") {
                                Text(
                                    "构建失败 (退出代码 $runExitCode)，未检测到结构化错误信息。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text("构建完成，无问题。", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("关闭", color = dialogTextColor) }
            }
        }
    )
}
