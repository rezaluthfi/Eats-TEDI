package com.example.eatstedi.api.service

import com.example.eatstedi.model.MenuItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET("get-menu")
    fun getMenu(): Call<MenuResponse>

    @Multipart
    @POST("create-menu")
    fun createMenu(
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("stock") stock: RequestBody,
        @Part("id_supplier") idSupplier: RequestBody,
        @Part("supplier_name") supplierName: RequestBody?,
        @Part("food_type") foodType: RequestBody,
        @Part menuPicture: MultipartBody.Part?
    ): Call<SingleMenuResponse>

    @POST("search-menu")
    fun searchMenu(@Body searchRequest: SearchRequest): Call<MenuResponse>

    @Multipart
    @POST("update-menu/{id}")
    fun updateMenu(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("stock") stock: RequestBody,
        @Part("id_supplier") idSupplier: RequestBody,
        @Part("supplier_name") supplierName: RequestBody?,
        @Part("food_type") foodType: RequestBody,
        @Part menuPicture: MultipartBody.Part?
    ): Call<SingleMenuResponse>

    @DELETE("delete-menu/{id}")
    fun deleteMenu(@Path("id") id: Int): Call<GenericResponse>

    @GET("filter-by-supplier/{id}")
    fun filterBySupplier(@Path("id") id: Int): Call<MenuResponse>

    @GET("filter-by-food-type/{type}")
    fun filterByFoodType(@Path("type") type: String): Call<MenuResponse>

    @POST("logout")
    fun logout(@Header("Authorization") token: String): Call<LogoutResponse>
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val activity: String?,
    val data: User? = null,
    val token: String? = null,
    val message: String? = null
)

data class User(
    val id: Int,
    val name: String,
    val role: String,
    val profile_picture: String?,
    val created_at: String?,
    val updated_at: String?
)

data class LogoutResponse(
    val success: Boolean,
    val message: String?
)

data class MenuResponse(
    val success: Boolean,
    val activity: String?,
    val data: List<MenuItem>,
    val message: String? = null
)

data class SingleMenuResponse(
    val success: Boolean,
    val activity: String?,
    val data: MenuItem,
    val message: String? = null
)

data class GenericResponse(
    val success: Boolean,
    val message: String?
)

data class SearchRequest(
    val name: String
)