package com.example.eatstedi.activity

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.StockLog
import com.example.eatstedi.api.service.UpdateStockResponse
import com.example.eatstedi.api.service.StockLogResponse
import com.example.eatstedi.databinding.ActivityManageStockMenuBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ManageStockMenuActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityManageStockMenuBinding.inflate(layoutInflater)
    }

    private var menuItemId: Int = 0
    private var currentStock: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Ambil data dari intent
        menuItemId = intent.getIntExtra("MENU_ITEM_ID", 0)
        val menuItemName = intent.getStringExtra("MENU_ITEM_NAME") ?: ""
        val imageUrl = intent.getStringExtra("MENU_ITEM_IMAGE_URL") ?: ""
        currentStock = intent.getIntExtra("MENU_ITEM_STOCK", 0)

        // Tampilkan nama menu, gambar, dan stok saat ini
        binding.tvStock.text = currentStock.toString()
        binding.tvNameMenu.text = menuItemName
        Glide.with(this)
            .load(Uri.parse(imageUrl))
            .placeholder(R.drawable.image_menu)
            .error(R.drawable.ic_launcher_background)
            .into(binding.ivImgMenu)

        // Ambil data riwayat stok dari API
        fetchStockLog(menuItemId)

        // Tombol kembali ke MainActivity
        binding.ivArrowBack.setOnClickListener {
            finish()
        }

        // Tombol simpan
        binding.btnSaveStock.setOnClickListener {
            val newStock = binding.etQuantity.text.toString().toIntOrNull()
            if (newStock == null || newStock < 0) {
                Toast.makeText(this, "Please enter a valid stock quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newStock != currentStock) {
                updateMenuStock(menuItemId, newStock)
            } else {
                Toast.makeText(this, "No changes to stock", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Tombol hapus
        binding.btnDeleteInputStock.setOnClickListener {
            binding.etQuantity.setText("0")
        }

        // Setup tombol plus dan minus
        setupQuantityButtons()
    }

    private fun setupQuantityButtons() {
        val btnPlus: Button = binding.btnPlus
        val btnMinus: Button = binding.btnMinus
        val etQuantity: EditText = binding.etQuantity

        btnPlus.setOnClickListener {
            val currentQuantity = etQuantity.text.toString().toIntOrNull() ?: 0
            etQuantity.setText((currentQuantity + 1).toString())
        }

        btnMinus.setOnClickListener {
            val currentQuantity = etQuantity.text.toString().toIntOrNull() ?: 0
            if (currentQuantity > 0) {
                etQuantity.setText((currentQuantity - 1).toString())
            }
        }

        etQuantity.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val currentQuantity = etQuantity.text.toString().toIntOrNull() ?: 0
                if (currentQuantity < 0) {
                    etQuantity.setText("0")
                }
            }
        }
    }

    private fun fetchStockLog(menuId: Int) {
        val call = RetrofitClient.getInstance(this).getStockLog(menuId)
        call.enqueue(object : Callback<StockLogResponse> {
            override fun onResponse(call: Call<StockLogResponse>, response: Response<StockLogResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val stockLogs = response.body()?.data ?: emptyList()
                    addHistoryChangeDataToTable(stockLogs)
                } else {
                    Toast.makeText(this@ManageStockMenuActivity, "Failed to load stock log", Toast.LENGTH_SHORT).show()
                    addHistoryChangeDataToTable(emptyList())
                }
            }

            override fun onFailure(call: Call<StockLogResponse>, t: Throwable) {
                Toast.makeText(this@ManageStockMenuActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                addHistoryChangeDataToTable(emptyList())
            }
        })
    }

    private fun updateMenuStock(menuId: Int, newStock: Int) {
        val call = RetrofitClient.getInstance(this).updateMenuStock(menuId, mapOf("stock" to newStock.toString()))
        call.enqueue(object : Callback<UpdateStockResponse> {
            override fun onResponse(call: Call<UpdateStockResponse>, response: Response<UpdateStockResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    currentStock = newStock
                    binding.tvStock.text = newStock.toString()
                    fetchStockLog(menuId) // Refresh stock log
                    Toast.makeText(this@ManageStockMenuActivity, "Stock updated successfully", Toast.LENGTH_SHORT).show()
                    // Kirim hasil kembali ke MenuFragment
                    val resultIntent = Intent().apply {
                        putExtra("MENU_ITEM_ID", menuItemId)
                        putExtra("NEW_STOCK", newStock)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this@ManageStockMenuActivity, "Failed to update stock", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateStockResponse>, t: Throwable) {
                Toast.makeText(this@ManageStockMenuActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addHistoryChangeDataToTable(historyList: List<StockLog>) {
        val tableLayout = binding.tableView

        // Hapus semua baris yang ada
        tableLayout.removeAllViews()

        // Menambahkan header untuk tabel
        val headerRow = TableRow(this).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@ManageStockMenuActivity, R.color.secondary))
        }

        // Menambahkan header kolom sesuai API
        headerRow.addView(createTextView("Nama Employee", isHeader = true))
        headerRow.addView(createTextView("Tanggal Update", isHeader = true))
        headerRow.addView(createTextView("Aktivitas", isHeader = true))
        headerRow.addView(createTextView("Jumlah", isHeader = true))

        tableLayout.addView(headerRow)

        // Menambahkan data ke dalam tabel
        for (history in historyList) {
            val row = TableRow(this).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@ManageStockMenuActivity, R.color.white))
            }

            // Format tanggal dari created_at
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC") // Set zona waktu ke UTC
            }
            val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val parsedDate = if (history.createdAt.isNullOrEmpty()) {
                Log.d("StockLog", "createdAt is null or empty for id: ${history.id}")
                "N/A" // Fallback jika createdAt null atau kosong
            } else {
                try {
                    Log.d("StockLog", "Raw createdAt: ${history.createdAt}")
                    dateFormat.parse(history.createdAt)?.let { date ->
                        displayDateFormat.format(date)
                    } ?: history.createdAt
                } catch (e: Exception) {
                    Log.e("StockLog", "Error parsing date: ${history.createdAt}, Error: ${e.message}")
                    history.createdAt // Fallback ke string asli jika parsing gagal
                }
            }

            // Tambahkan TextView untuk setiap kolom data
            row.addView(createTextView(history.name))
            row.addView(createTextView(parsedDate))
            row.addView(createTextView(history.activity))
            row.addView(createTextView(history.quantity.toString()))

            tableLayout.addView(row)
        }
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(this@ManageStockMenuActivity, if (isHeader) R.color.black else R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }
}