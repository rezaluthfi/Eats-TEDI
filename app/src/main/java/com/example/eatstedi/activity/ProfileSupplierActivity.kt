package com.example.eatstedi.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.UpdateSupplierResponse
import com.example.eatstedi.databinding.ActivityProfileSupplierBinding
import com.example.eatstedi.databinding.ViewDialogConfirmDeleteSupplierBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
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
            Glide.with(this).load(it).circleCrop().placeholder(R.drawable.img_avatar).into(binding.imgSupplier)
            uploadProfilePicture()
        } ?: Toast.makeText(this, "Gambar tidak dipilih", Toast.LENGTH_SHORT).show()
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) openGallery()
        else Toast.makeText(this, "Izin akses galeri ditolak", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSupplierBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!retrieveSupplierData()) return

        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        setupUI(sharedPreferences.getString("user_role", null) == "admin")
    }

    private fun retrieveSupplierData(): Boolean {
        supplierId = intent.getIntExtra("SUPPLIER_ID", 0)
        if (supplierId == 0) {
            Toast.makeText(this, "ID supplier tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        with(binding) {
            tvSupplierName.text = intent.getStringExtra("SUPPLIER_NAME")
            etName.setText(intent.getStringExtra("SUPPLIER_NAME"))
            etStatus.setText(intent.getStringExtra("SUPPLIER_STATUS"))
            etUsername.setText(intent.getStringExtra("SUPPLIER_USERNAME"))
            etPhoneNumber.setText(intent.getStringExtra("SUPPLIER_PHONE"))
            // BARU: Ambil data email dan alamat dari intent
            etEmail.setText(intent.getStringExtra("SUPPLIER_EMAIL"))
            etAddress.setText(intent.getStringExtra("SUPPLIER_ADDRESS"))
        }
        return true
    }

    private fun setupUI(isAdmin: Boolean) {
        val statusAdapter = ArrayAdapter.createFromResource(
            this, R.array.supplier_status, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        (binding.etStatus as AutoCompleteTextView).setAdapter(statusAdapter)

        loadProfilePicture()

        binding.ivArrowBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener {
            if (isEditing) {
                toggleEditMode(forceOff = true)
            } else {
                finish()
            }
        }

        if (isAdmin) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.tvDelete.visibility = View.VISIBLE
            binding.btnCameraSupplier.setOnClickListener {
                checkStoragePermission()
            }
            binding.btnEdit.setOnClickListener { toggleEditMode() }
            binding.tvDelete.setOnClickListener { showDeleteConfirmationDialog() }
        } else {
            binding.btnEdit.visibility = View.GONE
            binding.tvDelete.visibility = View.GONE
            binding.btnCameraSupplier.visibility = View.GONE
        }

        setEditTextEnabled(false)
    }

    private fun toggleEditMode(forceOff: Boolean = false) {
        if (forceOff) {
            isEditing = false
            retrieveSupplierData()
            loadProfilePicture()
        } else {
            isEditing = !isEditing
        }

        if (isEditing) {
            setEditTextEnabled(true)
            binding.btnEdit.text = "Simpan"
            binding.btnCancel.text = "Batal Edit"
        } else {
            if (!forceOff) {
                saveSupplierTextData()
            } else {
                setEditTextEnabled(false)
                binding.btnEdit.text = "Edit"
                binding.btnCancel.text = "Kembali"
            }
        }
    }

    private fun setEditTextEnabled(enabled: Boolean) {
        with(binding) {
            etName.isEnabled = enabled
            etStatus.isEnabled = enabled
            etUsername.isEnabled = enabled
            etPhoneNumber.isEnabled = enabled
            etEmail.isEnabled = enabled
            etAddress.isEnabled = enabled

            val clickListener = if (!enabled) View.OnClickListener {
                Toast.makeText(this@ProfileSupplierActivity, "Aktifkan mode edit untuk mengubah data teks", Toast.LENGTH_SHORT).show()
            } else null

            etName.setOnClickListener(clickListener)
            etStatus.setOnClickListener(clickListener)
            etUsername.setOnClickListener(clickListener)
            etPhoneNumber.setOnClickListener(clickListener)
            etEmail.setOnClickListener(clickListener)
            etAddress.setOnClickListener(clickListener)
        }
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

    private fun uploadProfilePicture() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "URI gambar tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val file: File
        try {
            file = File(cacheDir, "supplier_profile_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            loadProfilePicture()
            return
        }

        val nameBody = binding.etName.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = binding.etUsername.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneBody = binding.etPhoneNumber.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = binding.etEmail.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val addressBody = binding.etAddress.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val statusBody = binding.etStatus.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))

        val apiService = RetrofitClient.getInstance(this)
        // DIUBAH: Panggil API dengan parameter baru
        apiService.updateSupplier(supplierId, nameBody, usernameBody, phoneBody, emailBody, addressBody, statusBody, imagePart)
            .enqueue(object : Callback<UpdateSupplierResponse> {
                override fun onResponse(call: Call<UpdateSupplierResponse>, response: Response<UpdateSupplierResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ProfileSupplierActivity, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        loadProfilePicture()
                        selectedImageUri = null
                        setResult(Activity.RESULT_OK, Intent().putExtra("isDataUpdated", true))
                    } else {
                        val errorMsg = response.body()?.message ?: "Gagal memperbarui foto profil"
                        Toast.makeText(this@ProfileSupplierActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        loadProfilePicture()
                    }
                }

                override fun onFailure(call: Call<UpdateSupplierResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileSupplierActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                    loadProfilePicture()
                }
            })
    }


    private fun saveSupplierTextData() {
        with(binding) {
            val name = etName.text.toString().trim()
            val status = etStatus.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val phone = etPhoneNumber.text.toString().trim()
            val email = etEmail.text.toString().trim()      // BARU
            val address = etAddress.text.toString().trim()  // BARU

            if (name.isEmpty() || status.isEmpty() || username.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
                Toast.makeText(this@ProfileSupplierActivity, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
            val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())        // BARU
            val addressBody = address.toRequestBody("text/plain".toMediaTypeOrNull())    // BARU

            val imagePart: MultipartBody.Part? = null

            val apiService = RetrofitClient.getInstance(this@ProfileSupplierActivity)
            apiService.updateSupplier(supplierId, nameBody, usernameBody, phoneBody, emailBody, addressBody, statusBody, imagePart)
                .enqueue(object : Callback<UpdateSupplierResponse> {
                    override fun onResponse(call: Call<UpdateSupplierResponse>, response: Response<UpdateSupplierResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@ProfileSupplierActivity, "Data supplier berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            tvSupplierName.text = name
                            isEditing = false
                            setEditTextEnabled(false)
                            btnEdit.text = "Edit"
                            btnCancel.text = "Kembali"
                            setResult(Activity.RESULT_OK, Intent().putExtra("isDataUpdated", true))
                        } else {
                            val errorMsg = response.body()?.message ?: "Gagal memperbarui data"
                            Toast.makeText(this@ProfileSupplierActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            toggleEditMode(forceOff = true)
                        }
                    }
                    override fun onFailure(call: Call<UpdateSupplierResponse>, t: Throwable) {
                        Toast.makeText(this@ProfileSupplierActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                        toggleEditMode(forceOff = true)
                    }
                })
        }
    }

    private fun loadProfilePicture() {
        RetrofitClient.getInstance(this).getSupplierPhotoProfile(supplierId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val imageBytes = response.body()!!.bytes()
                        if (imageBytes.isEmpty()) {
                            binding.imgSupplier.setImageResource(R.drawable.img_avatar)
                            return
                        }
                        Glide.with(this@ProfileSupplierActivity)
                            .load(imageBytes)
                            .placeholder(R.drawable.img_avatar)
                            .error(R.drawable.img_avatar)
                            .signature(ObjectKey(System.currentTimeMillis().toString()))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .circleCrop()
                            .into(binding.imgSupplier)
                    } catch (e: Exception) {
                        binding.imgSupplier.setImageResource(R.drawable.img_avatar)
                    }
                } else {
                    binding.imgSupplier.setImageResource(R.drawable.img_avatar)
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.imgSupplier.setImageResource(R.drawable.img_avatar)
            }
        })
    }

    private fun showDeleteConfirmationDialog() {
        val dialogBinding = ViewDialogConfirmDeleteSupplierBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this).setView(dialogBinding.root)
        val dialog = builder.create()

        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
            deleteSupplier()
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun deleteSupplier() {
        RetrofitClient.getInstance(this).deleteSupplier(supplierId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ProfileSupplierActivity, "Supplier berhasil dihapus", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@ProfileSupplierActivity, "Gagal menghapus supplier: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProfileSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}