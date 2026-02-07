package com.intelli51.intelli51

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.nio.file.Path

@Composable
fun ProjectProblemsDialog(
    onDismiss: () -> Unit,
    problems: List<Problem>,
    rootPath: Path?,
    fileManager: FileManager,
    dialogBg: Color,
    dialogTextColor: Color
) {
    val scope = rememberCoroutineScope()

    BoxWithConstraints {
        val maxW = maxWidth
        val maxH = maxHeight
        // target dialog width: up to 760dp, or 90% of available width
        val dialogWidth = if (maxW > 760.dp) 760.dp else maxW * 0.9f
        // target dialog height: 60% of available height, but at least 220dp
        val dialogHeight = (maxH * 0.6f).coerceAtLeast(220.dp)

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = dialogBg,
            title = { Text("问题列表 (${problems.size})", style = MaterialTheme.typography.headlineSmall, color = dialogTextColor) },
            text = {
                Surface(
                    color = dialogBg,
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(8.dp),
                    // Allow height to shrink if content is small, but cap at dialogHeight
                    modifier = Modifier.width(dialogWidth).heightIn(max = dialogHeight)
                ) {
                    if (problems.isEmpty()) {
                        // Centered empty state
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("构建完成，无问题。", style = MaterialTheme.typography.bodyLarge, color = dialogTextColor)
                            }
                        }
                    } else {
                        // Use verticalScroll without fixed height so it uses intrinsic content height up to max constraints
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)) {
                            val grouped = problems.groupBy { it.file ?: "(global)" }
                            for ((file, list) in grouped) {
                                val errCount = list.count { it.severity.equals("Error", true) }
                                val warnCount = list.count { it.severity.equals("Warning", true) }

                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    tonalElevation = 2.dp,
                                    color = MaterialTheme.colorScheme.surface // Items contrast against dialogBg
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                                Text("$errCount 错误  •  $warnCount 警告", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (errCount > 0) {
                                                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), modifier = Modifier.padding(2.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                            Text("$errCount", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                                if (warnCount > 0) {
                                                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), modifier = Modifier.padding(2.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                            Text("$warnCount", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        val preview = list.sortedBy { it.line ?: Int.MAX_VALUE }
                                        for (p in preview) {
                                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                                scope.launch {
                                                    val target = if (file == "(global)") null else findFileInProject(rootPath, file)
                                                    if (target != null) fileManager.openOrFocus(target, 0, p.line ?: 1)
                                                    onDismiss()
                                                }
                                            }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                val isErr = p.severity.equals("Error", true)
                                                Text(p.line?.toString() ?: "-", color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, modifier = Modifier.width(48.dp))
                                                Column(modifier = Modifier.weight(1f)) { Text(p.message, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface) }
                                                Text(p.severity, color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                                            }
                                            HorizontalDivider()
                                        }
                                    }
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
}
