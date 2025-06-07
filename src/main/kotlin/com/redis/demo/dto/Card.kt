package com.redis.demo.dto

data class Card(
    val id: String,
    val holderName: String,
    val expirationDate: String,
    val cvv: String
)