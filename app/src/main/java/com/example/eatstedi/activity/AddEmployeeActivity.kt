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
import com.bumptech.glide.Glide
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
import java.io.FileOutputStream
import java.io.InputStream

class AddEmployeeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEmployeeBinding
    private var selectedImageUri: Uri? = null

    // UBAH: Gunakan GetContent yang lebih modern
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .circleCrop() // Sintaks yang lebih modern dan ringkas
                .placeholder(R.drawable.img_avatar)
                .into(binding.imgEmployee)
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

        val statusAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.employee_status,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        (binding.etStatus as AutoCompleteTextView).setAdapter(statusAdapter)

        with(binding) {
            ivArrowBack.setOnClickListener { finish() }
            btnCancel.setOnClickListener { finish() }
            btnCameraEmployee.setOnClickListener {
                pickImageLauncher.launch("image/*") // Panggil launcher yang baru
            }
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
            if (password.length < 8) {
                Toast.makeText(this@AddEmployeeActivity, "Kata sandi minimal 8 karakter", Toast.LENGTH_SHORT).show()
                return
            }

            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val passwordBody = password.toRequestBody("text/plain".toMediaTypeOrNull())
            val salaryBody = salary.toRequestBody("text/plain".toMediaTypeOrNull())
            val statusBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
            var imagePart: MultipartBody.Part? = null

            // Konversi URI ke File dengan metode yang aman
            selectedImageUri?.let { uri ->
                val file = uriToFile(uri)
                if (file != null) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
                } else {
                    Toast.makeText(this@AddEmployeeActivity, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                    return // Hentikan jika file gagal dibuat
                }
            }

            val apiService = RetrofitClient.getInstance(this@AddEmployeeActivity)
            apiService.registerEmployee(nameBody, usernameBody, phoneBody, passwordBody, salaryBody, statusBody, imagePart)
                .enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@AddEmployeeActivity, "Karyawan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            val errorMsg = response.body()?.message ?: "Gagal menambahkan karyawan"
                            Toast.makeText(this@AddEmployeeActivity, errorMsg.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(this@AddEmployeeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    // Fungsi utilitas yang aman untuk mengonversi URI ke File
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