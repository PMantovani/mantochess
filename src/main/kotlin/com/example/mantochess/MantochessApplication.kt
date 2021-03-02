package com.example.mantochess

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class MantochessApplication

fun main(args: Array<String>) {
	runApplication<MantochessApplication>(*args)
}
