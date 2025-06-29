package com.example.eatstedi.model

data class Supplier(
    val id: Int,
    val name: String,
    val username: String,
    val no_telp: String,
    val email: String,
    val alamat: String,
    val income: Int,
    val status: String,
    val profile_picture: String?,
    val created_at: String,
    val updated_at: String
)