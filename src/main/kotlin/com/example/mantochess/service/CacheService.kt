package com.example.mantochess.service

import com.example.mantochess.model.Game
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CacheService {

    @Cacheable("games")
    fun fetchGame(uuid: String): Game? {
        return null
    }

    @CachePut(value = ["games"], key = "#uuid")
    fun storeGame(uuid: String, game: Game): Game {
        return game
    }
}