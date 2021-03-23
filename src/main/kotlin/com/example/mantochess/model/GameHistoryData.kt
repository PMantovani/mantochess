package com.example.mantochess.model

import com.example.mantochess.model.pieces.Piece

data class GameHistoryData(
    val movement: Movement,
    val kingsideCastling: MutableMap<Color, Boolean>,
    val queensideCastling: MutableMap<Color, Boolean>,
    val enPassantTarget: Long?,
    val pseudoLegalMovements: Map<Piece, Long>
    )
