package com.example.mantochess.service

import com.example.mantochess.model.Color
import com.example.mantochess.model.CompleteMovementInfo
import com.example.mantochess.model.Game
import com.example.mantochess.model.PieceType
import org.springframework.stereotype.Service
import java.security.InvalidParameterException

@Service
class GameService(
    private val notationService: NotationService,
    private val cacheService: CacheService) {

    private val maxDepth = 5

    fun processGameMovement(gameUuid: String, notation: String): Game {
        val game = getGameFromUuid(gameUuid)
        val movement = notationService.convertNotationToMovement(notation, game)

        return playMovement(game, gameUuid, movement)
    }

    fun suggestGameMovement(gameUuid: String, shouldPlayMovement: Boolean = false): String {
        val game = getGameFromUuid(gameUuid)

        val suggestedMovement = suggestGameMovementAtDepth(game, 0, Int.MIN_VALUE, Int.MAX_VALUE)
        println("Game advantage: ${suggestedMovement.first}")
        println("Suggested move: ${suggestedMovement.second}")
        return if (suggestedMovement.second == null) {
            "Checkmate. ${if (game.currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE} wins"
        } else {
            if (shouldPlayMovement) {
                playMovement(game, gameUuid, suggestedMovement.second!!)
            }
            notationService.convertMovementToNotation(suggestedMovement.second!!)
        }
    }

    private fun suggestGameMovementAtDepth(game: Game, currentDepth: Int, alphaParam: Int, betaParam: Int): Pair<Int, CompleteMovementInfo?> {
        var alpha = alphaParam
        var beta = betaParam

        if (currentDepth == maxDepth) {
            return Pair(game.getCurrentAdvantage(), null)
        }

        val unsortedMovements = game.availableMovementFor(game.currentTurnColor)

        // Sorting movement first by pieces that would be captured enhances alpha-beta pruning algorithm,
        // as those movements are more likely to be the best ones, so pruning occurs more often
        val movements = unsortedMovements.sortedBy { m -> game.board.pieceAt(m.to.rank, m.to.file).isEmpty }

        if (movements.isEmpty()) {
            // If no movements are found, this means it's checkmate.
            return Pair(if (game.currentTurnColor == Color.WHITE) -PieceType.KING.value else PieceType.KING.value, null)
        }

        if (game.currentTurnColor == Color.WHITE) {
            var value = Pair<Int, CompleteMovementInfo?>(Int.MIN_VALUE, null)

            for (movement in movements) {
                val cloneGame = Game(game)
                cloneGame.makeMovement(movement)

                val suggestedMoveToOpponent = suggestGameMovementAtDepth(cloneGame, currentDepth + 1, alpha, beta)

                value = if (value.first > suggestedMoveToOpponent.first) value else Pair(suggestedMoveToOpponent.first, movement)
                alpha = Math.max(alpha, value.first)

                if (alpha >= beta) {
                    break
                }
            }
            return value
        } else {
            var value = Pair<Int, CompleteMovementInfo?>(Int.MAX_VALUE, null)

            for (movement in movements) {
                val cloneGame = Game(game)
                cloneGame.makeMovement(movement)

                val suggestedMoveToOpponent = suggestGameMovementAtDepth(cloneGame, currentDepth + 1, alpha, beta)

                value = if (value.first < suggestedMoveToOpponent.first) value else Pair(suggestedMoveToOpponent.first, movement)
                beta = Math.min(beta, value.first)

                if (beta <= alpha) {
                    break
                }
            }
            return value
        }
    }

    private fun getGameFromUuid(gameUuid: String): Game {
        return cacheService.fetchGame(gameUuid) ?: throw InvalidParameterException("Invalid game uuid")
    }

    private fun playMovement(game: Game, gameUuid: String, movement: CompleteMovementInfo): Game {
        val gameInNextMove = Game(game)
        gameInNextMove.makeMovement(movement)

        // Print board after play for debugging
        gameInNextMove.board.printBoard()

        cacheService.storeGame(gameUuid, gameInNextMove)

        return gameInNextMove
    }
}
