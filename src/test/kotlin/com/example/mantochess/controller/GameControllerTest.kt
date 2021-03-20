package com.example.mantochess.controller

import com.example.mantochess.service.CacheService
import com.example.mantochess.service.FenService
import com.example.mantochess.service.GameService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.regex.Pattern


@SpringBootTest
@Import(GameService::class, FenService::class, CacheService::class)
internal class GameControllerTest {

    val pattern = Pattern.compile("Total nodes: (\\d+)")

    @Autowired
    lateinit var gameController: GameController

    @Test
    fun testPerftInitialPosition() {
        val initialPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val result = gameController.suggestFenMovement(initialPosition, 5)
        val matcher = pattern.matcher(result)
        matcher.find()
        assertEquals("4865609", matcher.group(1))
    }
}