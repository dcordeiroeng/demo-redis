package com.redis.demo.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.data.redis.RedisConnectionFailureException

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RedisConnectionFailureException::class)
    fun handleRedisConnectionFailure(ex: RedisConnectionFailureException): ResponseEntity<Map<String, String>> {
        logger.error("Redis connection failure: ${ex.message}")
        val body = mapOf("error" to "Service temporarily unavailable. Please try again later.")
        return ResponseEntity(body, HttpStatus.SERVICE_UNAVAILABLE)
    }
}