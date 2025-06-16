package com.example.eatstedi.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
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
import com.example.eatstedi.databinding.ActivityProfileEmployeeBinding
import com.example.eatstedi.login.LoginActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ProfileEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityProfileEmployeeBinding.inflate(layoutInflater)
    }
    private var cashierId: Int = -1
    private var selectedImageUri: Uri? = null

    // Launcher untuk memilih gambar dari galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Hanya tampilkan pratinjau jika pengguna adalah kasir sendiri
            val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            val loggedInUserId = sharedPreferences.getInt("user_id", 0)
            if (cashierId == loggedInUserId) {
                Glide.with(this@ProfileEmployeeActivity)
                    .load(it)
                    .circleCrop()
                    .into(binding.imgMenu)
                uploadProfilePicture()
            } else {
                Toast.makeText(this, "Hanya kasir sendiri yang dapat mengubah foto profil", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Gambar tidak dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk meminta izin
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Izin akses galeri ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Terapkan padding untuk system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ambil dan set data cashier dari Intent
        cashierId = intent.getIntExtra("EMPLOYEE_ID", 0)
        Log.d("ProfileEmployeeActivity", "Received EMPLOYEE_ID: $cashierId")
        if (cashierId == 0) {
            Toast.makeText(this, "ID karyawan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setCashierData()

        // Nonaktifkan input field
        disableInputFields()

        // Periksa peran pengguna
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = sharedPreferences.getString("user_role", "cashier") ?: "cashier"
        val isAdmin = userRole == "admin"

        with(binding) {
            // Tombol kembali
            ivArrowBack.setOnClickListener {
                finish()
            }

            // Tombol jadwal
            tvSchedule.setOnClickListener {
                val name = intent.getStringExtra("EMPLOYEE_NAME") ?: "Cashier"
                val username = intent.getStringExtra("EMPLOYEE_USERNAME") ?: "cashier"
                val phone = intent.getStringExtra("EMPLOYEE_PHONE") ?: "N/A"
                val salary = intent.getIntExtra("EMPLOYEE_SALARY", 0).toString()
                val email = "cashier@kantin.com" // Placeholder

                val scheduleIntent = if (isAdmin) {
                    Intent(this@ProfileEmployeeActivity, AddScheduleEmployeeActivity::class.java)
                } else {
                    Intent(this@ProfileEmployeeActivity, ScheduleEmployeeActivity::class.java)
                }.apply {
                    putExtra("EMPLOYEE_ID", cashierId)
                    putExtra("EMPLOYEE_NAME", name)
                    putExtra("EMPLOYEE_USERNAME", username)
                    putExtra("EMPLOYEE_EMAIL", email)
                    putExtra("EMPLOYEE_PHONE", phone)
                    putExtra("EMPLOYEE_SALARY", salary)
                }
                startActivity(scheduleIntent)
            }

            // Tombol hapus
            if (!isAdmin) {
                tvDelete.visibility = View.GONE // Sembunyikan tombol hapus jika bukan admin
            } else {
                tvDelete.setOnClickListener {
                    showDeleteConfirmationDialog()
                }
            }

            // Tombol cancel
            btnCancel.setOnClickListener {
                finish()
            }

            // Sembunyikan tombol edit karena belum ada endpoint update
            btnEdit.visibility = View.GONE

            // Tombol kamera untuk upload gambar profil
            btnCameraMenu.setOnClickListener {
                val loggedInUserId = sharedPreferences.getInt("user_id", 0)
                if (cashierId == loggedInUserId) {
                    checkStoragePermission()
                } else {
                    Toast.makeText(this@ProfileEmployeeActivity, "Hanya kasir sendiri yang dapat mengubah foto profil", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setCashierData() {
        // Ambil data dari Intent
        val name = intent.getStringExtra("EMPLOYEE_NAME") ?: "Cashier"
        val username = intent.getStringExtra("EMPLOYEE_USERNAME") ?: "cashier"
        val noTelp = intent.getStringExtra("EMPLOYEE_PHONE") ?: "N/A"
        val salary = intent.getIntExtra("EMPLOYEE_SALARY", 0)
        val status = intent.getStringExtra("EMPLOYEE_STATUS") ?: "N/A"
        val profilePicture = intent.getStringExtra("EMPLOYEE_PROFILE_PICTURE") ?: ""

        with(binding) {
            tvEmployeeName.text = name
            etName.setText(name)
            etUsername.setText(username)
            etPhoneNumber.setText(noTelp)
            etStatus.setText(status)
            etEmail.setText("cashier@kantin.com") // Placeholder
            etAddress.setText("Yogyakarta") // Placeholder

            // Load profile picture menggunakan Glide
            if (profilePicture.isNotEmpty() && profilePicture != "path" && profilePicture.startsWith("http")) {
                Glide.with(this@ProfileEmployeeActivity)
                    .load(profilePicture)
                    .placeholder(R.drawable.img_avatar)
                    .error(R.drawable.img_avatar)
                    .circleCrop()
                    .into(imgMenu)
            } else {
                imgMenu.setImageResource(R.drawable.img_avatar)
                Log.w("ProfileEmployeeActivity", "Invalid profile picture URL: $profilePicture")
            }

            // Log untuk debugging
            Log.d("ProfileEmployeeActivity", "Displayed Name: $name, ID: $cashierId")
        }
    }

    private fun disableInputFields() {
        with(binding) {
            etName.isEnabled = false
            etName.isFocusable = false
            etName.isFocusableInTouchMode = false

            etUsername.isEnabled = false
            etUsername.isFocusable = false
            etUsername.isFocusableInTouchMode = false

            etPhoneNumber.isEnabled = false
            etPhoneNumber.isFocusable = false
            etPhoneNumber.isFocusableInTouchMode = false

            etStatus.isEnabled = false
            etStatus.isFocusable = false
            etStatus.isFocusableInTouchMode = false

            etEmail.isEnabled = false
            etEmail.isFocusable = false
            etEmail.isFocusableInTouchMode = false

            etAddress.isEnabled = false
            etAddress.isFocusable = false
            etAddress.isFocusableInTouchMode = false
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

    private fun uploadProfilePicture() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Gambar tidak dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi tipe file
        val mimeType = contentResolver.getType(selectedImageUri!!)
        if (mimeType !in listOf("image/jpeg", "image/png")) {
            Toast.makeText(this, "Hanya file JPG atau PNG yang diperbolehkan", Toast.LENGTH_SHORT).show()
            return
        }

        // Konversi URI ke File
        val file = File(cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Validasi ukuran file (maks 5MB)
        if (file.length() > 5 * 1024 * 1024) {
            Toast.makeText(this, "Gambar terlalu besar (maks 5MB)", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat RequestBody untuk file gambar
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val profilePicturePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)

        // Buat RequestBody untuk id_cashier
        val idCashierRequestBody = cashierId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Panggil API untuk upload
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        if (token == null) {
            Toast.makeText(this, "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val apiService = RetrofitClient.getInstance(this)
        apiService.updateCashierProfile(idCashierRequestBody, profilePicturePart).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ProfileEmployeeActivity, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()

                    // Perbarui UI dengan URL baru
                    val newProfilePictureUrl = try {
                        val messageStr = response.body()?.message?.toString()
                        if (!messageStr.isNullOrEmpty() && messageStr.contains("profile_picture_url")) {
                            val urlPattern = """"profile_picture_url"\s*:\s*"([^"]+)"""".toRegex()
                            val matchResult = urlPattern.find(messageStr)
                            matchResult?.groupValues?.get(1) ?: messageStr
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileEmployeeActivity", "Error parsing profile picture URL: ${e.message}")
                        null
                    }

                    if (!newProfilePictureUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileEmployeeActivity)
                            .load(newProfilePictureUrl)
                            .placeholder(R.drawable.img_avatar)
                            .error(R.drawable.img_avatar)
                            .circleCrop()
                            .into(binding.imgMenu)
                        intent.putExtra("EMPLOYEE_PROFILE_PICTURE", newProfilePictureUrl)
                    } else {
                        Log.w("ProfileEmployeeActivity", "No valid profile picture URL in response")
                    }
                } else {
                    val errorMessage = response.body()?.message?.toString() ?: "Unknown error"
                    if (response.code() == 401 || errorMessage.contains("Unauthorized", true)) {
                        Toast.makeText(this@ProfileEmployeeActivity, "Hanya kasir sendiri yang dapat mengubah foto profil", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileEmployeeActivity, "Gagal mengunggah foto: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("ProfileEmployeeActivity", "Upload error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProfileEmployeeActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProfileEmployeeActivity", "Upload failure: ${t.message}", t)
            }
        })
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.view_dialog_confirm_delete_employee, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = builder.create()

        // Get references to the buttons
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_employee)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_delete_employee)

        // Handle Cancel button
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        // Handle Delete button
        btnDelete.setOnClickListener {
            deleteCashier()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun deleteCashier() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null && cashierId > 0) {
            val apiService = RetrofitClient.getInstance(this)
            apiService.deleteCashier(cashierId, "Bearer $token").enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            Toast.makeText(this@ProfileEmployeeActivity, "Cashier berhasil dihapus", Toast.LENGTH_SHORT).show()
                            // Kembali ke AllEmployeeActivity setelah penghapusan
                            val intent = Intent(this@ProfileEmployeeActivity, AllEmployeeActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@ProfileEmployeeActivity, "Gagal menghapus cashier: ${body?.message}", Toast.LENGTH_SHORT).show()
                            Log.e("ProfileEmployeeActivity", "Delete error: ${response.errorBody()?.string()}")
                        }
                    } else {
                        Toast.makeText(this@ProfileEmployeeActivity, "Gagal menghapus cashier", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileEmployeeActivity", "Delete error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileEmployeeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileEmployeeActivity", "Delete failure: ${t.message}", t)
                }
            })
        } else {
            Toast.makeText(this@ProfileEmployeeActivity, "Tidak ada sesi aktif atau ID tidak valid", Toast.LENGTH_SHORT).show()
        }
    }
}