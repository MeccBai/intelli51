@file:Suppress("InvalidPackageDeclaration", "FunctionNaming", "ktlint:standard:function-naming")

package com.intelli51.intelli51

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.nio.file.Path

private val maxDialogWidth = 760.dp
private const val MAX_DIALOG_WIDTH_RATIO = 0.9f
private const val DIALOG_HEIGHT_RATIO = 0.6f
private val minDialogHeight = 220.dp
private val dialogCornerRadius = 8.dp
private val badgeCornerRadius = 12.dp

@Suppress("LongParameterList")
@Composable
fun ProjectProblemsDialog(
    onDismiss: () -> Unit,
    problems: List<Problem>,
    rootPath: Path?,
    fileManager: FileManager,
    dialogBg: Color,
    dialogTextColor: Color
) {
    BoxWithConstraints {
        val maxW = maxWidth
        val maxH = maxHeight
        val dialogWidth = if (maxW > maxDialogWidth) maxDialogWidth else maxW * MAX_DIALOG_WIDTH_RATIO
        val dialogHeight = (maxH * DIALOG_HEIGHT_RATIO).coerceAtLeast(minDialogHeight)

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = dialogBg,
            title = {
                Text(
                    "问题列表 (${problems.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = dialogTextColor
                )
            },
            text = {
                Surface(
                    color = dialogBg,
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(dialogCornerRadius),
                    modifier = Modifier.width(dialogWidth).heightIn(max = dialogHeight)
                ) {
                    if (problems.isEmpty()) {
                        EmptyProblemsView(dialogTextColor)
                    } else {
                        ProblemsList(
                            problems = problems,
                            rootPath = rootPath,
                            fileManager = fileManager,
                            onDismiss = onDismiss
                        )
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

@Composable
private fun EmptyProblemsView(textColor: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "构建完成，无问题。",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

@Composable
private fun ProblemsList(
    problems: List<Problem>,
    rootPath: Path?,
    fileManager: FileManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)) {
        val grouped = problems.groupBy { it.file ?: "(global)" }
        for ((file, list) in grouped) {
            val errCount = list.count { it.severity.equals("Error", true) }
            val warnCount = list.count { it.severity.equals("Warning", true) }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(dialogCornerRadius),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    ProblemGroupHeader(file, errCount, warnCount)

                    Spacer(Modifier.height(8.dp))

                    val preview = list.sortedBy { it.line ?: Int.MAX_VALUE }
                    for (p in preview) {
                        ProblemItem(p) {
                            scope.launch {
                                val target = if (file == "(global)") null else findFileInProject(rootPath, file)
                                if (target != null) fileManager.openOrFocus(target, 0, p.line ?: 1)
                                onDismiss()
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemGroupHeader(
    file: String,
    errCount: Int,
    warnCount: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                file,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "$errCount 错误  •  $warnCount 警告",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (errCount > 0) {
                CountBadge(
                    count = errCount,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.Error
                )
            }
            if (warnCount > 0) {
                CountBadge(
                    count = warnCount,
                    color = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Default.Warning
                )
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(badgeCornerRadius),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "$count",
                color = color,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ProblemItem(
    p: Problem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isErr = p.severity.equals("Error", true)
        val color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary

        Text(
            p.line?.toString() ?: "-",
            color = color,
            modifier = Modifier.width(48.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                p.message,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(p.severity, color = color)
    }
}
