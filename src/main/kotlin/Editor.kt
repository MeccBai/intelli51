package com.intelli51.intelli51

import com.intelli51.intelli51.UiSizes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.Alignment
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity

private val cKeywords = listOf(
    "break", "case", "char", "const", "continue", "default", "do",
    "double", "else", "enum", "extern", "float", "for", "goto", "if",
    "int", "long", "return", "short", "sizeof", "static",
    "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
    "sbit", "sfr", "interrupt", "using", "code", "data", "idata", "pdata", "xdata", "bit"
)

private val functionPattern = "\\b([a-zA-Z_]\\w*)\\b(?=\\s*\\()".toRegex()
private val keywordPattern = "\\b(${cKeywords.joinToString("|")})\\b".toRegex()
private val numberPattern = "\\b\\d+\\b".toRegex()
private val stringPattern = "\".*?\"".toRegex()
private val commentPattern = "//.*".toRegex()
// Preprocessor pattern (match common directives like '#define', '#if', '#ifdef', '#endif', etc.)
private val preprocessorPattern = "#\\s*(?:if|ifdef|ifndef|define|endif|else|elif|include|pragma|undef|error)\\b".toRegex()

// Helper to mix colors for contrast adjustment (renamed to avoid conflict)
private fun blendColors(c1: Color, c2: Color, ratio: Float): Color {
    val r = c1.red * (1 - ratio) + c2.green * ratio
    val g = c1.green * (1 - ratio) + c2.blue * ratio
    val b = c1.blue * (1 - ratio) + c2.blue * ratio
    val a = c1.alpha * (1 - ratio) + c2.alpha * ratio
    return Color(r, g, b, a)
}

data class DynamicHighlightColors(
    val keyword: Color,
    val function: Color,
    val number: Color,
    val string: Color,
    val comment: Color,
    // color for preprocessor tokens like #define
    val preprocessor: Color
)

fun getDynamicHighlightColors(primary: Color, isDark: Boolean): DynamicHighlightColors {
    // Calculate a complementary color by inverting RGB
    val complementary = Color(1f - primary.red, 1f - primary.green, 1f - primary.blue)

    // Permute channels to get another distinct color
    val triadic = Color(primary.blue, primary.red, primary.green)

    // Adjust brightness for contrast based on theme mode
    fun adjust(c: Color): Color {
        val luminance = c.red * 0.2126f + c.green * 0.7152f + c.blue * 0.0722f
        return if (isDark) {
            // In dark mode, ensure colors are bright enough
            if (luminance < 0.4f) blendColors(c, Color.White, 0.5f) else c
        } else {
            // In light mode, ensure colors are dark enough
            if (luminance > 0.6f) blendColors(c, Color.Black, 0.5f) else c
        }
    }

    // Choose a distinctive color for preprocessor: use a purple-ish tint derived from primary
    val preprocBase = Color(primary.red * 0.7f + 0.3f, primary.green * 0.1f, primary.blue * 0.7f)

    return DynamicHighlightColors(
        keyword = adjust(complementary),
        function = adjust(primary),
        number = adjust(complementary),
        string = adjust(triadic),
        comment = if (isDark) Color(0xFF6272A4) else Color(0xFF4A5568),
        preprocessor = adjust(preprocBase)
    )
}

fun getHighlightedText(text: String, colors: DynamicHighlightColors): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        // 0. Preprocessor directives (#define) - priority match so they don't get shadowed by keywords
        preprocessorPattern.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = colors.preprocessor, fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // 1. Keywords
        keywordPattern.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // 2. Function names
        functionPattern.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = colors.function, fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // 3. Numbers
        numberPattern.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = colors.number), match.range.first, match.range.last + 1)
        }

        // 4. Strings
        stringPattern.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1)
        }

        // 5. Comments
        commentPattern.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1)
        }
    }
}

class SyntaxHighlightTransformation(private val colors: DynamicHighlightColors) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = getHighlightedText(text.text, colors),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset
                override fun transformedToOriginal(offset: Int): Int = offset
            }
        )
    }
}

