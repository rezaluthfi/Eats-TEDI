package com.example.eatstedi

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityRecapDetailBinding
import com.example.eatstedi.model.TransactionDetail

class RecapDetailActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRecapDetailBinding.inflate(layoutInflater)
    }

    private val checkBoxList = mutableListOf<Pair<CheckBox, ImageView>>() // Menyimpan pasangan CheckBox dan tombol hapus
    private var isCheckBoxVisible = false  // Untuk kontrol visibilitas checkbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Setup window insets untuk edge-to-edge experience
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ambil nama employee dari Intent
        val employeeName = intent.getStringExtra("employeeName")

        // Panggil fungsi untuk menampilkan data transaksi
        val transactions = getTransactionsForEmployee(employeeName)
        displayTransactions(transactions)

        // Menangani klik pada iv_active_checkbox
        binding.ivActiveCheckbox.setOnClickListener {
            isCheckBoxVisible = !isCheckBoxVisible  // Toggle visibilitas checkbox
            toggleCheckBoxes(isCheckBoxVisible)
        }

        binding.ivArrowBack.setOnClickListener {
            finish()
        }
    }

    private fun getTransactionsForEmployee(employeeName: String?): List<TransactionDetail> {
        val allTransactions = listOf(
            TransactionDetail("Supplier A", "10:00", "Nasi Goreng", 25000, 2, "John"),
            TransactionDetail("Supplier B", "11:30", "Mie Ayam", 20000, 3, "Doe"),
            TransactionDetail("Supplier A", "13:00", "Sate Ayam", 30000, 1, "John"),
            TransactionDetail("Supplier C", "14:30", "Nasi Goreng", 25000, 2, "Dave"),
            TransactionDetail("Supplier B", "16:00", "Mie Goreng", 22000, 1, "Alice"),
            TransactionDetail("Supplier A", "17:15", "Sate Kambing", 35000, 3, "Bob"),
            TransactionDetail("Supplier C", "18:00", "Nasi Campur", 27000, 2, "Charlie"),
            TransactionDetail("Supplier B", "19:00", "Bakso", 20000, 4, "Alice"),
            TransactionDetail("Supplier A", "20:00", "Mie Ayam", 18000, 5, "Bob"),
            TransactionDetail("Supplier C", "21:30", "Sate Ayam", 25000, 2, "Charlie"),
            TransactionDetail("Supplier A", "08:30", "Soto Ayam", 22000, 3, "Eva"),
            TransactionDetail("Supplier B", "09:45", "Gado-Gado", 18000, 1, "Frank"),
            TransactionDetail("Supplier C", "10:30", "Nasi Lemak", 24000, 2, "Grace"),
            TransactionDetail("Supplier A", "12:00", "Nasi Goreng Spesial", 27000, 1, "Heidi"),
            TransactionDetail("Supplier B", "13:15", "Pecel Lele", 21000, 4, "Ivan"),
            TransactionDetail("Supplier C", "14:00", "Mie Ayam Goreng", 19000, 2, "Jack"),
            TransactionDetail("Supplier A", "15:30", "Sate Padang", 32000, 2, "Kim"),
            TransactionDetail("Supplier B", "16:45", "Tahu Tempe", 15000, 3, "Lily"),
            TransactionDetail("Supplier C", "17:00", "Nasi Bakar", 23000, 1, "Mallory"),
            TransactionDetail("Supplier A", "18:15", "Ikan Bakar", 30000, 4, "Nina"),
            TransactionDetail("Supplier B", "19:30", "Mie Ceker", 20000, 5, "Oscar")
        )

        return allTransactions.filter { it.employeeName == employeeName }
    }

    private fun displayTransactions(transactions: List<TransactionDetail>) {
        // Hapus semua baris dari tabel sebelumnya
        binding.tableView.removeAllViews()

        if (transactions.isEmpty()) {
            // Tampilkan pesan "data tidak ditemukan", sembunyikan tabel dan tombol hapus, dan nonaktifkan iv_active_checkbox
            binding.tvNoData.visibility = View.VISIBLE
            binding.tableView.visibility = View.GONE
            binding.ivDelete.visibility = View.GONE
            binding.ivActiveCheckbox.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE

            // Tambahkan header tabel
            val headerRow = TableRow(this).apply {
                setPadding(16, 16, 16, 16)
                setBackgroundColor(ContextCompat.getColor(this@RecapDetailActivity, R.color.secondary))
            }

            headerRow.addView(createTextView("Nama Pemasok", true))
            headerRow.addView(createTextView("Waktu", true))
            headerRow.addView(createTextView("Menu", true))
            headerRow.addView(createTextView("Harga", true))
            headerRow.addView(createTextView("Jumlah", true))
            headerRow.addView(createTextView("Total Harga", true))

            binding.tableView.addView(headerRow)

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

                // Tambahkan CheckBox ke baris, defaultnya disembunyikan
                val checkBox = CheckBox(this).apply {
                    setPadding(10, 10, 10, 10)
                    visibility = View.GONE // Awalnya disembunyikan
                }

                // Tambahkan ImageView untuk tombol hapus, defaultnya disembunyikan
                binding.ivDelete.setOnClickListener {
                    // Collect the indices of rows to remove
                    val rowsToRemove = mutableListOf<Int>()

                    checkBoxList.forEach { (checkBox, _) ->
                        if (checkBox.isChecked) {
                            // Add the index of the parent row
                            val rowIndex = binding.tableView.indexOfChild(checkBox.parent as? TableRow)
                            if (rowIndex >= 0) { // Ensure valid row index
                                rowsToRemove.add(rowIndex)
                            }
                        }
                    }

                    // Remove the rows in reverse order to avoid indexing issues
                    if (rowsToRemove.isNotEmpty()) {
                        rowsToRemove.sortedDescending().forEach { index ->
                            if (index in 0 until binding.tableView.childCount) { // Ensure index is valid
                                binding.tableView.removeViewAt(index)
                            }
                        }

                        // Reapply row colors based on the new table size
                        for (i in 0 until binding.tableView.childCount) {
                            val row = binding.tableView.getChildAt(i) as? TableRow
                            row?.setBackgroundColor(
                                // If the first row (header), use secondary color, else alternate between white and secondary
                                if (i == 0)
                                    ContextCompat.getColor(this@RecapDetailActivity, R.color.secondary)
                                else if ((i - 1) % 2 == 0) // Adjust for zero-based index after header
                                    ContextCompat.getColor(this@RecapDetailActivity, R.color.white)
                                else
                                    ContextCompat.getColor(this@RecapDetailActivity, R.color.secondary)
                            )
                        }
                    }

                    // Check if there are no data rows left (skip the header row at index 0)
                    val dataRowsRemaining = binding.tableView.childCount > 1 // Assuming the first row is the header

                    if (!dataRowsRemaining) {
                        // If no data rows left, show "No Data", hide the table, and hide the delete button
                        binding.tvNoData.visibility = View.VISIBLE
                        binding.tableView.visibility = View.GONE
                        binding.ivDelete.visibility = View.GONE
                        binding.ivActiveCheckbox.visibility = View.GONE
                    }
                }

                // Sambungkan CheckBox dengan tombol hapus
                checkBoxList.forEach { (checkBox, _) ->
                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        // Set the delete button visibility based on whether any checkbox is checked
                        val anyChecked = checkBoxList.any { (cb, _) -> cb.isChecked }
                        binding.ivDelete.visibility = if (anyChecked) View.VISIBLE else View.GONE
                    }
                }

                // Simpan pasangan CheckBox dan tombol hapus ke daftar
                checkBoxList.add(checkBox to binding.ivDelete)

                // Tambahkan CheckBox ke baris
                row.addView(checkBox)

                // Tambahkan TextViews untuk informasi lainnya
                row.addView(createTextView(transaction.supplierName))
                row.addView(createTextView(transaction.time))
                row.addView(createTextView(transaction.menu))
                row.addView(createTextView("Rp${transaction.price}"))
                row.addView(createTextView(transaction.quantity.toString()))
                row.addView(createTextView("Rp${transaction.totalPrice}"))

                binding.tableView.addView(row)
            }
        }
    }

    // Fungsi untuk toggle visibilitas checkbox
    private fun toggleCheckBoxes(visible: Boolean) {
        checkBoxList.forEach { (checkBox, _) ->
            checkBox.visibility = if (visible) View.VISIBLE else View.GONE
        }
        binding.ivDelete.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)

            // Set warna teks
            setTextColor(ContextCompat.getColor(this@RecapDetailActivity, R.color.black))
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            // Gunakan layout params agar kolom disusun secara merata
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                width = 0  // Pastikan semua kolom menggunakan bobot untuk distribusi yang merata
                weight = 1f
            }
        }
    }
}

