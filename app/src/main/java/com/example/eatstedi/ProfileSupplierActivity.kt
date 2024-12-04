package com.example.eatstedi

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityProfileSupplierBinding

class ProfileSupplierActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityProfileSupplierBinding.inflate(layoutInflater)
    }

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
            // Display data in TextView
            tvSupplierName.text = supplierName
            tvName.text = supplierName
            tvStatus.text = supplierStatus
            tvUsername.text = supplierUsername
            tvEmail.text = supplierEmail
            tvPhoneNumber.text = supplierPhone

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
                // Handle the edit button click
            }

            // Delete button
            tvDelete.setOnClickListener {
                // Show the confirmation dialog
                showDeleteConfirmationDialog()
            }
        }
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
