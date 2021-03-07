package com.example.mantochess.model

import java.io.Serializable
import kotlin.math.abs

class Game: Serializable {
    val board: Board
    var currentTurnColor = Color.WHITE
    val castlingKingsideAllowed: MutableMap<Color, Boolean>
    val castlingQueensideAllowed: MutableMap<Color, Boolean>
    var enPassantTarget: Position? = null

    constructor() {
        board = Board()
        castlingKingsideAllowed = mutableMapOf(Pair(Color.WHITE, true), Pair(Color.BLACK, true))
        castlingQueensideAllowed = mutableMapOf(Pair(Color.WHITE, true), Pair(Color.BLACK, true))
        board.pieces.forEach { (_, pieces) -> pieces.forEach { piece -> piece.reprocessAvailableMovements(this) } }
    }

    constructor(copyFromGame: Game) {
        board = Board(copyFromGame.board)
        currentTurnColor = copyFromGame.currentTurnColor
        castlingKingsideAllowed = copyFromGame.castlingKingsideAllowed.toMutableMap()
        castlingQueensideAllowed = copyFromGame.castlingQueensideAllowed.toMutableMap()
        enPassantTarget = if (copyFromGame.enPassantTarget != null)
            Position(copyFromGame.enPassantTarget!!.rank, copyFromGame.enPassantTarget!!.file) else null
    }

