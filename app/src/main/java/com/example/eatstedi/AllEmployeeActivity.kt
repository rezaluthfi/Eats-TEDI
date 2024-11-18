package com.example.eatstedi

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityAllEmployeeBinding
import com.example.eatstedi.model.Employee

class AllEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAllEmployeeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {
            btnAddEmployee.setOnClickListener {
                val intent = Intent(this@AllEmployeeActivity, ProfileEmployeeActivity::class.java)
                startActivity(intent)
            }

            ivArrowBack.setOnClickListener {
                val intent = Intent(this@AllEmployeeActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }


        // Panggil fungsi untuk mengisi tabel dengan data dummy
        addEmployeeDataToTable(getDummyEmployeeData())
    }

    // Fungsi untuk menambahkan data employee ke dalam TableLayout
    private fun addEmployeeDataToTable(employeeList: List<Employee>) {
        val tableLayout = binding.tableView

        // Hapus semua baris yang ada jika diperlukan
        tableLayout.removeAllViews()

        // Menambahkan header untuk tabel
        val headerRow = TableRow(this@AllEmployeeActivity).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.secondary))
        }

        headerRow.addView(createTextView("Nama Employee", isHeader = true))
        headerRow.addView(createTextView("Status", isHeader = true))
        headerRow.addView(createTextView("Username", isHeader = true))
        headerRow.addView(createTextView("Email", isHeader = true))
        headerRow.addView(createTextView("No. Telepon", isHeader = true))
        headerRow.addView(createTextView("Salary", isHeader = true))

        tableLayout.addView(headerRow)

        // Menambahkan data employee ke dalam table
        for (employee in employeeList) {
            val row = TableRow(this@AllEmployeeActivity).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.white))
            }

            row.addView(createTextView(employee.name))
            row.addView(createTextView(employee.status))
            row.addView(createTextView(employee.username))
            row.addView(createTextView(employee.email))
            row.addView(createTextView(employee.phone))
            row.addView(createTextView(employee.salary))

            tableLayout.addView(row)
        }
    }

    // Fungsi untuk membuat TextView dengan gaya header atau konten biasa
    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(this@AllEmployeeActivity).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(this@AllEmployeeActivity, if (isHeader) R.color.black else R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f) // Menyediakan bobot agar lebar kolom sama
        }
    }


    // Data dummy untuk employee
    private fun getDummyEmployeeData(): List<Employee> {
        return listOf(
            Employee("John Doe", "Aktif", "jdoe", "jdoe@example.com", "081234567890", "Rp 5,000,000"),
            Employee("Jane Smith", "Tidak Aktif", "jsmith", "jsmith@example.com", "081234567891", "Rp 4,500,000"),
            Employee("Michael Johnson", "Aktif", "mjohnson", "mjohnson@example.com", "081234567892", "Rp 6,200,000"),
            Employee("Emily Davis", "Aktif", "edavis", "edavis@example.com", "081234567893", "Rp 5,800,000"),
            Employee("Daniel Brown", "Tidak Aktif", "dbrown", "dbrown@example.com", "081234567894", "Rp 4,300,000"),
            Employee("Olivia Wilson", "Aktif", "owilson", "owilson@example.com", "081234567895", "Rp 5,200,000")
        )
    }

}
