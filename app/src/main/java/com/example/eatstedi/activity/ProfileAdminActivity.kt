package com.example.eatstedi.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityProfileAdminBinding

class ProfileAdminActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityProfileAdminBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {
            ivArrowBack.setOnClickListener {
                finish()
            }

            btnCancel.setOnClickListener {
                finish()
            }

            // Set data user dari SharedPreferences
            setUserData()

            // Nonaktifkan input field
            disableInputFields()

            // Nonaktifkan mode edit karena tidak ada endpoint update
            btnEdit.visibility = View.GONE // Sembunyikan tombol edit untuk saat ini
        }
    }

    private fun setUserData() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val adminName = sharedPreferences.getString("user_name", "AdminKantin") ?: "AdminKantin"
        val username = sharedPreferences.getString("username", "adminkantin") ?: "adminkantin"
        val phoneNumber = sharedPreferences.getString("no_telp", "081234567890") ?: "081234567890"
        val email = sharedPreferences.getString("email", "admin@kantin.com") ?: "admin@kantin.com"
        val address = sharedPreferences.getString("address", "Jl. Kaliurang KM 5,5") ?: "Jl. Kaliurang KM 5,5"
        val password = sharedPreferences.getString("password", "password") ?: "password"

        with(binding) {
            tvAdminName.text = adminName
            etName.setText(adminName)
            etUsername.setText(username)
            etPhoneNumber.setText(phoneNumber)
            etEmail.setText(email)
            etAddress.setText(address)
            etPassword.setText(password)

            // Log untuk debugging
            Log.d("ProfileAdminActivity", "Displayed Name: $adminName, Role: ${sharedPreferences.getString("user_role", "unknown")}")
        }
    }

    private fun disableInputFields() {
        with(binding) {
            etName.isEnabled = false
            etName.isFocusable = false
            etName.isFocusableInTouchMode = false

            etUsername.isEnabled = false
            etUsername.isFocusable = false
            etUsername.isFocusableInTouchMode = false

            etPhoneNumber.isEnabled = false
            etPhoneNumber.isFocusable = false
            etPhoneNumber.isFocusableInTouchMode = false

            etEmail.isEnabled = false
            etEmail.isFocusable = false
            etEmail.isFocusableInTouchMode = false

            etAddress.isEnabled = false
            etAddress.isFocusable = false
            etAddress.isFocusableInTouchMode = false

            etPassword.isEnabled = false
            etPassword.isFocusable = false
            etPassword.isFocusableInTouchMode = false
        }
    }
}