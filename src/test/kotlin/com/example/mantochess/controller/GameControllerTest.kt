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

    val pattern: Pattern = Pattern.compile("Total nodes: (\\d+)")

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

    @Test
    fun testPerftPosition2() {
        val initialPosition = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        val result = gameController.suggestFenMovement(initialPosition, 4)
        val matcher = pattern.matcher(result)
        matcher.find()
        assertEquals("4085603", matcher.group(1))
    }

    @Test
    fun testPerftPosition3() {
        val initialPosition = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
        val result = gameController.suggestFenMovement(initialPosition, 5)
        val matcher = pattern.matcher(result)
        matcher.find()
        assertEquals("674624", matcher.group(1))
    }

    @Test
    fun testPerftPosition4() {
        val initialPosition = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
        val result = gameController.suggestFenMovement(initialPosition, 4)
        val matcher = pattern.matcher(result)
        matcher.find()
        assertEquals("422333", matcher.group(1))
    }

    @Test
    fun testPerftPosition5() {
        val initialPosition = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
        val result = gameController.suggestFenMovement(initialPosition, 4)
        val matcher = pattern.matcher(result)
        matcher.find()
        assertEquals("2103487", matcher.group(1))
    }

    @Test
    fun testPerftPosition6() {
        val initialPosition = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
        val result = gameController.suggestFenMovement(initialPosition, 4)
        val matcher = pattern.matcher(result)
        matcher.find()
        assertEquals("3894594", matcher.group(1))
    }
}