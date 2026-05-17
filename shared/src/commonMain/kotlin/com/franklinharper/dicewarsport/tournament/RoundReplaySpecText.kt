package com.franklinharper.dicewarsport.tournament

data class RoundReplaySpecText(
    val roundSeed: Int,
    val seatIds: List<String>,
    val maxActions: Int,
    val lastSteps: Int = 50,
)

object RoundReplaySpecParser {
    private const val START = "ROUND_REPLAY_SPEC"
    private const val END = "END_ROUND_REPLAY_SPEC"

    fun format(spec: RoundReplaySpecText): String = listOf(
        START,
        "roundSeed=${spec.roundSeed}",
        "seats=${spec.seatIds.joinToString(",")}",
        "maxActions=${spec.maxActions}",
        "lastSteps=${spec.lastSteps}",
        END,
    ).joinToString("\n")

    fun parse(text: String): RoundReplaySpecText {
        val lines = text.lines().map { it.trim() }
        val startIndex = lines.indexOf(START)
        require(startIndex >= 0) { "Missing $START" }
        val endIndex = lines.indexOfFirstIndexed { index, line -> index > startIndex && line == END }
        require(endIndex > startIndex) { "Missing $END" }

        val values = lines.subList(startIndex + 1, endIndex)
            .filter { it.isNotBlank() }
            .associate { line ->
                require('=' in line) { "Invalid replay spec line '$line'" }
                line.substringBefore('=').trim() to line.substringAfter('=').trim()
            }

        val roundSeed = values["roundSeed"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing or invalid roundSeed")
        val seats = values["seats"]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Missing seats")
        require(seats.size >= 2) { "Replay spec requires at least two seats" }
        val maxActions = values["maxActions"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing or invalid maxActions")
        require(maxActions > 0) { "maxActions must be greater than zero" }
        val lastSteps = values["lastSteps"]?.toIntOrNull() ?: 50
        require(lastSteps > 0) { "lastSteps must be greater than zero" }

        return RoundReplaySpecText(
            roundSeed = roundSeed,
            seatIds = seats,
            maxActions = maxActions,
            lastSteps = lastSteps,
        )
    }
}

private inline fun <T> List<T>.indexOfFirstIndexed(predicate: (Int, T) -> Boolean): Int {
    for (index in indices) {
        if (predicate(index, this[index])) return index
    }
    return -1
}
