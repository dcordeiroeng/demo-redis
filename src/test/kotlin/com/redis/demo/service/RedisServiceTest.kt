package com.redis.demo.service

import com.redis.demo.dto.Card
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ValueOperations
import java.util.concurrent.TimeUnit

class RedisServiceTest {

    private lateinit var masterRedisTemplate: RedisTemplate<String, Any>
    private lateinit var replicaRedisTemplate: RedisTemplate<String, Any>
    private lateinit var valueOps: ValueOperations<String, Any>
    private lateinit var setOps: SetOperations<String, Any>
    private lateinit var replicaValueOps: ValueOperations<String, Any>
    private lateinit var replicaSetOps: SetOperations<String, Any>
    private lateinit var redisService: RedisService

    @BeforeEach
    fun setUp() {
        masterRedisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, Any>
        replicaRedisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, Any>
        valueOps = mock(ValueOperations::class.java) as ValueOperations<String, Any>
        setOps = mock(SetOperations::class.java) as SetOperations<String, Any>
        replicaValueOps = mock(ValueOperations::class.java) as ValueOperations<String, Any>
        replicaSetOps = mock(SetOperations::class.java) as SetOperations<String, Any>

        `when`(masterRedisTemplate.opsForValue()).thenReturn(valueOps)
        `when`(masterRedisTemplate.opsForSet()).thenReturn(setOps)
        `when`(replicaRedisTemplate.opsForValue()).thenReturn(replicaValueOps)
        `when`(replicaRedisTemplate.opsForSet()).thenReturn(replicaSetOps)

        redisService = RedisService(masterRedisTemplate, replicaRedisTemplate)
    }

    @Test
    fun `saveCards should set value, add to set, and expire document`() {
        val key = "testKey"
        val document = "testDoc"
        val cards = listOf<Card>(mock(Card::class.java))
        val ttl = 3 * 60L

        `when`(masterRedisTemplate.executePipelined(any<org.springframework.data.redis.core.RedisCallback<Any>>()))
            .thenAnswer { invocation ->
                val callback = invocation.arguments[0] as org.springframework.data.redis.core.RedisCallback<Any>
                callback.doInRedis(mock(org.springframework.data.redis.connection.RedisConnection::class.java))
                null
            }

        redisService.saveCards(key, document, cards)

        verify(valueOps).set(key, cards, ttl, TimeUnit.SECONDS)
        verify(setOps).add(document, key)
        verify(masterRedisTemplate).expire(document, ttl, TimeUnit.SECONDS)
    }

    @Test
    fun `retrieveCartoes should return cartoes when key is member`() {
        val cpf = "cpf"
        val key = "key"
        val cartoes = listOf<Card>(mock(Card::class.java))

        `when`(replicaSetOps.isMember(cpf, key)).thenReturn(true)
        `when`(replicaValueOps.get(key)).thenReturn(cartoes)

        val result = redisService.retrieveCards(cpf, key)
        assertEquals(cartoes, result)
    }

    @Test
    fun `retrieveCartoes should return null when key is not member`() {
        val cpf = "cpf"
        val key = "key"

        `when`(replicaSetOps.isMember(cpf, key)).thenReturn(false)

        val result = redisService.retrieveCards(cpf, key)
        assertNull(result)
    }

    @Test
    fun `deleteCartoes should return false when no keys`() {
        val cpf = "cpf"
        `when`(setOps.members(cpf)).thenReturn(null)

        val result = redisService.deleteCards(cpf)
        assertFalse(result)
    }

    @Test
    fun `deleteCartoes should unlink keys and return true`() {
        val cpf = "cpf"
        val keys = setOf("key1", "key2")
        `when`(setOps.members(cpf)).thenReturn(keys)

        val result = redisService.deleteCards(cpf)
        verify(masterRedisTemplate).unlink(listOf("key1", "key2", cpf))
        assertTrue(result)
    }
}