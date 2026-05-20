package com.franklinharper.dicewarsport.ai.neural

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NeuralModelMetadataTest {
    @Test
    fun parsesValidMetadataJson() {
        val metadata = NeuralModelMetadata.parse(validMetadataJson())

        assertEquals("neuralbot-20260520-1430-a1b2c3d", metadata.modelId)
        assertEquals("a1b2c3d", metadata.gitCommit)
        assertEquals("cfg123", metadata.trainingConfigHash)
        assertEquals(123456789, metadata.trainingSeed)
        assertEquals("encoder-v1-action-v1", metadata.environmentVersion)
        assertEquals("maps456", metadata.mapDatasetHash)
        assertEquals(250000, metadata.selfPlayGameCount)
        assertEquals("adamw lr=0.001", metadata.optimizerSettings)
        assertEquals("gnn hidden=64 layers=4", metadata.networkArchitecture)
        assertEquals("checkpoints/model.pt", metadata.checkpointPath)
        assertEquals("2026-05-20T14:30:00Z", metadata.exportTimestamp)
        assertEquals(18, metadata.onnxOpset)
        assertEquals("none", metadata.quantization)
        assertEquals("10k score=12345 wins=678", metadata.evaluationTournamentResults)
    }

    @Test
    fun parsesNullOptionalMapDatasetHash() {
        val metadata = NeuralModelMetadata.parse(validMetadataJson().replace("\"maps456\"", "null"))

        assertNull(metadata.mapDatasetHash)
    }

    @Test
    fun rejectsMissingRequiredField() {
        val json = validMetadataJson().replace("\"git_commit\": \"a1b2c3d\",", "")

        assertFailsWith<IllegalArgumentException> { NeuralModelMetadata.parse(json) }
    }

    @Test
    fun rejectsBlankRequiredString() {
        val json = validMetadataJson().replace("\"model_id\": \"neuralbot-20260520-1430-a1b2c3d\"", "\"model_id\": \"\"")

        assertFailsWith<IllegalArgumentException> { NeuralModelMetadata.parse(json) }
    }

    @Test
    fun rejectsNegativeTrainingSeed() {
        val json = validMetadataJson().replace("\"training_seed\": 123456789", "\"training_seed\": -1")

        assertFailsWith<IllegalArgumentException> { NeuralModelMetadata.parse(json) }
    }

    @Test
    fun rejectsNonPositiveOnnxOpset() {
        val json = validMetadataJson().replace("\"onnx_opset\": 18", "\"onnx_opset\": 0")

        assertFailsWith<IllegalArgumentException> { NeuralModelMetadata.parse(json) }
    }

    private fun validMetadataJson(): String = """
        {
          "model_id": "neuralbot-20260520-1430-a1b2c3d",
          "git_commit": "a1b2c3d",
          "training_config_hash": "cfg123",
          "training_seed": 123456789,
          "environment_version": "encoder-v1-action-v1",
          "map_dataset_hash": "maps456",
          "self_play_game_count": 250000,
          "optimizer_settings": "adamw lr=0.001",
          "network_architecture": "gnn hidden=64 layers=4",
          "checkpoint_path": "checkpoints/model.pt",
          "export_timestamp": "2026-05-20T14:30:00Z",
          "onnx_opset": 18,
          "quantization": "none",
          "evaluation_tournament_results": "10k score=12345 wins=678"
        }
    """.trimIndent()
}
