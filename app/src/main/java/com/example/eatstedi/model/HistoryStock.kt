package com.example.eatstedi.model

data class HistoryStock(
    val name: String,
    val updateDate: String,
    val updateTime: String,
    val stockBefore: Int,
    val stockAfter: Int
)
