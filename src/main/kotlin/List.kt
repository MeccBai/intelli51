package com.intelli51.intelli51

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale

@Composable
fun FileList(
    rootName: String = "文件",
    topEntries: List<FileEntry> = emptyList(),
    children: Map<Path, List<FileEntry>> = emptyMap(),
    expanded: List<Path> = emptyList(),
    modifier: Modifier = Modifier,
    activePath: Path? = null,
    onToggleExpand: (Path) -> Unit = {},
    onOpen: (Path) -> Unit = {},
    onDelete: (Path) -> Unit = {},
    onRequestCreateC: () -> Unit = {},
    onRequestCreateH: () -> Unit = {}
) {
    // Keep the outer wrapper transparent so each list item can show its own themed background
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Title row with two + buttons for creating .c and .h
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text(rootName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(8.dp))
                Spacer(Modifier.weight(1f))
                // Request create .c in src/
                IconButton(onClick = onRequestCreateC, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "在 src 中创建 .c")
                }
                // Request create .h in inc/
                IconButton(onClick = onRequestCreateH) {
                    Icon(Icons.Default.Add, contentDescription = "在 inc 中创建 .h")
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                topEntries.forEach { entry ->
                    if (entry.isDirectory) {
                        item {
                            DirectoryRow(entry = entry, isExpanded = expanded.contains(entry.path), onToggle = { onToggleExpand(entry.path) }, activePath = activePath)
                        }
                        if (expanded.contains(entry.path)) {
                            val kids = children[entry.path] ?: emptyList()
                            items(kids) { child ->
                                // increase indent for files inside a directory to show hierarchy clearly
                                FileListItem(entry = child, onClick = { onOpen(child.path) }, onDelete = { onDelete(child.path) }, indent = 40.dp, activePath = activePath)
                            }
                        }
                    } else {
                        item {
                            // small indent for top-level files so they don't sit flush with folder labels
                            FileListItem(entry = entry, onClick = { onOpen(entry.path) }, onDelete = { onDelete(entry.path) }, indent = 16.dp, activePath = activePath)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryRow(entry: FileEntry, isExpanded: Boolean, onToggle: () -> Unit, activePath: Path?) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val itemShape = RoundedCornerShape(12.dp)
    // Use primaryContainer so the themed surface shows properly in M3 and provides good contrast with onPrimaryContainer
    val baseColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.12f else 0.08f)
    val active = (activePath != null && entry.path == activePath)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = itemShape,
        tonalElevation = if (active) 2.dp else 1.dp,
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.22f else 0.16f) else baseColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (active) {
                Box(modifier = Modifier.width(4.dp).height(28.dp).background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
            }
            Icon(if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(entry.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FileListItem(entry: FileEntry, onClick: () -> Unit, onDelete: (Path) -> Unit, indent: Dp = 0.dp, activePath: Path? = null) {
    // Check if the file is under 'src' or 'inc' directory (anywhere in the path components)
    val isDeletable = entry.path.any { 
        val part = it.toString().lowercase()
        part == "src" || part == "inc" 
    }

    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val itemShape = RoundedCornerShape(12.dp)
    // Use primaryContainer for M3 theming; active items use stronger alpha
    val baseColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.12f else 0.08f)
    val isActive = (activePath != null && activePath == entry.path)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = itemShape,
        tonalElevation = if (isActive) 2.dp else 1.dp,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.22f else 0.16f) else baseColor
    ) {
         Row(
             modifier = Modifier
                 .fillMaxWidth()
                 .clickable(onClick = onClick)
                 .padding(start = indent, top = 4.dp, bottom = 4.dp, end = 4.dp),
             horizontalArrangement = Arrangement.Start,
             verticalAlignment = Alignment.CenterVertically
         ) {
            val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            if (isActive) {
                Box(modifier = Modifier.width(4.dp).height(24.dp).background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(4.dp))
                Spacer(Modifier.width(4.dp))
            }
            Icon(Icons.Default.Description, contentDescription = null, tint = contentColor)
             Spacer(Modifier.width(8.dp))
             Text(
                 entry.name,
                 style = MaterialTheme.typography.bodySmall,
                 color = contentColor,
                 modifier = Modifier.weight(1f),
                 maxLines = 1,
                 overflow = TextOverflow.Ellipsis
             )

             if (isDeletable) {
                 IconButton(
                     onClick = { onDelete(entry.path) },
                     modifier = Modifier.size(24.dp)
                 ) {
                     Icon(
                         Icons.Default.Close,
                         contentDescription = "删除文件",
                         tint = MaterialTheme.colorScheme.error,
                         modifier = Modifier.size(18.dp)
                     )
                 }
             }
         }
     }
 }
