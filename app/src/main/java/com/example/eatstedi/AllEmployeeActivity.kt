package com.example.eatstedi

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityAllEmployeeBinding
import com.example.eatstedi.model.Employee

class AllEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAllEmployeeBinding.inflate(layoutInflater)
    }

    // Data karyawan dummy
    private val employeeList = listOf(
        Employee("John Doe", "Aktif", "jdoe", "jdoe@example.com", "081234567890", "Rp 5,000,000"),
        Employee("Jane Smith", "Tidak Aktif", "jsmith", "jsmith@example.com", "081234567891", "Rp 4,500,000"),
        Employee("Michael Johnson", "Aktif", "mjohnson", "mjohnson@example.com", "081234567892", "Rp 6,200,000"),
        Employee("Emily Davis", "Aktif", "edavis", "edavis@example.com", "081234567893", "Rp 5,800,000"),
        Employee("Daniel Brown", "Tidak Aktif", "dbrown", "dbrown@example.com", "081234567894", "Rp 4,300,000"),
        Employee("Olivia Wilson", "Aktif", "owilson", "owilson@example.com", "081234567895", "Rp 5,200,000"),
        Employee("James Martinez", "Aktif", "jmartinez", "jmartinez@example.com", "081234567896", "Rp 5,600,000"),
        Employee("Sophia Anderson", "Tidak Aktif", "sanderson", "sanderson@example.com", "081234567897", "Rp 4,700,000"),
        Employee("David Thomas", "Aktif", "dthomas", "dthomas@example.com", "081234567898", "Rp 5,400,000"),
        Employee("Isabella Jackson", "Aktif", "ijackson", "ijackson@example.com", "081234567899", "Rp 5,700,000"),
        Employee("Joseph White", "Tidak Aktif", "jwhite", "jwhite@example.com", "081234567800", "Rp 4,900,000"),
        Employee("Mia Harris", "Aktif", "mharris", "mharris@example.com", "081234567801", "Rp 5,300,000"),
        Employee("William Nelson", "Aktif", "wnelson", "wnelson@example.com", "081234567802", "Rp 5,900,000")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Mengatur padding sesuai dengan insets sistem
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Tombol kembali ke MainActivity
        binding.ivArrowBack.setOnClickListener {
            val intent = Intent(this@AllEmployeeActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Tombol untuk menambah karyawan (dummy)
        binding.btnAddEmployee.setOnClickListener {
            val intent = Intent(this@AllEmployeeActivity, AddEmployeeActivity::class.java)
            startActivity(intent)
        }

        // Mengisi tabel dengan data karyawan
        addEmployeeDataToTable(employeeList)

        // Fitur pencarian otomatis tanpa menekan enter
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterEmployeeTable(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
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
        headerRow.addView(createTextView("Nama Pengguna", isHeader = true))
        headerRow.addView(createTextView("Email", isHeader = true))
        headerRow.addView(createTextView("No. Telepon", isHeader = true))
        headerRow.addView(createTextView("Salary", isHeader = true))

        tableLayout.addView(headerRow)

        // Menambahkan data employee ke dalam tabel
        for (employee in employeeList) {
            val row = TableRow(this@AllEmployeeActivity).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.white))
            }

            // Buat TextView untuk nama karyawan yang dapat diklik
            val nameTextView = createTextView(employee.name)
            nameTextView.setOnClickListener {
                // Ketika nama diklik, buka ProfileEmployeeActivity dan kirim data karyawan
                val intent = Intent(this@AllEmployeeActivity, ProfileEmployeeActivity::class.java).apply {
                    putExtra("EMPLOYEE_NAME", employee.name)
                    putExtra("EMPLOYEE_STATUS", employee.status)
                    putExtra("EMPLOYEE_USERNAME", employee.username)
                    putExtra("EMPLOYEE_EMAIL", employee.email)
                    putExtra("EMPLOYEE_PHONE", employee.phone)
                    putExtra("EMPLOYEE_SALARY", employee.salary)
                }
                startActivity(intent)
            }

            row.addView(nameTextView)
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
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    // Fungsi untuk memfilter karyawan berdasarkan input pencarian
    private fun filterEmployeeTable(query: String) {
        val filteredList = employeeList.filter { employee ->
            employee.name.contains(query, ignoreCase = true) ||
                    employee.username.contains(query, ignoreCase = true) ||
                    employee.email.contains(query, ignoreCase = true)
        }

        if (filteredList.isEmpty()) {
            // Tampilkan pesan "tidak ada data" jika tidak ditemukan hasil pencarian
            binding.tvNoData.visibility = View.VISIBLE
            binding.tableView.visibility = View.GONE
        } else {
            // Sembunyikan pesan "tidak ada data" jika hasil pencarian ditemukan
            binding.tvNoData.visibility = View.GONE
            binding.tableView.visibility = View.VISIBLE
        }

        // Mengisi tabel dengan data yang difilter
        addEmployeeDataToTable(filteredList)
    }

}
