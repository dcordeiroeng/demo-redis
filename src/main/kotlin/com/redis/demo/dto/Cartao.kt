package com.redis.demo.dto

data class Cartao(
    val numero: String,
    val nomePortador: String,
    val dataValidade: String,
    val cvv: String
) {
    override fun toString(): String {
        return "Cartao(numero='$numero', nomePortador='$nomePortador', dataValidade=$dataValidade)"
    }
}
