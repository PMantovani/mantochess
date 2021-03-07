package com.example.mantochess.service

import com.example.mantochess.model.*
import org.springframework.stereotype.Service
import java.lang.Integer.parseInt

@Service
class FenService {

    fun convertFenToGame(fen: String): Game {

        val game = Game()
        // Remove all pieces to start
        game.board.pieces[Color.WHITE]!!.removeAll { _ -> true }
        game.board.pieces[Color.BLACK]!!.removeAll { _ -> true }

        val ranks = fen.split("/")

        if (ranks.size != 8) {
            throw InvalidFenException("Fen notation should have exactly 8 slashes (/)")
        }

        for (rankIdx in (ranks.size - 1) downTo Board.MINIMUM_INDEX) {
            var fileIdx = 0

            val rank = ranks[Board.MAXIMUM_INDEX - rankIdx].split(" ")[0]

            for (c in rank.toCharArray()) {
                val pieceType = getPieceType(c)

                if (pieceType == null) {
                    fileIdx += parseInt(c.toString())
                } else {
                    val color = if (c.isUpperCase()) Color.WHITE else Color.BLACK
                    game.board.pieces[color]!!.add(Piece(pieceType, color, rankIdx, fileIdx))

                    fileIdx++
                }
            }

            // Verifies if file idx were specified correctly
            if (fileIdx != Board.MAXIMUM_INDEX + 1) {
                throw InvalidFenException("Wrong number of files for rank ${8 - rankIdx}")
            }
        }

        this.parseGameMetadata(game, ranks[7])

        game.board.pieces.forEach { _, pieces -> pieces.forEach { piece -> piece.reprocessAvailableMovements(game) } }

        println("Parsed FEN board")
        game.board.printBoard()

        return game
    }

    private fun getPieceType(char: Char): PieceType? {
        return when(char.toLowerCase()) {
            'k' -> PieceType.KING
            'q' -> PieceType.QUEEN
            'n' -> PieceType.KNIGHT
            'r' -> PieceType.ROOK
            'b' -> PieceType.BISHOP
            'p' -> PieceType.PAWN
            else -> null
        }
    }

    // Disable unused variables for game metadata we're not yet using
    @SuppressWarnings("kotlin:S1481")
    private fun parseGameMetadata(game: Game, lastRowFen: String) {
        val gameInfo = lastRowFen.split(" ")
        val nextTurnColor = gameInfo[1]
        val castlingAbility = gameInfo[2]
        val enPassetTargetSquare = gameInfo[3]
        val halfmoveClock = gameInfo[4]
        val fullmoveClock = gameInfo[5]

        game.currentTurnColor = when(nextTurnColor) {
            "w" -> Color.WHITE
            "b" -> Color.BLACK
            else -> throw InvalidFenException("Invalid next player $nextTurnColor")
        }

        if (castlingAbility == "-") {
            game.castlingKingsideAllowed[Color.WHITE] = false
            game.castlingKingsideAllowed[Color.BLACK] = false
            game.castlingQueensideAllowed[Color.WHITE] = false
            game.castlingQueensideAllowed[Color.BLACK] = false
        } else {
            for (c in castlingAbility.toCharArray()) {
                if (c.toLowerCase() != 'k' && c.toLowerCase() != 'q') {
                    throw InvalidFenException("Invalid castling ability notation $castlingAbility")
                }
            }

            game.castlingKingsideAllowed[Color.WHITE] = castlingAbility.contains('K')
            game.castlingKingsideAllowed[Color.BLACK] = castlingAbility.contains('k')
            game.castlingQueensideAllowed[Color.WHITE] = castlingAbility.contains('Q')
            game.castlingQueensideAllowed[Color.BLACK] = castlingAbility.contains('q')
        }

    }
}