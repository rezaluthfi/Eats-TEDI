package com.example.eatstedi.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.UpdateSupplierResponse
import com.example.eatstedi.databinding.ActivityProfileSupplierBinding
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

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                Glide.with(this@ProfileSupplierActivity)
                    .load(it)
                    .into(binding.imgSupplier)
            }
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
        val supplierName = intent.getStringExtra("SUPPLIER_NAME")
        val supplierStatus = intent.getStringExtra("SUPPLIER_STATUS")
        val supplierUsername = intent.getStringExtra("SUPPLIER_USERNAME")
        val supplierPhone = intent.getStringExtra("SUPPLIER_PHONE")
        val supplierImage = intent.getStringExtra("SUPPLIER_IMAGE")

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
            if (!supplierImage.isNullOrEmpty() && supplierImage != "noimage.jpg") {
                Glide.with(this@ProfileSupplierActivity)
                    .load("http://10.0.2.2:8000/storage/$supplierImage")
                    .placeholder(R.drawable.img_avatar)
                    .into(imgSupplier)
            } else {
                imgSupplier.setImageResource(R.drawable.img_avatar)
            }

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
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickImageLauncher.launch(intent)
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
                val file = File(getRealPathFromURI(it))
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
            }

            val apiService = RetrofitClient.getInstance(this@ProfileSupplierActivity)
            apiService.updateSupplier(supplierId, nameBody, usernameBody, phoneBody, statusBody, imagePart).enqueue(object : Callback<UpdateSupplierResponse> {
                override fun onResponse(call: Call<UpdateSupplierResponse>, response: Response<UpdateSupplierResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        tvSupplierName.text = name
                        if (selectedImageUri != null) {
                            Glide.with(this@ProfileSupplierActivity)
                                .load(selectedImageUri)
                                .into(imgSupplier)
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

    private fun getRealPathFromURI(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val idx = cursor?.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        val path = cursor?.getString(idx ?: 0) ?: ""
        cursor?.close()
        return path
    }
}