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
import com.example.eatstedi.api.service.SearchSupplierRequest
import com.example.eatstedi.api.service.SupplierResponse
import com.example.eatstedi.databinding.ActivityAllSupplierBinding
import com.example.eatstedi.model.Supplier
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class AllSupplierActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllSupplierBinding
    private val supplierList = mutableListOf<Supplier>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAllSupplierBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {
            ivArrowBack.setOnClickListener {
                val intent = Intent(this@AllSupplierActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            btnAddNewSupplier.setOnClickListener {
                val intent = Intent(this@AllSupplierActivity, AddSupplierActivity::class.java)
                startActivity(intent)
            }

            etSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s.toString().trim()
                    if (query.length >= 2) {
                        searchSupplier(query)
                    } else {
                        fetchSuppliers()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        fetchSuppliers()
    }

    private fun fetchSuppliers() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getSuppliers().enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        supplierList.clear()
                        supplierList.addAll(body.data)
                        addSupplierDataToTable(supplierList)
                        binding.tvNoData.visibility = if (supplierList.isEmpty()) View.VISIBLE else View.GONE
                        binding.tableView.visibility = if (supplierList.isEmpty()) View.GONE else View.VISIBLE
                        Log.d("AllSupplierActivity", "Suppliers fetched: ${supplierList.size} entries")
                    } else {
                        Toast.makeText(this@AllSupplierActivity, "Gagal mengambil data pemasok: ${body?.activity}", Toast.LENGTH_SHORT).show()
                        Log.e("AllSupplierActivity", "Fetch suppliers error: ${response.errorBody()?.string()}")
                    }
                } else {
                    Toast.makeText(this@AllSupplierActivity, "Gagal mengambil data pemasok", Toast.LENGTH_SHORT).show()
                    Log.e("AllSupplierActivity", "Fetch suppliers error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                Toast.makeText(this@AllSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("AllSupplierActivity", "Fetch suppliers failure: ${t.message}", t)
            }
        })
    }

    private fun searchSupplier(query: String) {
        val apiService = RetrofitClient.getInstance(this)
        val request = SearchSupplierRequest(query)
        apiService.searchSupplier(request).enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    supplierList.clear()
                    supplierList.addAll(response.body()?.data ?: emptyList())
                    addSupplierDataToTable(supplierList)
                    binding.tvNoData.visibility = if (supplierList.isEmpty()) View.VISIBLE else View.GONE
                    binding.tableView.visibility = if (supplierList.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    Toast.makeText(this@AllSupplierActivity, "Gagal mencari supplier", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                Toast.makeText(this@AllSupplierActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addSupplierDataToTable(supplierList: List<Supplier>) {
        binding.tableView.removeAllViews()

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
        for (supplier in supplierList) {
            val row = TableRow(this).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.white))
            }

            val nameTextView = createTextView(supplier.name)
            nameTextView.setOnClickListener {
                val intent = Intent(this@AllSupplierActivity, ProfileSupplierActivity::class.java).apply {
                    putExtra("SUPPLIER_ID", supplier.id)
                    putExtra("SUPPLIER_NAME", supplier.name)
                    putExtra("SUPPLIER_STATUS", supplier.status)
                    putExtra("SUPPLIER_USERNAME", supplier.username)
                    putExtra("SUPPLIER_PHONE", supplier.no_telp)
                    putExtra("SUPPLIER_INCOME", "Rp ${numberFormat.format(supplier.income)}")
                }
                startActivity(intent)
            }

            row.addView(nameTextView)
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