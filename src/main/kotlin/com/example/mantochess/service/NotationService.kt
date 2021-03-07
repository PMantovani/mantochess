package com.example.mantochess.service

import com.example.mantochess.model.*
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class NotationService {

    val notationRegex: Pattern = Pattern.compile(
        "(?:(?<piece>[A-z])?(?<originFile>[A-z])?(?<originRank>[0-9])?x?(?<targetFile>[A-z])(?<targetRank>[0-9])(?<promotionPiece>[A-z])?)|(?<castling>(?:O-O(?:-O)?)|(?:0-0(?:-0)?))")

    fun convertNotationToMovement(notation: String, game: Game): Movement {
        val notationMatcher = notationRegex.matcher(notation)

        if (notationMatcher.find()) {
            val pieceType = convertPieceTypeInternal(notationMatcher.group("piece"))
            val originFile = convertFileInternal(notationMatcher.group("originFile"), true)
            val originRank = convertRankInternal(notationMatcher.group("originRank"), true)
            val castling = notationMatcher.group("castling")
            val targetFile = convertFileInternal(notationMatcher.group("targetFile"), castling != null)
            val targetRank = convertRankInternal(notationMatcher.group("targetRank"), castling != null)
            val promotionPiece = getPromotionPieceType(notationMatcher.group("promotionPiece"))

            val movements = game.availableMovementsFor(game.currentTurnColor)

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

                return Movement(
                    movement.piece,
                    movement.from,
                    movement.to,
                    promotionPiece,
                    false,
                    notation)
            }
        } else {
            throw InvalidMovementException("Invalid notation sent")
        }
    }

    fun convertMovementToNotation(movement: Movement): String {
        val piece = convertPieceTypeExternal(movement.piece)
        val targetFile = convertFileExternal(movement.to.file)
        val targetRank = convertRankExternal(movement.to.rank)
        val promotionPiece = convertPieceTypeExternal(movement.promotionPiece)

        return "$piece$targetFile$targetRank$promotionPiece"
    }

    private fun findMatchingMovements(
        allMovements: List<Movement>, pieceType: PieceType, targetPosition: Position, originRank: Int?, originFile: Int?): List<Movement> {

        return allMovements.filter { m ->
            m.piece == pieceType &&
            m.to.rank == targetPosition.rank &&
            m.to.file == targetPosition.file &&
            (m.from.rank == originRank || originRank == null) &&
            (m.from.file == originFile || originFile == null) }
    }

    private fun castling(game: Game, availableMovements: List<Movement>, kingsSide: Boolean): Movement {
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

        val castlingMovement = availableMovements
            .filter { m -> m.piece == PieceType.KING }
            .find { m -> m.to == finalKingPosition }

        if (castlingMovement == null
            || (!game.castlingQueensideAllowed[game.currentTurnColor]!! && !kingsSide)
            || (!game.castlingKingsideAllowed[game.currentTurnColor]!! && kingsSide)) {
            throw InvalidMovementException("Castling not allowed")
        }

        return Movement(
            PieceType.KING,
            castlingMovement.from,
            finalKingPosition,
            null,
            false,
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

    private fun convertPieceTypeInternal(piece: String?): PieceType {
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

    private fun convertPieceTypeExternal(pieceType: PieceType?): String {
        return when(pieceType) {
            PieceType.PAWN -> ""
            PieceType.BISHOP -> "B"
            PieceType.KNIGHT -> "N"
            PieceType.ROOK -> "R"
            PieceType.QUEEN -> "Q"
            PieceType.KING -> "K"
            else -> ""
        }
    }

    fun convertFileInternal(file: String?, allowNull: Boolean): Int? {
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

    private fun convertFileExternal(file: Int): String {
        return when(file) {
            0 -> "a"
            1 -> "b"
            2 -> "c"
            3 -> "d"
            4 -> "e"
            5 -> "f"
            6 -> "g"
            else -> "h"
        }
    }

    fun convertRankInternal(rank: String?, allowNull: Boolean): Int? {
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

    private fun convertRankExternal(rank: Int): String {
        return when(rank) {
            0 -> "1"
            1 -> "2"
            2 -> "3"
            3 -> "4"
            4 -> "5"
            5 -> "6"
            6 -> "7"
            else -> "8"
        }
    }
}