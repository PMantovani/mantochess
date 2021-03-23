package com.example.mantochess.model

import com.example.mantochess.model.pieces.Piece
import com.example.mantochess.service.MovementService
import com.example.mantochess.service.NotationService
import com.example.mantochess.service.PositionHelper
import java.io.Serializable
import java.util.*

class Game: Serializable {
    val board: Board = Board()
    var currentTurnColor = Color.WHITE
    var castlingKingsideAllowed: MutableMap<Color, Boolean>
    var castlingQueensideAllowed: MutableMap<Color, Boolean>
    var enPassantTarget: Long? = null
    private val gameHistory = Stack<GameHistoryData>()

    init {
        castlingKingsideAllowed = mutableMapOf(Pair(Color.WHITE, true), Pair(Color.BLACK, true))
        castlingQueensideAllowed = mutableMapOf(Pair(Color.WHITE, true), Pair(Color.BLACK, true))
    }

    fun makeMovement(movement: Movement) {
        gameHistory.push(GameHistoryData(
            movement,
            castlingKingsideAllowed.toMutableMap(),
            castlingQueensideAllowed.toMutableMap(),
            enPassantTarget,
            board.pieces.map { p -> p to p.pseudoLegalMovements }.toMap()
        ))

        val opponentColor = if (currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE

        if (movement.capturedPiece != null) {
            board.pieces.remove(movement.capturedPiece)
            val queensideRookPosition = if (movement.capturedPiece.color == Color.WHITE) 0x01L else PositionHelper.toLong(7, 0)
            val kingsideRookPosition = if (movement.capturedPiece.color == Color.WHITE) 0x01L.shl(7) else PositionHelper.toLong(7, 7)
            if (movement.capturedPiece.type == PieceType.ROOK && movement.to == queensideRookPosition) {
                // After capturing rook, disable opponent's castling ability
                castlingQueensideAllowed[opponentColor] = false
            } else if (movement.capturedPiece.type == PieceType.ROOK && movement.to == kingsideRookPosition) {
                castlingKingsideAllowed[opponentColor] = false
            }
            // We need to also remove the captured piece here because it might be en-passant capture, so it wouldn't match
            // the target position below. So simply overriding the value in the map wouldn't work.
            board.piecePositions.remove(movement.capturedPiece.position)
        }

        updateCastlingAbility(movement.piece)

        board.piecePositions.remove(movement.piece.position)
        movement.piece.position = movement.to
        board.piecePositions[movement.piece.position] = movement.piece


        // Enables en passant square if moving pawn two squares
        val hasMovedTwoSquares = { from: Long, to: Long ->
            from.and(0xFFL.shl(8)) != 0x00L && to.and(0xFFL.shl(8 * 3)) != 0x00L ||
            from.and(0xFFL.shl(8 * 6)) != 0x00L && to.and(0xFFL.shl(8 * 4)) != 0x00L
        }
        enPassantTarget = if (movement.piece.type == PieceType.PAWN && hasMovedTwoSquares(movement.from, movement.to)) {
            if (currentTurnColor == Color.WHITE) movement.to.shr(8) else movement.to.shl(8)
        } else {
            null
        }

        // Process castling
        if (movement.notation == "O-O-O") {
            // King was naturally moved by previous statements on castling. Now we need to move the rook.
            val rookPosition = if (currentTurnColor == Color.WHITE) 0x01L else PositionHelper.toLong(7, 0)
            val castlingRook = board.pieceAt(rookPosition)!!
            board.piecePositions.remove(castlingRook.position)
            castlingRook.position = castlingRook.position.shl(3)
            board.piecePositions[castlingRook.position] = castlingRook
        } else if (movement.notation == "O-O") {
            val rookPosition = if (currentTurnColor == Color.WHITE) PositionHelper.toLong(0, 7) else PositionHelper.toLong(7, 7)
            val castlingRook = board.pieceAt(rookPosition)!!
            board.piecePositions.remove(castlingRook.position)
            castlingRook.position = castlingRook.position.ushr(2)
            board.piecePositions[castlingRook.position] = castlingRook
        }

        // Process pawn promotion
        if (movement.piece.isPawnToBePromoted(movement.to)) {
            if (movement.promotionPiece == null) {
                throw InvalidMovementException("Promotion piece must be specified after pawn reaches end of board")
            }
            movement.piece.type = movement.promotionPiece
            board.piecePositions[movement.piece.position] = movement.piece
        }

        currentTurnColor = if (currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE
        MovementService.reprocessAllAvailableMovements(this)
    }

    fun unmakeMovement() {
        val (movement, previousKingsideCastling, previousQueensideCastling, previousEnPassantTarget, previousMovements) = gameHistory.pop()
        castlingKingsideAllowed = previousKingsideCastling
        castlingQueensideAllowed = previousQueensideCastling
        enPassantTarget = previousEnPassantTarget

        val initialKingRank = if (movement.piece.color == Color.WHITE) 0 else 7
        if (movement.notation == NotationService.kingsideCastleNotation) {
            val rook = board.pieceAt(PositionHelper.toLong(initialKingRank, 5))!!
            val king = board.pieceAt(PositionHelper.toLong(initialKingRank, 6))!!

            board.piecePositions.remove(rook.position)
            rook.position = rook.position.shl(2)
            board.piecePositions[rook.position] = rook

            board.piecePositions.remove(king.position)
            king.position = king.position.ushr(2)
            board.piecePositions[king.position] = king
        } else if (movement.notation == NotationService.queensideCastleNotation) {
            val rook = board.pieceAt(PositionHelper.toLong(initialKingRank, 3))!!
            val king = board.pieceAt(PositionHelper.toLong(initialKingRank, 2))!!
            board.piecePositions.remove(rook.position)
            rook.position = rook.position.ushr(3)
            board.piecePositions[rook.position] = rook
            board.piecePositions.remove(king.position)
            king.position = king.position.shl(2)
            board.piecePositions[king.position] = king
        } else {
            board.piecePositions.remove(movement.piece.position)
            movement.piece.position = movement.from
            board.piecePositions[movement.piece.position] = movement.piece
            movement.promotionPiece?.let {
                // Undo promotion
                movement.piece.type = PieceType.PAWN
            }
            movement.capturedPiece?.let {
                board.pieces.add(movement.capturedPiece)
                board.piecePositions[movement.capturedPiece.position] = movement.capturedPiece
            }
        }

        currentTurnColor = if (currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE
        board.pieces.forEach { p -> p.pseudoLegalMovements = previousMovements[p]!! }
    }

    private fun updateCastlingAbility(pieceMoved: Piece) {
        if (pieceMoved.type == PieceType.KING) {
            castlingKingsideAllowed[currentTurnColor] = false
            castlingQueensideAllowed[currentTurnColor] = false
        }

        val queensideRookPosition = if (pieceMoved.color == Color.WHITE) 0x01L else PositionHelper.toLong(7, 0)
        val kingsideRookPosition = if (pieceMoved.color == Color.WHITE) PositionHelper.toLong(0, 7) else PositionHelper.toLong(7, 7)

        if (pieceMoved.type == PieceType.ROOK && pieceMoved.position == queensideRookPosition) {
            castlingQueensideAllowed[currentTurnColor] = false
        }

        if (pieceMoved.type == PieceType.ROOK && pieceMoved.position == kingsideRookPosition) {
            castlingKingsideAllowed[currentTurnColor] = false
        }
    }

    fun isColorAtCheck(color: Color): Boolean {
        val king = board.pieces.find { p -> p.type == PieceType.KING && p.color == color }!!

        val opponentMoves = availableMovementsFor(king.opponentColor, false)
        val checkMove = opponentMoves.find { move -> move.to == king.position }
        return checkMove != null
    }

    fun availableMovementsFor(color: Color, excludeMovementsThatCauseOwnCheck: Boolean = true): List<Movement> {
        val pieces = board.pieces.filter { piece -> piece.color == color }

        return if (excludeMovementsThatCauseOwnCheck)
            pieces.map { p -> p.legalMovements(this) }.flatten().filter { m -> !m.isMovementBlocked }
            else pieces.map { p -> MovementService.convertToMovementList(this, p) }.flatten().filter { m -> !m.isMovementBlocked }
    }

    fun getCurrentAdvantage(): Int {
        return board.pieces.fold(0,
            { acc, piece -> (if (piece.color == Color.WHITE) 1 else -1) * (piece.type.value) + acc })
    }
}