package com.intelli51.intelli51

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.nio.file.Path

@Composable
fun ProjectSideBar(
    rootPath: Path?,
    fileManager: FileManager,
    childrenCache: Map<Path, List<FileEntry>>,
    expandedDirs: SnapshotStateList<Path>,
    activePath: Path?,
    onToggleExpand: (Path) -> Unit,
    onOpen: (Path) -> Unit,
    onDelete: (Path) -> Unit,
    onRequestCreateC: () -> Unit,
    onRequestCreateH: () -> Unit
) {
    Box(modifier = Modifier.padding(bottom = UiSizes.BOTTOM_MARGIN)) {
        // Sidebar card: tint the sidebar with the theme primary container for a consistent accent
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(UiSizes.SIDEBAR_WIDTH),
            // keep sidebar neutral so individual list items can show the theme tint
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // keep a narrow full-strength primary stripe but slightly softened
                    Box(
                        modifier = Modifier.width(UiSizes.SIDEBAR_STRIPE_WIDTH).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    )
                    Column(modifier = Modifier.fillMaxWidth().padding(start = UiSizes.TAB_HORIZONTAL_PADDING, top = UiSizes.EDITOR_INNER_PADDING, end = UiSizes.TAB_HORIZONTAL_PADDING, bottom = UiSizes.EDITOR_INNER_PADDING)) {
                        Text(
                            rootPath?.fileName?.toString() ?: "文件",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "项目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f))

                // File list wrapper left transparent so each FileListItem can show themed tint
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                        FileList(
                            rootName = rootPath?.fileName?.toString() ?: "文件",
                            topEntries = (childrenCache[rootPath] ?: emptyList()).filter {
                                !it.isDirectory || it.name.equals("src", ignoreCase = true) || it.name.equals("inc", ignoreCase = true)
                            },
                            children = childrenCache,
                            expanded = expandedDirs,
                            modifier = Modifier.fillMaxWidth(),
                            activePath = activePath,
                            onToggleExpand = onToggleExpand,
                            onOpen = onOpen,
                            onDelete = onDelete,
                            onRequestCreateC = onRequestCreateC,
                            onRequestCreateH = onRequestCreateH
                        )
                    }
                }
            }
        }
    }
}
