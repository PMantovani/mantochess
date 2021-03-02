package com.example.mantochess.model

data class CompleteMovementInfo(
    val piece: PieceType,
    val from: Position,
    val to: Position,
    val movementType: String?,
    val promotionPiece: PieceType?,
    val notation: String
)
