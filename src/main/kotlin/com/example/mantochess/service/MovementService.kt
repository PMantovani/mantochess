package com.example.mantochess.service

import com.example.mantochess.model.*
import com.example.mantochess.model.pieces.Piece
import com.example.mantochess.model.pieces.PieceMovements

class MovementService {

    companion object {
        fun reprocessAllAvailableMovements(game: Game) {
            game.board.blackPositions = game.board.pieces.fold(0, {acc, piece -> acc.or(if (piece.color == Color.BLACK) piece.position else 0) })
            game.board.whitePositions = game.board.pieces.fold(0, {acc, piece -> acc.or(if (piece.color == Color.WHITE) piece.position else 0) })

            game.board.pieces.filter { piece -> piece.type != PieceType.KING }.forEach { piece -> reprocessAvailableMovements(piece, game) }
            // We need to process kings the last because castling depends on other pieces movements
            game.board.pieces.filter { piece -> piece.type == PieceType.KING }.forEach { king -> reprocessAvailableMovements(king, game) }
        }

        private fun reprocessAvailableMovements(piece: Piece, game: Game) {
            when (piece.type) {
                PieceType.ROOK -> rookMovements(piece, game.board)
                PieceType.QUEEN -> queenMovements(piece, game.board)
                PieceType.PAWN -> pawnMovements(piece, game, game.board)
                PieceType.KNIGHT -> knightMovements(piece, game.board)
                PieceType.KING -> kingMovements(piece, game)
                PieceType.BISHOP -> bishopMovements(piece, game.board)
            }
        }

        private fun kingMovements(piece: Piece, game: Game) {
            piece.emptyBoardMovements = PieceMovements.kingMovements[piece.position]!!.fold(0, { acc, m -> acc.or(m) })

            val ourPieces = if (piece.color == Color.WHITE) game.board.whitePositions else game.board.blackPositions
            val theirPieces = if (piece.color == Color.WHITE) game.board.blackPositions else game.board.whitePositions
            val filterOutMovementsWithSameColorPieceOnTarget = { m: Long -> m.and(ourPieces) == 0x00L }

            val movementsIncludingCastling = mutableListOf<Movement>()
            movementsIncludingCastling.addAll(
                PieceMovements.kingMovements[piece.position]!!
                    .filter(filterOutMovementsWithSameColorPieceOnTarget)
                    .map { target ->
                        var capturedPiece: Piece? = null
                        if (theirPieces.and(target) != 0x00L) {
                            // Only search for captured piece if we know our opponent has a piece there (by anding the target)
                            capturedPiece = game.board.pieceAt(target)
                        }
                        Movement(piece, piece.position, target, capturedPiece, null, false, "")
                    }
            )
            movementsIncludingCastling.addAll(kingsideCastlingMovement(piece, game))
            movementsIncludingCastling.addAll(queensideCastlingMovement(piece, game))
            piece.pseudoLegalMovements = movementsIncludingCastling
        }

        private fun kingsideCastlingMovement(piece: Piece, game: Game): List<Movement> {
            if (game.castlingKingsideAllowed[piece.color]!!) {
                val initialRank = if (piece.color == Color.WHITE) 0 else 7

                val mustBeEmptySquares = listOf(
                    PositionHelper.toLong(initialRank, 5),
                    PositionHelper.toLong(initialRank, 6))
                val mustBeNonAttackedSquares = listOf(
                    PositionHelper.toLong(initialRank, 4),
                    PositionHelper.toLong(initialRank, 5),
                    PositionHelper.toLong(initialRank, 6))

                if (otherPiecesAllowCastle(piece, mustBeEmptySquares, mustBeNonAttackedSquares, game)) {
                    val kingMovesTo = PositionHelper.toLong(initialRank, 6)
                    return listOf(Movement(piece, piece.position, kingMovesTo, null, null, false, "O-O") )
                }
            }
            return emptyList()
        }

        private fun queensideCastlingMovement(piece: Piece, game: Game): List<Movement> {
            if (game.castlingQueensideAllowed[piece.color]!!) {
                val initialRank = if (piece.color == Color.WHITE) 0 else 7

                val mustBeEmptySquares = listOf(
                    PositionHelper.toLong(initialRank, 1),
                    PositionHelper.toLong(initialRank, 2),
                    PositionHelper.toLong(initialRank, 3))
                val mustBeNonAttackedSquares = listOf(
                    PositionHelper.toLong(initialRank, 2),
                    PositionHelper.toLong(initialRank, 3),
                    PositionHelper.toLong(initialRank, 4))

                if (otherPiecesAllowCastle(piece, mustBeEmptySquares, mustBeNonAttackedSquares, game)) {
                    val kingMovesTo = PositionHelper.toLong(initialRank, 2)
                    return listOf(Movement(piece, piece.position, kingMovesTo, null, null, false, "O-O-O") )
                }
            }
            return emptyList()
        }

        private fun otherPiecesAllowCastle(piece: Piece, mustBeEmptySquares: List<Long>, mustBeNonAttackedSquares: List<Long>, game: Game): Boolean {
            val intercept = mustBeEmptySquares.find { square -> game.board.pieceAt(square) != null }
            if (intercept == null) {

                val attackingPiece = mustBeNonAttackedSquares.find { square ->
                    game.board.pieces.find { p -> p.color == piece.opponentColor && p.isAttackingSquare(square) } != null
                }

                return attackingPiece == null
            }

            return false
        }

        private fun knightMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = PieceMovements.knightMovements[piece.position]!!.fold(0, { acc, m -> acc.or(m) })

            val ourPieces = if (piece.color == Color.WHITE) board.whitePositions else board.blackPositions
            val theirPieces = if (piece.color == Color.WHITE) board.blackPositions else board.whitePositions
            val filterOutMovementsWithSameColorPieceOnTarget = { m: Long -> m.and(ourPieces) == 0x00L }

            piece.pseudoLegalMovements = PieceMovements.knightMovements[piece.position]!!
                .filter(filterOutMovementsWithSameColorPieceOnTarget)
                .map { target ->
                    var capturedPiece: Piece? = null
                    if (theirPieces.and(target) != 0x00L) {
                        // Only search for captured piece if we know our opponent has a piece there (by anding the target)
                        capturedPiece = board.pieceAt(target)
                    }
                    Movement(piece, piece.position, target, capturedPiece, null, false, "")
                }
        }