    fun makeMovement(movement: Movement) {
        val piece = board.pieceAt(movement.from.rank, movement.from.file).get()
        val opponentColor = if (currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE

        val pieceCaptured = board.pieceAt(movement.to.rank, movement.to.file)
        if (pieceCaptured.isPresent) {
            board.pieces[pieceCaptured.get().color]!!.remove(pieceCaptured.get())
            if (pieceCaptured.get().type == PieceType.ROOK && pieceCaptured.get().file == 0) {
                // After capturing rook, disable opponent's castling ability
                castlingQueensideAllowed[opponentColor] = false
            } else if (pieceCaptured.get().type == PieceType.ROOK && pieceCaptured.get().file == 7) {
                castlingKingsideAllowed[opponentColor] = false
            }
        }

        updateCastlingAbility(piece)

        piece.rank = movement.to.rank
        piece.file = movement.to.file

        // Records last en passant square to process pawns there
        val lastEnPassantTarget = enPassantTarget

        // Enables en passant square if moving pawn two squares
        enPassantTarget = if (movement.piece == PieceType.PAWN && abs(movement.to.rank - movement.from.rank) == 2) {
            Position(
                if (piece.color == Color.WHITE) movement.from.rank + 1 else movement.from.rank - 1,
                movement.from.file)
        } else {
            null
        }

        // Process en passant movement
        var lastEnPassantPawnPosition: Position? = null
        if (movement.piece == PieceType.PAWN && movement.to == lastEnPassantTarget) {
            lastEnPassantPawnPosition = Position(
                if (piece.color == Color.WHITE) movement.to.rank - 1 else movement.to.rank + 1,
                movement.to.file)
            val enPassantPieceCaptured = board.pieceAt(lastEnPassantPawnPosition.rank, lastEnPassantPawnPosition.file)
            board.pieces[enPassantPieceCaptured.get().color]!!.remove(enPassantPieceCaptured.get())
        }

        // Process castling
        var castlingRook: Piece? = null
        val rookSquaresInCastling = mutableListOf<Position>()
        if (movement.notation == "O-O-O") {
            // King was naturally moved by previous statements on castling. Now we need to move the rook.
            val rank = if (currentTurnColor == Color.WHITE) Board.MINIMUM_INDEX else Board.MAXIMUM_INDEX
            castlingRook = board.pieceAt(rank, Board.MINIMUM_INDEX).get()
            castlingRook.file = 3
            rookSquaresInCastling.add(Position(rank, 0))
            rookSquaresInCastling.add(Position(rank, 3))
        } else if (movement.notation == "O-O") {
            val rank = if (currentTurnColor == Color.WHITE) Board.MINIMUM_INDEX else Board.MAXIMUM_INDEX
            castlingRook = board.pieceAt(rank, Board.MAXIMUM_INDEX).get()
            castlingRook.file = 5
            rookSquaresInCastling.add(Position(rank, 7))
            rookSquaresInCastling.add(Position(rank, 5))
        }

        // Process pawn promotion
        if (piece.isPawnToBePromoted(Position(piece.rank, piece.file))) {
            if (movement.promotionPiece == null) {
                throw InvalidMovementException("Promotion piece must be specified after pawn reaches end of board")
            }
            piece.type = movement.promotionPiece
        }

        // For delta movement processing, we need to figure out which pieces need to be reprocessed.
        // The moved piece for sure needs to be reprocessed, in addition for the castling rook, if it was a castling movement
        // And also with the addition of any pieces touching the source and target squares.
        val piecesToBeReprocessed = mutableSetOf(piece)
        castlingRook?.let {
            piecesToBeReprocessed.add(castlingRook)
            rookSquaresInCastling.forEach { rookSquares -> piecesToBeReprocessed.addAll(piecesThatNeedReprocessingAtSquare(rookSquares)) }
        }
        piecesToBeReprocessed.addAll(piecesThatNeedReprocessingAtSquare(movement.from))
        piecesToBeReprocessed.addAll(piecesThatNeedReprocessingAtSquare(movement.to))
        if (enPassantTarget != null) {
            piecesToBeReprocessed.addAll(piecesThatNeedReprocessingAtSquare(enPassantTarget!!))
        }
        if (lastEnPassantTarget != null) {
            piecesToBeReprocessed.addAll(piecesThatNeedReprocessingAtSquare(lastEnPassantTarget))
            if (lastEnPassantPawnPosition != null) {
                piecesToBeReprocessed.addAll(piecesThatNeedReprocessingAtSquare(lastEnPassantPawnPosition))
            }
        }
        if (castlingKingsideAllowed[currentTurnColor]!! || castlingQueensideAllowed[currentTurnColor]!!) {
            // If castling is still available, reprocess kings movement every time
            val king = board.pieces[currentTurnColor]!!.find { p -> p.type == PieceType.KING }!!
            piecesToBeReprocessed.add(king)
        }

        if ((pieceCaptured.isPresent && pieceCaptured.get().type == PieceType.ROOK)
            || castlingKingsideAllowed[opponentColor]!! || castlingQueensideAllowed[opponentColor]!!) {
            // If a rook is captured, we need to re-evaluate opponent king's movements, since castling is now disabled
            // Also, if castling is still enabled for opponent, check movement since king might be in check now
            val king = board.pieces[opponentColor]!!.find { p -> p.type == PieceType.KING }!!
            piecesToBeReprocessed.add(king)
        }
        if (piece.type == PieceType.ROOK) {
            // If a rook is moved, we need to re-evaluate our own king's movements, since castling is now disabled
            val king = board.pieces[currentTurnColor]!!.find { p -> p.type == PieceType.KING }!!
            piecesToBeReprocessed.add(king)
        }

        // Reprocess movements for pieces
        piecesToBeReprocessed.forEach { pieceToReprocess -> pieceToReprocess.reprocessAvailableMovements(this) }

        currentTurnColor = if (currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE
    }

    private fun piecesThatNeedReprocessingAtSquare(position: Position): Set<Piece> {
        val piecesToBeReprocessed = mutableSetOf<Piece>()
        board.pieces.forEach { (_, pieces) -> pieces.forEach { piece ->
            if (piece.pseudoLegalMovements.find { movement -> movement.to == position } != null) {
                piecesToBeReprocessed.add(piece)
            }
        } }
        return piecesToBeReprocessed
    }

    private fun updateCastlingAbility(pieceMoved: Piece) {
        if (pieceMoved.type == PieceType.KING) {
            castlingKingsideAllowed[currentTurnColor] = false
            castlingQueensideAllowed[currentTurnColor] = false
        }

        if (pieceMoved.type == PieceType.ROOK && pieceMoved.file == Board.MINIMUM_INDEX) {
            castlingQueensideAllowed[currentTurnColor] = false
        }

        if (pieceMoved.type == PieceType.ROOK && pieceMoved.file == Board.MAXIMUM_INDEX) {
            castlingKingsideAllowed[currentTurnColor] = false
        }
    }

    fun isColorAtCheck(color: Color): Boolean {
        val king = board.pieces[color]!!.find { p -> p.type == PieceType.KING }!!

        val opponentMoves = availableMovementsFor(if (color == Color.WHITE) Color.BLACK else Color.WHITE, false)
        val checkMove = opponentMoves.find { move -> move.to.rank == king.rank && move.to.file == king.file }
        return checkMove != null
    }

    fun availableMovementsFor(color: Color, excludeMovementsThatCauseOwnCheck: Boolean = true): List<Movement> {
        val pieces = board.pieces[color]!!

        return if (excludeMovementsThatCauseOwnCheck)
            pieces.map { p -> p.legalMovements(this) }.flatten()
            else pieces.map { p -> p.pseudoLegalMovements }.flatten().filter { m -> !m.isMovementBlocked }
    }

    fun getCurrentAdvantage(): Int {
        val whiteSum = board.pieces[Color.WHITE]!!.fold(0, { acc, piece -> piece.type.value + acc })
        val blackSum = board.pieces[Color.BLACK]!!.fold(0, { acc, piece -> piece.type.value + acc })
        return whiteSum - blackSum
    }
}