package com.example.eatstedi.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
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
// IMPORT BINDING UNTUK DIALOG KONFIRMASI HAPUS
import com.example.eatstedi.databinding.ViewDialogConfirmDeleteEmployeeBinding
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
            // Hanya tampilkan pratinjau dan minta password jika pengguna adalah kasir sendiri
            val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            val loggedInUserId = sharedPreferences.getInt("user_id", 0)
            if (cashierId == loggedInUserId) {
                Glide.with(this@ProfileEmployeeActivity)
                    .load(it)
                    .circleCrop()
                    .into(binding.imgMenu)
                showPasswordDialog()
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
                val salary = intent.getIntExtra("EMPLOYEE_SALARY", 0)
                val status = intent.getStringExtra("EMPLOYEE_STATUS") ?: "N/A"

                val scheduleIntent = if (isAdmin) {
                    Intent(this@ProfileEmployeeActivity, AddScheduleEmployeeActivity::class.java)
                } else {
                    Intent(this@ProfileEmployeeActivity, ScheduleEmployeeActivity::class.java)
                }.apply {
                    putExtra("EMPLOYEE_ID", cashierId)
                    putExtra("EMPLOYEE_NAME", name)
                    putExtra("EMPLOYEE_USERNAME", username)
                    putExtra("EMPLOYEE_PHONE", phone)
                    putExtra("EMPLOYEE_SALARY", salary)
                    putExtra("EMPLOYEE_STATUS", status)
                }
                startActivity(scheduleIntent)
            }

            // Tombol hapus
            if (!isAdmin) {
                tvDelete.visibility = View.GONE // Sembunyikan tombol hapus jika bukan admin
            } else {
                tvDelete.setOnClickListener {
                    // ===== BAGIAN YANG DIUBAH =====
                    // Panggil dialog konfirmasi hapus
                    showDeleteEmployeeConfirmationDialog()
                    // ==============================
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

        with(binding) {
            tvEmployeeName.text = name
            etUsername.setText(username)
            etPhoneNumber.setText(noTelp)
            etStatus.setText(status)
            etSalary.setText(salary.toString())

            // Load profile picture menggunakan endpoint get-cashier-photo-profile
            loadProfilePicture()
        }

        // Log untuk debugging
        Log.d("ProfileEmployeeActivity", "Displayed Name: $name, ID: $cashierId, Username: $username, Phone: $noTelp, Salary: $salary, Status: $status")
    }

    private fun loadProfilePicture() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getCashierPhotoProfile(cashierId).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val imageBytes = response.body()!!.bytes()
                        Glide.with(this@ProfileEmployeeActivity)
                            .load(imageBytes)
                            .placeholder(R.drawable.img_avatar)
                            .error(R.drawable.img_avatar)
                            .circleCrop()
                            .into(binding.imgMenu)
                        Log.d("ProfileEmployeeActivity", "Profile picture loaded successfully")
                    } catch (e: Exception) {
                        Log.e("ProfileEmployeeActivity", "Error converting ResponseBody to bytes: ${e.message}", e)
                        binding.imgMenu.setImageResource(R.drawable.img_avatar)
                    }
                } else {
                    binding.imgMenu.setImageResource(R.drawable.img_avatar)
                    Log.w("ProfileEmployeeActivity", "Failed to load profile picture: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                binding.imgMenu.setImageResource(R.drawable.img_avatar)
                Log.e("ProfileEmployeeActivity", "Error loading profile picture: ${t.message}", t)
            }
        })
    }

    private fun disableInputFields() {
        with(binding) {
            etUsername.isEnabled = false
            etUsername.isFocusable = false
            etUsername.isFocusableInTouchMode = false

            etPhoneNumber.isEnabled = false
            etPhoneNumber.isFocusable = false
            etPhoneNumber.isFocusableInTouchMode = false

            etStatus.isEnabled = false
            etStatus.isFocusable = false
            etStatus.isFocusableInTouchMode = false

            etSalary.isEnabled = false
            etSalary.isFocusable = false
            etSalary.isFocusableInTouchMode = false
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
            binding.imgMenu.setImageResource(R.drawable.img_avatar)
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

        val name = intent.getStringExtra("EMPLOYEE_NAME") ?: return
        val username = intent.getStringExtra("EMPLOYEE_USERNAME") ?: return
        val noTelp = intent.getStringExtra("EMPLOYEE_PHONE") ?: return
        val salary = intent.getIntExtra("EMPLOYEE_SALARY", 0).toString()
        val status = intent.getStringExtra("EMPLOYEE_STATUS") ?: return

        val idCashierRequestBody = cashierId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val nameRequestBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameRequestBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val noTelpRequestBody = noTelp.toRequestBody("text/plain".toMediaTypeOrNull())
        val salaryRequestBody = salary.toRequestBody("text/plain".toMediaTypeOrNull())
        val statusRequestBody = status.toRequestBody("text/plain".toMediaTypeOrNull())
        val passwordRequestBody = password.toRequestBody("text/plain".toMediaTypeOrNull())

        Log.d("ProfileEmployeeActivity", "Uploading: id_cashier=$cashierId, name=$name, username=$username, no_telp=$noTelp, salary=$salary, status=$status, password=****")

        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        if (token == null) {
            Toast.makeText(this, "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val apiService = RetrofitClient.getInstance(this)
        apiService.updateCashierProfile(
            idCashier = idCashierRequestBody,
            name = nameRequestBody,
            username = usernameRequestBody,
            noTelp = noTelpRequestBody,
            salary = salaryRequestBody,
            status = statusRequestBody,
            password = passwordRequestBody,
            profilePicture = profilePicturePart
        ).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ProfileEmployeeActivity, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    loadProfilePicture()
                } else {
                    val errorMessage = response.body()?.message?.toString() ?: response.errorBody()?.string() ?: "Unknown error"
                    if (response.code() == 401 || errorMessage.contains("Unauthorized", true)) {
                        Toast.makeText(this@ProfileEmployeeActivity, "Hanya kasir sendiri yang dapat mengubah foto profil", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileEmployeeActivity, "Gagal mengunggah foto: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                    Log.e("ProfileEmployeeActivity", "Upload error: $errorMessage")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProfileEmployeeActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProfileEmployeeActivity", "Upload failure: ${t.message}", t)
            }
        })
    }

    // ===== FUNGSI BARU UNTUK DIALOG KONFIRMASI HAPUS =====
    private fun showDeleteEmployeeConfirmationDialog() {
        // Inflate layout dialog menggunakan View Binding
        val dialogBinding = ViewDialogConfirmDeleteEmployeeBinding.inflate(layoutInflater)

        // Buat AlertDialog
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)

        val dialog = builder.create()

        // Atur listener untuk tombol batal
        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        // Atur listener untuk tombol hapus
        dialogBinding.btnConfirmDelete.setOnClickListener {
            deleteCashier() // Panggil fungsi untuk menghapus kasir
            dialog.dismiss()
        }

        // Buat background dialog transparan agar sudut membulat terlihat
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Tampilkan dialog
        dialog.show()
    }
    // =========================================================

    private fun deleteCashier() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null && cashierId > 0) {
            val apiService = RetrofitClient.getInstance(this)
            apiService.deleteCashier(cashierId,"Bearer $token").enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            Toast.makeText(this@ProfileEmployeeActivity, "Karyawan berhasil dihapus", Toast.LENGTH_SHORT).show()
                            // Kembali ke AllEmployeeActivity setelah penghapusan
                            val intent = Intent(this@ProfileEmployeeActivity, AllEmployeeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@ProfileEmployeeActivity, "Gagal menghapus karyawan: ${body?.message}", Toast.LENGTH_SHORT).show()
                            Log.e("ProfileEmployeeActivity", "Delete error: ${body?.message}")
                        }
                    } else {
                        Toast.makeText(this@ProfileEmployeeActivity, "Gagal menghapus karyawan (Error: ${response.code()})", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileEmployeeActivity", "Delete error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileEmployeeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileEmployeeActivity", "Delete failure: ${t.message}", t)
                }
            })
        } else {
            Toast.makeText(this, "Tidak ada sesi aktif atau ID tidak valid", Toast.LENGTH_SHORT).show()
        }
    }
}