        private fun bishopMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = 0L
            val allMovements = mutableListOf<Movement>()
            allMovements.addAll(positiveRayMovements(piece, PieceMovements.northeastMovements, board))
            allMovements.addAll(positiveRayMovements(piece, PieceMovements.northwestMovements, board))
            allMovements.addAll(negativeRayMovements(piece, PieceMovements.southeastMovements, board))
            allMovements.addAll(negativeRayMovements(piece, PieceMovements.southwestMovements, board))
            piece.pseudoLegalMovements = allMovements
        }

        private fun pawnMovements(piece: Piece, game: Game, board: Board) {
            piece.emptyBoardMovements = PieceMovements.pawnMovements[Pair(piece.color, piece.position)]!!.second.fold(0, { acc, m -> acc.or(m) })

            val ourPieces = if (piece.color == Color.WHITE) board.whitePositions else board.blackPositions
            val theirPieces = if (piece.color == Color.WHITE) board.blackPositions else board.whitePositions
            val allPieces = ourPieces.or(theirPieces)
            val filterOutMovementsWithPieceOnTarget = { m: Long -> m.and(allPieces) == 0x00L }
            val filterOutMovementsWithNoOpponentPieceOnTarget = { m: Long -> m.and(theirPieces) != 0x00L || m == game.enPassantTarget }
            val isLastRank = { target: Long -> if (piece.color == Color.WHITE) target.and(0xFFL.shl(8 * 7)) != 0x00L else target.and(0xFF) != 0x00L }
            val isDoubleMovement = { target: Long -> target - piece.position > 0xFF || piece.position - target > 0xFF }
            val findEnPassantCapturedTarget = { if (game.enPassantTarget?.and(0xFFL.shl(8 * 2)) != 0L) game.enPassantTarget?.shl(8) else game.enPassantTarget?.ushr(8) }
            val containsPieceInNextRank = if (piece.color == Color.WHITE) piece.position.shl(8).and(allPieces) != 0L else piece.position.shr(8).and(allPieces) != 0L
            val filterOutDoubleMovementsWithPiecesInFront = { m: Long -> !(isDoubleMovement(m) && containsPieceInNextRank) }

            val nonCapturingMovements = PieceMovements.pawnMovements[Pair(piece.color, piece.position)]!!.first
                .filter(filterOutMovementsWithPieceOnTarget)
                .filter(filterOutDoubleMovementsWithPiecesInFront)
                .map { target ->
                    Movement(piece, piece.position, target, null, null, false, "")
                }

            val capturingMovements = PieceMovements.pawnMovements[Pair(piece.color, piece.position)]!!.second
                .filter(filterOutMovementsWithNoOpponentPieceOnTarget)
                .map { target ->
                    // Captured piece could be either at target or at the enPassant square
                    val capturedPiece = board.pieceAt(target) ?: board.pieceAt(findEnPassantCapturedTarget()!!)
                    Movement(piece, piece.position, target, capturedPiece, null, false, "" )
                }

            val movementsIncludingPromotions = mutableListOf<Movement>()

            val addToMovementListIncludingPromotion: (m: Movement) -> Unit = { m: Movement ->
                if (isLastRank(m.to)) {
                    movementsIncludingPromotions.add(Movement(m.piece, m.from, m.to, m.capturedPiece, PieceType.QUEEN, false, ""))
                    movementsIncludingPromotions.add(Movement(m.piece, m.from, m.to, m.capturedPiece, PieceType.KNIGHT, false, ""))
                    movementsIncludingPromotions.add(Movement(m.piece, m.from, m.to, m.capturedPiece, PieceType.ROOK, false, ""))
                    movementsIncludingPromotions.add(Movement(m.piece, m.from, m.to, m.capturedPiece, PieceType.BISHOP, false, ""))
                } else {
                    movementsIncludingPromotions.add(m)
                }
            }

            nonCapturingMovements.forEach(addToMovementListIncludingPromotion)
            capturingMovements.forEach(addToMovementListIncludingPromotion)

            piece.pseudoLegalMovements = movementsIncludingPromotions
        }

        private fun queenMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = 0L
            val allMovements = mutableListOf<Movement>()
            allMovements.addAll(positiveRayMovements(piece, PieceMovements.northMovements, board))
            allMovements.addAll(positiveRayMovements(piece, PieceMovements.eastMovements, board))
            allMovements.addAll(negativeRayMovements(piece, PieceMovements.westMovements, board))
            allMovements.addAll(negativeRayMovements(piece, PieceMovements.southMovements, board))
            allMovements.addAll(positiveRayMovements(piece, PieceMovements.northeastMovements, board))
            allMovements.addAll(positiveRayMovements(piece, PieceMovements.northwestMovements, board))
            allMovements.addAll(negativeRayMovements(piece, PieceMovements.southeastMovements, board))
            allMovements.addAll(negativeRayMovements(piece, PieceMovements.southwestMovements, board))
            piece.pseudoLegalMovements = allMovements
        }

        private fun rookMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = 0L
            val pseudoLegalMovements = mutableListOf<Movement>()
            pseudoLegalMovements.addAll(positiveRayMovements(piece, PieceMovements.northMovements, board))
            pseudoLegalMovements.addAll(positiveRayMovements(piece, PieceMovements.eastMovements, board))
            pseudoLegalMovements.addAll(negativeRayMovements(piece, PieceMovements.westMovements, board))
            pseudoLegalMovements.addAll(negativeRayMovements(piece, PieceMovements.southMovements, board))
            piece.pseudoLegalMovements = pseudoLegalMovements
        }

        private fun negativeRayMovements(piece: Piece, movementsMap: HashMap<Long, Long>, board: Board): List<Movement> {
            return rayMovements(piece, movementsMap, board, false)
        }

        private fun positiveRayMovements(piece: Piece, movementsMap: HashMap<Long, Long>, board: Board): List<Movement> {
            return rayMovements(piece, movementsMap, board, true)
        }

        private fun rayMovements(piece: Piece, movementsMap: HashMap<Long, Long>, board: Board, positiveRay: Boolean): List<Movement> {
            val ourPieces = if (piece.color == Color.WHITE) board.whitePositions else board.blackPositions
            val theirPieces = if (piece.color == Color.WHITE) board.blackPositions else board.whitePositions
            val allPieces = board.blackPositions.or(board.whitePositions)

            val rayEmptyBoardMovements = movementsMap[piece.position]!!
            piece.emptyBoardMovements = piece.emptyBoardMovements.or(rayEmptyBoardMovements)

            val intercepts = rayEmptyBoardMovements.and(allPieces)
            val zeroBits = if (positiveRay) intercepts.countTrailingZeroBits() else intercepts.countLeadingZeroBits()
            val hasIntercept = zeroBits != 64
            val firstIntercept = if (positiveRay) 0x01L.shl(zeroBits) else 0x01L.shl(63 - zeroBits)
            val outOfRange = if (hasIntercept) movementsMap[firstIntercept]!! else 0L
            var movements = rayEmptyBoardMovements.xor(outOfRange)

            val formattedMovements = mutableListOf<Movement>()
            while (movements != 0L) {
                val nextMovePosition = 0x01L.shl(movements.countTrailingZeroBits())
                if (!hasIntercept || (nextMovePosition.and(firstIntercept) == 0L || nextMovePosition.and(ourPieces) == 0L)) {
                    val capturedPiece = if (nextMovePosition.and(theirPieces) != 0L) board.pieceAt(firstIntercept) else null
                    formattedMovements.add(Movement(piece, piece.position, nextMovePosition, capturedPiece, null, false, ""))
                }
                movements = movements.and(movements - 1)
            }

            return formattedMovements
        }
    }
}