package com.example.eatstedi.model

data class Schedule(
    val id: Int,
    val id_shifts: Int,
    val id_cashiers: Int,
    val day: String,
    val created_at: String,
    val updated_at: String
)