package com.redis.demo.controller

import com.redis.demo.dto.Card
import com.redis.demo.service.RedisService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cards")
class CardController(private val redisService: RedisService) {

    private val logger = LoggerFactory.getLogger(CardController::class.java)

    @PostMapping("/save")
    fun saveCards(
        @RequestParam document: String,
        @RequestParam channel: String,
        @RequestBody cards: List<Card>
    ): ResponseEntity<String> {
        val key = "$document#$channel"
        redisService.saveCards(key, document, cards)
        logger.info("Cards saved for document: $document, channel: $channel, key: $key")
        return ResponseEntity.ok("Cards saved successfully!")
    }

    @GetMapping("/retrieve")
    fun retrieveCards(
        @RequestParam document: String,
        @RequestParam channel: String
    ): ResponseEntity<Any> {
        val key = "$document#$channel"
        val cards = redisService.retrieveCards(document, key)
        return if (cards != null) {
            redisService.saveCards(key, document, cards)
            logger.info("Cards returned: ${cards.size} for document: $document, channel: $channel, key: $key")
            ResponseEntity.ok(cards)
        } else {
            logger.info("Not found cards for document: $document, channel: $channel, key: $key")
            ResponseEntity.status(404).body("Not found cards for this document")
        }
    }

    @DeleteMapping("/delete")
    fun deleteCards(@RequestParam document: String): ResponseEntity<Any> {
        return if (redisService.deleteCards(document)) {
            logger.info("Cards deleted for document: $document")
            ResponseEntity.ok("Cards deleted successfully!")
        } else {
            logger.info("Not found cards for document: $document")
            ResponseEntity.status(404).body("Document not found or no cards to delete")
        }
    }
}