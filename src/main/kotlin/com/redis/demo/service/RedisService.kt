package com.redis.demo.service

import com.redis.demo.dto.Cartao
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
    private val TTL: Long = 15 * 60

    fun saveCartoes(key: String, cpf: String, cartoes: List<Cartao>) {
        masterRedisTemplate.executePipelined {
            valueOps.set(key, cartoes)
            masterRedisTemplate.expire(key, TTL, TimeUnit.SECONDS)
            setOps.add(cpf, key)
            masterRedisTemplate.expire(cpf, TTL, TimeUnit.SECONDS)
            null
        }
    }

    fun retrieveCartoes(cpf: String, key: String): List<Cartao>? {
        val keysForCpf = replicaSetOps.members(cpf)
        if (keysForCpf.isNullOrEmpty()) {
            return null
        }
        if (keysForCpf.contains(key)) {
            @Suppress("UNCHECKED_CAST")
            return replicaValueOps.get(key) as List<Cartao>?
        }
        return null
    }

    fun deleteCartoes(cpf: String) : Boolean {
        val keysForCpf = setOps.members(cpf)
        if (keysForCpf.isNullOrEmpty()) {
            return false
        }
        masterRedisTemplate.executePipelined { connection ->
            val redisSerializer = masterRedisTemplate.stringSerializer
            keysForCpf.forEach { key ->
                val redisKey = redisSerializer.serialize(key.toString())
                if (redisKey != null) {
                    connection.del(redisKey)
                }
            }
            val cpfKey = redisSerializer.serialize(cpf)
            if (cpfKey != null) {
                connection.del(cpfKey)
            }
            null
        }
        return true
    }
}
