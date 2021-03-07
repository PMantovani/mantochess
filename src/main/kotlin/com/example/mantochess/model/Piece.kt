package com.example.mantochess.model

import java.io.Serializable
import kotlin.math.abs

class Piece: Serializable {

    var type: PieceType
    val color: Color
    var rank: Int
    var file: Int
    var pseudoLegalMovements: List<Movement> = mutableListOf()

    constructor(type: PieceType, color: Color, rank: Int, file: Int) {
        this.type = type
        this.color = color
        this.rank = rank
        this.file = file
    }

    constructor(copyFromPiece: Piece) : this(copyFromPiece.type, copyFromPiece.color, copyFromPiece.rank, copyFromPiece.file) {
        pseudoLegalMovements = copyFromPiece.pseudoLegalMovements
            .map { m -> Movement(m.piece, Position(m.from.rank, m.from.file), Position(m.to.rank, m.to.file),
                m.promotionPiece, m.isMovementBlocked, m.notation) }
    }

    private enum class MovementDirection { HORIZONTAL, VERTICAL, DIAGONAL, INVERSE_DIAGONAL }

    fun legalMovements(game: Game): List<Movement> {
        return pseudoLegalMovements.filter { movement -> !movement.isMovementBlocked && !movementCauseOwnCheck(game, movement) }
    }

    fun reprocessAvailableMovements(game: Game) {
        pseudoLegalMovements = when(type) {
            PieceType.PAWN -> pawnMovements(game, game.board)
            PieceType.ROOK -> rookMovements(game.board)
            PieceType.KNIGHT -> knightMovements(game.board)
            PieceType.BISHOP -> bishopMovements(game.board)
            PieceType.QUEEN -> queenMovements(game.board)
            PieceType.KING -> kingMovements(game)
        }
    }

    private fun movementCauseOwnCheck(game: Game, movement: Movement): Boolean {
        // Copy game to check move to avoid mutability
        val cloneGame = Game(game)
        cloneGame.makeMovement(movement)

        return cloneGame.isColorAtCheck(game.currentTurnColor)
    }

    private fun isAttackingSquare(square: Position): Boolean {
        return pseudoLegalMovements.find { movement -> movement.to == square } != null
    }

    private fun pawnMovements(game: Game, board: Board): List<Movement> {
        val direction = when(color) {
            Color.WHITE -> 1
            else -> -1
        }

        val movements = mutableListOf<Movement>()

        // Basic forward movement
        if ((rank + direction) in Board.MINIMUM_INDEX..Board.MAXIMUM_INDEX) {
            val isMovementBlocked = board.pieceAt(rank + direction, file).isPresent

            val target = Position(rank + direction, file)
            movements.addAll(getPawnMovementPromotion(target, isMovementBlocked))
        }

        // Double movement
        if (((color == Color.WHITE && rank == 1) || (color == Color.BLACK && rank == 6))) {
            val isMovementBlocked = board.pieceAt(rank + direction, file).isPresent || board.pieceAt(rank + (2 * direction), file).isPresent

            movements.add(Movement(type, Position(rank, file), Position(rank + (2 * direction), file), null, isMovementBlocked, ""))
        }

        // Capturing diagonals
        val diagonalPosition = Position(rank + direction, file + 1)
        var diagonalPiece = board.pieceAt(diagonalPosition.rank, diagonalPosition.file)
        var isMovementBlocked = game.enPassantTarget != diagonalPosition  && (diagonalPiece.isEmpty || diagonalPiece.get().color == color)
        movements.addAll(getPawnMovementPromotion(diagonalPosition, isMovementBlocked))

        val inverseDiagonalPosition = Position(rank + direction, file -1)
        diagonalPiece = board.pieceAt(inverseDiagonalPosition.rank, inverseDiagonalPosition.file)
        isMovementBlocked = game.enPassantTarget != inverseDiagonalPosition && (diagonalPiece.isEmpty || diagonalPiece.get().color == color)
        movements.addAll(getPawnMovementPromotion(inverseDiagonalPosition, isMovementBlocked))

        return movements
    }

