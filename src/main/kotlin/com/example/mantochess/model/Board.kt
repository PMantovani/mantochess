package com.example.mantochess.model

import java.io.Serializable
import java.util.*
import java.util.function.Consumer

class Board: Serializable {
    companion object {
        const val MINIMUM_INDEX = 0
        const val MAXIMUM_INDEX = 7
    }

    val pieces: Map<Color, MutableList<Piece>>

    constructor(copyFromBoard: Board) {
        pieces = EnumMap(Color::class.java)
        pieces[Color.WHITE] = mutableListOf()
        pieces[Color.BLACK] = mutableListOf()
        copyFromBoard.pieces[Color.WHITE]!!.forEach { wp -> pieces[Color.WHITE]!!.add(Piece(wp)) }
        copyFromBoard.pieces[Color.BLACK]!!.forEach { bp -> pieces[Color.BLACK]!!.add(Piece(bp)) }
    }

    constructor() {
        pieces = EnumMap(Color::class.java)
        pieces[Color.WHITE] = mutableListOf(
            Piece(PieceType.PAWN, Color.WHITE, 1, 0),
            Piece(PieceType.PAWN, Color.WHITE, 1, 1),
            Piece(PieceType.PAWN, Color.WHITE, 1, 2),
            Piece(PieceType.PAWN, Color.WHITE, 1, 3),
            Piece(PieceType.PAWN, Color.WHITE, 1, 4),
            Piece(PieceType.PAWN, Color.WHITE, 1, 5),
            Piece(PieceType.PAWN, Color.WHITE, 1, 6),
            Piece(PieceType.PAWN, Color.WHITE, 1, 7),
            Piece(PieceType.ROOK, Color.WHITE, 0, 0),
            Piece(PieceType.KNIGHT, Color.WHITE, 0, 1),
            Piece(PieceType.BISHOP, Color.WHITE, 0, 2),
            Piece(PieceType.QUEEN, Color.WHITE, 0, 3),
            Piece(PieceType.KING, Color.WHITE, 0, 4),
            Piece(PieceType.BISHOP, Color.WHITE, 0, 5),
            Piece(PieceType.KNIGHT, Color.WHITE, 0, 6),
            Piece(PieceType.ROOK, Color.WHITE, 0, 7),
        )

        pieces[Color.BLACK] = mutableListOf(
            Piece(PieceType.PAWN, Color.BLACK, 6, 0),
            Piece(PieceType.PAWN, Color.BLACK, 6, 1),
            Piece(PieceType.PAWN, Color.BLACK, 6, 2),
            Piece(PieceType.PAWN, Color.BLACK, 6, 3),
            Piece(PieceType.PAWN, Color.BLACK, 6, 4),
            Piece(PieceType.PAWN, Color.BLACK, 6, 5),
            Piece(PieceType.PAWN, Color.BLACK, 6, 6),
            Piece(PieceType.PAWN, Color.BLACK, 6, 7),
            Piece(PieceType.ROOK, Color.BLACK, 7, 0),
            Piece(PieceType.KNIGHT, Color.BLACK, 7, 1),
            Piece(PieceType.BISHOP, Color.BLACK, 7, 2),
            Piece(PieceType.QUEEN, Color.BLACK, 7, 3),
            Piece(PieceType.KING, Color.BLACK, 7, 4),
            Piece(PieceType.BISHOP, Color.BLACK, 7, 5),
            Piece(PieceType.KNIGHT, Color.BLACK, 7, 6),
            Piece(PieceType.ROOK, Color.BLACK, 7, 7),
        )
    }

    fun pieceAt(rank: Int, file: Int): Optional<Piece> {
        var pieceAtSquare: Piece? = null

        val checkSquareFun = Consumer<Piece> { piece ->
            if (piece.rank == rank && piece.file == file) {
                pieceAtSquare = piece
            }
        }

        pieces.entries.forEach { p -> p.value.forEach(checkSquareFun) }
        return Optional.ofNullable(pieceAtSquare)
    }

    fun printBoard() {
        println("----------")
        for (rank in 7 downTo 0) {
            print("|")
            for (file in 0..7) {
                val piece = pieceAt(rank, file)
                if (piece.isEmpty) {
                    print(" ")
                } else {
                    val printChar = when(piece.get().type) {
                        PieceType.PAWN -> "p"
                        PieceType.ROOK -> "r"
                        PieceType.KNIGHT -> "n"
                        PieceType.BISHOP -> "b"
                        PieceType.QUEEN -> "q"
                        PieceType.KING -> "k"
                    }
                    print(if (piece.get().color == Color.WHITE) printChar.toUpperCase() else printChar)
                }
            }
            println("|")
        }
        println("----------")
    }

}