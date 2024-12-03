package com.redis.demo.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    @Primary
    fun masterRedisConnectionFactory(): LettuceConnectionFactory {
        val configuration = RedisStandaloneConfiguration("127.0.0.1", 6379)
        return LettuceConnectionFactory(configuration)
    }

    @Bean
    fun replicaRedisConnectionFactory(): LettuceConnectionFactory {
        val configuration = RedisStandaloneConfiguration("127.0.0.1", 6380)
        return LettuceConnectionFactory(configuration)
    }

    @Bean(name = ["masterRedisTemplate"])
    @Primary
    fun masterRedisTemplate(
        @Qualifier("masterRedisConnectionFactory") connectionFactory: LettuceConnectionFactory
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = Jackson2JsonRedisSerializer(Any::class.java)
        return template
    }

    @Bean(name = ["replicaRedisTemplate"])
    fun replicaRedisTemplate(
        @Qualifier("replicaRedisConnectionFactory") connectionFactory: LettuceConnectionFactory
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = Jackson2JsonRedisSerializer(Any::class.java)
        return template
    }
}
