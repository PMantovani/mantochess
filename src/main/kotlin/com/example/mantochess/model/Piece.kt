package com.example.mantochess.model

import java.io.Serializable
import kotlin.math.abs

class Piece constructor(var type: PieceType, val color: Color, var rank: Int, var file: Int): Serializable {

    constructor(copyFromPiece: Piece) : this(copyFromPiece.type, copyFromPiece.color, copyFromPiece.rank, copyFromPiece.file) {}

    private enum class MovementDirection { HORIZONTAL, VERTICAL, DIAGONAL, INVERSE_DIAGONAL }

    fun availableMovements(game: Game, excludeMovementsThatCauseOwnCheck: Boolean = true, includeCastling: Boolean = true): List<CompleteMovementInfo> {
        val movementsBeforeCheckVerification: List<Position> = when(type) {
            PieceType.PAWN -> pawnMovements(game.board)
            PieceType.ROOK -> rookMovements(game.board)
            PieceType.KNIGHT -> knightMovements(game.board)
            PieceType.BISHOP -> bishopMovements(game.board)
            PieceType.QUEEN -> queenMovements(game.board)
            PieceType.KING -> kingMovements(game, includeCastling)
        }

        val finalMovements: MutableList<Position> = mutableListOf()

        // Check if movement wouldn't cause a check on their own pieces. This movement would be illegal, as it is a pinned piece.
        // However, there are some cases that are not concerned whether piece is pinned or not, so we introduce the excludeMovementsThatCauseOwnCheck parameter.
        movementsBeforeCheckVerification.forEach { movement ->
            if ( !excludeMovementsThatCauseOwnCheck || !movementCauseOwnCheck(game, movement)) {
                // If move does not result in own-check, this movement is allowed
                finalMovements.add(movement)
            }
        }

        val completeMovements = mutableListOf<CompleteMovementInfo>()

        finalMovements.forEach { movement ->
            if (isPawnToBePromoted(movement)) {
                completeMovements.add(CompleteMovementInfo(this.type, Position(rank, file), Position(movement.rank, movement.file), null, PieceType.KNIGHT, ""))
                completeMovements.add(CompleteMovementInfo(this.type, Position(rank, file), Position(movement.rank, movement.file), null, PieceType.BISHOP, ""))
                completeMovements.add(CompleteMovementInfo(this.type, Position(rank, file), Position(movement.rank, movement.file), null, PieceType.QUEEN, ""))
                completeMovements.add(CompleteMovementInfo(this.type, Position(rank, file), Position(movement.rank, movement.file), null, PieceType.ROOK, ""))
            } else {
                completeMovements.add(CompleteMovementInfo(this.type, Position(rank, file), Position(movement.rank, movement.file), null, null, ""))
            }
        }

        return completeMovements
    }

    private fun movementCauseOwnCheck(game: Game, movement: Position): Boolean {
        // Copy game to check move to avoid mutability
        val cloneGame = Game(game)

        val currentPieceAtTargetSquare = cloneGame.board.pieceAt(movement.rank, movement.file)
        if (currentPieceAtTargetSquare.isPresent) {
            cloneGame.board.pieces[currentPieceAtTargetSquare.get().color]!!.remove(currentPieceAtTargetSquare.get())
        }

        val clonePiece = cloneGame.board.pieceAt(rank, file).get()

        clonePiece.rank = movement.rank
        clonePiece.file = movement.file

        return cloneGame.isColorAtCheck(cloneGame.currentTurnColor)
    }

    private fun isAttackingSquare(game: Game, square: Position): Boolean {
        return availableMovements(game, excludeMovementsThatCauseOwnCheck = false, includeCastling = false).find { mov -> mov.to == square } != null
    }

    private fun pawnMovements(board: Board): List<Position> {
        val direction = when(color) {
            Color.WHITE -> 1
            else -> -1
        }

        val movements = mutableListOf<Position>()

        // Basic movement
        if (board.pieceAt(rank + direction, file).isEmpty
            && (rank + direction) in Board.MINIMUM_INDEX..Board.MAXIMUM_INDEX) {

            movements.add(Position(rank + direction, file))
        }

        // Double movement
        if (((color == Color.WHITE && rank == 1) || (color == Color.BLACK && rank == 6))
            && board.pieceAt(rank + direction, file).isEmpty
            && board.pieceAt(rank + (2 * direction), file).isEmpty) {

            movements.add(Position(rank + (2 * direction), file))
        }

        // Capturing diagonals
        var diagonalPiece = board.pieceAt(rank + direction, file + 1)
        if (diagonalPiece.isPresent && diagonalPiece.get().color != color) {
            movements.add(Position(rank + direction, file + 1))
        }

        diagonalPiece = board.pieceAt(rank + direction, file - 1)
        if (diagonalPiece.isPresent && diagonalPiece.get().color != color) {
            movements.add(Position(rank + direction, file - 1))
        }

        // TODO: Add en-passant movement

        return movements
    }

    private fun rookMovements(board: Board): List<Position> {
        val movements = mutableListOf<Position>()
        movements.addAll(verticalMovement(board))
        movements.addAll(horizontalMovement(board))
        return movements
    }

