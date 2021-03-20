package com.example.mantochess.service

import com.example.mantochess.model.PieceType
import com.example.mantochess.model.Position

class PositionHelper {

    companion object {

        fun toAlgebraicNotation(position: Long, promotionPiece: PieceType? = null): String {
            val rank = findPositionRank(position)
            val file = findPositionFile(position)

            val promotionPieceStr = when (promotionPiece) {
                PieceType.QUEEN -> "q"
                PieceType.KNIGHT -> "n"
                PieceType.ROOK -> "r"
                PieceType.BISHOP -> "b"
                else -> ""
            }

            return "${file}${rank}${ promotionPieceStr }"
        }

        fun toLong(rank: Int, file: Int): Long {
            return 0x01L.shl((rank * 8) + file)
        }

        fun toPair(position: Long): Position {
            val rank = findPositionRank(position) - 1
            val file = findPositionFile(position)[0].minus('a')
            return Position(rank, file)
        }

        fun printPositionInBoard(position: Long) {
            println("--------------------------")
            for (rank in 7 downTo 0) {
                print("|")
                for (file in 0..7) {
                    val piece = toLong(rank, file).and(position)
                    if (piece == 0L) {
                        print(" . ")
                    } else {
                        print(" 1 ")
                    }
                }
                println("|")
            }
            println("--------------------------")
        }

        private fun findPositionRank(position: Long): Int {
            return when {
                position.and(0xFFL) != 0L -> 1
                position.and(0xFFL.shl(8)) != 0L -> 2
                position.and(0xFFL.shl(8 * 2)) != 0L -> 3
                position.and(0xFFL.shl(8 * 3)) != 0L -> 4
                position.and(0xFFL.shl(8 * 4)) != 0L -> 5
                position.and(0xFFL.shl(8 * 5)) != 0L -> 6
                position.and(0xFFL.shl(8 * 6)) != 0L -> 7
                else -> 8
            }
        }

        private fun findPositionFile(position: Long): String {
            return when {
                position.and(0x01_01_01_01_01_01_01_01L) != 0L -> "a"
                position.and(0x02_02_02_02_02_02_02_02L) != 0L -> "b"
                position.and(0x04_04_04_04_04_04_04_04L) != 0L -> "c"
                position.and(0x08_08_08_08_08_08_08_08L) != 0L -> "d"
                position.and(0x10_10_10_10_10_10_10_10L) != 0L -> "e"
                position.and(0x20_20_20_20_20_20_20_20L) != 0L -> "f"
                position.and(0x40_40_40_40_40_40_40_40L) != 0L -> "g"
                else -> "h"
            }
        }

    }
}