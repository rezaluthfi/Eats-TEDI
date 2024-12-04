package com.example.eatstedi

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TableLayout
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

    // Data supplier dummy
    private val supplierList = listOf(
        Supplier("Alpha", "Aktif", "alpha", "alpha@example.com", "081234567800", "Rp 20,000,000"),
        Supplier("Beta", "Tidak Aktif", "beta", "beta@example.com", "081234567801", "Rp 15,000,000"),
        Supplier("Gamma", "Aktif", "gamma", "gamma@example.com", "081234567802", "Rp 30,000,000"),
        Supplier("Delta", "Aktif", "delta", "delta@example.com", "081234567803", "Rp 25,000,000"),
        Supplier("Epsilon", "Tidak Aktif", "epsilon", "epsilon@example.com", "081234567804", "Rp 18,000,000"),
        Supplier("Zeta", "Aktif", "zeta", "zeta@example.com", "081234567805", "Rp 22,000,000"),
        Supplier("Eta", "Aktif", "eta", "eta@example.com", "081234567806", "Rp 26,000,000"),
        Supplier("Theta", "Tidak Aktif", "theta", "theta@example.com", "081234567807", "Rp 19,000,000"),
        Supplier("Iota", "Aktif", "iota", "iota@example.com", "081234567808", "Rp 28,000,000"),
        Supplier("Kappa", "Aktif", "kappa", "kappa@example.com", "081234567809", "Rp 27,000,000"),
        Supplier("Lambda", "Tidak Aktif", "lambda", "lambda@example.com", "081234567810", "Rp 21,000,000"),
        Supplier("Mu", "Aktif", "mu", "mu@example.com", "081234567811", "Rp 29,000,000"),
        Supplier("Nu", "Aktif", "nu", "nu@example.com", "081234567812", "Rp 31,000,000"),
        Supplier("Xi", "Aktif", "xi", "xi@example.com", "081234567813", "Rp 32,000,000")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Mengatur padding sesuai dengan insets sistem
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Tombol kembali ke MainActivity
        binding.ivArrowBack.setOnClickListener {
            val intent = Intent(this@AllSupplierActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Tombol untuk menambah supplier (dummy)
        binding.btnAddNewSupplier.setOnClickListener {
            val intent = Intent(this@AllSupplierActivity, AddSupplierActivity::class.java)
            startActivity(intent)
        }

        // Mengisi tabel dengan data supplier
        addSupplierDataToTable(supplierList)

        // Fitur pencarian otomatis tanpa menekan enter
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterSupplierTable(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Fungsi untuk menambahkan data supplier ke dalam TableLayout
    private fun addSupplierDataToTable(supplierList: List<Supplier>) {
        val tableLayout = binding.tableView

        // Hapus semua baris yang ada jika diperlukan
        tableLayout.removeAllViews()

        // Menambahkan header untuk tabel
        val headerRow = TableRow(this@AllSupplierActivity).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.secondary))
        }

        headerRow.addView(createTextView("Nama Supplier", isHeader = true))
        headerRow.addView(createTextView("Status", isHeader = true))
        headerRow.addView(createTextView("Nama Pengguna", isHeader = true))
        headerRow.addView(createTextView("Email", isHeader = true))
        headerRow.addView(createTextView("No. Telepon", isHeader = true))
        headerRow.addView(createTextView("Pemasukan", isHeader = true))

        tableLayout.addView(headerRow)

        // Menambahkan data supplier ke dalam tabel
        for (supplier in supplierList) {
            val row = TableRow(this@AllSupplierActivity).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(this@AllSupplierActivity, R.color.white))
            }

            // Buat TextView untuk nama supplier yang dapat diklik
            val nameTextView = createTextView(supplier.name)
            nameTextView.setOnClickListener {
                // Ketika nama diklik, buka ProfileSupplierActivity dan kirim data supplier
                val intent = Intent(this@AllSupplierActivity, ProfileSupplierActivity::class.java).apply {
                    putExtra("SUPPLIER_NAME", supplier.name)
                    putExtra("SUPPLIER_STATUS", supplier.status)
                    putExtra("SUPPLIER_USERNAME", supplier.username)
                    putExtra("SUPPLIER_EMAIL", supplier.email)
                    putExtra("SUPPLIER_PHONE", supplier.phone)
                    putExtra("SUPPLIER_INCOME", supplier.income)
                }
                startActivity(intent)
            }

            row.addView(nameTextView)
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
        return TextView(this@AllSupplierActivity).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(this@AllSupplierActivity, if (isHeader) R.color.black else R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    // Fungsi untuk memfilter supplier berdasarkan input pencarian
    private fun filterSupplierTable(query: String) {
        val filteredList = supplierList.filter { supplier ->
            supplier.name.contains(query, ignoreCase = true) ||
                    supplier.username.contains(query, ignoreCase = true) ||
                    supplier.email.contains(query, ignoreCase = true)
        }

        if (filteredList.isEmpty()) {
            // Tampilkan pesan "tidak ada data" jika tidak ditemukan hasil pencarian
            binding.tvNoData.visibility = View.VISIBLE
            binding.tableView.visibility = View.GONE
        } else {
            // Sembunyikan pesan "tidak ada data" jika hasil pencarian ditemukan
            binding.tvNoData.visibility = View.GONE
            binding.tableView.visibility = View.VISIBLE
        }

        // Mengisi tabel dengan data yang difilter
        addSupplierDataToTable(filteredList)
    }
}
