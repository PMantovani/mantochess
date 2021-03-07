package com.example.mantochess.service

import com.example.mantochess.model.Color
import com.example.mantochess.model.Movement
import com.example.mantochess.model.Game
import com.example.mantochess.model.PieceType
import org.springframework.stereotype.Service
import java.security.InvalidParameterException
import kotlin.system.measureTimeMillis

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

        val countMap = HashMap<Int, Int>()

        val suggestedMovement: Pair<Int, Movement?>
        val ellapsedTime = measureTimeMillis {
            suggestedMovement = suggestGameMovementAtDepth(game, 0, Int.MIN_VALUE, Int.MAX_VALUE, countMap)
        }
        println("Ellapsed time: ${ellapsedTime}ms")
        println("Depth movements $countMap")
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

    private fun suggestGameMovementAtDepth(game: Game, currentDepth: Int, alphaParam: Int, betaParam: Int, countMap: MutableMap<Int, Int>
        ): Pair<Int, Movement?> {
        var alpha = alphaParam
        var beta = betaParam

        if (currentDepth == maxDepth) {
            return Pair(game.getCurrentAdvantage(), null)
        }

        val unsortedMovements = game.availableMovementsFor(game.currentTurnColor)

        // Sorting movement first by pieces that would be captured enhances alpha-beta pruning algorithm,
        // as those movements are more likely to be the best ones, so pruning occurs more often
        val movements = unsortedMovements.sortedBy { m -> game.board.pieceAt(m.to.rank, m.to.file).isEmpty }

        if (movements.isEmpty()) {
            // If no movements are found, this means it's checkmate or stalemate, depending on whether the current turn
            // is at check.
            return if (game.isColorAtCheck(game.currentTurnColor)) {
                var checkmateValue = PieceType.KING.value
                checkmateValue += ((maxDepth - currentDepth) * 10) // Increases checkmate value for earlier checkmates
                checkmateValue *= if (game.currentTurnColor == Color.WHITE) -1 else 1
                Pair(checkmateValue, null)
            } else {
                Pair(0, null)
            }
        }

        if (game.currentTurnColor == Color.WHITE) {
            var value = Pair<Int, Movement?>(Int.MIN_VALUE, null)

            for (movement in movements) {
                val cloneGame = Game(game)
                cloneGame.makeMovement(movement)

                countMap[currentDepth] = (countMap[currentDepth]?: 0) + 1

                val suggestedMoveToOpponent = suggestGameMovementAtDepth(cloneGame, currentDepth + 1, alpha, beta, countMap)

                value = if (value.first > suggestedMoveToOpponent.first) value else Pair(suggestedMoveToOpponent.first, movement)
                alpha = Math.max(alpha, value.first)

                if (alpha >= beta) {
                    break
                }
            }
            return value
        } else {
            var value = Pair<Int, Movement?>(Int.MAX_VALUE, null)

            for (movement in movements) {
                val cloneGame = Game(game)
                cloneGame.makeMovement(movement)

                countMap[currentDepth] = (countMap[currentDepth]?: 0) + 1

                val suggestedMoveToOpponent = suggestGameMovementAtDepth(cloneGame, currentDepth + 1, alpha, beta, countMap)

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

    private fun playMovement(game: Game, gameUuid: String, movement: Movement): Game {
        val gameInNextMove = Game(game)
        gameInNextMove.makeMovement(movement)

        // Print board after play for debugging
        gameInNextMove.board.printBoard()

        cacheService.storeGame(gameUuid, gameInNextMove)

        return gameInNextMove
    }
}
