package com.intelli51.intelli51

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Helper to mix colors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(
    primaryColorState: MutableState<Color>,
    darkModeState: MutableState<Boolean>,
    codeFontSizeState: MutableState<Float>,
    launchArgs: Array<String> = emptyArray()
) {
    val fileManager = remember { FileManager() }
    val scope = rememberCoroutineScope()
    // CLI polling configuration
    var cliCommand by remember { mutableStateOf("") } // set to e.g. "echo hello" to test
    var rootPath by remember { mutableStateOf<Path?>(null) }
    val childrenCache = remember { mutableStateMapOf<Path, List<FileEntry>>() }
    val expandedDirs = remember { mutableStateListOf<Path>() }

    // Dialog state
    var showNameDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var creatingType by remember { mutableStateOf("C") } // "C" or "H"
    var newFileName by remember { mutableStateOf("") }

    // Project init helpers (missing previously)
    var showStartupDialog by remember { mutableStateOf(rootPath == null) }
    var showInvalidPathDialog by remember { mutableStateOf(false) }
    // Check first-run status:
    var showWelcomeDialog by remember { mutableStateOf(loadIsFirstRunFromConfig()) }

    // Per-project advanced setting: download serial port (MutableState so SettingsDialog can bind to it)
    val projectDownloadPortState = remember { mutableStateOf("") }

    // Delete confirmation dialog state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<Path?>(null) }

    // Folder not found state
    var showFolderNotFoundDialog by remember { mutableStateOf(false) }

    // Settings dialog state
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Search dialog state
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchMatch>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Run output state
    var runOutput by remember { mutableStateOf("") }
    var runStatus by remember { mutableStateOf("") }
    var runExitCode by remember { mutableStateOf(0) } // Store exit code
    var showRunOutputDialog by remember { mutableStateOf(false) }
    var isBuilding by remember { mutableStateOf(false) }

    // Problems state
    var problems by remember { mutableStateOf<List<Problem>>(emptyList()) }
    var problemsByFile by remember { mutableStateOf<Map<String, Set<Int>>>(emptyMap()) }
    var showProblemsDialog by remember { mutableStateOf(false) }

    // Build progress dialog state
    var showBuildProgress by remember { mutableStateOf(false) }
    var buildStatusText by remember { mutableStateOf("") }
    var buildCanTerminate by remember { mutableStateOf(false) }
    var terminator: (() -> Unit)? by remember { mutableStateOf(null) }
    var runFailureMessage by remember { mutableStateOf<String?>(null) } // New state for download failure message

    // whether theme is dark
    val isDark = darkModeState.value
    // In Dark Mode: use Surface tinted with a bit of Primary (approx 12%) so it's not pitch black but not too saturated
    // In Light Mode: use PrimaryContainer (as before, user seemed happy with light mode dialogs or didn't complain)
    val dialogBg = if (isDark) {
        val s = MaterialTheme.colorScheme.surface
        val p = MaterialTheme.colorScheme.primary
        // Manual mix: Surface + 12% Primary
        // Note: Simple RGB mix.
        Color(
            red = s.red * 0.88f + p.red * 0.12f,
            green = s.green * 0.88f + p.green * 0.12f,
            blue = s.blue * 0.88f + p.blue * 0.12f,
            alpha = 1f
        )
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val dialogTextColor = if (isDark)
        MaterialTheme.colorScheme.onSurface
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    // Helper to perform the actual folder opening and cache loading
    val openFolderAction = { path: Path ->
        scope.launch {
            // Ensure src and inc exist
            rootPath = path
            val list = fileManager.listDirectory(path)
            childrenCache.clear()
            childrenCache[path] = list
            // Persist last opened folder and include code font size
            saveSettingsToConfig(primaryColorState.value, darkModeState.value, path.toString(), codeFontSizeState.value)

            // Load per-project download port into shared state so Settings dialog shows it
            projectDownloadPortState.value = loadDownloadPortFromProject(path) ?: ""
        }
    }

    // Process launch arguments
    LaunchedEffect(Unit) {
        if (launchArgs.isNotEmpty()) {
            val pathStr = launchArgs.firstOrNull()?.replace("\"", "")
            if (!pathStr.isNullOrBlank()) {
                try {
                    val path = Paths.get(pathStr)
                    if (Files.exists(path)) {
                        val folderToOpen = if (pathStr.endsWith(".i51", ignoreCase = true)) {
                            path.parent
                        } else if (Files.isDirectory(path)) {
                            path
                        } else {
                            null
                        }

                        if (folderToOpen != null) {
                            openFolderAction(folderToOpen)
                            showStartupDialog = false
                        }
                    }
                } catch (e: kotlin.Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Checking and opening logic
    val checkAndOpenFolder = { path: Path ->
        scope.launch(Dispatchers.IO) {
            val absPath = path.toAbsolutePath()
            if (absPath.nameCount < 1) {
                withContext(Dispatchers.Main) {
                    showInvalidPathDialog = true
                }
                return@launch
            }

            val i51Files = try {
                Files.list(path).use { stream ->
                    stream.filter { it.fileName.toString().endsWith(".i51") }.findFirst()
                }
            } catch (e: Exception) {
                java.util.Optional.empty()
            }

            if (i51Files.isPresent) {
                openFolderAction(path)
            } else {
                try {
                    val exeDir = try {
                        val loc = (object {}::class.java).protectionDomain.codeSource.location.toURI()
                        val p = Paths.get(loc).toAbsolutePath()
                        if (Files.isDirectory(p)) p.toString() else p.parent.toString()
                    } catch (_: Exception) {
                        System.getProperty("user.dir") ?: ""
                    }
                    val toolsPath = Paths.get(exeDir, "Tools").toAbsolutePath().toString()
                    val creatorExe = Paths.get(toolsPath, "creator.exe").toString()

                    val process = ProcessBuilder(creatorExe,"create",absPath.toString())
                        .directory(File(exeDir))
                        .start()
                    process.waitFor()

                    openFolderAction(path)

                    withContext(Dispatchers.Main) {
                        projectDownloadPortState.value = loadDownloadPortFromProject(path) ?: ""
                    }
                } catch (e: kotlin.Exception) {
                    e.printStackTrace()
                    openFolderAction(path)
                }
            }
        }
    }

    // === DIALOGS ===

    if (showInvalidPathDialog) {
        InvalidPathDialog(
            onDismiss = { showInvalidPathDialog = false },
            dialogBg = dialogBg,
            dialogTextColor = dialogTextColor
        )
    }

    if (showFolderNotFoundDialog) {
        // Simple error alert
        AlertDialog(
            onDismissRequest = { false },
            containerColor = dialogBg,
            title = { Text("无法打开文件夹", color = dialogTextColor) },
            text = { Text("上次打开的文件夹不存在或无法访问。", color = dialogTextColor) },
            confirmButton = {
                Button(onClick = { false }) { Text("确定") }
            }
        )
    }

    if (showWelcomeDialog) {
        welcomeDialog(
            dialogBg = dialogBg,
            dialogTextColor = dialogTextColor,
            primaryColor = primaryColorState.value,
            darkMode = darkModeState.value,
            lastFolder = loadLastFolderFromConfig(),
            codeFontSize = codeFontSizeState.value,
            onCloseWelcome = { showWelcomeDialog = false }
        )
    }

    if (showStartupDialog && rootPath == null && !showWelcomeDialog) {
        val lastFolder = remember { loadLastFolderFromConfig() }
        startupDialog(
            lastFolder = lastFolder,
            onOpenFolder = {
                val chooser = JFileChooser()
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val selected = chooser.selectedFile.toPath()
                    showStartupDialog = false
                    checkAndOpenFolder(selected)
                    // load per-project download port if present
                    projectDownloadPortState.value = loadDownloadPortFromProject(selected) ?: ""
                }
            },
            onOpenLastFolder = {
                if (!lastFolder.isNullOrBlank()) {
                    try {
                        val path = Paths.get(lastFolder)
                        if (Files.exists(path)) {
                            showStartupDialog = false
                            checkAndOpenFolder(path)
                        } else {
                            // Folder missed
                            showFolderNotFoundDialog = true
                        }
                    } catch (e: kotlin.Exception) {
                         e.printStackTrace()
                         showFolderNotFoundDialog = true
                    }
                }
            },
            dialogBg = dialogBg,
            dialogTextColor = dialogTextColor
        )
    }

    Scaffold(
        modifier = Modifier.padding(UiSizes.WINDOW_MARGIN),
        topBar = {
            MainTopBar(
                onOpenFolder = {
                    // open native folder chooser and scan selected folder (non-recursive initial load)
                    val chooser = JFileChooser()
                    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val selected: File = chooser.selectedFile
                        checkAndOpenFolder(selected.toPath())
                    }
                },
                onSaveActiveFile = {
                    val activeId = fileManager.activeFileId.value
                    if (activeId != null) {
                        scope.launch { fileManager.saveOpenFile(activeId) }
                    }
                },
                activeFileId = fileManager.activeFileId.value,
                isProjectOpen = rootPath != null,
                onSearchClick = { showSearchDialog = true },
                onRunClick = {
                    if (rootPath == null) return@MainTopBar
                    // mark building state and run in coroutine; always clear isBuilding in finally
                    isBuilding = true
                    showBuildProgress = true
                    buildStatusText = "构建中..."
                    buildCanTerminate = false
                    terminator = null

                    scope.launch {
                        try {
                            runFailureMessage = null // Reset before run
                            val result = runInteractiveBuild(
                                command = "",
                                onPhaseChange = { status, canTerm ->
                                    buildStatusText = status
                                    buildCanTerminate = canTerm
                                },
                                registerTerminator = { quit -> terminator = quit },
                                projectPathInput = rootPath.toString()
                            )
                            runOutput = tryPrettyJson(result.json)
                            runExitCode = result.exitCode
                            // Parse structured problems from the returned JSON and prepare the per-file index
                            try {
                                val parsed = parseProblemsFromJson(result.json)
                                problems = parsed
                                problemsByFile = buildProblemsMap(parsed)
                                // Determine concise run status (three states)
                                val hasError = parsed.any { it.severity.equals("Error", true) }
                                val hasWarning = parsed.any { it.severity.equals("Warning", true) }

                                if (result.failureMessage != null) {
                                    runStatus = "下载失败"
                                    runFailureMessage = result.failureMessage
                                } else {
                                    runStatus = when {
                                        result.exitCode != 0 -> "错误"
                                        !hasError && !hasWarning -> "完美成功"
                                        !hasError && hasWarning -> "成功（有警告）"
                                        else -> "错误"
                                    }
                                }
                            } catch (pe: kotlin.Exception) {
                                pe.printStackTrace()
                                if (result.failureMessage != null) {
                                    runStatus = "下载失败"
                                    runFailureMessage = result.failureMessage
                                } else {
                                    runStatus = "错误"
                                }
                            }
                            // Only show run output if not terminated manually (though here we just show it)
                            showRunOutputDialog = true
                        } catch (e: kotlin.Exception) {
                            e.printStackTrace()
                            runOutput = "Error: ${e.message}"
                            runStatus = "错误"
                            showRunOutputDialog = true
                        } finally {
                            isBuilding = false
                            showBuildProgress = false
                        }
                    }
                },
                isBuilding = isBuilding,
                problemsCount = problems.size,
                onShowProblems = { showProblemsDialog = true },
                onShowSettings = { showSettingsDialog = true }
            )
        },
    ) { inner ->
        // Use the scaffold inner padding (accounts for topBar height) and add a small gap under the top bar
        Row(modifier = Modifier.fillMaxSize()
            .padding(inner)
            .padding(top = UiSizes.WINDOW_MARGIN), verticalAlignment = Alignment.Top) {

            // Sidebar
            ProjectSideBar(
                rootPath = rootPath,
                fileManager = fileManager,
                childrenCache = childrenCache,
                expandedDirs = expandedDirs,
                activePath = fileManager.openFiles.firstOrNull { it.id == fileManager.activeFileId.value }?.path,
                onToggleExpand = { dir ->
                    if (expandedDirs.contains(dir)) {
                        expandedDirs.remove(dir)
                    } else {
                        // expand: load children if not cached
                        scope.launch {
                            val list = fileManager.listDirectory(dir)
                            childrenCache[dir] = list
                            expandedDirs.add(dir)
                        }
                    }
                },
                onOpen = { path: Path -> scope.launch { fileManager.openOrFocus(path) } },
                onDelete = { path -> fileToDelete = path; showDeleteConfirmDialog = true },
                onRequestCreateC = {
                    if (rootPath == null) showErrorDialog = true else {
                        creatingType = "C"; newFileName = ""; showNameDialog = true
                    }
                },
                onRequestCreateH = {
                    if (rootPath == null) showErrorDialog = true else {
                        creatingType = "H"; newFileName = ""; showNameDialog = true
                    }
                }
            )

            // remove visual divider line; preserve spacing
            Spacer(modifier = Modifier.width(UiSizes.CONTENT_GAP))

            // Editor area in a surfaced card — increase elevation and gap between tab-bar and editor content;
            // use surfaceVariant as base and add a very subtle primary tint overlay so the editor isn't pure black/white
            Surface(
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
                    .padding(end = UiSizes.EDITOR_RIGHT_PADDING, bottom = UiSizes.BOTTOM_MARGIN)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // faint tint layer derived from primary color to create a mixed-theme look
                    Box(modifier = Modifier.matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)))

                    EditorHost(
                        openFiles = fileManager.openFiles,
                        activeFileId = fileManager.activeFileId.value,
                        onActivate = { id -> fileManager.activeFileId.value = id },
                        onClose = { id -> fileManager.close(id) },
                        onChange = { id, new -> fileManager.updateContent(id, new) },
                        onSave = { id -> scope.launch { fileManager.saveOpenFile(id) } },
                        pendingJump = fileManager.pendingJump.value,
                        onJumpConsumed = { fileManager.pendingJump.value = null },
                        modifier = Modifier.fillMaxSize(),
                        codeFontSizeState = codeFontSizeState,
                        problemsByFile = problemsByFile
                    )
                }
            }
        }


        // Problems dialog (inside App composable) — adaptive/responsive layout
        if (showProblemsDialog) {
            ProjectProblemsDialog(
                onDismiss = { showProblemsDialog = false },
                problems = problems,
                rootPath = rootPath,
                fileManager = fileManager,
                dialogBg = dialogBg,
                dialogTextColor = dialogTextColor
            )
        }

        // Name input dialog (themed)
        if (showNameDialog) {
            NameInputDialog(
                onDismiss = { showNameDialog = false },
                onConfirm = {
                    // perform create
                    scope.launch {
                        val r = rootPath ?: return@launch
                        try {
                            val sub = if (creatingType == "C") "src" else "inc"
                            val dir = r.resolve(sub)
                            Files.createDirectories(dir)
                            var candidate =
                                dir.resolve("${newFileName.ifBlank { "new" }}.${if (creatingType == "C") "c" else "h"}")
                            var i = 1
                            while (Files.exists(candidate)) {
                                candidate =
                                    dir.resolve(newFileName.ifBlank { "new" } +
                                            "${i}.${if (creatingType == "C") "c" else "h"}")
                                i++
                            }
                            val content = if (creatingType == "C") "// new C file\n" else "// new H file\n"
                            Files.writeString(candidate, content, StandardCharsets.UTF_8)

                            // Refresh current parent dir (usually 'src' or 'inc')
                            val listSub = fileManager.listDirectory(dir)
                            childrenCache[dir] = listSub

                            // Also refresh root if necessary
                            val listRoot = fileManager.listDirectory(r)
                            childrenCache[r] = listRoot

                            fileManager.openOrFocus(candidate)
                        } catch (_: Exception) {
                        }
                    }
                    showNameDialog = false
                },
                dialogBg = dialogBg,
                dialogTextColor = dialogTextColor,
                creatingType = creatingType,
                fileName = newFileName,
                onFileNameChange = { newFileName = it }
            )
        }

        // Error dialog when no folder is open
        if (showErrorDialog) {
            GenericErrorDialog(
                onDismiss = { showErrorDialog = false },
                title = "未打开文件夹",
                content = "请在创建文件前先打开一个文件夹。"
            )
        }

        // Delete confirmation dialog
        if (showDeleteConfirmDialog && fileToDelete != null) {
            DeleteConfirmDialog(
                onDismiss = {
                    showDeleteConfirmDialog = false
                    fileToDelete = null
                },
                onConfirm = {
                    val path = fileToDelete ?: return@DeleteConfirmDialog
                    scope.launch(Dispatchers.IO) {
                        try {
                            Files.deleteIfExists(path)
                            withContext(Dispatchers.Main) {
                                // Close the file if it's open in the editor
                                fileManager.openFiles.find { it.path == path }?.id?.let { id ->
                                    fileManager.close(id)
                                }
                                // Refresh the file list
                                val r = rootPath
                                if (r != null) {
                                    val list = fileManager.listDirectory(r)
                                    childrenCache[r] = list
                                    // Also refresh subdirectories if the file was in one
                                    path.parent?.let { parent ->
                                        if (parent != r) {
                                            val subList = fileManager.listDirectory(parent)
                                            childrenCache[parent] = subList
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            withContext(Dispatchers.Main) {
                                showDeleteConfirmDialog = false
                                fileToDelete = null
                            }
                        }
                    }
                },
                fileName = fileToDelete?.fileName.toString()
            )
        }

        // Settings dialog using the component from Settings.kt
        SettingsDialog(
            visible = showSettingsDialog,
            onDismiss = { showSettingsDialog = false },
            primaryColorState = primaryColorState,
            darkModeState = darkModeState,
            codeFontSizeState = codeFontSizeState,
            projectRoot = rootPath,
            downloadPortState = projectDownloadPortState
        )

        // Build Progress Dialog
        if (showBuildProgress) {
            BuildProgressDialog(
                statusText = buildStatusText,
                dialogBg = dialogBg,
                dialogTextColor = dialogTextColor,
                canTerminate = buildCanTerminate,
                onTerminate = { terminator?.invoke() }
            )
        }

        // Show enhanced run output dialog (themed)
        if (showRunOutputDialog) {
            RunOutputDialog(
                onDismiss = { showRunOutputDialog = false },
                dialogBg = dialogBg,
                dialogTextColor = dialogTextColor,
                runStatus = runStatus,
                runExitCode = runExitCode,
                runFailureMessage = runFailureMessage,
                problems = problems,
                onShowProblems = { showProblemsDialog = true }
            )
        }

        // Search dialog
        if (showSearchDialog) {
            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotEmpty() && rootPath != null) {
                    // Start search immediately (no delay)
                    isSearching = true
                    searchResults = fileManager.search(rootPath!!, searchQuery)
                    isSearching = false
                } else {
                    searchResults = emptyList()
                }
            }

            SearchDialog(
                onDismiss = { showSearchDialog = false },
                dialogBg = dialogBg,
                dialogTextColor = dialogTextColor,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                searchResults = searchResults,
                rootPath = rootPath,
                onResultClick = { match ->
                    scope.launch {
                         showSearchDialog = false
                         fileManager.openOrFocus(match.path, match.offset, match.lineNumber)
                    }
                }
            )
        }
    }
}

// Pre-start function (runs before the application starts)
fun preStart() {
    // 留空，程序打开之前执行
}

fun main(args: Array<String>) {
    preStart()
    application {
        // Set a larger default window size so the app opens wider
        Window(
            onCloseRequest = ::exitApplication,
            title = "intelli51 编辑器",
            state = rememberWindowState(width = 1200.dp, height = 800.dp)
        ) {
            // load stored primary color and dark mode if exist, otherwise use defaults
            val initial = loadPrimaryFromConfig() ?: Color(0xFF0B63D6)
            val initialDark = loadDarkFromConfig() ?: false
            // load code font size (sp) from config
            val initialCodeFont = loadCodeFontSizeFromConfig() ?: 13f
            // remember primary color so we can change it at runtime from settings dialog
            val primaryColor = remember { mutableStateOf(initial) }
            // dark mode state (toggle in settings)
            val darkMode = remember { mutableStateOf(initialDark) }
            // code font size state
            val codeFontSizeState = remember { mutableStateOf(initialCodeFont) }

            MaterialTheme(
                colorScheme = if (darkMode.value) createDarkColorScheme(primaryColor.value) else createLightColorScheme(
                    primaryColor.value
                ),
                shapes = Shapes(
                    small = RoundedCornerShape(8.dp),
                    medium = RoundedCornerShape(12.dp),
                    large = RoundedCornerShape(16.dp)
                )
            ) {
                // Ensure the whole window background matches the theme background (prevents white border on dark mode)
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App(
                        primaryColorState = primaryColor,
                        darkModeState = darkMode,
                        codeFontSizeState = codeFontSizeState,
                        launchArgs = args
                    )
                }
            }
        }
    }
}