    private fun knightMovements(board: Board): List<Position> {
        val movements = mutableListOf<Position>()
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

    private fun moveToPosition(board: Board, rank: Int, file: Int): List<Position> {
        val pieceAtNextPosition = board.pieceAt(rank, file)
        if ((pieceAtNextPosition.isEmpty || pieceAtNextPosition.get().color != this.color)
            && rank in Board.MINIMUM_INDEX..Board.MAXIMUM_INDEX
            && file in Board.MINIMUM_INDEX..Board.MAXIMUM_INDEX) {
            return listOf(Position(rank, file))
        }
        return emptyList()
    }

    private fun bishopMovements(board: Board): List<Position> {
        return diagonalMovements(board)
    }

    private fun queenMovements(board: Board): List<Position> {
        val movements = mutableListOf<Position>()
        movements.addAll(verticalMovement(board))
        movements.addAll(horizontalMovement(board))
        movements.addAll(diagonalMovements(board))
        return movements
    }

    private fun kingMovements(game: Game, includeCastling: Boolean): List<Position> {
        val movements = mutableListOf<Position>()
        movements.addAll(iterativeMovements(game.board, -1, 0, MovementDirection.VERTICAL))
        movements.addAll(iterativeMovements(game.board, 0, 1, MovementDirection.VERTICAL))
        movements.addAll(iterativeMovements(game.board, -1, 0, MovementDirection.HORIZONTAL))
        movements.addAll(iterativeMovements(game.board, 0, 1, MovementDirection.HORIZONTAL))
        movements.addAll(iterativeMovements(game.board, -1, 0, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(game.board, 0, 1, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(game.board, -1, 0, MovementDirection.INVERSE_DIAGONAL))
        movements.addAll(iterativeMovements(game.board, 0, 1, MovementDirection.INVERSE_DIAGONAL))

        // Castling logic
        if (includeCastling) {
            movements.addAll(kingsideCastlingMovement(game))
            movements.addAll(queensideCastlingMovement(game))
        }

        return movements
    }

    private fun verticalMovement(board: Board): List<Position> {
        val movements = mutableListOf<Position>()
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.VERTICAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.VERTICAL))
        return movements
    }

    private fun horizontalMovement(board: Board): List<Position> {
        val movements = mutableListOf<Position>()
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.HORIZONTAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.HORIZONTAL))
        return movements
    }

    private fun diagonalMovements(board: Board): List<Position> {
        val movements = mutableListOf<Position>()
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.DIAGONAL))
        movements.addAll(iterativeMovements(board, -Board.MAXIMUM_INDEX, -1, MovementDirection.INVERSE_DIAGONAL))
        movements.addAll(iterativeMovements(board, 1, Board.MAXIMUM_INDEX, MovementDirection.INVERSE_DIAGONAL))
        return movements
    }

    private fun iterativeMovements(board: Board, from: Int, to: Int, movementType: MovementDirection): List<Position> {
        val movements = mutableListOf<Position>()

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
                || nextCoordinates.file < Board.MINIMUM_INDEX || nextCoordinates.file > Board.MAXIMUM_INDEX
                || (nextCoordinates.rank == rank && nextCoordinates.file == file)) {
                continue
            }

            val pieceAtNextSquare = board.pieceAt(nextCoordinates.rank, nextCoordinates.file)
            if (pieceAtNextSquare.isEmpty) {
                movements.add(nextCoordinates)
            } else {
                if (pieceAtNextSquare.get().color != color) {
                    movements.add(nextCoordinates)
                }
                break
            }
        }
        return movements
    }

    private fun kingsideCastlingMovement(game: Game): List<Position> {
        if (game.castlingKingsideAllowed[game.currentTurnColor]!!) {
            val mustBeEmptySquares = when(color) {
                Color.WHITE -> listOf(Position(0, 5), Position(0, 6))
                else -> listOf(Position(7, 5), Position(7, 6))
            }

            if (otherPiecesAllowCastle(mustBeEmptySquares, game)) {
                val kingMovesTo = when(color) {
                    Color.WHITE -> Position(0, 6)
                    else -> Position(7, 6)
                }
                return listOf(kingMovesTo)
            }
        }
        return emptyList()
    }

    private fun queensideCastlingMovement(game: Game): List<Position> {
        if (game.castlingQueensideAllowed[game.currentTurnColor]!!) {
            val mustBeEmptySquares = when(color) {
                Color.WHITE -> listOf(Position(0, 1), Position(0, 2), Position(0, 3))
                else -> listOf(Position(7, 1), Position(7, 2), Position(7, 3))
            }

            if (otherPiecesAllowCastle(mustBeEmptySquares, game)) {
                val kingMovesTo = when(color) {
                    Color.WHITE -> Position(0, 2)
                    else -> Position(7, 2)
                }
                return listOf(kingMovesTo)
            }
        }
        return emptyList()
    }

    private fun otherPiecesAllowCastle(mustBeEmptySquares: List<Position>, game: Game): Boolean {
        val intercept = mustBeEmptySquares.find { square -> game.board.pieceAt(square.rank, square.file).isPresent }
        if (intercept == null) {
            val opponentPieces = when(color) {
                Color.WHITE -> game.board.pieces[Color.BLACK]!!
                else -> game.board.pieces[Color.WHITE]!!
            }

            val attackingPiece = mustBeEmptySquares.find { square ->
                opponentPieces.find { piece -> piece.isAttackingSquare(game, square) } != null
            }

            return attackingPiece == null
        }

        return false
    }

    fun isPawnToBePromoted(position: Position): Boolean {
        return type == PieceType.PAWN && (position.rank == Board.MINIMUM_INDEX || position.rank == Board.MAXIMUM_INDEX)
    }
}