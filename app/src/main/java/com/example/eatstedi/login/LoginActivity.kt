package com.example.eatstedi.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.eatstedi.activity.MainActivity
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.LoginRequest
import com.example.eatstedi.api.service.LoginResponse
import com.example.eatstedi.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Periksa token di SharedPreferences
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("auth_token", null)
        Log.d("LoginActivity", "Token tersimpan: $savedToken")

        if (!savedToken.isNullOrEmpty()) {
            Log.d("LoginActivity", "Token ditemukan, mengarahkan ke MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Inisialisasi UI
        binding.btnLoginProgress.visibility = View.GONE
        Log.d("LoginActivity", "onCreate: btnLoginProgress set to GONE")

        with(binding) {
            btnLogin.setOnClickListener {
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "Masukkan username dan password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                Log.d("LoginActivity", "Mengirim request login dengan username: $username")
                loginUser(username, password)
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        // Tampilkan ProgressBar di tombol dan nonaktifkan tombol
        binding.btnLoginProgress.visibility = View.VISIBLE
        binding.btnLoginText.visibility = View.GONE
        binding.btnLogin.isEnabled = false
        Log.d("LoginActivity", "loginUser: btnLoginProgress set to VISIBLE, btnLoginText set to GONE, btnLogin disabled")

        val apiService = RetrofitClient.getInstance(this)
        val loginRequest = LoginRequest(username, password)

        apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                // Sembunyikan ProgressBar dan kembalikan teks tombol
                binding.btnLoginProgress.visibility = View.GONE
                binding.btnLoginText.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = true
                Log.d("LoginActivity", "onResponse: btnLoginProgress set to GONE, btnLoginText set to VISIBLE, btnLogin enabled")

                Log.d("LoginActivity", "Response code: ${response.code()}")
                Log.d("LoginActivity", "Response body: ${response.body()}")
                Log.d("LoginActivity", "Response error: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.token
                    if (token != null) {
                        // Simpan token di SharedPreferences
                        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putString("auth_token", token).apply()
                        Log.d("LoginActivity", "Token disimpan: $token")

                        // Pindah ke MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                        Toast.makeText(this@LoginActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LoginActivity, "Token tidak ditemukan", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMessage = response.body()?.message
                        ?: response.errorBody()?.string()?.let { parseErrorMessage(it) }
                        ?: "Username atau password salah"
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Sembunyikan ProgressBar dan kembalikan teks tombol
                binding.btnLoginProgress.visibility = View.GONE
                binding.btnLoginText.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = true
                Log.d("LoginActivity", "onFailure: btnLoginProgress set to GONE, btnLoginText set to VISIBLE, btnLogin enabled")

                Log.e("LoginActivity", "Gagal terhubung: ${t.message}", t)
                Toast.makeText(this@LoginActivity, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun parseErrorMessage(errorBody: String): String? {
        return try {
            val json = errorBody.replace(Regex("[\\n\\r]"), "")
            val messageMatch = Regex(""""message":"(.*?)"""").find(json)
            messageMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Gagal parsing error message: ${e.message}")
            null
        }
    }
}