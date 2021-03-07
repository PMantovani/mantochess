package com.example.mantochess.controller

import com.example.mantochess.model.Game
import com.example.mantochess.service.CacheService
import com.example.mantochess.service.FenService
import com.example.mantochess.service.GameService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/game")
class GameController(
    private val cacheService: CacheService,
    private val gameService: GameService,
    private val fenService: FenService) {

    @PostMapping("/new")
    fun newGame(@RequestBody fen: String?): String {
        val game = if (fen != null) fenService.convertFenToGame(fen) else Game()
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
        @PathVariable("id") gameUuid: String,
        @RequestParam("max-depth", defaultValue = "3") maxDepth: Int): String {
        return gameService.suggestGameMovement(gameUuid, false, maxDepth)
    }

    @PostMapping("/{id}/suggest-and-play")
    fun suggestMovementAndPlay(
        @PathVariable("id") gameUuid: String): String {
        return gameService.suggestGameMovement(gameUuid, true)
    }
}