package com.franklinharper.dicewarsport.trainingcli

import com.franklinharper.dicewarsport.ai.neural.NeuralModelMetadata
import java.io.File

class ModelArtifactValidator(
    private val warningSizeBytes: Long = 10L * 1024L * 1024L,
    private val hardMaxSizeBytes: Long = 25L * 1024L * 1024L,
) {
    fun validate(modelsDir: File): ModelArtifactValidationResult {
        require(modelsDir.isDirectory) { "Models directory does not exist: ${modelsDir.path}" }

        val currentFile = File(modelsDir, "current.txt")
        require(currentFile.isFile) { "Missing current.txt in ${modelsDir.path}" }
        val currentVersion = currentFile.readText().trim()
        require(currentVersion.isNotBlank()) { "current.txt must contain a model version directory name" }
        require('/' !in currentVersion && '\\' !in currentVersion) {
            "current.txt must contain only a model version directory name"
        }

        val versionDir = File(modelsDir, currentVersion)
        require(versionDir.isDirectory) { "Current model directory does not exist: ${versionDir.path}" }

        val metadataFile = File(versionDir, "model.metadata.json")
        require(metadataFile.isFile) { "Missing model.metadata.json in ${versionDir.path}" }
        val metadata = NeuralModelMetadata.parse(metadataFile.readText())
        require(metadata.modelId == currentVersion) {
            "model_id '${metadata.modelId}' must match current model directory '$currentVersion'"
        }

        val modelFile = File(versionDir, "model.onnx")
        require(modelFile.isFile) { "Missing model.onnx in ${versionDir.path}" }
        val modelBytes = modelFile.length()
        require(modelBytes <= hardMaxSizeBytes) {
            "model.onnx size $modelBytes exceeds hard max $hardMaxSizeBytes bytes"
        }

        val warning = if (modelBytes > warningSizeBytes) {
            "model.onnx size $modelBytes exceeds preferred size $warningSizeBytes bytes"
        } else {
            null
        }

        return ModelArtifactValidationResult(
            modelId = metadata.modelId,
            modelBytes = modelBytes,
            warning = warning,
        )
    }
}

data class ModelArtifactValidationResult(
    val modelId: String,
    val modelBytes: Long,
    val warning: String?,
)
