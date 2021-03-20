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
        val filledFen = fen ?: FenService.INITIAL_POSITION_FEN
        val game = fenService.convertFenToGame(filledFen)
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

    @PostMapping("/fen/suggest")
    fun suggestFenMovement(
        @RequestParam("fen", defaultValue = FenService.INITIAL_POSITION_FEN) fen: String,
        @RequestParam("max-depth", defaultValue = "3") maxDepth: Int): String {

        val game = fenService.convertFenToGame(fen)
        val gameUuid = UUID.randomUUID().toString()
        cacheService.storeGame(gameUuid, game)

        return gameService.suggestGameMovement(gameUuid, false, maxDepth)
    }

    @PostMapping("/{id}/suggest-and-play")
    fun suggestMovementAndPlay(
        @PathVariable("id") gameUuid: String): String {
        return gameService.suggestGameMovement(gameUuid, true)
    }
}