package com.example.mantochess.model.pieces

import com.example.mantochess.model.*
import com.example.mantochess.service.MovementService
import com.example.mantochess.service.PositionHelper
import java.io.Serializable

class Piece(var type: PieceType, val color: Color, var position: Long): Serializable {

    var pseudoLegalMovements: Long = 0L
    var emptyBoardMovements: Long = 0L
    val opponentColor = if (color == Color.WHITE) Color.BLACK else Color.WHITE

    fun legalMovements(game: Game): List<Movement> {
        return MovementService.convertToMovementList(game, this)
            .filter { movement -> !movement.isMovementBlocked && !movementCauseOwnCheck(game, movement) }
    }

    fun isAttackingSquares(squares: Long): Boolean {
        return if (type == PieceType.PAWN) {
            // Pawn is attacking squares that are not in its pseudoLegalMovements (diagonals), so the logic should include
            // squares in empty board movements instead.
            emptyBoardMovements.and(squares) != 0L
        } else {
            pseudoLegalMovements.and(squares) != 0L
        }
    }

    private fun movementCauseOwnCheck(game: Game, movement: Movement): Boolean {
        val opponentEmptyBoardMovements = game.board.pieces
            .fold(0L, { acc, p -> if (p.color == opponentColor) acc.or(p.emptyBoardMovements) else acc })

        val king = game.board.pieces.find { p -> p.type == PieceType.KING && p.color == color }!!

        if (opponentEmptyBoardMovements.and(king.position) != 0L || movement.piece === king) {
            // An empty board movement would cause king in check, only then we check the movement more thoroughly.
            // Or we also double check in case the king has moved
            val color = game.currentTurnColor
            game.makeMovement(movement)
            val ownCheck = game.isColorAtCheck(color)
            game.unmakeMovement()
            return ownCheck
        }
        return false
    }

    fun isPawnToBePromoted(targetPosition: Long): Boolean {
        val isLastRow = { target: Long -> 0xFFL.shl(8 * 7).and(target) != 0L || 0xFFL.and(target) != 0L }
        return type == PieceType.PAWN && isLastRow(targetPosition)
    }
}