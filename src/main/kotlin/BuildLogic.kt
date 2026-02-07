package com.intelli51.intelli51

// Removed: parseProblemsFromJson, buildProblemsMap
// They are now in Analyzer.kt

// Helper: simple JSON pretty-printer (no external deps)
fun tryPrettyJson(raw: String): String {
    val s = raw.trim()
    if (s.isEmpty()) return "(empty)"
    if (!(s.startsWith("{") || s.startsWith("["))) return raw

    val sb = StringBuilder()
    var indent = 0
    var inString = false
    var escape = false
    for (ch in s) {
        if (escape) {
            sb.append(ch)
            escape = false
            continue
        }
        when (ch) {
            '\\' -> {
                sb.append(ch)
                if (inString) escape = true
            }

            '"' -> {
                sb.append(ch)
                inString = !inString
            }

            '{', '[' -> {
                sb.append(ch)
                if (!inString) {
                    sb.append('\n')
                    indent++
                    repeat(indent) { sb.append(' ', 4) }
                }
            }

            '}', ']' -> {
                if (!inString) {
                    sb.append('\n')
                    indent--
                    repeat(indent) { sb.append(' ', 4) }
                    sb.append(ch)
                } else sb.append(ch)
            }

            ',' -> {
                sb.append(ch)
                if (!inString) {
                    sb.append('\n')
                    repeat(indent) { sb.append(' ', 4) }
                }
            }

            ':' -> {
                if (!inString) {
                    sb.append(": ")
                } else sb.append(ch)
            }

            else -> {
                sb.append(ch)
            }
        }
    }
    return sb.toString()
}
