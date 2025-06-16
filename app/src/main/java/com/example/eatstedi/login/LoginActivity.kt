package com.example.eatstedi.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.eatstedi.R
import com.example.eatstedi.activity.MainActivity
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.LoginRequest
import com.example.eatstedi.api.service.LoginResponse
import com.example.eatstedi.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.EOFException

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("auth_token", null)

        if (!savedToken.isNullOrEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.btnLoginProgress.visibility = View.GONE
        setupPasswordToggle()

        with(binding) {
            btnLogin.setOnClickListener {
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "Masukkan username dan password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                loginUser(username, password)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle() {
        binding.etPassword.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etPassword.right - binding.etPassword.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - binding.etPassword.paddingRight)) {
                    togglePasswordVisibility()
                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility() {
        if (binding.etPassword.transformationMethod is PasswordTransformationMethod) {
            binding.etPassword.transformationMethod = null
            binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_open, 0)
        } else {
            binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_closed, 0)
        }
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }

    private fun loginUser(username: String, password: String) {
        setLoginButtonState(isLoading = true)

        val apiService = RetrofitClient.getInstance(this)
        val loginRequest = LoginRequest(username, password)

        apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                setLoginButtonState(isLoading = false)

                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.token
                    val user = response.body()?.data

                    if (token != null && user != null) {
                        saveUserSession(token, user)
                        goToMainActivity()
                    } else {
                        Toast.makeText(this@LoginActivity, "Respons tidak valid dari server", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMessage = response.body()?.message
                        ?: response.errorBody()?.string()?.let { parseErrorMessage(it) }
                        ?: "Username atau password salah"
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                setLoginButtonState(isLoading = false)
                Log.e("LoginActivity", "Gagal terhubung: ${t.message}", t)

                // --- PERBAIKAN UTAMA DI SINI ---
                // Beri pesan yang lebih spesifik jika errornya EOFException
                if (t is EOFException) {
                    Toast.makeText(this@LoginActivity, "Koneksi terputus saat menerima data. Coba lagi.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@LoginActivity, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun setLoginButtonState(isLoading: Boolean) {
        binding.btnLoginProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLoginText.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnLogin.isEnabled = !isLoading
    }

    private fun saveUserSession(token: String, user: com.example.eatstedi.api.service.User) {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("auth_token", token)
            putString("user_role", user.role)
            putString("user_name", user.name)
            putInt("user_id", user.id)
            putString("profile_picture", user.profile_picture ?: "")
            apply()
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
        Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
    }

    private fun parseErrorMessage(errorBody: String): String? {
        return try {
            val json = errorBody.replace(Regex("[\\n\\r]"), "")
            val messageMatch = Regex(""""message":"(.*?)"""").find(json)
            messageMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
}