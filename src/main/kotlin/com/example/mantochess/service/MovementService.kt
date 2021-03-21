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

        fun convertToMovementList(game: Game, piece: Piece): List<Movement> {
            return when (piece.type) {
                PieceType.PAWN -> convertPawnNumberToMovements(game, piece)
                PieceType.KING -> convertKingNumberToMovements(game, piece)
                else -> convertNumberToMovements(game, piece)
            }
        }

        private fun convertNumberToMovements(game: Game, piece: Piece): List<Movement> {
            var movementsNumber = piece.pseudoLegalMovements
            val movements = mutableListOf<Movement>()

            while (movementsNumber != 0L) {
                val nextMovePosition = 0x01L.shl(movementsNumber.countTrailingZeroBits())
                movements.add(Movement(piece, piece.position, nextMovePosition, game.board.pieceAt(nextMovePosition), null, false, ""))
                movementsNumber = movementsNumber.and(movementsNumber - 1)
            }
            return movements
        }

        private fun convertKingNumberToMovements(game: Game, piece: Piece): List<Movement> {
            var movementsNumber = piece.pseudoLegalMovements
            val movements = mutableListOf<Movement>()
            val initialRank = if (piece.color == Color.WHITE) 0 else 7
            val isOriginalPosition = PositionHelper.toLong(initialRank, 4) == piece.position

            while (movementsNumber != 0L) {
                val nextMovePosition = 0x01L.shl(movementsNumber.countTrailingZeroBits())
                if (isOriginalPosition && nextMovePosition == PositionHelper.toLong(initialRank, 2)) {
                    movements.add(Movement(piece, piece.position, nextMovePosition, null, null, false, "O-O-O"))
                } else if (isOriginalPosition && nextMovePosition == PositionHelper.toLong(initialRank, 6)) {
                    movements.add(Movement(piece, piece.position, nextMovePosition, null, null, false, "O-O"))
                } else {
                    movements.add(Movement(piece, piece.position, nextMovePosition, game.board.pieceAt(nextMovePosition), null, false, ""))
                }
                movementsNumber = movementsNumber.and(movementsNumber - 1)
            }
            return movements
        }

        private fun convertPawnNumberToMovements(game: Game, piece: Piece): List<Movement> {
            var movementsNumber = piece.pseudoLegalMovements
            val movements = mutableListOf<Movement>()
            val findEnPassantCapturedTarget = { if (game.enPassantTarget?.and(0xFFL.shl(8 * 2)) != 0L) game.enPassantTarget?.shl(8) else game.enPassantTarget?.ushr(8) }
            val isLastRank = { target: Long -> target.and(0xFFL.shl(8 * 7).or(0xFFL)) != 0x00L }

            while (movementsNumber != 0L) {
                val nextMovePosition = 0x01L.shl(movementsNumber.countTrailingZeroBits())
                val capturedPiece =
                    if (nextMovePosition == game.enPassantTarget) game.board.pieceAt(findEnPassantCapturedTarget() ?: 0)
                    else game.board.pieceAt(nextMovePosition)
                if (isLastRank(nextMovePosition)) {
                    movements.add(Movement(piece, piece.position, nextMovePosition, capturedPiece, PieceType.QUEEN, false, ""))
                    movements.add(Movement(piece, piece.position, nextMovePosition, capturedPiece, PieceType.KNIGHT, false, ""))
                    movements.add(Movement(piece, piece.position, nextMovePosition, capturedPiece, PieceType.BISHOP, false, ""))
                    movements.add(Movement(piece, piece.position, nextMovePosition, capturedPiece, PieceType.ROOK, false, ""))
                } else {
                    movements.add(Movement(piece, piece.position, nextMovePosition, capturedPiece, null, false, ""))
                }
                movementsNumber = movementsNumber.and(movementsNumber - 1)
            }
            return movements
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
            piece.emptyBoardMovements = PieceMovements.kingMovements[piece.position]!!

            val ourPieces = if (piece.color == Color.WHITE) game.board.whitePositions else game.board.blackPositions

            val movements = PieceMovements.kingMovements[piece.position]!!
                .and(ourPieces.inv())
                .or(kingsideCastlingMovement(piece, game))
                .or(queensideCastlingMovement(piece, game))

            piece.pseudoLegalMovements = movements
        }

        private fun kingsideCastlingMovement(piece: Piece, game: Game): Long {
            if (game.castlingKingsideAllowed[piece.color]!!) {
                val initialRank = if (piece.color == Color.WHITE) 0 else 7

                val mustBeEmptySquares = PositionHelper.toLong(initialRank, 5)
                    .or(PositionHelper.toLong(initialRank, 6))
                val mustBeNonAttackedSquares = PositionHelper.toLong(initialRank, 4)
                    .or(PositionHelper.toLong(initialRank, 5))
                    .or(PositionHelper.toLong(initialRank, 6))

                if (otherPiecesAllowCastle(piece, mustBeEmptySquares, mustBeNonAttackedSquares, game)) {
                    return PositionHelper.toLong(initialRank, 6)
                }
            }
            return 0L
        }

        private fun queensideCastlingMovement(piece: Piece, game: Game): Long {
            if (game.castlingQueensideAllowed[piece.color]!!) {
                val initialRank = if (piece.color == Color.WHITE) 0 else 7

                val mustBeEmptySquares = PositionHelper.toLong(initialRank, 1)
                    .or(PositionHelper.toLong(initialRank, 2))
                    .or(PositionHelper.toLong(initialRank, 3))
                val mustBeNonAttackedSquares = PositionHelper.toLong(initialRank, 2)
                    .or(PositionHelper.toLong(initialRank, 3))
                    .or(PositionHelper.toLong(initialRank, 4))

                if (otherPiecesAllowCastle(piece, mustBeEmptySquares, mustBeNonAttackedSquares, game)) {
                    return PositionHelper.toLong(initialRank, 2)
                }
            }
            return 0L
        }

        private fun otherPiecesAllowCastle(piece: Piece, mustBeEmptySquares: Long, mustBeNonAttackedSquares: Long, game: Game): Boolean {
            val intercept = mustBeEmptySquares.and(game.board.whitePositions.or(game.board.blackPositions)) != 0L
            if (!intercept) {

                val attackingPiece = game.board.pieces
                    .find { p -> p.color == piece.opponentColor && p.isAttackingSquares(mustBeNonAttackedSquares) }

                return attackingPiece == null
            }

            return false
        }

        private fun knightMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = PieceMovements.knightMovements[piece.position]!!

            val ourPieces = if (piece.color == Color.WHITE) board.whitePositions else board.blackPositions
            piece.pseudoLegalMovements = PieceMovements.knightMovements[piece.position]!!.and(ourPieces.inv())
        }

        private fun bishopMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = 0L
            piece.pseudoLegalMovements = 0L
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.northeastMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.northwestMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.southeastMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.southwestMovements, board))
        }

        private fun pawnMovements(piece: Piece, game: Game, board: Board) {
            val id = Pair(piece.color, piece.position)
            piece.emptyBoardMovements = PieceMovements.pawnCaptureMovements[id]!!.or(PieceMovements.pawnNonCaptureMovements[id]!!)

            val ourPieces = if (piece.color == Color.WHITE) board.whitePositions else board.blackPositions
            val theirPieces = if (piece.color == Color.WHITE) board.blackPositions else board.whitePositions
            val allPieces = ourPieces.or(theirPieces)

            var nonCapturingMovements = PieceMovements.pawnNonCaptureMovements[id]!!
                .and(allPieces.inv())

            // Checks if there is a piece in front of the pawn that would prevent a double movement
            nonCapturingMovements = if (piece.position.and(0xFFL.shl(8)) != 0L
                && nonCapturingMovements.and(0xFFL.shl(8 * 2)) == 0L) nonCapturingMovements.and(0xFFL.shl(8 * 3).inv()) else nonCapturingMovements

            nonCapturingMovements = if (piece.position.and(0xFFL.shl(8 * 6)) != 0L
                && nonCapturingMovements.and(0xFFL.shl(8 * 5)) == 0L) nonCapturingMovements.and(0xFFL.shl(8 * 4).inv()) else nonCapturingMovements

            val capturingMovements = PieceMovements.pawnCaptureMovements[id]!!.and(theirPieces.or(game.enPassantTarget ?: 0L))

            piece.pseudoLegalMovements = nonCapturingMovements.or(capturingMovements)
        }

        private fun queenMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = 0L
            piece.pseudoLegalMovements = 0L
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.northMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.eastMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.westMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.southMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.northeastMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.northwestMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.southeastMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.southwestMovements, board))
        }

        private fun rookMovements(piece: Piece, board: Board) {
            piece.emptyBoardMovements = 0L
            piece.pseudoLegalMovements = 0L
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.northMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(positiveRayMovements(piece, PieceMovements.eastMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.westMovements, board))
            piece.pseudoLegalMovements = piece.pseudoLegalMovements.or(negativeRayMovements(piece, PieceMovements.southMovements, board))
        }

        private fun negativeRayMovements(piece: Piece, movementsMap: HashMap<Long, Long>, board: Board): Long {
            return rayMovements(piece, movementsMap, board, false)
        }

        private fun positiveRayMovements(piece: Piece, movementsMap: HashMap<Long, Long>, board: Board): Long {
            return rayMovements(piece, movementsMap, board, true)
        }

        private fun rayMovements(piece: Piece, movementsMap: HashMap<Long, Long>, board: Board, positiveRay: Boolean): Long {
            val ourPieces = if (piece.color == Color.WHITE) board.whitePositions else board.blackPositions
            val allPieces = board.blackPositions.or(board.whitePositions)

            val rayEmptyBoardMovements = movementsMap[piece.position]!!
            piece.emptyBoardMovements = piece.emptyBoardMovements.or(rayEmptyBoardMovements)

            val intercepts = rayEmptyBoardMovements.and(allPieces)
            val zeroBits = if (positiveRay) intercepts.countTrailingZeroBits() else intercepts.countLeadingZeroBits()
            val hasIntercept = zeroBits != 64
            val firstIntercept = if (positiveRay) 0x01L.shl(zeroBits) else 0x01L.shl(63 - zeroBits)
            val outOfRange = if (hasIntercept) movementsMap[firstIntercept]!! else 0L
            return rayEmptyBoardMovements.xor(outOfRange).and(ourPieces.inv())
        }
    }
}