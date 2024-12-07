package com.example.eatstedi.model

data class MenuItem(
    val id: Int,
    val menuName: String,
    val ownerName: String,
    val category: String,
    val price: String,
    var stock: Int,
    val imageUrl: String // URL atau resource ID untuk gambar
)

