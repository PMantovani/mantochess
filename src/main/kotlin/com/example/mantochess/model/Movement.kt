package com.example.mantochess.model

import com.example.mantochess.model.pieces.Piece

data class Movement(
    val piece: Piece,
    val from: Long,
    val to: Long,
    val capturedPiece: Piece?,
    val promotionPiece: PieceType?,
    /**
     * This property flags it's not really a legal movement, but a movement that would be legal in case there were no pieces in target square.
     * It's useful for finding out which pieces needs their movements reprocessed.
     * */
    val isMovementBlocked: Boolean,
    val notation: String
)
