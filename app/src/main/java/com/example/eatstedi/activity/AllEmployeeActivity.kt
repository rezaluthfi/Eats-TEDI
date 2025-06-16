package com.example.eatstedi.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.CashierResponse
import com.example.eatstedi.databinding.ActivityAllEmployeeBinding
import com.example.eatstedi.model.Employee
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class AllEmployeeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAllEmployeeBinding.inflate(layoutInflater) }
    private val employeeList = mutableListOf<Employee>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.ivArrowBack.setOnClickListener {
            val intent = Intent(this@AllEmployeeActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnAddEmployee.setOnClickListener {
            val intent = Intent(this@AllEmployeeActivity, AddEmployeeActivity::class.java)
            startActivity(intent)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterEmployeeTable(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fetchEmployees()
    }

    private fun fetchEmployees() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getCashiers().enqueue(object : Callback<CashierResponse> {
            override fun onResponse(call: Call<CashierResponse>, response: Response<CashierResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        employeeList.clear()
                        employeeList.addAll(body.data)
                        addEmployeeDataToTable(employeeList)
                        Log.d("AllEmployeeActivity", "Employees fetched: ${employeeList.size} entries")
                    } else {
                        Toast.makeText(this@AllEmployeeActivity, "Gagal mengambil data karyawan", Toast.LENGTH_SHORT).show()
                        Log.e("AllEmployeeActivity", "Fetch employees error: ${response.errorBody()?.string()}")
                        addEmployeeDataToTable(emptyList()) // Tampilkan tabel kosong dengan header
                    }
                } else {
                    Toast.makeText(this@AllEmployeeActivity, "Gagal mengambil data karyawan", Toast.LENGTH_SHORT).show()
                    Log.e("AllEmployeeActivity", "Fetch employees error: ${response.errorBody()?.string()}")
                    addEmployeeDataToTable(emptyList())
                }
            }

            override fun onFailure(call: Call<CashierResponse>, t: Throwable) {
                Toast.makeText(this@AllEmployeeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("AllEmployeeActivity", "Fetch employees failure: ${t.message}", t)
                addEmployeeDataToTable(emptyList())
            }
        })
    }

    private fun addEmployeeDataToTable(employeeList: List<Employee>) {
        val tableLayout = binding.tableView
        tableLayout.removeAllViews()

        // Tambahkan header tabel
        val headerRow = TableRow(this@AllEmployeeActivity).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.secondary))
        }

        headerRow.addView(createTextView("Nama Karyawan", isHeader = true))
        headerRow.addView(createTextView("Status", isHeader = true))
        headerRow.addView(createTextView("Nama Pengguna", isHeader = true))
        headerRow.addView(createTextView("No. Telepon", isHeader = true))
        headerRow.addView(createTextView("Gaji", isHeader = true))

        tableLayout.addView(headerRow)

        if (employeeList.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.tableView.visibility = View.VISIBLE // Tetap tampilkan header
            Log.d("AllEmployeeActivity", "No employees to display")
            return
        }

        binding.tvNoData.visibility = View.GONE
        binding.tableView.visibility = View.VISIBLE

        for (employee in employeeList) {
            val row = TableRow(this@AllEmployeeActivity).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.white))
            }

            if (employee.id <= 0) {
                Log.e("AllEmployeeActivity", "Invalid employee ID: ${employee.id} for ${employee.name}")
                continue // Lewati karyawan dengan ID tidak valid
            }

            val nameTextView = createTextView(employee.name).apply {
                setOnClickListener {
                    Log.d("AllEmployeeActivity", "Navigating to ProfileEmployeeActivity with EMPLOYEE_ID: ${employee.id}")
                    val intent = Intent(this@AllEmployeeActivity, ProfileEmployeeActivity::class.java).apply {
                        putExtra("EMPLOYEE_ID", employee.id)
                        putExtra("EMPLOYEE_NAME", employee.name)
                        putExtra("EMPLOYEE_STATUS", employee.status)
                        putExtra("EMPLOYEE_USERNAME", employee.username)
                        putExtra("EMPLOYEE_PHONE", employee.no_telp)
                        putExtra("EMPLOYEE_SALARY", employee.salary)
                        putExtra("EMPLOYEE_PROFILE_PICTURE", employee.profile_picture ?: "")
                    }
                    startActivity(intent)
                }
            }

            val salaryFormatted = employee.salary?.let {
                "Rp${NumberFormat.getNumberInstance(Locale("id", "ID")).format(it)}"
            } ?: "-"

            row.addView(nameTextView)
            row.addView(createTextView(employee.status ?: "-"))
            row.addView(createTextView(employee.username ?: "-"))
            row.addView(createTextView(employee.no_telp ?: "-"))
            row.addView(createTextView(salaryFormatted))

            tableLayout.addView(row)
            Log.d("AllEmployeeActivity", "Added employee: ID=${employee.id}, Name=${employee.name}, Salary=$salaryFormatted")
        }
    }

    private fun createTextView(text: Any, isHeader: Boolean = false): TextView {
        return TextView(this@AllEmployeeActivity).apply {
            this.text = text.toString()
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun filterEmployeeTable(query: String) {
        val filteredList = employeeList.filter { employee ->
            employee.name.contains(query, ignoreCase = true) ||
                    (employee.username?.contains(query, ignoreCase = true) ?: false) ||
                    (employee.no_telp?.contains(query, ignoreCase = true) ?: false)
        }

        addEmployeeDataToTable(filteredList)
        Log.d("AllEmployeeActivity", "Filtered employees: ${filteredList.size} entries for query '$query'")
    }
}