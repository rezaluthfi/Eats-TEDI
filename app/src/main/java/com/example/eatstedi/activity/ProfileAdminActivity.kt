package com.example.eatstedi.activity

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.AdminProfileData
import com.example.eatstedi.api.service.AdminProfileResponse
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.databinding.ActivityProfileAdminBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileAdminBinding
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        fetchAdminProfile(showShimmer = true)
    }

    private fun setupClickListeners() {
        with(binding) {
            ivArrowBack.setOnClickListener { finish() }
            btnEdit.setOnClickListener { toggleEditMode() }
            btnCancel.setOnClickListener {
                if (isEditing) toggleEditMode(forceOff = true)
                else finish()
            }
        }
    }

    private fun fetchAdminProfile(showShimmer: Boolean) {
        if (showShimmer) {
            binding.shimmerViewContainer.visibility = View.VISIBLE
            binding.scrollViewContent.visibility = View.GONE
            binding.shimmerViewContainer.startShimmer()
        }

        RetrofitClient.getInstance(this).getAdminProfile().enqueue(object : Callback<AdminProfileResponse> {
            override fun onResponse(call: Call<AdminProfileResponse>, response: Response<AdminProfileResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val adminData = response.body()!!.data
                    updateUI(adminData)
                } else {
                    showErrorState("Gagal mengambil data profil")
                }
                if (showShimmer) {
                    showContent()
                }
            }
            override fun onFailure(call: Call<AdminProfileResponse>, t: Throwable) {
                showErrorState("Error jaringan: ${t.message}")
                if (showShimmer) {
                    showContent()
                }
            }
        })
    }

    private fun showContent() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.scrollViewContent.visibility = View.VISIBLE
    }

    private fun updateUI(adminData: AdminProfileData) {
        with(binding) {
            Glide.with(this@ProfileAdminActivity)
                .load(adminData.profile_picture)
                .placeholder(R.drawable.img_avatar)
                .error(R.drawable.img_avatar)
                .circleCrop()
                .into(imgAdmin)

            tvAdminName.text = adminData.name
            etName.setText(adminData.name)
            etEmail.setText(adminData.email ?: "")
            etPhoneNumber.setText(adminData.no_telp ?: "")
        }
    }

    private fun showErrorState(message: String) {
        with(binding) {
            imgAdmin.setImageResource(R.drawable.img_avatar)
            tvAdminName.text = "Error"
            etName.setText("Gagal memuat data")
            etEmail.setText("-")
            etPhoneNumber.setText("-")
        }
        Toast.makeText(this@ProfileAdminActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun toggleEditMode(forceOff: Boolean = false) {
        if (forceOff) {
            isEditing = false
            fetchAdminProfile(showShimmer = false)
        } else {
            isEditing = !isEditing
        }

        if (isEditing) {
            setFieldsEnabled(true)
            binding.btnEdit.text = "Simpan"
            binding.btnCancel.text = "Batal Edit"
        } else {
            if (!forceOff) {
                showPasswordConfirmationDialog()
            } else {
                setFieldsEnabled(false)
                binding.btnEdit.text = "Edit"
                binding.btnCancel.text = "Kembali"
                binding.etNewPassword.text.clear()
            }
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        with(binding) {
            etName.isEnabled = enabled
            etEmail.isEnabled = enabled
            etPhoneNumber.isEnabled = enabled
            etNewPassword.isEnabled = enabled
        }
    }

    private fun showPasswordConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.view_dialog_password, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val etPassword = dialogView.findViewById<EditText>(R.id.et_password)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        val newPassword = binding.etNewPassword.text.toString()
        val isChangingPassword = newPassword.isNotEmpty()

        if (isChangingPassword) {
            titleTextView.text = "Ulangi Kata Sandi Baru"
            etPassword.hint = "Kata Sandi Baru"
        } else {
            titleTextView.text = "Konfirmasi Kata Sandi Lama"
            etPassword.hint = "Kata Sandi Lama"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val passwordConfirmation = etPassword.text.toString().trim()

            if (passwordConfirmation.isEmpty()) {
                etPassword.error = "Password tidak boleh kosong"
                return@setOnClickListener
            }

            if (isChangingPassword && newPassword != passwordConfirmation) {
                etPassword.error = "Kata sandi baru tidak cocok"
                return@setOnClickListener
            }

            dialog.dismiss()
            saveChanges(passwordConfirmation)
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveChanges(passwordForValidation: String) {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val noTelp = binding.etPhoneNumber.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString()

        if (name.isEmpty() || email.isEmpty() || noTelp.isEmpty()) {
            Toast.makeText(this, "Nama, Email, dan No. Telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isNotEmpty() && newPassword.length < 8) {
            Toast.makeText(this, "Password baru minimal 8 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val noTelpBody = noTelp.toRequestBody("text/plain".toMediaTypeOrNull())
        val passwordBody = passwordForValidation.toRequestBody("text/plain".toMediaTypeOrNull())
        val newPasswordBody = if (newPassword.isNotEmpty()) newPassword.toRequestBody("text/plain".toMediaTypeOrNull()) else null

        RetrofitClient.getInstance(this).updateAdminProfile(
            name = nameBody,
            username = usernameBody,
            email = emailBody,
            noTelp = noTelpBody,
            password = passwordBody,
            newPassword = newPasswordBody
        ).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ProfileAdminActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    toggleEditMode(forceOff = true)
                } else {
                    val errorMsg = response.body()?.message?.toString() ?: "Gagal memperbarui profil. Periksa kembali password Anda."
                    Toast.makeText(this@ProfileAdminActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProfileAdminActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}