package com.example.mantochess.service

import com.example.mantochess.model.*
import org.springframework.stereotype.Service
import java.security.InvalidParameterException
import kotlin.system.measureTimeMillis

@Service
class GameService(
    private val notationService: NotationService,
    private val cacheService: CacheService) {

    private var maxDepth = 3
    private val enableAlphaBetaPruning = false

    fun processGameMovement(gameUuid: String, notation: String): Game {
        val game = getGameFromUuid(gameUuid)
        val movement = notationService.convertNotationToMovement(notation, game)

        return playMovement(game, gameUuid, movement)
    }

    fun suggestGameMovement(gameUuid: String, shouldPlayMovement: Boolean = false, depth: Int = maxDepth): String {
        maxDepth = depth
        val game = getGameFromUuid(gameUuid)

        val countMap = HashMap<String, Int>()

        val suggestedMovement: Pair<Int, Movement?>
        val ellapsedTime = measureTimeMillis {
            suggestedMovement = suggestGameMovementAtDepth(game, 0, Int.MIN_VALUE, Int.MAX_VALUE, countMap, "")
        }

        var returnStr = "Ellapsed time: ${ellapsedTime}ms\n"
        returnStr += "Perft:\n"

        var total = 0
        countMap.toList()
            .sortedBy { (m, _) -> m }
            .forEach { (movement, count) ->
                returnStr += "${movement}: $count\n"
                total += count
            }

        return if (suggestedMovement.second == null) {
            "Checkmate. ${if (game.currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE} wins"
        } else {
            if (shouldPlayMovement) {
                playMovement(game, gameUuid, suggestedMovement.second!!)
            }

            returnStr += "Depth: $maxDepth\n"
            returnStr += "Total nodes: $total\n"
            returnStr += "Nodes per sec: ${total/(ellapsedTime / 1000.0)/1000.0} kN/s\n"
            returnStr += "Game advantage: ${suggestedMovement.first}\n"
            returnStr += "Suggested move: ${notationService.convertMovementToNotation(suggestedMovement.second!!)}\n"
            print(returnStr)
            returnStr
        }
    }

    private fun suggestGameMovementAtDepth(game: Game, currentDepth: Int, alphaParam: Int, betaParam: Int, countMap: MutableMap<String, Int>, moveStr: String
        ): Pair<Int, Movement?> {
        var alpha = alphaParam
        var beta = betaParam

        if (currentDepth == maxDepth) {
            countMap[moveStr] = (countMap[moveStr]?: 0) + 1
            return Pair(game.getCurrentAdvantage(), null)
        }

        val unsortedMovements = game.availableMovementsFor(game.currentTurnColor)

        // Sorting movement first by pieces that would be captured enhances alpha-beta pruning algorithm,
        // as those movements are more likely to be the best ones, so pruning occurs more often
        val movements = unsortedMovements.sortedBy { m -> game.board.pieceAt(m.to) == null }

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
                val moveStr2 = if (currentDepth == 0) {
                    PositionHelper.toAlgebraicNotation(movement.from, movement.promotionPiece) +
                            PositionHelper.toAlgebraicNotation(movement.to, movement.promotionPiece)
                } else moveStr

                game.makeMovement(movement)
                val suggestedMoveToOpponent = suggestGameMovementAtDepth(game, currentDepth + 1, alpha, beta, countMap, moveStr2)
                game.unmakeMovement()

                value = if (value.first > suggestedMoveToOpponent.first) value else Pair(suggestedMoveToOpponent.first, movement)
                alpha = Math.max(alpha, value.first)

                if (enableAlphaBetaPruning && alpha >= beta) {
                    break
                }
            }
            return value

        } else {
            var value = Pair<Int, Movement?>(Int.MAX_VALUE, null)

            for (movement in movements) {
                val moveStr2 = if (currentDepth == 0) {
                    PositionHelper.toAlgebraicNotation(movement.from, movement.promotionPiece) +
                            PositionHelper.toAlgebraicNotation(movement.to, movement.promotionPiece)
                } else moveStr

                game.makeMovement(movement)
                val suggestedMoveToOpponent = suggestGameMovementAtDepth(game, currentDepth + 1, alpha, beta, countMap, moveStr2)
                game.unmakeMovement()

                value = if (value.first < suggestedMoveToOpponent.first) value else Pair(suggestedMoveToOpponent.first, movement)
                beta = Math.min(beta, value.first)

                if (enableAlphaBetaPruning && beta <= alpha) {
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
        val colorToPlay = game.currentTurnColor
        game.makeMovement(movement)

        // Print board after play for debugging
        game.board.printBoard()

        if (game.isColorAtCheck(colorToPlay)) {
            game.unmakeMovement()
            throw InvalidMovementException("Specified movement would cause own-check")
        }

        cacheService.storeGame(gameUuid, game)

        return game
    }
}
