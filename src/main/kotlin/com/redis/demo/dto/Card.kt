package com.redis.demo.dto

data class Card(
    val id: String,
    val holderName: String,
    val expirationDate: String,
    val cvv: String
) {
    override fun toString(): String {
        return "Card(id='$id', holderName='$holderName', expirationDate=$expirationDate)"
    }
}
