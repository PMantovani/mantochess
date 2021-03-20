package com.example.mantochess.model

data class GameHistoryData(
    val movement: Movement,
    val kingsideCastling: MutableMap<Color, Boolean>,
    val queensideCastling: MutableMap<Color, Boolean>,
    val enPassantTarget: Long?
    )
