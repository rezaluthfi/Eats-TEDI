package com.example.eatstedi.model

import com.google.gson.annotations.SerializedName
import java.text.NumberFormat
import java.util.Locale

data class MenuItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val menuName: String,
    @SerializedName("id_supplier") val idSupplier: Int,
    @SerializedName("supplier_name") val supplierName: String?,
    @SerializedName("food_type") val foodType: String,
    @SerializedName("price") private val priceRaw: String,
    @SerializedName("stock") private val stockRaw: String,
    @SerializedName("menu_picture") val menuPicture: String,
    var quantity: Int = 1
) {
    val price: Int
        get() = priceRaw.toIntOrNull() ?: priceRaw.replace("Rp", "").replace(".", "").toIntOrNull() ?: 0

    var stock: Int
        get() = stockRaw.toIntOrNull() ?: stockRaw.replace("Rp", "").replace(".", "").toIntOrNull() ?: 0
        set(value) {
            // Tidak perlu mengubah stockRaw karena hanya digunakan untuk deserialisasi
        }

    val formattedPrice: String
        get() = "Rp${NumberFormat.getNumberInstance(Locale("id", "ID")).format(price)}"

    val category: String
        get() = when (foodType.lowercase()) {
            "food" -> "Makanan"
            "snack" -> "Camilan"
            "drink" -> "Minuman"
            else -> "Lainnya"
        }

    val imageUrl: String
        get() = if (menuPicture.startsWith("http")) {
            menuPicture
        } else {
            "http://127.0.0.1:8000/storage/menu/$menuPicture"
        }

    val ownerName: String
        get() = supplierName ?: when (idSupplier) {
            1 -> "Warung Bu Tuti"
            else -> "Pemasok $idSupplier"
        }
}