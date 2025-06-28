
package com.example.eatstedi.model

import com.google.gson.annotations.SerializedName
import java.text.NumberFormat
import java.util.Locale

data class MenuItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val menuName: String,
    @SerializedName("id_supplier") val idSupplier: Int,
    @SerializedName("supplier_name") var supplierName: String?,
    @SerializedName("food_type") val foodType: String,
    @SerializedName("price") private val priceRaw: String,
    @SerializedName("stock") private var stockRaw: String,
    @SerializedName("menu_picture") val menuPicture: String,
    var quantity: Int = 1
) {
    val price: Int
        get() = priceRaw.toIntOrNull() ?: priceRaw.replace("Rp", "").replace(".", "").toIntOrNull() ?: 0

    var stock: Int
        get() {
            val parsed = stockRaw.toIntOrNull() ?: stockRaw.replace("Rp", "").replace(".", "").toIntOrNull() ?: 0
            if (stockRaw.contains("Rp") || stockRaw.contains(".")) {
                android.util.Log.w("MenuItem", "Unexpected stock format: $stockRaw")
            }
            return parsed
        }
        set(value) {
            stockRaw = value.toString()
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
        get() = "http://10.0.2.2:8000/api/get-menu-photo/$id"

    val ownerName: String
        get() = supplierName ?: "Pemasok Tidak Diketahui"

    fun isValidForTransaction(): Boolean {
        return stock > 0
    }
}