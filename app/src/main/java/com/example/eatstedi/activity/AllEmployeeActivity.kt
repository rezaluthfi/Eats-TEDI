package com.example.eatstedi.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.CashierResponse
import com.example.eatstedi.api.service.SearchCashierRequest
import com.example.eatstedi.databinding.ActivityAllEmployeeBinding
import com.example.eatstedi.model.Employee
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class AllEmployeeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAllEmployeeBinding.inflate(layoutInflater) }
    // Simpan daftar asli untuk dikembalikan saat pencarian kosong
    private var originalEmployeeList = mutableListOf<Employee>()
    // Handler untuk menunda pencarian (debounce)
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    // Launcher untuk menangani hasil kembali dari activity lain
    private val activityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Jika ada data yang diupdate atau dihapus, muat ulang daftar
                Log.d("AllEmployeeActivity", "Data potentially updated, refreshing employee list.")
                fetchEmployees()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupSearch()
        fetchEmployees()
    }

    private fun setupClickListeners() {
        binding.ivArrowBack.setOnClickListener { finish() }
        binding.btnAddEmployee.setOnClickListener {
            val intent = Intent(this, AddEmployeeActivity::class.java)
            activityLauncher.launch(intent)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
            }
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                searchRunnable = Runnable {
                    if (query.isNotEmpty()) {
                        searchEmployees(query)
                    } else {
                        // Jika query kosong, tampilkan kembali daftar asli
                        addEmployeeDataToTable(originalEmployeeList)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun fetchEmployees() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getCashiers().enqueue(object : Callback<CashierResponse> {
            override fun onResponse(call: Call<CashierResponse>, response: Response<CashierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val employees = response.body()?.data ?: emptyList()
                    originalEmployeeList.clear()
                    originalEmployeeList.addAll(employees)
                    addEmployeeDataToTable(originalEmployeeList)
                } else {
                    Toast.makeText(this@AllEmployeeActivity, "Gagal mengambil data karyawan", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<CashierResponse>, t: Throwable) {
                Toast.makeText(this@AllEmployeeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchEmployees(query: String) {
        val apiService = RetrofitClient.getInstance(this)
        apiService.searchCashiers(SearchCashierRequest(query)).enqueue(object : Callback<CashierResponse> {
            override fun onResponse(call: Call<CashierResponse>, response: Response<CashierResponse>) {
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    val searchResults = body.data ?: emptyList()
                    addEmployeeDataToTable(searchResults)
                    if (searchResults.isEmpty()) {
                        Toast.makeText(this@AllEmployeeActivity, "Tidak ada karyawan yang cocok dengan '$query'", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    addEmployeeDataToTable(emptyList())
                    // Coba baca pesan error dari backend
                    try {
                        val errorBodyString = response.errorBody()?.string()
                        if (errorBodyString != null) {
                            val errorResponse = Gson().fromJson(errorBodyString, CashierResponse::class.java)
                            val message = errorResponse.message ?: "Karyawan tidak ditemukan"
                            Toast.makeText(this@AllEmployeeActivity, message, Toast.LENGTH_SHORT).show()
                        } else {
                            val message = body?.message ?: "Gagal mencari karyawan"
                            Toast.makeText(this@AllEmployeeActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@AllEmployeeActivity, "Gagal memproses respons error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<CashierResponse>, t: Throwable) {
                Toast.makeText(this@AllEmployeeActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addEmployeeDataToTable(employeeList: List<Employee>) {
        binding.tableView.removeAllViews()
        binding.tvNoData.visibility = if (employeeList.isEmpty()) View.VISIBLE else View.GONE
        binding.tableView.visibility = if (employeeList.isEmpty()) View.GONE else View.VISIBLE
        if (employeeList.isEmpty()) return

        val headerRow = TableRow(this).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.secondary))
        }
        headerRow.addView(createTextView("Nama Karyawan", true))
        headerRow.addView(createTextView("Status", true))
        headerRow.addView(createTextView("Nama Pengguna", true))
        headerRow.addView(createTextView("No. Telepon", true))
        headerRow.addView(createTextView("Gaji", true))
        binding.tableView.addView(headerRow)

        val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
        employeeList.forEach { employee ->
            val row = TableRow(this).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.white))
                setOnClickListener {
                    val intent = Intent(this@AllEmployeeActivity, ProfileEmployeeActivity::class.java).apply {
                        putExtra("EMPLOYEE_ID", employee.id)
                        putExtra("EMPLOYEE_NAME", employee.name)
                        putExtra("EMPLOYEE_STATUS", employee.status)
                        putExtra("EMPLOYEE_USERNAME", employee.username)
                        putExtra("EMPLOYEE_PHONE", employee.no_telp)
                        putExtra("EMPLOYEE_SALARY", employee.salary)
                    }
                    activityLauncher.launch(intent)
                }
            }
            row.addView(createTextView(employee.name))
            row.addView(createTextView(employee.status ?: "-"))
            row.addView(createTextView(employee.username ?: "-"))
            row.addView(createTextView(employee.no_telp ?: "-"))
            val salaryFormatted = employee.salary?.let { "Rp${numberFormat.format(it)}" } ?: "-"
            row.addView(createTextView(salaryFormatted))
            binding.tableView.addView(row)
        }
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(this@AllEmployeeActivity, R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }
}