package com.example.eatstedi.model

data class Transaction(
    val employeeName: String,
    val date: String,
    val paymentType: String,
    val totalPrice: Int,
    val change: Int
)
