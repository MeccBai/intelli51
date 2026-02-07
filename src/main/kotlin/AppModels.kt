package com.intelli51.intelli51

import kotlinx.serialization.Serializable

// Top-level Problem model
data class Problem(val file: String?, val line: Int?, val message: String, val severity: String)

@Serializable
data class CompileWarning(
    val CompileLine: String? = null,
    val CompileWarningInfo: String? = null,
    val CompileWarningType: String? = null
)

@Serializable
data class CompileError(
    val CompileLine: String? = null,
    val CompileErrorInfo: String? = null,
    val CompileErrorType: String? = null
)

@Serializable
data class CompileTip(
    val CompileWarningFile: String? = null,
    val CompileWarnings: List<CompileWarning>? = null,
    val CompileErrorFile: String? = null,
    val CompileErrors: List<CompileError>? = null,
    // Sometimes result might be success or warning/error
    val Result: String? = null
)

@Serializable
data class CompileInfo(
    val CompileTips: List<CompileTip>? = null,
    val Result: Boolean? = null
)

@Serializable
data class LinkTip(
    val `object`: String? = null,
    val symbol: String? = null
)

@Serializable
data class LinkInfo(
    val LinkTips: List<LinkTip>? = null,
    val Result: Boolean? = null
)

@Serializable
data class BuilderOutput(
    val CompileInfo: CompileInfo? = null,
    val LinkInfo: LinkInfo? = null,
    val FinalResult: Boolean? = null,
    // Init failed cases
    val Result: String? = null,
    val InitFailedType: String? = null
)
