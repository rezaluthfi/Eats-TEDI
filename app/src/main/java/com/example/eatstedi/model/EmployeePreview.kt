package com.example.eatstedi.model

data class EmployeePreview(
    val name: String,
    val imageUrl: String? // Ganti imageResId dengan imageUrl untuk mendukung URL dari API
)