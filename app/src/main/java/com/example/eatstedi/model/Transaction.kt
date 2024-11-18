package com.example.eatstedi.model

data class Transaction(
    val employeeName: String,
    val menu: String,
    val supplier: String,
    val price: Int,
    val quantity: Int,
    val totalPrice: Int
)
