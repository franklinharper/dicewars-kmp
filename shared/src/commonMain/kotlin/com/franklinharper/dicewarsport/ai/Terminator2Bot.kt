package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame

/** Portfolio/meta-strategy using phase classification and opponent pressure. */
class Terminator2Bot : AiStrategy {
    override val name: String = "Terminator 2"

    private val terminator = TerminatorBot()
    private val optimus = OptimusBot()
    private val frontier = FrontierCommanderBot()
    private val max = MaxBot()

    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayerId()
        val me = game.players[player]
        if (me.areaCount <= 0) return null
        val enemies = (0 until game.maxPlayers).filter { it != player && game.players[it].areaCount > 0 }
        val activeEnemies = enemies.size
        val leader = enemies.maxByOrNull { leaderValue(game, it) }
        val leaderData = leader?.let { game.players[it] }

        return when {
            activeEnemies <= 1 -> optimus.chooseMove(game) ?: max.chooseMove(game) ?: terminator.chooseMove(game)
            leaderData != null && leaderData.areaCount >= me.areaCount + 3 -> frontier.chooseMove(game) ?: terminator.chooseMove(game)
            leaderData == null || leaderValue(game, player) >= leaderValue(game, leader) -> terminator.chooseMove(game)
            else -> terminator.chooseMove(game)
        }
    }

    private fun leaderValue(game: DicewarsGame, player: Int): Int {
        val p = game.players[player]
        return p.areaCount * 100 + p.maxConnectedAreaCount * 130 + p.diceCount * 10 + p.stock * 2
    }
}
