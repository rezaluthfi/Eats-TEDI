package com.example.eatstedi.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.DeleteSpecificOrdersRequest
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.SpecificReceiptResponse
import com.example.eatstedi.api.service.TransactionDetail
import com.example.eatstedi.databinding.ActivityRecapDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class RecapDetailActivity : AppCompatActivity() {

    private val binding by lazy { ActivityRecapDetailBinding.inflate(layoutInflater) }
    private val checkBoxList = mutableListOf<Pair<CheckBox, TransactionDetail>>()
    private var isCheckBoxVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ambil receiptId dari Intent
        val receiptId = intent.getIntExtra("receiptId", -1)
        if (receiptId == -1) {
            Toast.makeText(this, "ID Transaksi tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Panggil API untuk mengambil detail transaksi
        fetchTransactionDetails(receiptId)

        // Toggle checkbox visibility
        binding.ivActiveCheckbox.setOnClickListener {
            isCheckBoxVisible = !isCheckBoxVisible
            toggleCheckBoxes(isCheckBoxVisible)
        }

        // Kembali ke activity sebelumnya
        binding.ivArrowBack.setOnClickListener {
            finish()
        }

        // Hapus transaksi yang dipilih
        binding.ivDelete.setOnClickListener {
            deleteSelectedTransactions()
        }
    }

    private fun fetchTransactionDetails(receiptId: Int) {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getSpecificReceipt(receiptId).enqueue(object : Callback<SpecificReceiptResponse> {
            override fun onResponse(call: Call<SpecificReceiptResponse>, response: Response<SpecificReceiptResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        displayTransactions(body.data)
                        Log.d("RecapDetailActivity", "Fetched ${body.data.size} transaction details")
                    } else {
                        Toast.makeText(this@RecapDetailActivity, "Gagal mengambil detail transaksi: ${body?.message}", Toast.LENGTH_LONG).show()
                        showNoData()
                        Log.e("RecapDetailActivity", "Fetch error: ${body?.message}")
                    }
                } else {
                    Toast.makeText(this@RecapDetailActivity, "Gagal mengambil detail transaksi: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                    showNoData()
                    Log.e("RecapDetailActivity", "Fetch HTTP error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<SpecificReceiptResponse>, t: Throwable) {
                Toast.makeText(this@RecapDetailActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                showNoData()
                Log.e("RecapDetailActivity", "Fetch failure: ${t.message}", t)
            }
        })
    }

    private fun displayTransactions(transactions: List<TransactionDetail>) {
        binding.tableView.removeAllViews()
        checkBoxList.clear()

        if (transactions.isEmpty()) {
            showNoData()
            return
        }

        binding.tvNoData.visibility = View.GONE
        binding.tableView.visibility = View.VISIBLE
        binding.ivActiveCheckbox.visibility = View.VISIBLE

        // Tambahkan header tabel
        val headerRow = TableRow(this).apply {
            setPadding(16, 16, 16, 16)
            setBackgroundColor(ContextCompat.getColor(this@RecapDetailActivity, R.color.secondary))
        }

        // Checkbox header (nonaktif untuk alignment)
        val headerCheckBox = CheckBox(this).apply {
            isEnabled = false
            setPadding(16, 8, 16, 8)
            visibility = if (isCheckBoxVisible) View.VISIBLE else View.GONE
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                width = 0
                weight = 1f
            }
        }
        headerRow.addView(headerCheckBox)
        headerRow.addView(createTextView("Nama Pemasok", true))
        headerRow.addView(createTextView("Waktu", true))
        headerRow.addView(createTextView("Menu", true))
        headerRow.addView(createTextView("Harga", true))
        headerRow.addView(createTextView("Jumlah", true))
        headerRow.addView(createTextView("Total Harga", true))

        binding.tableView.addView(headerRow)

        // Format tanggal untuk created_at
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Tambahkan data ke tabel
        transactions.forEachIndexed { index, transaction ->
            val row = TableRow(this).apply {
                setPadding(16, 16, 16, 16)
                setBackgroundColor(
                    if (index % 2 == 0)
                        ContextCompat.getColor(this@RecapDetailActivity, R.color.white)
                    else
                        ContextCompat.getColor(this@RecapDetailActivity, R.color.secondary)
                )
            }

            // Tambahkan CheckBox
            val checkBox = CheckBox(this).apply {
                setPadding(16, 8, 16, 8)
                visibility = if (isCheckBoxVisible) View.VISIBLE else View.GONE
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                    width = 0
                    weight = 1f
                }
                setOnCheckedChangeListener { _, isChecked ->
                    binding.ivDelete.visibility = if (checkBoxList.any { it.first.isChecked }) View.VISIBLE else View.GONE
                }
            }

            checkBoxList.add(checkBox to transaction)

            // Parsing waktu dari created_at
            val time = try {
                val date = inputFormat.parse(transaction.created_at)
                outputFormat.format(date)
            } catch (e: Exception) {
                "N/A"
            }

            // Tambahkan kolom
            row.addView(checkBox)
            row.addView(createTextView(transaction.supplier_name))
            row.addView(createTextView(time))
            row.addView(createTextView(transaction.menu_name))
            row.addView(createTextView("Rp${transaction.price}"))
            row.addView(createTextView(transaction.amount.toString()))
            row.addView(createTextView("Rp${transaction.price * transaction.amount}"))

            binding.tableView.addView(row)
        }
    }

    private fun deleteSelectedTransactions() {
        val selectedOrderIds = checkBoxList
            .filter { it.first.isChecked }
            .map { it.second.order_id }

        if (selectedOrderIds.isEmpty()) {
            Toast.makeText(this, "Pilih transaksi untuk dihapus!", Toast.LENGTH_SHORT).show()
            return
        }

        val request = DeleteSpecificOrdersRequest(id_orders = selectedOrderIds)
        val apiService = RetrofitClient.getInstance(this)
        Log.d("RecapDetailActivity", "Sending DELETE request to /api/delete-specific-orders with order IDs: $selectedOrderIds, Payload: $request")
        apiService.deleteSpecificOrders(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                Log.d("RecapDetailActivity", "Response code: ${response.code()}, Body: ${response.body()?.toString()}, Error: ${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Toast.makeText(this@RecapDetailActivity, "Transaksi berhasil dihapus!", Toast.LENGTH_LONG).show()
                        // Refresh data
                        val receiptId = intent.getIntExtra("receiptId", -1)
                        if (receiptId != -1) {
                            fetchTransactionDetails(receiptId)
                        }
                        Log.d("RecapDetailActivity", "Deleted orders: $selectedOrderIds")
                    } else {
                        val errorMessage = when (body?.message) {
                            is String -> body.message as String
                            is Map<*, *> -> (body.message as Map<*, *>).toString()
                            else -> "Unknown error"
                        }
                        Toast.makeText(this@RecapDetailActivity, "Gagal menghapus transaksi: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e("RecapDetailActivity", "Delete error: $errorMessage")
                    }
                } else {
                    Toast.makeText(this@RecapDetailActivity, "Gagal menghapus transaksi: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                    Log.e("RecapDetailActivity", "Delete HTTP error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@RecapDetailActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("RecapDetailActivity", "Delete failure: ${t.message}", t)
            }
        })
    }

    private fun showNoData() {
        binding.tvNoData.visibility = View.VISIBLE
        binding.tableView.visibility = View.GONE
        binding.ivDelete.visibility = View.GONE
        binding.ivActiveCheckbox.visibility = View.GONE
    }

    private fun toggleCheckBoxes(visible: Boolean) {
        checkBoxList.forEach { (checkBox, _) ->
            checkBox.visibility = if (visible) View.VISIBLE else View.GONE
        }
        // Update header checkbox visibility
        val headerRow = binding.tableView.getChildAt(0) as? TableRow
        val headerCheckBox = headerRow?.getChildAt(0) as? CheckBox
        headerCheckBox?.visibility = if (visible) View.VISIBLE else View.GONE
        binding.ivDelete.visibility = if (visible && checkBoxList.any { it.first.isChecked }) View.VISIBLE else View.GONE
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            setTextColor(ContextCompat.getColor(this@RecapDetailActivity, R.color.black))
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            gravity = android.view.Gravity.CENTER // Rata tengah untuk konsistensi
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                width = 0
                weight = 1f
            }
        }
    }
}