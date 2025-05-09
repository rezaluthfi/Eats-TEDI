package com.example.eatstedi.activity

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.R
import com.example.eatstedi.databinding.ActivityProfileSupplierBinding

class ProfileSupplierActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityProfileSupplierBinding.inflate(layoutInflater)
    }

    private var isEditing = false // Menandakan apakah dalam mode edit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Set the padding to avoid content being hidden behind system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve data passed through Intent
        val supplierName = intent.getStringExtra("SUPPLIER_NAME")
        val supplierStatus = intent.getStringExtra("SUPPLIER_STATUS")
        val supplierUsername = intent.getStringExtra("SUPPLIER_USERNAME")
        val supplierEmail = intent.getStringExtra("SUPPLIER_EMAIL")
        val supplierPhone = intent.getStringExtra("SUPPLIER_PHONE")
        val supplierIncome = intent.getStringExtra("SUPPLIER_INCOME")

        with(binding) {
            // Display data in EditText
            tvSupplierName.text = supplierName
            etName.setText(supplierName)
            etStatus.setText(supplierStatus)
            etUsername.setText(supplierUsername)
            etEmail.setText(supplierEmail)
            etPhoneNumber.setText(supplierPhone)

            // Back button
            ivArrowBack.setOnClickListener {
                finish()
            }

            // Cancel button
            btnCancel.setOnClickListener {
                finish()
            }

            // Edit button
            btnEdit.setOnClickListener {
                if (isEditing) {
                    // Save changes
                    saveSupplierData()
                    // Switch back to non-edit mode
                    setEditTextEnabled(false)
                    btnEdit.text = "Edit"
                } else {
                    // Switch to edit mode
                    setEditTextEnabled(true)
                    btnEdit.text = "Simpan"
                }
                isEditing = !isEditing // Toggle editing mode
            }

            // Delete button
            tvDelete.setOnClickListener {
                // Show the confirmation dialog
                showDeleteConfirmationDialog()
            }

            // Set editable false initially
            setEditTextEnabled(false)

            // Tambahkan listener untuk setiap EditText
            setEditTextClickListener()
        }
    }

    private fun setEditTextClickListener() {
        with(binding) {
            etName.setOnClickListener { showToastIfNotEditing() }
            etEmail.setOnClickListener { showToastIfNotEditing() }
            etAddress.setOnClickListener { showToastIfNotEditing() }
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
            etName.isFocusable = enabled
            etName.isFocusableInTouchMode = enabled
            etStatus.isFocusable = enabled
            etStatus.isFocusableInTouchMode = enabled
            etUsername.isFocusable = enabled
            etUsername.isFocusableInTouchMode = enabled
            etEmail.isFocusable = enabled
            etEmail.isFocusableInTouchMode = enabled
            etPhoneNumber.isFocusable = enabled
            etPhoneNumber.isFocusableInTouchMode = enabled
        }
    }

    private fun saveSupplierData() {
        // Get data from EditText
        val name = binding.etName.text.toString()
        val supplierName = binding.tvSupplierName.text.toString()
        supplierName.let {
            binding.tvSupplierName.text = name
        }
        val status = binding.etStatus.text.toString()
        val username = binding.etUsername.text.toString()
        val email = binding.etEmail.text.toString()
        val phoneNumber = binding.etPhoneNumber.text.toString()

        // Log to see the saved data
        println("Supplier Data Saved: Name: $name, Status: $status, Username: $username, Email: $email, Phone: $phoneNumber")

        // Show toast to inform the user that the data has been updated
        Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.view_dialog_confirm_delete_supplier, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = builder.create()

        // Get references to the buttons
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_supplier)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_delete_supplier)

        // Handle Cancel button
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        // Handle Delete button
        btnDelete.setOnClickListener {
            // Handle the deletion of the supplier
            // Add your deletion logic here

            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
