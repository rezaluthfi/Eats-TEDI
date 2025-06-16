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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.databinding.ActivityAddEmployeeBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class AddEmployeeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEmployeeBinding
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                Glide.with(this@AddEmployeeActivity)
                    .load(it)
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.imgEmployee)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddEmployeeBinding.inflate(layoutInflater)
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
            // Back button
            ivArrowBack.setOnClickListener {
                finish()
            }

            // Cancel button
            btnCancel.setOnClickListener {
                finish()
            }

            // Camera button
            btnCameraEmployee.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                pickImageLauncher.launch(intent)
            }

            // Save button
            btnSave.setOnClickListener {
                saveEmployeeData()
            }
        }
    }

    private fun saveEmployeeData() {
        with(binding) {
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val phone = etPhoneNumber.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val salary = etSalary.text.toString().trim()
            val status = etStatus.text.toString().trim()

            if (name.isEmpty() || username.isEmpty() || phone.isEmpty() || password.isEmpty() || salary.isEmpty() || status.isEmpty()) {
                Toast.makeText(this@AddEmployeeActivity, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            if (status !in listOf("active", "inactive")) {
                Toast.makeText(this@AddEmployeeActivity, "Status harus 'active' atau 'inactive'", Toast.LENGTH_SHORT).show()
                return
            }

            if (password.length < 6) {
                Toast.makeText(this@AddEmployeeActivity, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return
            }

            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val passwordBody = password.toRequestBody("text/plain".toMediaTypeOrNull())
            val salaryBody = salary.toRequestBody("text/plain".toMediaTypeOrNull())
            val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
            var imagePart: MultipartBody.Part? = null

            selectedImageUri?.let {
                val file = File(getRealPathFromURI(it))
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
            }

            val apiService = RetrofitClient.getInstance(this@AddEmployeeActivity)
            apiService.registerEmployee(nameBody, usernameBody, phoneBody, passwordBody, salaryBody, statusBody, imagePart).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AddEmployeeActivity, "Karyawan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@AddEmployeeActivity, "Gagal menambahkan karyawan: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@AddEmployeeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
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