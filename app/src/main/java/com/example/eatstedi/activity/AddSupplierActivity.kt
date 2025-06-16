package com.example.eatstedi.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class AddSupplierActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSupplierBinding
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                binding.imgSupplier.setImageURI(it)
            }
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
            ivArrowBack.setOnClickListener {
                finish()
            }

            btnCancel.setOnClickListener {
                finish()
            }

            btnCameraSupplier.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                pickImageLauncher.launch(intent)
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
        val apiService = RetrofitClient.getInstance(this)
        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
        val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
        var imagePart: MultipartBody.Part? = null

        imageUri?.let {
            val file = File(getRealPathFromURI(it))
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
        }

        apiService.createSupplier(nameBody, usernameBody, phoneBody, statusBody, imagePart).enqueue(object : Callback<CreateSupplierResponse> {
            override fun onResponse(call: Call<CreateSupplierResponse>, response: Response<CreateSupplierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AddSupplierActivity, "Supplier berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddSupplierActivity, "Gagal menambahkan supplier: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CreateSupplierResponse>, t: Throwable) {
                Toast.makeText(this@AddSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val idx = cursor?.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        val path = cursor?.getString(idx ?: 0) ?: ""
        cursor?.close()
        return path
    }
}