package com.example.eatstedi.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide // UBAH: Import Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.CreateSupplierResponse
import com.example.eatstedi.databinding.ActivityAddSupplierBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AddSupplierActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSupplierBinding
    private var selectedImageUri: Uri? = null

    // Gunakan GetContent dan tampilkan gambar dengan Glide.circleCrop()
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            // Tampilkan gambar pratinjau sebagai lingkaran menggunakan Glide
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(R.drawable.img_avatar) // Gambar default saat loading
                .into(binding.imgSupplier)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddSupplierBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val statusAdapter = ArrayAdapter.createFromResource(
            this, R.array.supplier_status, android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        (binding.etStatus as AutoCompleteTextView).setAdapter(statusAdapter)

        with(binding) {
            ivArrowBack.setOnClickListener { finish() }
            btnCancel.setOnClickListener { finish() }

            btnCameraSupplier.setOnClickListener {
                // Panggil launcher dengan tipe "image/*"
                pickImageLauncher.launch("image/*")
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val username = etUsername.text.toString().trim()
                val phone = etPhoneNumber.text.toString().trim()
                val status = etStatus.text.toString().trim()

                if (name.isEmpty() || username.isEmpty() || phone.isEmpty() || status.isEmpty()) {
                    Toast.makeText(this@AddSupplierActivity, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                createSupplier(name, username, phone, status, selectedImageUri)
            }
        }
    }

    private fun createSupplier(name: String, username: String, phone: String, status: String, imageUri: Uri?) {
        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
        val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
        var imagePart: MultipartBody.Part? = null

        imageUri?.let { uri ->
            val file = uriToFile(uri)
            if (file != null) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
            } else {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                return // Hentikan proses jika file tidak bisa dibuat
            }
        }

        val apiService = RetrofitClient.getInstance(this)
        apiService.createSupplier(nameBody, usernameBody, phoneBody, statusBody, imagePart).enqueue(object : Callback<CreateSupplierResponse> {
            override fun onResponse(call: Call<CreateSupplierResponse>, response: Response<CreateSupplierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AddSupplierActivity, "Supplier berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Beri tahu activity sebelumnya bahwa ada data baru
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: "Gagal menambahkan supplier"
                    Toast.makeText(this@AddSupplierActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CreateSupplierResponse>, t: Throwable) {
                Toast.makeText(this@AddSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}