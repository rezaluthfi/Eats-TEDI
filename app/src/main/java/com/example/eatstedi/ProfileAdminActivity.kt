package com.example.eatstedi

import android.os.Bundle
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

    private var isEditing = false // Menandakan apakah dalam mode edit

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
                finish() // Kembali ke aktivitas sebelumnya
            }

            btnCancel.setOnClickListener {
                finish() // Kembali ke aktivitas sebelumnya
            }

            // Set editable false
            setEditTextEnabled(false)

            // Set user data
            setUserData()

            btnEdit.setOnClickListener {
                if (isEditing) {
                    // Simpan perubahan
                    saveUserData()
                    // Kembali ke mode tidak edit
                    setEditTextEnabled(false)
                    btnEdit.text = "Edit"
                } else {
                    // Masuk ke mode edit
                    setEditTextEnabled(true)
                    btnEdit.text = "Simpan"
                }
                isEditing = !isEditing // Toggle mode editing
            }

            // Tambahkan listener untuk setiap EditText
            setEditTextClickListener()
        }
    }

    private fun setEditTextClickListener() {
        with(binding) {
            etName.setOnClickListener { showToastIfNotEditing() }
            etEmail.setOnClickListener { showToastIfNotEditing() }
            etAddress.setOnClickListener { showToastIfNotEditing() }
            etUsername.setOnClickListener { showToastIfNotEditing() }
            etPhoneNumber.setOnClickListener { showToastIfNotEditing() }
            etPassword.setOnClickListener { showToastIfNotEditing() }
        }
    }

    private fun showToastIfNotEditing() {
        if (!isEditing) {
            Toast.makeText(this, "Aktifkan mode edit terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setEditTextEnabled(enabled: Boolean) {
        with(binding) {
            etName.isFocusable = enabled
            etName.isFocusableInTouchMode = enabled
            etEmail.isFocusable = enabled
            etEmail.isFocusableInTouchMode = enabled
            etAddress.isFocusable = enabled
            etAddress.isFocusableInTouchMode = enabled
            etUsername.isFocusable = enabled
            etUsername.isFocusableInTouchMode = enabled
            etPhoneNumber.isFocusable = enabled
            etPhoneNumber.isFocusableInTouchMode = enabled
            etPassword.isFocusable = enabled
            etPassword.isFocusableInTouchMode = enabled
        }
    }

    private fun setUserData() {
        // Set user data
        binding.etName.setText("Reza Luthfi Akbar")
        binding.etEmail.setText("hello.rezaluthfi@gmail.com")
        binding.etAddress.setText("Jl. Kaliurang KM 5,5")
        binding.etUsername.setText("rezaluthf_")
        binding.etPhoneNumber.setText("081234567890")
        binding.etPassword.setText("password")
    }

    private fun saveUserData() {
        // Ambil data dari EditText
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val address = binding.etAddress.text.toString()
        val username = binding.etUsername.text.toString()
        val phoneNumber = binding.etPhoneNumber.text.toString()
        val password = binding.etPassword.text.toString()

        // Log untuk melihat data yang disimpan
        println("User Data Saved: Name: $name, Email: $email, Address: $address, Username: $username, Phone: $phoneNumber, Password: $password")

        // Tampilkan toast untuk memberi tahu pengguna bahwa data telah diperbarui
        Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
    }
}
