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
import com.example.eatstedi.api.service.SearchSupplierRequest
import com.example.eatstedi.api.service.SupplierResponse
import com.example.eatstedi.databinding.ActivityAllSupplierBinding
import com.example.eatstedi.model.Supplier
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class AllSupplierActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllSupplierBinding
    private var originalSupplierList = mutableListOf<Supplier>()
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val profileActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data?.getBooleanExtra("isDataUpdated", false) == true) {
                Log.d("AllSupplierActivity", "Data updated, refreshing supplier list.")
                fetchSuppliers()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllSupplierBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupSearch()
        fetchSuppliers()
    }

    private fun setupClickListeners() {
        with(binding) {
            ivArrowBack.setOnClickListener { finish() }
            btnAddNewSupplier.setOnClickListener {
                val intent = Intent(this@AllSupplierActivity, AddSupplierActivity::class.java)
                profileActivityLauncher.launch(intent)
            }
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
                        searchSupplier(query)
                    } else {
                        addSupplierDataToTable(originalSupplierList)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun fetchSuppliers() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getSuppliers().enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val suppliers = response.body()?.data ?: emptyList()
                    originalSupplierList.clear()
                    originalSupplierList.addAll(suppliers)
                    addSupplierDataToTable(originalSupplierList)
                } else {
                    val errorMessage = response.body()?.message ?: "Gagal mengambil data pemasok"
                    Toast.makeText(this@AllSupplierActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                Toast.makeText(this@AllSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchSupplier(query: String) {

        binding.tvNoData.visibility = View.GONE

        val apiService = RetrofitClient.getInstance(this)
        apiService.searchSupplier(SearchSupplierRequest(query)).enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                // Kondisi 1: Respons sukses (kode 2xx)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        // Sukses dan ada data
                        val searchResults = body.data ?: emptyList()
                        addSupplierDataToTable(searchResults)

                        // Tampilkan pesan jika hasilnya kosong
                        if (searchResults.isEmpty()) {
                            Toast.makeText(this@AllSupplierActivity, "Tidak ada supplier yang cocok dengan '$query'", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Sukses tapi flag success=false dari backend
                        addSupplierDataToTable(emptyList())
                        val message = body?.message ?: "Supplier tidak ditemukan"
                        Toast.makeText(this@AllSupplierActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                // Kondisi 2: Respons GAGAL (kode 4xx atau 5xx), tapi mungkin ada pesan di errorBody
                else {
                    addSupplierDataToTable(emptyList()) // Pastikan tabel kosong

                    // ================== PERBAIKAN UTAMA DI SINI ==================
                    try {
                        // Coba baca errorBody dan parse JSON-nya
                        val errorBodyString = response.errorBody()?.string()
                        if (errorBodyString != null) {
                            // Asumsi error body memiliki struktur yang sama dengan SupplierResponse
                            val errorResponse = Gson().fromJson(errorBodyString, SupplierResponse::class.java)
                            val message = errorResponse.message ?: "Gagal mencari supplier"
                            Toast.makeText(this@AllSupplierActivity, message, Toast.LENGTH_SHORT).show()
                        } else {
                            // Jika errorBody null, tampilkan pesan generik
                            Toast.makeText(this@AllSupplierActivity, "Terjadi kesalahan (Kode: ${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // Jika terjadi error saat parsing JSON
                        Toast.makeText(this@AllSupplierActivity, "Gagal memproses respons error", Toast.LENGTH_SHORT).show()
                        Log.e("AllSupplierActivity", "Error parsing error body", e)
                    }
                    // =============================================================
                }
            }

            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {

                Toast.makeText(this@AllSupplierActivity, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addSupplierDataToTable(supplierList: List<Supplier>) {
        binding.tableView.removeAllViews()

        binding.tvNoData.visibility = if (supplierList.isEmpty()) View.VISIBLE else View.GONE
        binding.tableView.visibility = if (supplierList.isEmpty()) View.GONE else View.VISIBLE

        if (supplierList.isEmpty()) return

        val headerRow = TableRow(this).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.secondary))
        }
        headerRow.addView(createTextView("Nama Supplier", isHeader = true))
        headerRow.addView(createTextView("Status", isHeader = true))
        headerRow.addView(createTextView("Nama Pengguna", isHeader = true))
        headerRow.addView(createTextView("No. Telepon", isHeader = true))
        headerRow.addView(createTextView("Pemasukan", isHeader = true))
        binding.tableView.addView(headerRow)

        val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
        supplierList.forEach { supplier ->
            val row = TableRow(this).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.white))
                setOnClickListener {
                    val intent = Intent(this@AllSupplierActivity, ProfileSupplierActivity::class.java).apply {
                        putExtra("SUPPLIER_ID", supplier.id)
                        putExtra("SUPPLIER_NAME", supplier.name)
                        putExtra("SUPPLIER_STATUS", supplier.status)
                        putExtra("SUPPLIER_USERNAME", supplier.username)
                        putExtra("SUPPLIER_PHONE", supplier.no_telp)
                    }
                    profileActivityLauncher.launch(intent)
                }
            }
            row.addView(createTextView(supplier.name))
            row.addView(createTextView(supplier.status))
            row.addView(createTextView(supplier.username))
            row.addView(createTextView(supplier.no_telp))
            row.addView(createTextView("Rp ${numberFormat.format(supplier.income)}"))
            binding.tableView.addView(row)
        }
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }
}