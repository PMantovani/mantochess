package com.example.mantochess.model.pieces

import com.example.mantochess.model.Color
import com.example.mantochess.service.PositionHelper

class PieceMovements {

    companion object {
        val northMovements: HashMap<Long, Long> = HashMap()
        val northeastMovements: HashMap<Long, Long> = HashMap()
        val northwestMovements: HashMap<Long, Long> = HashMap()
        val eastMovements: HashMap<Long, Long> = HashMap()
        val westMovements: HashMap<Long, Long> = HashMap()
        val southeastMovements: HashMap<Long, Long> = HashMap()
        val southwestMovements: HashMap<Long, Long> = HashMap()
        val southMovements: HashMap<Long, Long> = HashMap()
        val knightMovements: HashMap<Long, ArrayList<Long>> = HashMap()
        // The first in the pair are non-capture movements, and the second are capture movements
        val pawnMovements: HashMap<Pair<Color, Long>, Pair<ArrayList<Long>, ArrayList<Long>>> = HashMap()
        val kingMovements: HashMap<Long, ArrayList<Long>> = HashMap()

        init {
            for (rank in 0..7) {
                for (file in 0..7) {
                    val position = PositionHelper.toLong(rank, file)

                    computeNorthMovements(position, rank, file)
                    computeNortheastMovements(position, rank, file)
                    computeNorthwestMovements(position, rank, file)
                    computeEastMovements(position, rank, file)
                    computeWestMovements(position, rank, file)
                    computeSouthMovements(position, rank, file)
                    computeSoutheastMovements(position, rank, file)
                    computeSouthwestMovements(position, rank, file)
                    computeKnightMovements(rank, file)
                    computePawnMovements(position, rank, file)
                    computeKingMovements(position, rank, file)
                }
            }

        }

        private fun computeKingMovements(position: Long, rank: Int, file: Int) {
            kingMovements[position] = ArrayList()

            if (file > 0) kingMovements[position]!!.add(PositionHelper.toLong(rank, file - 1))
            if (file < 7) kingMovements[position]!!.add(PositionHelper.toLong(rank, file + 1))

            if (rank > 0) {
                kingMovements[position]!!.add(PositionHelper.toLong(rank - 1, file))
                if (file > 0) kingMovements[position]!!.add(PositionHelper.toLong(rank - 1, file - 1))
                if (file < 7) kingMovements[position]!!.add(PositionHelper.toLong(rank - 1, file + 1))
            }

            if (rank < 7) {
                kingMovements[position]!!.add(PositionHelper.toLong(rank + 1, file))
                if (file > 0) kingMovements[position]!!.add(PositionHelper.toLong(rank + 1, file - 1))
                if (file < 7) kingMovements[position]!!.add(PositionHelper.toLong(rank + 1, file + 1))
            }
        }

        private fun computePawnMovements(position: Long, rank: Int, file: Int) {
            for (color in listOf(Color.WHITE, Color.BLACK)) {
                val directionMultiplier = if (color == Color.WHITE) 1 else -1
                val initialRank = if (color == Color.WHITE) 1 else 6
                pawnMovements[Pair(color, position)] = Pair(ArrayList(), ArrayList())

                // Simple forward movement
                pawnMovements[Pair(color, position)]!!.first.add(0x01L.shl(file + 8 * (rank + (1 * directionMultiplier))))
                if (rank == initialRank) {
                    // Double movement
                    pawnMovements[Pair(color, position)]!!.first.add(0x01L.shl(file + 8 * (rank + (2 * directionMultiplier))))
                }
                if (file > 0) {
                    // Add left-capturing diagonal
                    pawnMovements[Pair(color, position)]!!.second.add(0x01L.shl((file - 1) + 8 * (rank + (1 * directionMultiplier))))
                }
                if (file < 7) {
                    // Add right-capturing diagonal
                    pawnMovements[Pair(color, position)]!!.second.add(0x01L.shl((file + 1) + 8 * (rank + (1 * directionMultiplier))))
                }
            }
        }

        private fun computeNorthMovements(position: Long, startingRank: Int, startingFile: Int) {
            var northMovesInPosition = 0L
            for (attackingRank in (startingRank + 1)..7) {
                northMovesInPosition = northMovesInPosition.or(PositionHelper.toLong(attackingRank, startingFile))
            }
            northMovements[position] = northMovesInPosition
        }

        private fun computeNortheastMovements(position: Long, startingRank: Int, startingFile: Int) {
            var northeastMovesInPosition = 0L
            for (diagonalInc in 1..7) {
                if (startingRank + diagonalInc > 7 || startingFile + diagonalInc > 7) {
                    break
                }
                northeastMovesInPosition = northeastMovesInPosition.or(PositionHelper.toLong(startingRank + diagonalInc, startingFile + diagonalInc))
            }
            northeastMovements[position] = northeastMovesInPosition
        }

        private fun computeNorthwestMovements(position: Long, startingRank: Int, startingFile: Int) {
            var northwestMovesInPosition = 0L
            for (diagonalInc in 1..7) {
                if (startingRank + diagonalInc > 7 || startingFile - diagonalInc < 0) {
                    break
                }
                northwestMovesInPosition = northwestMovesInPosition.or(PositionHelper.toLong(startingRank + diagonalInc, startingFile - diagonalInc))
            }
            northwestMovements[position] = northwestMovesInPosition
        }

        private fun computeEastMovements(position: Long, startingRank: Int, startingFile: Int) {
            var eastMovesInPosition = 0L
            for (attackingFile in (startingFile + 1)..7) {
                eastMovesInPosition = eastMovesInPosition.or(PositionHelper.toLong(startingRank, attackingFile))
            }
            eastMovements[position] = eastMovesInPosition
        }

        private fun computeWestMovements(position: Long, startingRank: Int, startingFile: Int) {
            var westMovesInPosition = 0L
            for (attackingFile in (startingFile - 1) downTo 0) {
                westMovesInPosition = westMovesInPosition.or(PositionHelper.toLong(startingRank, attackingFile))
            }
            westMovements[position] = westMovesInPosition
        }

        private fun computeSouthMovements(position: Long, startingRank: Int, startingFile: Int) {
            var southMovesInPosition = 0L
            for (attackingRank in (startingRank - 1) downTo 0) {
                southMovesInPosition = southMovesInPosition.or(PositionHelper.toLong(attackingRank, startingFile))
            }
            southMovements[position] = southMovesInPosition
        }

        private fun computeSoutheastMovements(position: Long, startingRank: Int, startingFile: Int) {
            var southeastMovesInPosition = 0L
            for (diagonalInc in 1..7) {
                if (startingRank - diagonalInc < 0 || startingFile + diagonalInc > 7) {
                    break
                }
                southeastMovesInPosition =
                    southeastMovesInPosition.or(PositionHelper.toLong(startingRank - diagonalInc, startingFile + diagonalInc))
            }
            southeastMovements[position] = southeastMovesInPosition
        }

        private fun computeSouthwestMovements(position: Long, startingRank: Int, startingFile: Int) {
            var southwestMovesInPosition = 0L
            for (diagonalInc in 1..7) {
                if (startingRank - diagonalInc < 0 || startingFile - diagonalInc < 0) {
                    break
                }
                southwestMovesInPosition =
                    southwestMovesInPosition.or(PositionHelper.toLong(startingRank - diagonalInc, startingFile - diagonalInc))
            }
            southwestMovements[position] = southwestMovesInPosition
        }

        private fun computeKnightMovements(rank: Int, file: Int) {
            val knightPosition = PositionHelper.toLong(rank, file)
            knightMovements[knightPosition] = ArrayList()
            if (rank > 1) {
                if (file > 0) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank - 2, file - 1))
                }
                if (file < 7) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank - 2, file + 1))
                }
            }
            if (rank > 0) {
                if (file > 1) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank - 1, file - 2))
                }
                if (file < 6) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank - 1, file + 2))
                }
            }
            if (rank < 6) {
                if (file > 0) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank + 2, file - 1))
                }
                if (file < 7) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank + 2, file + 1))
                }
            }
            if (rank < 7) {
                if (file > 1) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank + 1, file - 2))
                }
                if (file < 6) {
                    knightMovements[knightPosition]!!.add(PositionHelper.toLong(rank + 1, file + 2))
                }
            }
        }
    }

}