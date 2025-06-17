package com.example.eatstedi.api.retrofit

import android.content.Context
import android.util.Log
import com.example.eatstedi.api.service.ApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8000/api/"

    @Volatile
    private var apiService: ApiService? = null

    fun getInstance(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: create(context).also { apiService = it }
        }
    }

    private fun create(context: Context): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", null)
            Log.d("RetrofitClient", "Request URL: ${original.url}, Path: ${original.url.encodedPath}, Token: ${token ?: "null"}")

            val requestBuilder = original.newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
            if (token != null && !isExcludedEndpoint(original.url.encodedPath)) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
                Log.d("RetrofitClient", "Added Authorization header: Bearer $token")
            } else {
                Log.w("RetrofitClient", "No token or excluded endpoint: ${original.url.encodedPath}, Token: ${token ?: "null"}")
            }

            val request = requestBuilder.build()
            Log.d("RetrofitClient", "Request headers: ${request.headers}")
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        Log.d("RetrofitClient", "Retrofit instance created with base URL: $BASE_URL")
        return retrofit.create(ApiService::class.java)
    }

    private fun isExcludedEndpoint(path: String): Boolean {
        val excludedPaths = listOf("/api/login", "/api/logout")
        return excludedPaths.any { path.endsWith(it) || path == it }
    }
}