package com.franklinharper.dicewarsport.trainingcli

import com.franklinharper.dicewarsport.tournament.BotScore

object NeuralCandidateEvaluator {
    const val NEURAL_BOT_ID: String = "neural"

    /**
     * Applies the locked replacement rule to already-ranked tournament scores.
     *
     * The input list must use the tournament's final ranking order. This avoids
     * duplicating tie-break behavior here and keeps this class focused on the
     * neural-candidate acceptance rule.
     */
    fun evaluate(scores: List<BotScore>): NeuralCandidateDecision {
        val neuralIndex = scores.indexOfFirst { it.participantId == NEURAL_BOT_ID }
        if (neuralIndex < 0) {
            return NeuralCandidateDecision(
                accepted = false,
                eliminatedBotId = null,
                reason = "neural bot is missing from scores",
            )
        }
        if (neuralIndex == scores.lastIndex) {
            return NeuralCandidateDecision(
                accepted = false,
                eliminatedBotId = null,
                reason = "neural finished last",
            )
        }

        val lowestNonNeural = scores.asReversed().firstOrNull { it.participantId != NEURAL_BOT_ID }
            ?: return NeuralCandidateDecision(
                accepted = false,
                eliminatedBotId = null,
                reason = "no non-neural bot is available to eliminate",
            )

        return NeuralCandidateDecision(
            accepted = true,
            eliminatedBotId = lowestNonNeural.participantId,
            reason = "neural did not finish last; eliminate lowest non-neural bot ${lowestNonNeural.participantId}",
        )
    }
}

data class NeuralCandidateDecision(
    val accepted: Boolean,
    val eliminatedBotId: String?,
    val reason: String,
)
