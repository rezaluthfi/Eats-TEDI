package com.example.eatstedi

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityAllSupplierBinding
import com.example.eatstedi.model.Supplier

class AllSupplierActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAllSupplierBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {
            btnAddNewSupplier.setOnClickListener {
                val intent = Intent(this@AllSupplierActivity, ProfileSupplierActivity::class.java)
                startActivity(intent)
            }

            ivArrowBack.setOnClickListener {
                val intent = Intent(this@AllSupplierActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Panggil fungsi untuk mengisi tabel dengan data dummy
        addSupplierDataToTable(getDummySupplierData())
    }

    // Fungsi untuk menambahkan data supplier ke dalam TableLayout
    private fun addSupplierDataToTable(supplierList: List<Supplier>) {
        val tableLayout = binding.tableView

        // Hapus semua baris yang ada jika diperlukan
        tableLayout.removeAllViews()

        // Menambahkan header untuk tabel
        val headerRow = TableRow(this).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.secondary))
        }

        headerRow.addView(createTextView("Nama Supplier", isHeader = true))
        headerRow.addView(createTextView("Status", isHeader = true))
        headerRow.addView(createTextView("Username", isHeader = true))
        headerRow.addView(createTextView("Email", isHeader = true))
        headerRow.addView(createTextView("No. Telepon", isHeader = true))
        headerRow.addView(createTextView("Income", isHeader = true))

        tableLayout.addView(headerRow)

        // Menambahkan data supplier ke dalam table
        for (supplier in supplierList) {
            val row = TableRow(this).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.white))
            }

            row.addView(createTextView(supplier.name))
            row.addView(createTextView(supplier.status))
            row.addView(createTextView(supplier.username))
            row.addView(createTextView(supplier.email))
            row.addView(createTextView(supplier.phone))
            row.addView(createTextView(supplier.income))

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
            setTextColor(ContextCompat.getColor(this@AllSupplierActivity, if (isHeader) R.color.black else R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    // Data dummy untuk supplier
    private fun getDummySupplierData(): List<Supplier> {
        return listOf(
            Supplier("Alpha", "Aktif", "alpha", "alpha@example.com", "081234567800", "Rp 20,000,000"),
            Supplier("Beta", "Tidak Aktif", "beta", "beta@example.com", "081234567801", "Rp 15,000,000"),
            Supplier("Gamma", "Aktif", "gamma", "gamma@example.com", "081234567802", "Rp 30,000,000"),
            Supplier("Delta", "Aktif", "delta", "delta@example.com", "081234567803", "Rp 25,000,000"),
            Supplier("Epsilon", "Tidak Aktif", "epsilon", "epsilon@example.com", "081234567804", "Rp 18,000,000"),
            Supplier("Zeta", "Aktif", "zeta", "zeta@example.com", "081234567805", "Rp 22,000,000")
        )
    }
}