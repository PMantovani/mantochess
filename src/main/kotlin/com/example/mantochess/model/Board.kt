package com.example.mantochess.model

import com.example.mantochess.model.pieces.*
import com.example.mantochess.service.PositionHelper
import java.io.Serializable

class Board: Serializable {

    val pieces: MutableSet<Piece> = mutableSetOf()

    var whitePositions: Long = 0xFFFFL
    var blackPositions: Long = 0xFFFFL.shl(8 * 6)

    val piecePositions: HashMap<Long, Piece> = HashMap()

    fun recalculatePiecePositions() {
        piecePositions.clear()
        pieces.forEach { p -> piecePositions[p.position] = p }
    }

    fun pieceAt(position: Long): Piece? {
        return piecePositions[position]
    }

    fun printBoard() {
        println("--------------------------")
        for (rank in 7 downTo 0) {
            print("|")
            for (file in 0..7) {
                val piece = pieceAt(PositionHelper.toLong(rank, file))
                if (piece == null) {
                    print(" . ")
                } else {
                    val printChar = when(piece.type) {
                        PieceType.PAWN -> " p "
                        PieceType.ROOK -> " r "
                        PieceType.KNIGHT -> " n "
                        PieceType.BISHOP -> " b "
                        PieceType.QUEEN -> " q "
                        PieceType.KING -> " k "
                    }
                    print(if (piece.color == Color.WHITE) printChar.toUpperCase() else printChar)
                }
            }
            println("|")
        }
        println("--------------------------")
    }

}