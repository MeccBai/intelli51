package com.intelli51.intelli51

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.nio.file.Path

@Composable
fun SearchDialog(
    onDismiss: () -> Unit,
    dialogBg: Color,
    dialogTextColor: Color,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<SearchMatch>,
    rootPath: Path?,
    onResultClick: (SearchMatch) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("搜索", color = dialogTextColor) },
        text = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("输入搜索关键词：", color = dialogTextColor)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
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

                    Spacer(Modifier.height(12.dp))
                    Text("搜索结果：", color = dialogTextColor)
                    Spacer(Modifier.height(4.dp))
                    // Results list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { match ->
                            val relativePath = rootPath?.relativize(match.path)?.toString() ?: match.path.toString()
                            // Each result item
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onResultClick(match)
                                },
                                shape = RoundedCornerShape(8.dp),
                                tonalElevation = 2.dp,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                    Text(relativePath, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text("第 ${match.lineNumber} 行", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(4.dp))
                                    Text(match.lineContent.trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        // Empty state
                        if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("未找到匹配结果", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = dialogTextColor) }
        }
    )
}
