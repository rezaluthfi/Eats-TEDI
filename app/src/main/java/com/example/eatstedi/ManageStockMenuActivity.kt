package com.example.eatstedi

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.eatstedi.databinding.ActivityManageStockMenuBinding
import com.example.eatstedi.model.HistoryStock

class ManageStockMenuActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityManageStockMenuBinding.inflate(layoutInflater)
    }

    private var menuItemId: Int = 0
    private var currentStock: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Data dummy untuk riwayat perubahan stok
        val historyList = listOf(
            HistoryStock("John Doe", "05/12/2024", "14:35", 50, 45),
            HistoryStock("Jane Smith", "04/12/2024", "15:00", 60, 55),
            HistoryStock("Mike Johnson", "01/12/2024", "13:20", 70, 65),
            HistoryStock("Emily Davis", "30/11/2024", "12:45", 80, 75),
            HistoryStock("Daniel Brown", "29/11/2024", "11:56", 90, 85),
            HistoryStock("Olivia Wilson", "28/11/2024", "10:30", 100, 95),
            HistoryStock("James Martinez", "27/11/2024", "09:45", 110, 105),
        )

        // Tambahkan data ke dalam TableView
        addHistoryChangeDataToTable(historyList)

        // Tombol kembali ke MainActivity
        binding.ivArrowBack.setOnClickListener {
            finish()
        }

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

        // Tombol simpan
        binding.btnSaveStock.setOnClickListener {
            // Ambil data stok baru dari EditText, jumlah
            val newStock = binding.etQuantity.text.toString().toIntOrNull() ?: 0
            if (newStock != currentStock && newStock >= 0) {
                // Tambahkan data riwayat perubahan stok ke tabel riwayat tanpa menghilangkan data yang sudah ada
                val newHistory = HistoryStock(
                    "Employee Name",
                    "12/12/2024",
                    "12:00",
                    currentStock,
                    newStock
                )
                addHistoryChangeDataToTable(listOf(newHistory) + historyList)

                // Update stok saat ini
                currentStock = newStock
                binding.tvStock.text = newStock.toString() // Update tampilan stok di activity

                // Kirim hasil kembali ke MenuFragment
                val resultIntent = Intent().apply {
                    putExtra("MENU_ITEM_ID", menuItemId) // Kirim ID menu
                    putExtra("NEW_STOCK", newStock) // Kirim stok baru
                }
                setResult(RESULT_OK, resultIntent) // Set result OK
            }
            finish() // Tutup activity
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

    private fun addHistoryChangeDataToTable(historyList: List<HistoryStock>) {
        val tableLayout = binding.tableView

        // Hapus semua baris yang ada jika diperlukan
        tableLayout.removeAllViews()

        // Menambahkan header untuk tabel
        val headerRow = TableRow(this).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@ManageStockMenuActivity, R.color.secondary))
        }

        // Menambahkan header kolom
        headerRow.addView(createTextView("Nama Employee", isHeader = true))
        headerRow.addView(createTextView("Tanggal Update", isHeader = true))
        headerRow.addView(createTextView("Waktu Update", isHeader = true))
        headerRow.addView(createTextView("Stok Sebelum", isHeader = true))
        headerRow.addView(createTextView("Stok Sesudah", isHeader = true))

        tableLayout.addView(headerRow)

        // Menambahkan data employee ke dalam tabel
        for (history in historyList) {
            val row = TableRow(this).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@ManageStockMenuActivity, R.color.white))
            }

            // Tambahkan TextView untuk setiap kolom data
            row.addView(createTextView(history.name))
            row.addView(createTextView(history.updateDate))
            row.addView(createTextView(history.updateTime))
            row.addView(createTextView(history.stockBefore.toString()))
            row.addView(createTextView(history.stockAfter.toString()))

            tableLayout.addView(row)
        }
    }

    // Fungsi untuk membuat TextView dengan gaya header atau konten biasa
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
