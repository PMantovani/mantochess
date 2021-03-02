package com.example.mantochess.controller

import com.example.mantochess.model.InvalidMovementException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(InvalidMovementException::class)
    fun handleRestException(ex: InvalidMovementException, request: WebRequest): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.UNPROCESSABLE_ENTITY)
    }

}