    private fun getPawnMovementPromotion(target: Position, isMovementBlocked: Boolean): List<Movement> {
        return if (target.rank == Board.MAXIMUM_INDEX || target.rank == Board.MINIMUM_INDEX) {
            listOf(
                Movement(type, Position(rank, file), target, PieceType.QUEEN, isMovementBlocked, ""),
                Movement(type, Position(rank, file), target, PieceType.KNIGHT, isMovementBlocked, ""),
                Movement(type, Position(rank, file), target, PieceType.ROOK, isMovementBlocked, ""),
                Movement(type, Position(rank, file), target, PieceType.BISHOP, isMovementBlocked, "")
            )
        } else {
            listOf(Movement(type, Position(rank, file), target, null, isMovementBlocked, ""))
        }
    }

    private fun rookMovements(board: Board): List<Movement> {
        val movements = mutableListOf<Movement>()
        movements.addAll(verticalMovement(board))
        movements.addAll(horizontalMovement(board))
        return movements
    }

    private fun knightMovements(board: Board): List<Movement> {
        val movements = mutableListOf<Movement>()
        movements.addAll(moveToPosition(board, rank + 2, file + 1))
        movements.addAll(moveToPosition(board, rank + 2, file - 1))
        movements.addAll(moveToPosition(board, rank - 2, file + 1))
        movements.addAll(moveToPosition(board, rank - 2, file - 1))
        movements.addAll(moveToPosition(board, rank + 1, file + 2))
        movements.addAll(moveToPosition(board, rank - 1, file + 2))
        movements.addAll(moveToPosition(board, rank + 1, file - 2))
        movements.addAll(moveToPosition(board, rank - 1, file - 2))

        return movements
    }

    private fun moveToPosition(board: Board, targetRank: Int, targetFile: Int): List<Movement> {
        val pieceAtNextPosition = board.pieceAt(targetRank, targetFile)
        if (targetRank in Board.MINIMUM_INDEX..Board.MAXIMUM_INDEX && targetFile in Board.MINIMUM_INDEX..Board.MAXIMUM_INDEX) {
            val isMovementBlocked = pieceAtNextPosition.isPresent && pieceAtNextPosition.get().color == this.color
            return listOf(Movement(type, Position(this.rank, this.file), Position(targetRank, targetFile), null, isMovementBlocked, ""))
        }
        return emptyList()
    }

    private fun bishopMovements(board: Board): List<Movement> {
        return diagonalMovements(board)
    }

    private fun queenMovements(board: Board): List<Movement> {
        val movements = mutableListOf<Movement>()
        movements.addAll(verticalMovement(board))
        movements.addAll(horizontalMovement(board))
        movements.addAll(diagonalMovements(board))
        return movements
    }

    private fun kingMovements(game: Game): List<Movement> {
        val movements = mutableListOf<Movement>()
        movements.addAll(iterativeMovements(game.board, -1, -1, MovementDirection.VERTICAL))
        movements.addAll(iterativeMovements(game.board, 1, 1, MovementDirection.VERTICAL))
        movements.addAll(iterativeMovements(game.board, -1, -1, MovementDirection.HORIZONTAL))
        movements.addAll(iterativeMovements(game.board, 1, 1, MovementDirection.HORIZONTAL))
        movements.addAll(iterativeMovements(game.board, -1, -1, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(game.board, 1, 1, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(game.board, -1, -1, MovementDirection.INVERSE_DIAGONAL))
        movements.addAll(iterativeMovements(game.board, 1, 1, MovementDirection.INVERSE_DIAGONAL))

        // Castling logic
        movements.addAll(kingsideCastlingMovement(game))
        movements.addAll(queensideCastlingMovement(game))

        return movements
    }

    private fun verticalMovement(board: Board): List<Movement> {
        val movements = mutableListOf<Movement>()
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.VERTICAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.VERTICAL))
        return movements
    }

