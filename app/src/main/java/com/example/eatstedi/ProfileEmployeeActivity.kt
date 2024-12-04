package com.example.eatstedi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.eatstedi.databinding.ActivityProfileEmployeeBinding

class ProfileEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityProfileEmployeeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Menerima data dari Intent
        val employeeName = intent.getStringExtra("EMPLOYEE_NAME")
        val employeeStatus = intent.getStringExtra("EMPLOYEE_STATUS")
        val employeeUsername = intent.getStringExtra("EMPLOYEE_USERNAME")
        val employeeEmail = intent.getStringExtra("EMPLOYEE_EMAIL")
        val employeePhone = intent.getStringExtra("EMPLOYEE_PHONE")
        val employeeSalary = intent.getStringExtra("EMPLOYEE_SALARY")

        // Menampilkan data pada TextView
        with(binding) {
            tvEmployeeName.text = employeeName
            tvName.text = employeeName
            tvStatus.text = employeeStatus
            tvUsername.text = employeeUsername
            tvEmail.text = employeeEmail
            tvPhoneNumber.text = employeePhone

            // Tombol kembali
            ivArrowBack.setOnClickListener {
                finish()
            }

            // Tombol jadwal
            tvSchedule.setOnClickListener {
                val intent = Intent(this@ProfileEmployeeActivity, ScheduleEmployeeActivity::class.java)
                startActivity(intent)
            }

            // Tombol hapus
            tvDelete.setOnClickListener {
                // Menampilkan dialog konfirmasi
                showDeleteConfirmationDialog()
            }

            // Tombol cancel
            btnCancel.setOnClickListener {
                finish()
            }

            // Tombol edit
            btnEdit.setOnClickListener {
                // Handle edit button click
            }
        }

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
            // Handle the deletion of the supplier
            // Add your deletion logic here

            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
