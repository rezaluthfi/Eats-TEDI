package com.example.eatstedi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.eatstedi.databinding.ActivityProfileEmployeeBinding

class ProfileEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityProfileEmployeeBinding.inflate(layoutInflater)
    }

    private var isEditing = false // Menandakan apakah dalam mode edit

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

        // Menampilkan data pada EditText
        with(binding) {
            tvEmployeeName.text = employeeName
            etName.setText(employeeName)
            etStatus.setText(employeeStatus)
            etUsername.setText(employeeUsername)
            etEmail.setText(employeeEmail)
            etPhoneNumber.setText(employeePhone)

            // Tombol kembali
            ivArrowBack.setOnClickListener {
                finish()
            }

            // Tombol jadwal
            tvSchedule.setOnClickListener {
                val intent = Intent(this@ProfileEmployeeActivity, ScheduleEmployeeActivity::class.java).apply {
                    putExtra("EMPLOYEE_NAME", employeeName)
                    putExtra("EMPLOYEE_USERNAME", employeeUsername)
                    putExtra("EMPLOYEE_EMAIL", employeeEmail)
                    putExtra("EMPLOYEE_PHONE", employeePhone)
                    putExtra("EMPLOYEE_SALARY", employeeSalary)
                }
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
                if (isEditing) {
                    // Simpan perubahan
                    saveUserData()
                    // Kembali ke mode tidak edit
                    setEditTextEnabled(false)
                    btnEdit.text = "Edit"
                } else {
                    // Masuk ke mode edit
                    setEditTextEnabled(true)
                    btnEdit.text = "Simpan"
                }
                isEditing = !isEditing // Toggle mode editing
            }

            // Set editable false
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
            etStatus.isFocusable = enabled // Status sekarang bisa diedit
            etStatus.isFocusableInTouchMode = enabled
            etUsername.isFocusable = enabled
            etUsername.isFocusableInTouchMode = enabled
            etAddress.isFocusable = enabled
            etAddress.isFocusableInTouchMode = enabled
            etEmail.isFocusable = enabled
            etEmail.isFocusableInTouchMode = enabled
            etPhoneNumber.isFocusable = enabled
            etPhoneNumber.isFocusableInTouchMode = enabled
        }
    }

    private fun saveUserData() {
        val employeeName = binding.etName.text.toString()
        employeeName.let {
            binding.tvEmployeeName.text = it
        }
        val address = binding.etAddress.text.toString()
        val name = binding.etName.text.toString()
        val status = binding.etStatus.text.toString() // Ambil status yang diedit
        val username = binding.etUsername.text.toString()
        val email = binding.etEmail.text.toString()
        val phoneNumber = binding.etPhoneNumber.text.toString()

        // Log untuk melihat data yang disimpan
        println("User Data Saved: Name: $name, Status: $status, Username: $username, Address: $address, Email: $email, Phone: $phoneNumber")

        // Tampilkan toast untuk memberi tahu pengguna bahwa data telah diperbarui
        Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
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
            // Handle the deletion of the employee
            // Add your deletion logic here

            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
