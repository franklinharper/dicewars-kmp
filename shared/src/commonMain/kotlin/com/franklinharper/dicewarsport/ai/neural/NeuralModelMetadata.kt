package com.franklinharper.dicewarsport.ai.neural

/**
 * Metadata for a bundled neural bot model.
 *
 * The v1 parser intentionally supports the simple JSON object emitted by the
 * training/export pipeline. It validates required fields and fails loudly on
 * malformed or incomplete metadata without introducing a JSON dependency yet.
 */
data class NeuralModelMetadata(
    val modelId: String,
    val gitCommit: String,
    val trainingConfigHash: String,
    val trainingSeed: Int,
    val environmentVersion: String,
    val mapDatasetHash: String?,
    val selfPlayGameCount: Int,
    val optimizerSettings: String,
    val networkArchitecture: String,
    val checkpointPath: String,
    val exportTimestamp: String,
    val onnxOpset: Int,
    val quantization: String,
    val evaluationTournamentResults: String,
) {
    companion object {
        fun parse(json: String): NeuralModelMetadata {
            val metadata = NeuralModelMetadata(
                modelId = requiredString(json, "model_id"),
                gitCommit = requiredString(json, "git_commit"),
                trainingConfigHash = requiredString(json, "training_config_hash"),
                trainingSeed = requiredInt(json, "training_seed"),
                environmentVersion = requiredString(json, "environment_version"),
                mapDatasetHash = optionalString(json, "map_dataset_hash"),
                selfPlayGameCount = requiredInt(json, "self_play_game_count"),
                optimizerSettings = requiredString(json, "optimizer_settings"),
                networkArchitecture = requiredString(json, "network_architecture"),
                checkpointPath = requiredString(json, "checkpoint_path"),
                exportTimestamp = requiredString(json, "export_timestamp"),
                onnxOpset = requiredInt(json, "onnx_opset"),
                quantization = requiredString(json, "quantization"),
                evaluationTournamentResults = requiredString(json, "evaluation_tournament_results"),
            )
            metadata.validate()
            return metadata
        }

        private fun requiredString(json: String, key: String): String =
            optionalString(json, key) ?: throw IllegalArgumentException("Missing required metadata field '$key'")

        private fun optionalString(json: String, key: String): String? {
            val stringMatch = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]*)\"").find(json)
            if (stringMatch != null) return stringMatch.groupValues[1]
            val nullMatch = Regex("\"${Regex.escape(key)}\"\\s*:\\s*null\\b").find(json)
            if (nullMatch != null) return null
            return null
        }

        private fun requiredInt(json: String, key: String): Int {
            val match = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(-?\\d+)").find(json)
                ?: throw IllegalArgumentException("Missing required metadata field '$key'")
            return match.groupValues[1].toIntOrNull()
                ?: throw IllegalArgumentException("Metadata field '$key' must be an integer")
        }
    }

    private fun validate() {
        require(modelId.isNotBlank()) { "model_id must not be blank" }
        require(gitCommit.isNotBlank()) { "git_commit must not be blank" }
        require(trainingConfigHash.isNotBlank()) { "training_config_hash must not be blank" }
        require(trainingSeed >= 0) { "training_seed must be non-negative" }
        require(environmentVersion.isNotBlank()) { "environment_version must not be blank" }
        require(selfPlayGameCount >= 0) { "self_play_game_count must be non-negative" }
        require(optimizerSettings.isNotBlank()) { "optimizer_settings must not be blank" }
        require(networkArchitecture.isNotBlank()) { "network_architecture must not be blank" }
        require(checkpointPath.isNotBlank()) { "checkpoint_path must not be blank" }
        require(exportTimestamp.isNotBlank()) { "export_timestamp must not be blank" }
        require(onnxOpset > 0) { "onnx_opset must be positive" }
        require(quantization.isNotBlank()) { "quantization must not be blank" }
        require(evaluationTournamentResults.isNotBlank()) { "evaluation_tournament_results must not be blank" }
    }
}
