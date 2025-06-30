package com.example.eatstedi.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.databinding.ActivityProfileEmployeeBinding
import com.example.eatstedi.databinding.ViewDialogConfirmDeleteEmployeeBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ProfileEmployeeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEmployeeBinding
    private var isEditing = false
    private var cashierId: Int = 0
    private var selectedImageUri: Uri? = null
    private var isAdmin = false
    private var isViewingOwnProfile = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (isEditing) {
                selectedImageUri = it
                Glide.with(this).load(it).circleCrop().placeholder(R.drawable.img_avatar).into(binding.imgEmployee)
                Toast.makeText(this, "Foto akan diubah saat menekan tombol Simpan", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Gambar tidak dipilih", Toast.LENGTH_SHORT).show()
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) openGallery()
        else Toast.makeText(this, "Izin akses galeri ditolak", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEmployeeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPrefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPrefs.getInt("user_id", -1)
        isAdmin = sharedPrefs.getString("user_role", "")?.equals("admin", ignoreCase = true) == true

        if (!retrieveEmployeeData(loggedInUserId)) return

        setupUI()
    }

    private fun retrieveEmployeeData(loggedInUserId: Int): Boolean {
        cashierId = intent.getIntExtra("EMPLOYEE_ID", 0)
        isViewingOwnProfile = (cashierId == loggedInUserId)

        if (cashierId == 0) {
            Toast.makeText(this, "ID karyawan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        with(binding) {
            tvEmployeeName.text = intent.getStringExtra("EMPLOYEE_NAME")
            etName.setText(intent.getStringExtra("EMPLOYEE_NAME"))
            etStatus.setText(intent.getStringExtra("EMPLOYEE_STATUS"), false)
            etUsername.setText(intent.getStringExtra("EMPLOYEE_USERNAME"))
            etPhoneNumber.setText(intent.getStringExtra("EMPLOYEE_PHONE"))
            etEmail.setText(intent.getStringExtra("EMPLOYEE_EMAIL"))
            etAddress.setText(intent.getStringExtra("EMPLOYEE_ADDRESS"))
            etPassword.text.clear()
        }
        return true
    }

    private fun setupUI() {
        val statusOptions = arrayOf("aktif", "tidak aktif")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusOptions)
        binding.etStatus.setAdapter(statusAdapter)

        loadProfilePicture()

        binding.ivArrowBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener {
            if (isEditing) toggleEditMode(forceOff = true)
            else finish()
        }

        val canEdit = isAdmin || (!isAdmin && isViewingOwnProfile)
        if (canEdit) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnCameraEmployee.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener { toggleEditMode() }

            binding.btnCameraEmployee.setOnClickListener {
                if (isEditing) {
                    checkStoragePermission()
                } else {
                    Toast.makeText(this, "Aktifkan mode edit untuk mengubah foto", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.btnEdit.visibility = View.GONE
            binding.btnCameraEmployee.visibility = View.GONE
        }

        binding.tvDelete.visibility = if (isAdmin && !isViewingOwnProfile) View.VISIBLE else View.GONE
        binding.tvDelete.setOnClickListener { showDeleteConfirmationDialog() }

        binding.llPasswordField.visibility = if (!isAdmin && isViewingOwnProfile) View.VISIBLE else View.GONE

        binding.tvSchedule.visibility = if (!isAdmin) View.VISIBLE else View.GONE
        binding.tvSchedule.setOnClickListener {
            val intent = Intent(this@ProfileEmployeeActivity, ScheduleEmployeeActivity::class.java)
            intent.putExtras(this@ProfileEmployeeActivity.intent.extras ?: Bundle())
            startActivity(intent)
        }

        if (!isAdmin && isViewingOwnProfile) {
            // 1. Sembunyikan field status
            binding.llStatusField.visibility = View.GONE

            // 2. Ubah susunan layout menggunakan ConstraintSet
            // PERBAIKAN: Gunakan ID langsung dari binding, ini menyelesaikan ambiguity
            val constraintLayout = binding.formConstraintLayout
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)

            // Pindahkan Alamat ke sebelah Nama Pengguna
            constraintSet.connect(R.id.ll_address_field, ConstraintSet.TOP, R.id.ll_username_field, ConstraintSet.TOP, 0)
            constraintSet.connect(R.id.ll_address_field, ConstraintSet.START, R.id.guideline_vertical, ConstraintSet.END, 16)
            constraintSet.connect(R.id.ll_address_field, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)

            // Pindahkan Kata Sandi ke sebelah Email
            constraintSet.connect(R.id.ll_password_field, ConstraintSet.TOP, R.id.ll_email_field, ConstraintSet.TOP, 0)
            constraintSet.connect(R.id.ll_password_field, ConstraintSet.START, R.id.guideline_vertical, ConstraintSet.END, 16)
            constraintSet.connect(R.id.ll_password_field, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
            constraintSet.constrainWidth(R.id.ll_password_field, ConstraintSet.MATCH_CONSTRAINT)

            // Pindahkan Email agar berada di bawah Nama Pengguna
            constraintSet.connect(R.id.ll_email_field, ConstraintSet.TOP, R.id.ll_username_field, ConstraintSet.BOTTOM, 16)

            // Terapkan perubahan
            constraintSet.applyTo(constraintLayout)

        } else if (isAdmin) {
            // Jika admin yang melihat, sembunyikan field password
            binding.llPasswordField.visibility = View.GONE
        }

        setFieldsEnabled(false)
    }

    private fun toggleEditMode(forceOff: Boolean = false) {
        if (forceOff) {
            isEditing = false
            val sharedPrefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            retrieveEmployeeData(sharedPrefs.getInt("user_id", -1))
            loadProfilePicture()
        } else {
            isEditing = !isEditing
        }

        if (isEditing) {
            setFieldsEnabled(true)
            binding.btnEdit.text = "Simpan"
            binding.btnCancel.text = "Batal Edit"
        } else {
            if (!forceOff) {
                if (isAdmin) {
                    saveEmployeeDataByAdmin()
                } else {
                    showPasswordConfirmationDialog()
                }
            } else {
                setFieldsEnabled(false)
                binding.btnEdit.text = "Edit"
                binding.btnCancel.text = "Kembali"
            }
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        with(binding) {
            etName.isEnabled = enabled
            // Untuk kasir, status tidak bisa diubah oleh dirinya sendiri
            etStatus.isEnabled = enabled && isAdmin
            etUsername.isEnabled = enabled
            etPhoneNumber.isEnabled = enabled
            etEmail.isEnabled = enabled
            etAddress.isEnabled = enabled
            etPassword.isEnabled = enabled && !isAdmin && isViewingOwnProfile
        }
    }

    // ... Sisa kode lainnya tidak perlu diubah ...
    // ... (saveEmployeeDataByAdmin, showPasswordConfirmationDialog, etc.) ...

    private fun saveEmployeeDataByAdmin() {
        val name = binding.etName.text.toString().trim()
        val status = binding.etStatus.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhoneNumber.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (name.isEmpty() || status.isEmpty() || username.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
        val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val addressBody = address.toRequestBody("text/plain".toMediaTypeOrNull())

        var imagePart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            try {
                val file = File(cacheDir, "employee_profile_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }

        RetrofitClient.getInstance(this).updateCashierByAdmin(cashierId, nameBody, usernameBody, phoneBody, emailBody, addressBody, statusBody, imagePart)
            .enqueue(getUpdateCallback())
    }

    private fun showPasswordConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.view_dialog_password, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val etPasswordDialog = dialogView.findViewById<EditText>(R.id.et_password)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        val newPassword = binding.etPassword.text.toString()
        val isChangingPassword = newPassword.isNotEmpty()

        titleTextView.text = if (isChangingPassword) "Ulangi Kata Sandi Baru" else "Konfirmasi Password Lama"
        etPasswordDialog.hint = if (isChangingPassword) "Kata Sandi Baru" else "Password Saat Ini"

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val passwordConfirmation = etPasswordDialog.text.toString().trim()
            if (passwordConfirmation.isEmpty()) {
                etPasswordDialog.error = "Password tidak boleh kosong"; return@setOnClickListener
            }
            if (isChangingPassword && newPassword != passwordConfirmation) {
                etPasswordDialog.error = "Kata sandi baru tidak cocok"; return@setOnClickListener
            }
            dialog.dismiss()
            saveEmployeeDataByCashier(passwordConfirmation)
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveEmployeeDataByCashier(passwordConfirmation: String) {
        val name = binding.etName.text.toString().trim()
        val status = binding.etStatus.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhoneNumber.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val newPassword = binding.etPassword.text.toString()

        if (name.isEmpty() || status.isEmpty() || username.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Semua field (selain password) harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isNotEmpty() && newPassword.length < 8) {
            Toast.makeText(this, "Password baru minimal 8 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        val idBody = cashierId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
        val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val addressBody = address.toRequestBody("text/plain".toMediaTypeOrNull())
        val passConfirmBody = passwordConfirmation.toRequestBody("text/plain".toMediaTypeOrNull())
        val newPassBody = if (newPassword.isNotEmpty()) newPassword.toRequestBody("text/plain".toMediaTypeOrNull()) else null

        var imagePart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            try {
                val file = File(cacheDir, "employee_profile_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }

        RetrofitClient.getInstance(this).updateCashierProfile(idBody, nameBody, usernameBody, phoneBody, emailBody, addressBody, statusBody, passConfirmBody, newPassBody, imagePart)
            .enqueue(getUpdateCallback())
    }

    private fun getUpdateCallback(): Callback<GenericResponse> {
        return object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ProfileEmployeeActivity, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK, Intent().putExtra("isDataUpdated", true))

                    updateIntentWithNewData()

                    isEditing = false
                    setFieldsEnabled(false)
                    binding.btnEdit.text = "Edit"
                    binding.btnCancel.text = "Kembali"

                    selectedImageUri = null
                    loadProfilePicture()
                } else {
                    val errorMsg = response.body()?.message?.toString() ?: "Gagal memperbarui data"
                    Toast.makeText(this@ProfileEmployeeActivity, "Gagal: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProfileEmployeeActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateIntentWithNewData() {
        with(binding) {
            intent.putExtra("EMPLOYEE_NAME", etName.text.toString())
            intent.putExtra("EMPLOYEE_USERNAME", etUsername.text.toString())
            intent.putExtra("EMPLOYEE_PHONE", etPhoneNumber.text.toString())
            intent.putExtra("EMPLOYEE_EMAIL", etEmail.text.toString())
            intent.putExtra("EMPLOYEE_ADDRESS", etAddress.text.toString())
            intent.putExtra("EMPLOYEE_STATUS", etStatus.text.toString())
            tvEmployeeName.text = etName.text.toString()
        }
    }

    private fun loadProfilePicture() {
        RetrofitClient.getInstance(this).getCashierPhotoProfile(cashierId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val imageBytes = response.body()!!.bytes()
                        Glide.with(this@ProfileEmployeeActivity)
                            .load(imageBytes)
                            .placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar)
                            .signature(ObjectKey(System.currentTimeMillis().toString()))
                            .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                            .circleCrop().into(binding.imgEmployee)
                    } catch (e: Exception) {
                        binding.imgEmployee.setImageResource(R.drawable.img_avatar)
                    }
                } else {
                    binding.imgEmployee.setImageResource(R.drawable.img_avatar)
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.imgEmployee.setImageResource(R.drawable.img_avatar)
            }
        })
    }

    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun showDeleteConfirmationDialog() {
        val dialogBinding = ViewDialogConfirmDeleteEmployeeBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this).setView(dialogBinding.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.btnCancelDelete.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnConfirmDelete.setOnClickListener {
            deleteCashier()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun deleteCashier() {
        val token = getSharedPreferences("auth_prefs", MODE_PRIVATE).getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.getInstance(this).deleteCashier(cashierId, "Bearer $token").enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ProfileEmployeeActivity, "Karyawan berhasil dihapus", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.body()?.message?.toString() ?: "Gagal menghapus karyawan"
                    Toast.makeText(this@ProfileEmployeeActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProfileEmployeeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}