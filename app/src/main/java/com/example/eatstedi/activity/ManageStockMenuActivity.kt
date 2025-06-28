package com.example.eatstedi.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.StockLog
import com.example.eatstedi.api.service.StockLogResponse
import com.example.eatstedi.api.service.UpdateStockResponse
import com.example.eatstedi.databinding.ActivityManageStockMenuBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ManageStockMenuActivity : AppCompatActivity() {

    private val binding by lazy { ActivityManageStockMenuBinding.inflate(layoutInflater) }
    private var menuItemId: Int = 0
    private var currentStock: Int = 0
    private var allStockLogs = listOf<StockLog>()
    private var currentPage = 1
    private val itemsPerPage = 10
    private var totalPages = 1

    // --- Lifecycle Methods ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initializeData()
        setupUI()
        showShimmer()
        fetchStockLog(menuItemId)
    }

    // --- Shimmer Control ---

    private fun showShimmer() {
        binding.shimmerTable.visibility = View.VISIBLE
        binding.shimmerPagination.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE
        binding.paginationLayout.visibility = View.GONE
        binding.shimmerTable.startShimmer()
        binding.shimmerPagination.startShimmer()
    }

    private fun hideShimmer() {
        binding.shimmerTable.stopShimmer()
        binding.shimmerPagination.stopShimmer()
        binding.shimmerTable.visibility = View.GONE
        binding.shimmerPagination.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
    }

    // --- UI Setup ---

    private fun initializeData() {
        menuItemId = intent.getIntExtra("MENU_ITEM_ID", 0)
        currentStock = intent.getIntExtra("MENU_ITEM_STOCK", 0)
        val menuName = intent.getStringExtra("MENU_ITEM_NAME") ?: "Nama Menu Tidak Ditemukan"

        binding.tvStock.text = currentStock.toString()
        binding.etQuantity.setText(currentStock.toString())
        binding.tvNameMenu.text = menuName

        if (menuItemId != 0) {
            loadMenuImage(menuItemId)
        } else {
            // Fallback jika ID menu tidak valid
            binding.ivImgMenu.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    private fun loadMenuImage(menuId: Int) {
        // Set placeholder awal
        binding.ivImgMenu.setImageResource(R.drawable.image_menu)

        val apiService = RetrofitClient.getInstance(this)
        apiService.getMenuPhoto(menuId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isFinishing && response.isSuccessful && response.body() != null) {
                    try {
                        val imageBytes = response.body()!!.bytes()
                        Glide.with(this@ManageStockMenuActivity)
                            .load(imageBytes)
                            .placeholder(R.drawable.image_menu)
                            .error(R.drawable.ic_launcher_background)
                            .centerCrop()
                            .into(binding.ivImgMenu)
                    } catch (e: Exception) {
                        Log.e("ManageStockActivity", "Error reading image bytes", e)
                        binding.ivImgMenu.setImageResource(R.drawable.ic_launcher_background)
                    }
                } else {
                    Log.w("ManageStockActivity", "Failed to load image. Code: ${response.code()}")
                    binding.ivImgMenu.setImageResource(R.drawable.ic_launcher_background)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (!isFinishing) {
                    Log.e("ManageStockActivity", "Network failure loading image", t)
                    binding.ivImgMenu.setImageResource(R.drawable.ic_launcher_background)
                }
            }
        })
    }

    private fun setupUI() {
        setupButtons()
        setupPagination()
    }

    private fun setupButtons() {
        binding.ivArrowBack.setOnClickListener { finish() }

        binding.btnSaveStock.setOnClickListener {
            val newStock = binding.etQuantity.text.toString().toIntOrNull()
            if (newStock == null || newStock < 0) {
                showToast("Masukkan jumlah stok yang valid")
                return@setOnClickListener
            }
            if (newStock != currentStock) {
                binding.btnSaveStock.isEnabled = false
                updateMenuStock(menuItemId, newStock)
            } else {
                showToast("Tidak ada perubahan stok")
                finish()
            }
        }

        binding.btnDeleteInputStock.setOnClickListener {
            binding.etQuantity.setText("0")
        }

        setupQuantityButtons()
    }

    private fun setupQuantityButtons() {
        binding.btnPlus.setOnClickListener {
            val current = binding.etQuantity.text.toString().toIntOrNull() ?: 0
            binding.etQuantity.setText((current + 1).toString())
        }

        binding.btnMinus.setOnClickListener {
            val current = binding.etQuantity.text.toString().toIntOrNull() ?: 0
            if (current > 0) binding.etQuantity.setText((current - 1).toString())
        }
    }

    private fun setupPagination() {
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePageDisplay()
                displayCurrentPageData()
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePageDisplay()
                displayCurrentPageData()
            }
        }
    }

    // --- Data Fetching ---

    private fun fetchStockLog(menuId: Int) {
        showShimmer()
        RetrofitClient.getInstance(this).getStockLog(menuId).enqueue(object : Callback<StockLogResponse> {
            override fun onResponse(call: Call<StockLogResponse>, response: Response<StockLogResponse>) {
                hideShimmer()
                if (response.isSuccessful && response.body()?.success == true) {
                    allStockLogs = response.body()?.data ?: emptyList()
                } else {
                    showToast("Gagal memuat riwayat stok")
                    allStockLogs = emptyList()
                }
                currentPage = 1
                totalPages = (allStockLogs.size + itemsPerPage - 1) / itemsPerPage
                updatePageDisplay()
                displayCurrentPageData()
            }

            override fun onFailure(call: Call<StockLogResponse>, t: Throwable) {
                hideShimmer()
                showToast("Error: ${t.message}")
                allStockLogs = emptyList()
                updatePageDisplay()
                displayCurrentPageData()
            }
        })
    }

    private fun updateMenuStock(menuId: Int, newStock: Int) {
        RetrofitClient.getInstance(this).updateMenuStock(menuId, mapOf("stock" to newStock.toString()))
            .enqueue(object : Callback<UpdateStockResponse> {
                override fun onResponse(call: Call<UpdateStockResponse>, response: Response<UpdateStockResponse>) {
                    binding.btnSaveStock.isEnabled = true
                    if (response.isSuccessful && response.body()?.success == true) {
                        showToast("Stok berhasil diperbarui")
                        val resultIntent = Intent().apply {
                            putExtra("MENU_ITEM_ID", menuItemId)
                            putExtra("NEW_STOCK", newStock)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        showToast("Gagal memperbarui stok")
                    }
                }

                override fun onFailure(call: Call<UpdateStockResponse>, t: Throwable) {
                    binding.btnSaveStock.isEnabled = true
                    showToast("Error: ${t.message}")
                }
            })
    }

    // --- Pagination ---

    private fun updatePageDisplay() {
        totalPages = if (allStockLogs.isEmpty()) 1 else (allStockLogs.size + itemsPerPage - 1) / itemsPerPage
        binding.tvPageInfo.text = "Halaman $currentPage / $totalPages"
        binding.btnPrevPage.isEnabled = currentPage > 1
        binding.btnPrevPage.alpha = if (currentPage > 1) 1.0f else 0.5f
        binding.btnNextPage.isEnabled = currentPage < totalPages
        binding.btnNextPage.alpha = if (currentPage < totalPages) 1.0f else 0.5f
        binding.paginationLayout.visibility = if (allStockLogs.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun displayCurrentPageData() {
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, allStockLogs.size)
        val pageData = if (allStockLogs.isEmpty()) emptyList() else allStockLogs.subList(startIndex, endIndex)
        addHistoryChangeDataToTable(pageData)
    }

    // --- Table Display ---

    private fun addHistoryChangeDataToTable(historyList: List<StockLog>) {
        binding.tableView.removeAllViews()

        // Add header
        val headerRow = TableRow(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@ManageStockMenuActivity, R.color.secondary))
            addView(createTextView("Nama Employee", true))
            addView(createTextView("Tanggal Update", true))
            addView(createTextView("Aktivitas", true))
            addView(createTextView("Jumlah", true))
        }
        binding.tableView.addView(headerRow)

        if (historyList.isEmpty()) {
            val noDataRow = TableRow(this)
            val noDataText = createTextView("Tidak ada riwayat stok.").apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f)
            }
            noDataRow.addView(noDataText)
            binding.tableView.addView(noDataRow)
            return
        }

        // Add data rows
        historyList.forEach { history ->
            val row = TableRow(this).apply {
                setBackgroundColor(ContextCompat.getColor(this@ManageStockMenuActivity, R.color.white))
            }
            val parsedDate = formatUtcDate(history.createdAt)
            row.addView(createTextView(history.name))
            row.addView(createTextView(parsedDate))
            row.addView(createTextView(history.activity))
            row.addView(createTextView(history.quantity.toString()))
            binding.tableView.addView(row)
        }
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 16, 16, 16)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(this@ManageStockMenuActivity, R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    // --- Formatting ---

    private fun formatUtcDate(utcDateStr: String?): String {
        if (utcDateStr.isNullOrEmpty()) return "N/A"
        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = utcFormat.parse(utcDateStr)
            date?.let { displayFormat.format(it) } ?: "Invalid Date"
        } catch (e: Exception) {
            Log.w("ManageStockMenuActivity", "Error parsing date: $utcDateStr", e)
            utcDateStr
        }
    }

    // --- Utility ---

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    private fun minOf(a: Int, b: Int): Int = if (a < b) a else b
}