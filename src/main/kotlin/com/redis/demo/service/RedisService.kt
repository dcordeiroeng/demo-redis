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
    private val ttl: Long = 3 * 60

    fun saveCartoes(key: String, cpf: String, cartoes: List<Cartao>) {
        masterRedisTemplate.executePipelined {
            valueOps.set(key, cartoes, ttl, TimeUnit.SECONDS)
            setOps.add(cpf, key)
            masterRedisTemplate.expire(cpf, ttl, TimeUnit.SECONDS)
            null
        }
    }

    fun retrieveCartoes(cpf: String, key: String): List<Cartao>? {
        val isMember = replicaSetOps.isMember(cpf, key)
        if (isMember == true) {
            return replicaValueOps.get(key) as List<Cartao>?
        }
        return null
    }

    fun deleteCartoes(cpf: String): Boolean {
        val keysForCpf = setOps.members(cpf)
        if (keysForCpf.isNullOrEmpty()) {
            return false
        }
        val allKeys = (keysForCpf + cpf).map { it.toString() }
        masterRedisTemplate.unlink(allKeys)
        return true
    }
}
