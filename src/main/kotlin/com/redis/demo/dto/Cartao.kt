package com.redis.demo.dto

data class Cartao(
    val numero: String, // Número do cartão
    val nomePortador: String, // Nome do titular
    val dataValidade: String, // Data de validade
    val cvv: String // Código de segurança
) {
    override fun toString(): String {
        return "Cartao(numero='$numero', nomePortador='$nomePortador', dataValidade=$dataValidade)"
    }
}
