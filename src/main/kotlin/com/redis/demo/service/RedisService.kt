package com.redis.demo.service

import com.redis.demo.dto.Card
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisService(
    @Qualifier("masterRedisTemplate") private val masterRedisTemplate: RedisTemplate<String, Any>,
    @Qualifier("replicaRedisTemplate") private val replicaRedisTemplate: RedisTemplate<String, Any>
) {

    private val valueOps = masterRedisTemplate.opsForValue()
    private val setOps = masterRedisTemplate.opsForSet()
    private val replicaValueOps = replicaRedisTemplate.opsForValue()
    private val replicaSetOps = replicaRedisTemplate.opsForSet()
    private val ttl: Long = 3 * 60

    fun saveCards(key: String, document: String, cards: List<Card>) {
        masterRedisTemplate.executePipelined {
            valueOps.set(key, cards, ttl, TimeUnit.SECONDS)
            setOps.add(document, key)
            masterRedisTemplate.expire(document, ttl, TimeUnit.SECONDS)
            null
        }
    }

    fun retrieveCards(document: String, key: String): List<Card>? {
        val isMember = replicaSetOps.isMember(document, key)
        if (isMember == true) {
            return replicaValueOps.get(key) as List<Card>
        }
        return null
    }

    fun deleteCards(document: String): Boolean {
        val keysForDocument = setOps.members(document) ?: return false
        masterRedisTemplate.unlink((keysForDocument + document).map { it.toString() })
        return true
    }
}
