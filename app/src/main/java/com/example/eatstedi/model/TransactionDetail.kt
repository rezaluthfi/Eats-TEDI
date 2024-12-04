package com.example.eatstedi.model

data class TransactionDetail(
    val supplierName: String,
    val time: String,
    val menu: String,
    val price: Int,
    val quantity: Int,
    val employeeName: String // Tambahkan properti ini
) {
    val totalPrice: Int
        get() = price * quantity
}

