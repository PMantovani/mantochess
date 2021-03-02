package com.example.mantochess.service

import com.example.mantochess.model.Game
import org.springframework.stereotype.Service
import java.security.InvalidParameterException

@Service
class GameService(
    private val notationService: NotationService,
    private val cacheService: CacheService) {

    fun processGameMovement(gameUuid: String, notation: String): Game {
        val game = cacheService.fetchGame(gameUuid) ?: throw InvalidParameterException("Invalid game uuid")
        val movement = notationService.convertNotationToMovement(notation, game)

        val gameInNextMove = Game(game)
        gameInNextMove.makeMovement(movement)

        cacheService.storeGame(gameUuid, gameInNextMove)

        return gameInNextMove
    }
}