package com.example.eatstedi.model

import com.google.gson.annotations.SerializedName

data class Employee(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("no_telp") val no_telp: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("alamat") val alamat: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("profile_picture") val profile_picture: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
    // Field 'salary' telah dihapus
)