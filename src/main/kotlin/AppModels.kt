@file:Suppress("InvalidPackageDeclaration")

package com.intelli51.intelli51

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Top-level Problem model
data class Problem(val file: String?, val line: Int?, val message: String, val severity: String)

@Serializable
data class CompileWarning(
    @SerialName("CompileLine") val compileLine: String? = null,
    @SerialName("CompileWarningInfo") val compileWarningInfo: String? = null,
    @SerialName("CompileWarningType") val compileWarningType: String? = null
)

@Serializable
data class CompileError(
    @SerialName("CompileLine") val compileLine: String? = null,
    @SerialName("CompileErrorInfo") val compileErrorInfo: String? = null,
    @SerialName("CompileErrorType") val compileErrorType: String? = null
)

@Serializable
data class CompileTip(
    @SerialName("CompileWarningFile") val compileWarningFile: String? = null,
    @SerialName("CompileWarnings") val compileWarnings: List<CompileWarning>? = null,
    @SerialName("CompileErrorFile") val compileErrorFile: String? = null,
    @SerialName("CompileErrors") val compileErrors: List<CompileError>? = null,
    // Sometimes result might be success or warning/error
    @SerialName("Result") val result: String? = null
)

@Serializable
data class CompileInfo(
    @SerialName("CompileTips") val compileTips: List<CompileTip>? = null,
    @SerialName("Result") val result: Boolean? = null
)

@Serializable
data class LinkTip(
    @SerialName("object") val objectName: String? = null,
    val symbol: String? = null
)

@Serializable
data class LinkInfo(
    @SerialName("LinkTips") val linkTips: List<LinkTip>? = null,
    @SerialName("Result") val result: Boolean? = null
)

@Serializable
data class BuilderOutput(
    @SerialName("CompileInfo") val compileInfo: CompileInfo? = null,
    @SerialName("LinkInfo") val linkInfo: LinkInfo? = null,
    @SerialName("FinalResult") val finalResult: Boolean? = null,
    // Init failed cases
    @SerialName("Result") val result: String? = null,
    @SerialName("InitFailedType") val initFailedType: String? = null
)