@Composable
fun EditorHost(
    openFiles: List<OpenFile>,
    activeFileId: String?,
    onActivate: (String) -> Unit,
    onClose: (String) -> Unit,
    onChange: (String, String) -> Unit,
    onSave: (String) -> Unit,
    pendingJump: Triple<String, Int, Int>? = null,
    onJumpConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    // receive code font size state
    codeFontSizeState: MutableState<Float>,
    // map from filename to set of line numbers that have problems (1-based)
    problemsByFile: Map<String, Set<Int>> = emptyMap()
) {
    // Dynamic colors for syntax highlighting based on theme
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.red < 0.5f
    val highlightColors = remember(colorScheme.primary, isDark) {
        getDynamicHighlightColors(colorScheme.primary, isDark)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tabs
        val scrollState = rememberScrollState()

        // Merge tab bar visually with editor surface: tabs sit flush on the top edge of the editor surface.
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = UiSizes.TAB_HORIZONTAL_PADDING, vertical = UiSizes.TAB_VERTICAL_PADDING),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                openFiles.forEach { f ->
                    val isActive = (f.id == activeFileId)
                    // Customize active tab color: use primaryContainer in dark mode, but use Primary in light mode for better contrast
                    val tabContainer = if (isActive) {
                        if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
                    } else Color.Transparent

                    val contentColor = if (isActive) {
                        if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                    } else MaterialTheme.colorScheme.onSurface

                    AssistChip(
                        onClick = { onActivate(f.id) },
                        label = { Text((f.path.fileName?.toString() ?: f.id) + if (f.isDirty) " *" else "") },
                        trailingIcon = {
                            IconButton(onClick = { onClose(f.id) }) {
                                val iconTint = if (isActive) {
                                    if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                                Icon(Icons.Default.Close, contentDescription = "关闭", tint = iconTint)
                            }
                        },
                        modifier = Modifier.padding(end = 0.dp),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                        border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = tabContainer,
                            labelColor = contentColor
                        ),
                        leadingIcon = null
                    )
                }
            }
            // small divider to separate tab row from editor content
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        }

        // Editor area (tabs visually merged above)
        val active = openFiles.firstOrNull { it.id == activeFileId } ?: openFiles.firstOrNull()
        if (active == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未打开文件", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Use remember(active.id) so the state is reset when switching files
            var textState by remember(active.id) { mutableStateOf(TextFieldValue(active.content)) }

            // 添加焦点请求器
            val focusRequester = remember { FocusRequester() }

            // shared vertical scroll state for line numbers and text area
            val editorScrollState = rememberScrollState()

            // Handle pending jump: update cursor and scroll
            val density = LocalDensity.current
            LaunchedEffect(pendingJump, active.id) {
                if (pendingJump != null && pendingJump.first == active.id) {
                    val targetOffset = pendingJump.second
                    val targetLine = pendingJump.third // 1-based

                    // Clamp offset
                    val safeOffset = targetOffset.coerceIn(0, textState.text.length)
                    textState = textState.copy(selection = TextRange(safeOffset))

                    // Calculate scroll position (approximate)
                    // Line height is fontSize * 1.25
                    val fontSize = codeFontSizeState.value
                    val lineHeightSp = fontSize * 1.25f
                    // Convert SP to pixels using density
                    val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }

                    // Scroll to center the line if possible
                    // Target scroll Y = (lineIndex) * lineHeight
                    val lineIndex = (targetLine - 1).coerceAtLeast(0)
                    val scrollY = (lineIndex * lineHeightPx).toInt()

                    // Animate or snap
                    editorScrollState.animateScrollTo(scrollY)
                    focusRequester.requestFocus()
                    onJumpConsumed()
                }
            }

            // Determine file name to look up problems (use file name only)
            val fileName = active.path.fileName?.toString() ?: active.id
            val problemLinesForFile = problemsByFile[fileName] ?: emptySet()

            Column(modifier = Modifier.fillMaxSize().padding(UiSizes.EDITOR_INNER_PADDING)) {
                val codeTextStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = codeFontSizeState.value.sp,
                    lineHeight = (codeFontSizeState.value * 1.25).sp
                )

                // The outer container (`Main.kt` Surface) already provides the surface, border and shape.
                // Render the editor content directly so tabs (above) visually integrate with the same container.
                Row(modifier = Modifier.fillMaxSize()) {
                    val lines = textState.text.split('\n')

                    // Build annotated string for line numbers with per-line coloring for problems
                    val numbersAnnotated = buildAnnotatedString {
                        lines.forEachIndexed { idx, _ ->
                            val lineNo = idx + 1
                            val lineStr = lineNo.toString()
                            val color = if (problemLinesForFile.contains(lineNo)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            pushStyle(SpanStyle(color = color))
                            append(lineStr)
                            pop()
                            if (idx != lines.lastIndex) append('\n')
                        }
                    }

                    // Render line numbers using Text (non-focusable) so we can apply per-line styles
                    Text(
                        text = numbersAnnotated,
                        style = codeTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .width(UiSizes.LINE_NUMBER_WIDTH)
                            .fillMaxHeight()
                            .verticalScroll(editorScrollState)
                            .padding(vertical = UiSizes.LINE_NUMBER_VERTICAL_PADDING, horizontal = UiSizes.LINE_NUMBER_HORIZONTAL_PADDING)
                    )

                    // Text editing area
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(editorScrollState)
                        .padding(start = UiSizes.EDITOR_TEXT_PADDING, top = 0.dp, end = UiSizes.EDITOR_TEXT_PADDING, bottom = UiSizes.EDITOR_TEXT_PADDING)) {

                        BasicTextField(
                            value = textState,
                            onValueChange = { nv ->
                                // Accept the new TextFieldValue from the platform/IME but ensure the selection
                                // is clamped to valid bounds. This fixes the caret not moving when inserting
                                // a space at the end of a line (IME may produce a selection that needs clamping).
                                val start = nv.selection.start.coerceIn(0, nv.text.length)
                                val end = nv.selection.end.coerceIn(0, nv.text.length)
                                textState = nv.copy(selection = TextRange(start, end))
                                onChange(active.id, textState.text)
                            },
                            visualTransformation = SyntaxHighlightTransformation(highlightColors),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            textStyle = codeTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                                .focusable(true)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        if (keyEvent.isCtrlPressed && keyEvent.key == Key.S) {
                                            onChange(active.id, textState.text)
                                            onSave(active.id)
                                            return@onPreviewKeyEvent true
                                        }
                                        if (keyEvent.key == Key.Tab) {
                                            val sel = textState.selection
                                            val start = sel.start.coerceIn(0, textState.text.length)
                                            val end = sel.end.coerceIn(0, textState.text.length)
                                            val newText = textState.text.substring(0, start) + "    " + textState.text.substring(end)
                                            val newPos = start + 4
                                            textState = TextFieldValue(text = newText, selection = TextRange(newPos))
                                            onChange(active.id, newText)
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    false
                                }
                        )
                    }
                }
            }
        }
    }
}
