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
// IMPORT BINDING UNTUK DIALOG KUSTOM ANDA
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

    // Setelah gambar dipilih, langsung panggil fungsi upload
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Tampilkan pratinjau gambar yang dipilih
            Glide.with(this).load(it).circleCrop().placeholder(R.drawable.img_avatar).into(binding.imgSupplier)
            // Langsung mulai proses upload
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
            // Tombol kamera tidak lagi bergantung pada 'isEditing'
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
                // Fungsi ini sekarang hanya menyimpan data teks
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

            val clickListener = if (!enabled) View.OnClickListener {
                Toast.makeText(this@ProfileSupplierActivity, "Aktifkan mode edit untuk mengubah data teks", Toast.LENGTH_SHORT).show()
            } else null
            etName.setOnClickListener(clickListener)
            etStatus.setOnClickListener(clickListener)
            etUsername.setOnClickListener(clickListener)
            etPhoneNumber.setOnClickListener(clickListener)
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

    /**
     * Fungsi ini menangani upload foto profil secara terpisah.
     * Ia akan mengirimkan data teks yang ada saat ini bersama dengan gambar baru.
     */
    private fun uploadProfilePicture() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "URI gambar tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat file dari URI
        val file: File
        try {
            file = File(cacheDir, "supplier_profile_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            loadProfilePicture() // Kembalikan ke gambar lama jika gagal
            return
        }

        // Siapkan data untuk API call (termasuk data teks dari EditText)
        val nameBody = binding.etName.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = binding.etUsername.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneBody = binding.etPhoneNumber.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val statusBody = binding.etStatus.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))

        val apiService = RetrofitClient.getInstance(this)
        apiService.updateSupplier(supplierId, nameBody, usernameBody, phoneBody, statusBody, imagePart)
            .enqueue(object : Callback<UpdateSupplierResponse> {
                override fun onResponse(call: Call<UpdateSupplierResponse>, response: Response<UpdateSupplierResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ProfileSupplierActivity, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        loadProfilePicture() // Muat ulang gambar dari server untuk cache-busting
                        selectedImageUri = null // Reset URI setelah berhasil
                        setResult(Activity.RESULT_OK, Intent().putExtra("isDataUpdated", true))
                    } else {
                        val errorMsg = response.body()?.message ?: "Gagal memperbarui foto profil"
                        Toast.makeText(this@ProfileSupplierActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        loadProfilePicture() // Kembalikan ke gambar lama jika gagal
                    }
                }

                override fun onFailure(call: Call<UpdateSupplierResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileSupplierActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                    loadProfilePicture() // Kembalikan ke gambar lama jika gagal
                }
            })
    }


    /**
     * Fungsi ini hanya menyimpan data teks saat tombol 'Simpan' ditekan.
     * Gambar tidak dikirimkan di sini (Multipart part akan null).
     */
    private fun saveSupplierTextData() {
        with(binding) {
            val name = etName.text.toString().trim()
            val status = etStatus.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val phone = etPhoneNumber.text.toString().trim()

            if (name.isEmpty() || status.isEmpty() || username.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this@ProfileSupplierActivity, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())

            // Gambar tidak dikirim saat hanya menyimpan data teks, jadi 'imagePart' adalah null
            val imagePart: MultipartBody.Part? = null

            val apiService = RetrofitClient.getInstance(this@ProfileSupplierActivity)
            apiService.updateSupplier(supplierId, nameBody, usernameBody, phoneBody, statusBody, imagePart)
                .enqueue(object : Callback<UpdateSupplierResponse> {
                    override fun onResponse(call: Call<UpdateSupplierResponse>, response: Response<UpdateSupplierResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@ProfileSupplierActivity, "Data supplier berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            tvSupplierName.text = name // Update nama di header
                            isEditing = false
                            setEditTextEnabled(false)
                            btnEdit.text = "Edit"
                            btnCancel.text = "Kembali"
                            setResult(Activity.RESULT_OK, Intent().putExtra("isDataUpdated", true))
                        } else {
                            val errorMsg = response.body()?.message ?: "Gagal memperbarui data"
                            Toast.makeText(this@ProfileSupplierActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            toggleEditMode(forceOff = true) // Batalkan perubahan jika gagal
                        }
                    }
                    override fun onFailure(call: Call<UpdateSupplierResponse>, t: Throwable) {
                        Toast.makeText(this@ProfileSupplierActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                        toggleEditMode(forceOff = true) // Batalkan perubahan jika gagal
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
                            // Kunci unik untuk memaksa Glide memuat ulang gambar dari server, bukan dari cache
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

    // ================== BAGIAN YANG DIUBAH DENGAN VIEW BINDING ==================
    private fun showDeleteConfirmationDialog() {
        val dialogBinding = ViewDialogConfirmDeleteSupplierBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)

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