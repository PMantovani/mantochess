package com.example.mantochess.controller

import com.example.mantochess.model.Game
import com.example.mantochess.service.CacheService
import com.example.mantochess.service.GameService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/game")
class GameController(
    private val cacheService: CacheService,
    private val gameService: GameService) {

    @PostMapping("/new")
    fun newGame(): String {
        val game = Game()
        val gameUuid = UUID.randomUUID().toString()
        cacheService.storeGame(gameUuid, game)
        return gameUuid
    }

    @PostMapping("/{id}/{movement}")
    fun postMovement(
        @PathVariable("id") gameUuid: String,
        @PathVariable("movement") movement: String) {
        gameService.processGameMovement(gameUuid, movement)
    }

    @PostMapping("/{id}/suggest")
    fun suggestMovement(
        @PathVariable("id") gameUuid: String): String {
        return gameService.suggestGameMovement(gameUuid)
    }

    @PostMapping("/{id}/suggest-and-play")
    fun suggestMovementAndPlay(
        @PathVariable("id") gameUuid: String): String {
        return gameService.suggestGameMovement(gameUuid, true)
    }
}