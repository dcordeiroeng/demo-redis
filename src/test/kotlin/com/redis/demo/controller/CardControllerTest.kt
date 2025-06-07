package com.redis.demo.controller

import com.redis.demo.dto.Card
import com.redis.demo.service.RedisService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus

class CardControllerTest {

    private val redisService = mock(RedisService::class.java)
    private val controller = CardController(redisService)

    @Test
    fun `saveCards should return ok response`() {
        val cards = listOf(Card(id = "1", holderName = "John Doe", expirationDate = "12/25", cvv = "123"))
        val document = "doc1"
        val channel = "ch1"
        val key = "$document#$channel"

        doNothing().`when`(redisService).saveCards(key, document, cards)

        val response = controller.saveCards(document, channel, cards)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Cards saved successfully!", response.body)
        verify(redisService).saveCards(key, document, cards)
    }

    @Test
    fun `retrieveCards should return cards when found`() {
        val document = "doc1"
        val channel = "ch1"
        val key = "$document#$channel"
        val cards = listOf(Card(id = "1", holderName = "John Doe", expirationDate = "12/25", cvv = "123"))

        `when`(redisService.retrieveCards(document, key)).thenReturn(cards)
        doNothing().`when`(redisService).saveCards(key, document, cards)

        val response = controller.retrieveCards(document, channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(cards, response.body)
        verify(redisService).retrieveCards(document, key)
    }

    @Test
    fun `retrieveCards should return 404 when not found`() {
        val document = "doc1"
        val channel = "ch1"
        val key = "$document#$channel"

        `when`(redisService.retrieveCards(document, key)).thenReturn(null)

        val response = controller.retrieveCards(document, channel)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Not found cards for this document", response.body)
    }

    @Test
    fun `deleteCards should return ok when deleted`() {
        val document = "doc1"
        `when`(redisService.deleteCards(document)).thenReturn(true)

        val response = controller.deleteCards(document)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Cards deleted successfully!", response.body)
    }

    @Test
    fun `deleteCards should return 404 when not found`() {
        val document = "doc1"
        `when`(redisService.deleteCards(document)).thenReturn(false)

        val response = controller.deleteCards(document)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Document not found or no cards to delete", response.body)
    }
}