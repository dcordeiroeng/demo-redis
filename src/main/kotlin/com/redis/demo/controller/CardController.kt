package com.redis.demo.controller

import com.redis.demo.dto.Cartao
import com.redis.demo.service.RedisService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.util.StopWatch
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cartoes")
class CartaoController(
    private val redisService: RedisService
) {

    private val logger: Logger = LoggerFactory.getLogger(CartaoController::class.java)

    @PostMapping("/save")
    fun saveCartoes(
        @RequestParam cpf: String,
        @RequestParam canal: String,
        @RequestBody cartoes: List<Cartao>
    ): ResponseEntity<String> {

        val key = "VQ1$cpf#$canal"
        redisService.saveCartoes(key, cpf, cartoes)

        logger.info("Cartões salvos!")

        return ResponseEntity.ok("Cartões salvos com sucesso!")
    }

    @GetMapping("/retrieve")
    fun retrieveCartoes(
        @RequestParam cpf: String,
        @RequestParam canal: String,
    ): ResponseEntity<Any> {

        val key = "VQ1$cpf#$canal"
        val cartoes = redisService.retrieveCartoes(cpf, key)

        if (cartoes != null) {
            redisService.saveCartoes(key, cpf, cartoes)
            logger.info("Cartões retornados: ${cartoes.size}")
            return ResponseEntity.ok(cartoes)
        } else {
            return ResponseEntity.status(404).body("Cartões não encontrados para a chave fornecida.")
        }
    }

    @DeleteMapping("/delete")
    fun deleteCartoes(
        @RequestParam cpf: String
    ): ResponseEntity<Any> {

        val result = redisService.deleteCartoes(cpf)

        if (result) {
            return ResponseEntity.ok("DELETE feito com sucesso")
        } else {
            return ResponseEntity.status(404).body("CPF nao encontrado")
        }
    }
}
