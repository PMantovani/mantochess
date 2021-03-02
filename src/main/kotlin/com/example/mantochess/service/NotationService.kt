package com.example.mantochess.service

import com.example.mantochess.model.*
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class NotationService {

    val notationRegex: Pattern = Pattern.compile(
        "(?:(?<piece>[A-z])?(?<originFile>[A-z])?(?<originRank>[0-9])?x?(?<targetFile>[A-z])(?<targetRank>[0-9])(?<promotionPiece>[A-z])?)|(?<castling>(?:O-O(?:-O)?)|(?:0-0(?:-0)?))")

    fun convertNotationToMovement(notation: String, game: Game): CompleteMovementInfo {
        val notationMatcher = notationRegex.matcher(notation)

        if (notationMatcher.find()) {
            val pieceType = getPieceType(notationMatcher.group("piece"))
            val originFile = convertFile(notationMatcher.group("originFile"), true)
            val originRank = convertRank(notationMatcher.group("originRank"), true)
            val castling = notationMatcher.group("castling")
            val targetFile = convertFile(notationMatcher.group("targetFile"), castling != null)
            val targetRank = convertRank(notationMatcher.group("targetRank"), castling != null)
            val promotionPiece = getPromotionPieceType(notationMatcher.group("promotionPiece"))

            val movements = game.availableMovementFor(game.currentTurnColor)

            if (castling == "O-O-O" || castling == "0-0-0") {
                return castling(game, movements, false)
            } else if (castling == "O-O" || castling == "0-0") {
                return castling(game, movements, true)
            } else {
                val matchingMovements = findMatchingMovements(movements, pieceType, Position(targetRank!!, targetFile!!), originRank, originFile)

                if (matchingMovements.size > 1) {
                    throw InvalidMovementException("More than one matching movement for $notation")
                }

                if (matchingMovements.isEmpty()) {
                    throw InvalidMovementException("No movements match the notation $notation")
                }

                val movement = matchingMovements[0]

                return CompleteMovementInfo(
                    movement.first.type,
                    Position(movement.first.rank, movement.first.file),
                    movement.second,
                    null,
                    promotionPiece,
                    notation)
            }
        } else {
            throw InvalidMovementException("Invalid notation sent")
        }
    }

    private fun findMatchingMovements(
        allMovements: List<Pair<Piece, Position>>, pieceType: PieceType, targetPosition: Position, originRank: Int?, originFile: Int?): List<Pair<Piece, Position>> {

        return allMovements.filter { m ->
            m.first.type == pieceType &&
                    m.second.rank == targetPosition.rank &&
                    m.second.file == targetPosition.file &&
                    (m.first.rank == originRank || originRank == null) &&
                    (m.first.file == originFile || originFile == null) }
    }

    private fun castling(game: Game, availableMovements: List<Pair<Piece, Position>>, kingsSide: Boolean): CompleteMovementInfo {
        val finalKingPosition =
            if (kingsSide)
                when(game.currentTurnColor) {
                    Color.WHITE -> Position(0, 6)
                    else -> Position(7, 6)
                }
            else when(game.currentTurnColor) {
                    Color.WHITE -> Position(0, 2)
                    else -> Position(7, 2)
                }

        val king =  availableMovements
            .find { m -> m.first.type == PieceType.KING }!!.first

        val castlingMovement = availableMovements
            .filter { m -> m.first == king }
            .find { m -> m.second == finalKingPosition }

        if (castlingMovement == null
            || (!game.castlingQueensideAllowed[game.currentTurnColor]!! && !kingsSide)
            || (!game.castlingKingsideAllowed[game.currentTurnColor]!! && kingsSide)) {
            throw InvalidMovementException("Castling not allowed")
        }

        return CompleteMovementInfo(
            PieceType.KING,
            Position(king.rank, king.file),
            finalKingPosition,
            if (kingsSide) "King's side castling" else "Queen's side castling",
            null,
            if (kingsSide) "O-O" else "O-O-O",
        )
    }

    private fun getPromotionPieceType(piece: String?): PieceType? {
        return when(piece) {
            "B" -> PieceType.BISHOP
            "N" -> PieceType.KNIGHT
            "R" -> PieceType.ROOK
            "Q" -> PieceType.QUEEN
            null -> null
            else -> throw InvalidMovementException("Invalid promotion piece $piece")
        }
    }

    private fun getPieceType(piece: String?): PieceType {
        return when(piece) {
            null -> PieceType.PAWN
            "B" -> PieceType.BISHOP
            "N" -> PieceType.KNIGHT
            "R" -> PieceType.ROOK
            "Q" -> PieceType.QUEEN
            "K" -> PieceType.KING
            else -> throw InvalidMovementException("Unknown piece $piece")
        }
    }

    private fun convertFile(file: String?, allowNull: Boolean): Int? {
        return when(file) {
            "a" -> 0
            "b" -> 1
            "c" -> 2
            "d" -> 3
            "e" -> 4
            "f" -> 5
            "g" -> 6
            "h" -> 7
            null -> if (allowNull) null else throw InvalidMovementException("File position is mandatory")
            else -> throw InvalidMovementException("Invalid file $file")
        }
    }

    private fun convertRank(rank: String?, allowNull: Boolean): Int? {
        return when(rank) {
            "1" -> 0
            "2" -> 1
            "3" -> 2
            "4" -> 3
            "5" -> 4
            "6" -> 5
            "7" -> 6
            "8" -> 7
            null -> if (allowNull) null else throw InvalidMovementException("Rank position is mandatory")
            else -> throw InvalidMovementException("Invalid file $rank")
        }
    }
}