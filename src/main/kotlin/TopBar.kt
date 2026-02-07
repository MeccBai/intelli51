package com.intelli51.intelli51

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import javax.swing.JFileChooser
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    onOpenFolder: () -> Unit,
    onSaveActiveFile: () -> Unit,
    activeFileId: String?,
    isProjectOpen: Boolean,
    onSearchClick: () -> Unit,
    onRunClick: () -> Unit,
    isBuilding: Boolean,
    problemsCount: Int,
    onShowProblems: () -> Unit,
    onShowSettings: () -> Unit
) {
    Surface(
        // Surface uses the scaffold outer padding now; keep it full width inside scaffold
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        TopAppBar(
            title = { Text("intelli51 - 简易编辑器", style = MaterialTheme.typography.titleLarge) },
            actions = {
                IconButton(onClick = onOpenFolder) { Icon(Icons.Default.FolderOpen, contentDescription = "打开文件夹") }
                Spacer(Modifier.width(8.dp))

                IconButton(onClick = onSaveActiveFile, enabled = activeFileId != null) { Icon(Icons.Default.Save, contentDescription = "保存") }

                Spacer(Modifier.width(8.dp))
                // Search button
                IconButton(onClick = onSearchClick, enabled = isProjectOpen) { Icon(Icons.Default.Search, contentDescription = "搜索") }

                Spacer(Modifier.width(8.dp))
                // Run button
                IconButton(onClick = onRunClick, enabled = !isBuilding && isProjectOpen) {
                    if (isBuilding) {
                        // small inline spinner to indicate building
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "运行")
                    }
                }

                Spacer(Modifier.width(8.dp))
                // Problems button (opens structured problems dialog)
                IconButton(onClick = onShowProblems, enabled = problemsCount > 0) {
                    BadgedBox(badge = {
                        if (problemsCount > 0) Badge { Text("$problemsCount") }
                    }) {
                        Icon(Icons.Default.Error, contentDescription = "问题")
                    }
                }

                Spacer(Modifier.width(8.dp))
                // Settings button
                IconButton(onClick = onShowSettings) { Icon(Icons.Default.Settings, contentDescription = "设置") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}
