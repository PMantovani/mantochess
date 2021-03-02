package com.example.mantochess.model

import java.io.Serializable

class Game: Serializable {
    val board: Board
    var currentTurnColor = Color.WHITE
    var advantage = 0
    val castlingKingsideAllowed: MutableMap<Color, Boolean>
    val castlingQueensideAllowed: MutableMap<Color, Boolean>

    constructor() {
        board = Board()
        castlingKingsideAllowed = mutableMapOf(Pair(Color.WHITE, true), Pair(Color.BLACK, true))
        castlingQueensideAllowed = mutableMapOf(Pair(Color.WHITE, true), Pair(Color.BLACK, true))
    }

    constructor(copyFromGame: Game) {
        board = Board(copyFromGame.board)
        currentTurnColor = copyFromGame.currentTurnColor
        castlingKingsideAllowed = copyFromGame.castlingKingsideAllowed.toMutableMap()
        castlingQueensideAllowed = copyFromGame.castlingQueensideAllowed.toMutableMap()
        advantage = copyFromGame.advantage
    }

    fun makeMovement(movement: CompleteMovementInfo) {
        val piece = board.pieceAt(movement.from.rank, movement.from.file).get()

        val pieceCaptured = board.pieceAt(movement.to.rank, movement.to.file)
        if (pieceCaptured.isPresent) {
            board.pieces[pieceCaptured.get().color]!!.remove(pieceCaptured.get())
        }

        updateCastlingAbility(piece)

        piece.rank = movement.to.rank
        piece.file = movement.to.file

        // Process castling
        if (movement.notation == "O-O-O") {
            // King was naturally moved by previous statements on castling. Now we need to move the rook.
            val rank = if (currentTurnColor == Color.WHITE) Board.MINIMUM_INDEX else Board.MAXIMUM_INDEX
            val rook = board.pieceAt(rank, Board.MINIMUM_INDEX).get()
            rook.file = 3
        } else if (movement.notation == "O-O") {
            val rank = if (currentTurnColor == Color.WHITE) Board.MINIMUM_INDEX else Board.MAXIMUM_INDEX
            val rook = board.pieceAt(rank, Board.MAXIMUM_INDEX).get()
            rook.file = 5
        }

        if (isColorAtCheck(currentTurnColor)) {
            throw InvalidMovementException("King is checked at this position. Please try another move")
        }

        currentTurnColor = if (currentTurnColor == Color.WHITE) Color.BLACK else Color.WHITE

        // Print board after play for debugging
        board.printBoard()
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

    private fun isColorAtCheck(color: Color): Boolean {
        val king = board.pieces[color]!!.find { p -> p.type == PieceType.KING }!!

        val opponentMoves = availableMovementFor(if (color == Color.WHITE) Color.BLACK else Color.WHITE )
        val checkMove = opponentMoves.find { move -> move.second.rank == king.rank && move.second.file == king.file }
        return checkMove != null
    }

    fun availableMovementFor(color: Color): List<Pair<Piece, Position>> {
        val pieces = board.pieces[color]!!

        return pieces.map { p -> p.availableMovements(this).map { m -> Pair(p, m) } }.flatten()
    }
}