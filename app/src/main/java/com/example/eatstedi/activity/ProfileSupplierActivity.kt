package com.example.eatstedi.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.UpdateSupplierResponse
import com.example.eatstedi.databinding.ActivityProfileSupplierBinding
import com.example.eatstedi.login.LoginActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ProfileSupplierActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSupplierBinding
    private var isEditing = false
    private var supplierId: Int = 0
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showPasswordDialog()
        } ?: run {
            Toast.makeText(this, "Gambar tidak dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Izin akses galeri ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileSupplierBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve data from Intent
        supplierId = intent.getIntExtra("SUPPLIER_ID", 0)
        if (supplierId == 0) {
            Toast.makeText(this, "ID supplier tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val supplierName = intent.getStringExtra("SUPPLIER_NAME")
        val supplierStatus = intent.getStringExtra("SUPPLIER_STATUS")
        val supplierUsername = intent.getStringExtra("SUPPLIER_USERNAME")
        val supplierPhone = intent.getStringExtra("SUPPLIER_PHONE")

        // Set up status dropdown
        val statusAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.supplier_status,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val etStatus = binding.etStatus as AutoCompleteTextView
        etStatus.setAdapter(statusAdapter)

        with(binding) {
            // Set initial values
            tvSupplierName.text = supplierName
            etName.setText(supplierName)
            etStatus.setText(supplierStatus)
            etUsername.setText(supplierUsername)
            etPhoneNumber.setText(supplierPhone)

            // Load profile picture
            loadProfilePicture()

            // Back button
            ivArrowBack.setOnClickListener {
                finish()
            }

            // Cancel button
            btnCancel.setOnClickListener {
                finish()
            }

            // Camera button
            btnCameraSupplier.setOnClickListener {
                if (isEditing) {
                    checkStoragePermission()
                } else {
                    Toast.makeText(this@ProfileSupplierActivity, "Aktifkan mode edit terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            }

            // Edit button
            btnEdit.setOnClickListener {
                if (isEditing) {
                    saveSupplierData()
                    setEditTextEnabled(false)
                    btnEdit.text = "Edit"
                } else {
                    setEditTextEnabled(true)
                    btnEdit.text = "Simpan"
                }
                isEditing = !isEditing
            }

            // Delete button
            tvDelete.setOnClickListener {
                showDeleteConfirmationDialog()
            }

            // Set initial state
            setEditTextEnabled(false)
            setEditTextClickListener()
        }
    }

    private fun setEditTextClickListener() {
        with(binding) {
            etName.setOnClickListener { showToastIfNotEditing() }
            etStatus.setOnClickListener { showToastIfNotEditing() }
            etUsername.setOnClickListener { showToastIfNotEditing() }
            etPhoneNumber.setOnClickListener { showToastIfNotEditing() }
        }
    }

    private fun showToastIfNotEditing() {
        if (!isEditing) {
            Toast.makeText(this, "Aktifkan mode edit terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setEditTextEnabled(enabled: Boolean) {
        with(binding) {
            etName.isEnabled = enabled
            etStatus.isEnabled = enabled
            etUsername.isEnabled = enabled
            etPhoneNumber.isEnabled = enabled
            btnCameraSupplier.isEnabled = enabled
        }
    }

    private fun checkStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.view_dialog_password, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = builder.create()

        val etPassword = dialogView.findViewById<EditText>(R.id.et_password)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)

        btnCancel.setOnClickListener {
            selectedImageUri = null
            loadProfilePicture()
            alertDialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val password = etPassword.text.toString().trim()
            if (password.isEmpty()) {
                etPassword.error = "Password tidak boleh kosong"
            } else {
                alertDialog.dismiss()
                doUploadProfilePicture(password)
            }
        }

        alertDialog.show()
    }

    private fun doUploadProfilePicture(password: String) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Gambar tidak dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = contentResolver.getType(selectedImageUri!!)
        if (mimeType !in listOf("image/jpeg", "image/png")) {
            Toast.makeText(this, "Hanya file JPG atau PNG yang diperbolehkan", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (file.length() > 5 * 1024 * 1024) {
            Toast.makeText(this, "Gambar terlalu besar (maks 5MB)", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val profilePicturePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)

        val name = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val noTelp = binding.etPhoneNumber.text.toString().trim()
        val status = binding.etStatus.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || noTelp.isEmpty() || status.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val nameRequestBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameRequestBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val noTelpRequestBody = noTelp.toRequestBody("text/plain".toMediaTypeOrNull())
        val statusRequestBody = status.toRequestBody("text/plain".toMediaTypeOrNull())

        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        if (token == null) {
            Toast.makeText(this, "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val apiService = RetrofitClient.getInstance(this)
        apiService.updateSupplier(
            supplierId,
            nameRequestBody,
            usernameRequestBody,
            noTelpRequestBody,
            statusRequestBody,
            profilePicturePart
        ).enqueue(object : Callback<UpdateSupplierResponse> {
            override fun onResponse(call: Call<UpdateSupplierResponse>, response: Response<UpdateSupplierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ProfileSupplierActivity, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    binding.tvSupplierName.text = name
                    loadProfilePicture()
                } else {
                    val errorMessage = response.body()?.message?.toString() ?: response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@ProfileSupplierActivity, "Gagal mengunggah foto: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("ProfileSupplierActivity", "Upload error: $errorMessage")
                }
            }

            override fun onFailure(call: Call<UpdateSupplierResponse>, t: Throwable) {
                Toast.makeText(this@ProfileSupplierActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProfileSupplierActivity", "Upload failure: ${t.message}", t)
            }
        })
    }

    private fun loadProfilePicture() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getSupplierPhotoProfile(supplierId).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val imageBytes = response.body()!!.bytes()
                        Glide.with(this@ProfileSupplierActivity)
                            .load(imageBytes)
                            .placeholder(R.drawable.img_avatar)
                            .error(R.drawable.img_avatar)
                            .circleCrop()
                            .into(binding.imgSupplier)
                        Log.d("ProfileSupplierActivity", "Profile picture loaded successfully")
                    } catch (e: Exception) {
                        Log.e("ProfileSupplierActivity", "Error converting ResponseBody to bytes: ${e.message}", e)
                        binding.imgSupplier.setImageResource(R.drawable.img_avatar)
                    }
                } else {
                    binding.imgSupplier.setImageResource(R.drawable.img_avatar)
                    Log.w("ProfileSupplierActivity", "Failed to load profile picture: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                binding.imgSupplier.setImageResource(R.drawable.img_avatar)
                Log.e("ProfileSupplierActivity", "Error loading profile picture: ${t.message}", t)
            }
        })
    }

    private fun saveSupplierData() {
        with(binding) {
            val name = etName.text.toString().trim()
            val status = etStatus.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val phone = etPhoneNumber.text.toString().trim()

            if (name.isEmpty() || status.isEmpty() || username.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this@ProfileSupplierActivity, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            if (status !in listOf("active", "inactive")) {
                Toast.makeText(this@ProfileSupplierActivity, "Status harus 'active' atau 'inactive'", Toast.LENGTH_SHORT).show()
                return
            }

            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
            var imagePart: MultipartBody.Part? = null

            selectedImageUri?.let {
                val file = File(cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(it)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (file.length() > 5 * 1024 * 1024) {
                    Toast.makeText(this@ProfileSupplierActivity, "Gambar terlalu besar (maks 5MB)", Toast.LENGTH_SHORT).show()
                    return
                }
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
            }

            val apiService = RetrofitClient.getInstance(this@ProfileSupplierActivity)
            apiService.updateSupplier(supplierId, nameBody, usernameBody, phoneBody, statusBody, imagePart).enqueue(object : Callback<UpdateSupplierResponse> {
                override fun onResponse(call: Call<UpdateSupplierResponse>, response: Response<UpdateSupplierResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        tvSupplierName.text = name
                        if (selectedImageUri != null) {
                            loadProfilePicture()
                        }
                        Toast.makeText(this@ProfileSupplierActivity, "Data supplier berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileSupplierActivity, "Gagal memperbarui supplier: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UpdateSupplierResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.view_dialog_confirm_delete_supplier, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = builder.create()

        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_supplier)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_delete_supplier)

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val apiService = RetrofitClient.getInstance(this@ProfileSupplierActivity)
            apiService.deleteSupplier(supplierId).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ProfileSupplierActivity, "Supplier berhasil dihapus", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ProfileSupplierActivity, "Gagal menghapus supplier: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}