    private fun horizontalMovement(board: Board): List<Movement> {
        val movements = mutableListOf<Movement>()
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.HORIZONTAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.HORIZONTAL))
        return movements
    }

    private fun diagonalMovements(board: Board): List<Movement> {
        val movements = mutableListOf<Movement>()
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.INVERSE_DIAGONAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.INVERSE_DIAGONAL))
        return movements
    }

    private fun iterativeMovements(board: Board, from: Int, to: Int, movementType: MovementDirection): List<Movement> {
        val movements = mutableListOf<Movement>()

        // We have to use downTo for ranges when from is larger than to because we stop in the first interception
        val squareRangeProgression = if (abs(from) < abs(to)) from..to else to downTo from

        for (nextVal in squareRangeProgression) {
            val nextCoordinates = when(movementType) {
                MovementDirection.VERTICAL -> Position(rank + nextVal, file)
                MovementDirection.HORIZONTAL -> Position(rank, file + nextVal)
                MovementDirection.DIAGONAL -> Position(rank + nextVal, file + nextVal)
                else -> Position(rank + nextVal, file - nextVal)
            }

            // Check if we are still inside the boundaries of the game and it's not a movement to the same square.
            // Otherwise skip movement.
            if (nextCoordinates.rank < Board.MINIMUM_INDEX || nextCoordinates.rank > Board.MAXIMUM_INDEX
                || nextCoordinates.file < Board.MINIMUM_INDEX || nextCoordinates.file > Board.MAXIMUM_INDEX) {
                break
            }

            val pieceAtNextSquare = board.pieceAt(nextCoordinates.rank, nextCoordinates.file)
            if (pieceAtNextSquare.isEmpty) {
                movements.add(Movement(type, Position(rank, file), nextCoordinates, null, false, "" ))
            } else {
                val isMovementBlocked = pieceAtNextSquare.get().color == color
                movements.add(Movement(type, Position(rank, file), nextCoordinates, null, isMovementBlocked, "" ))
                break
            }
        }
        return movements
    }

    private fun kingsideCastlingMovement(game: Game): List<Movement> {
        if (game.castlingKingsideAllowed[color]!!) {
            val mustBeEmptySquares = listOf(Position(rank, 5), Position(rank, 6))
            val mustBeNonAttackedSquares = listOf(Position(rank, 4), Position(rank, 5), Position(rank, 6))

            if (otherPiecesAllowCastle(mustBeEmptySquares, mustBeNonAttackedSquares, game)) {
                val kingMovesTo = Position(rank, 6)
                return listOf(Movement(type, Position(rank, file), kingMovesTo, null, false, "O-O") )
            }
        }
        return emptyList()
    }

    private fun queensideCastlingMovement(game: Game): List<Movement> {
        if (game.castlingQueensideAllowed[color]!!) {
            val mustBeEmptySquares = listOf(Position(rank, 1), Position(rank, 2), Position(rank, 3))
            val mustBeNonAttackedSquares = listOf(Position(rank, 2), Position(rank, 3), Position(rank, 4))

            if (otherPiecesAllowCastle(mustBeEmptySquares, mustBeNonAttackedSquares, game)) {
                val kingMovesTo = Position(rank, 2)
                return listOf(Movement(type, Position(rank, file), kingMovesTo, null, false, "O-O-O") )
            }
        }
        return emptyList()
    }

    private fun otherPiecesAllowCastle(mustBeEmptySquares: List<Position>, mustBeNonAttackedSquares: List<Position>, game: Game): Boolean {
        val intercept = mustBeEmptySquares.find { square -> game.board.pieceAt(square.rank, square.file).isPresent }
        if (intercept == null) {
            val opponentPieces = when(color) {
                Color.WHITE -> game.board.pieces[Color.BLACK]!!
                else -> game.board.pieces[Color.WHITE]!!
            }

            val attackingPiece = mustBeNonAttackedSquares.find { square ->
                opponentPieces.find { piece -> piece.isAttackingSquare(square) } != null
            }

            return attackingPiece == null
        }

        return false
    }

    fun isPawnToBePromoted(position: Position): Boolean {
        return type == PieceType.PAWN && (position.rank == Board.MINIMUM_INDEX || position.rank == Board.MAXIMUM_INDEX)
    }
}