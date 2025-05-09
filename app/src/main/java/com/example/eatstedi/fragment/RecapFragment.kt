package com.example.eatstedi.fragment

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.eatstedi.R
import com.example.eatstedi.activity.RecapDetailActivity
import com.example.eatstedi.databinding.FragmentRecapBinding
import com.example.eatstedi.model.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecapFragment : Fragment() {

    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding!!

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var isCheckboxVisible = false
    // Variabel untuk menyimpan referensi semua checkbox
    private val checkBoxList = mutableListOf<CheckBox>()

    // Pagination variables
    private var currentPage = 1
    private val itemsPerPage = 10// Jumlah item per halaman
    private var currentDisplayedData = listOf<Transaction>() // Data yang sedang ditampilkan setelah filter
    private var totalPages = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up button click listeners for start and end date
        setupDateFilter()

        // Set up pagination controls
        setupPagination()

        // Menampilkan transaksi awal tanpa filter
        filterAndDisplayTransactions(transactions)

        // Set up listener for search EditText to filter data without pressing Enter
        setupSearchListener()

        // Set listener untuk etSearch
        binding.etSearch.setOnClickListener {
            // Mengecek apakah tombol-tombol lainnya sudah disembunyikan
            if (binding.btnStartDate.visibility == View.VISIBLE) {
                // Menyembunyikan tombol lainnya saat et_search dipencet
                binding.btnStartDate.visibility = View.GONE
                binding.btnEndDate.visibility = View.GONE
                binding.ivActiveCheckbox.visibility = View.GONE
                binding.ivDownload.visibility = View.GONE
                binding.ivClearSearch.visibility = View.GONE

                // Mengubah lebar et_search menjadi 355dp
                val params = binding.etSearch.layoutParams
                params.width = resources.getDimensionPixelSize(R.dimen.search_expanded_width)
                binding.etSearch.layoutParams = params

                // Mengubah teks dan hint menjadi putih
                binding.etSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.etSearch.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                // Mengubah drawable dari drawableTop menjadi drawableLeft
                binding.etSearch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_search, 0, 0, 0)
            }
        }

        // Menambahkan listener untuk area lain di luar etSearch agar bisa mereset tampilan
        binding.root.setOnTouchListener { _, event ->
            // Mengecek apakah sentuhan terjadi di luar etSearch
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!isTouchInsideEditText(event)) {
                    // Reset tampilan jika klik di luar etSearch
                    resetSearchView()
                }
            }
            false
        }

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Reset the view when the EditText loses focus
                resetSearchView()
            }
        }

        binding.ivActiveCheckbox.setOnClickListener {
            // Toggle status visibility checkbox
            isCheckboxVisible = !isCheckboxVisible
            // Refresh table to toggle checkbox visibility
            displayTransactions(getPageItems())

            // Panggil toggleDeleteButtonVisibility untuk memperbarui visibilitas tombol delete
            toggleDeleteButtonVisibility()
        }

        binding.ivDelete.setOnClickListener {
            // Handle delete rekap transaksi
            deleteSelectedItems()
        }
    }

    private fun setupPagination() {
        // Listener for previous page button
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePageDisplay()
                displayTransactions(getPageItems())
            }
        }

        // Listener for next page button
        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePageDisplay()
                displayTransactions(getPageItems())
            }
        }
    }

    private fun updatePageDisplay() {
        binding.tvPageInfo.text = "Halaman $currentPage / $totalPages"

        // Enable/disable pagination buttons based on current page
        binding.btnPrevPage.isEnabled = currentPage > 1
        binding.btnNextPage.isEnabled = currentPage < totalPages

        // Update button opacity based on enabled state
        binding.btnPrevPage.alpha = if (currentPage > 1) 1.0f else 0.5f
        binding.btnNextPage.alpha = if (currentPage < totalPages) 1.0f else 0.5f
    }

    private fun calculateTotalPages(dataSize: Int): Int {
        return if (dataSize == 0) 1 else (dataSize + itemsPerPage - 1) / itemsPerPage
    }

    private fun getPageItems(): List<Transaction> {
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, currentDisplayedData.size)

        return if (currentDisplayedData.isEmpty()) {
            emptyList()
        } else if (startIndex >= currentDisplayedData.size) {
            // If current page is now invalid (e.g., after deletion of items), adjust to last valid page
            currentPage = maxOf(1, totalPages)
            getPageItems()
        } else {
            currentDisplayedData.subList(startIndex, endIndex)
        }
    }

    private fun deleteSelectedItems() {
        val checkedIndices = checkBoxList.mapIndexedNotNull { index, checkBox ->
            if (checkBox.isChecked) index else null
        }

        if (checkedIndices.isNotEmpty()) {
            // Get the current page's data
            val pageItems = getPageItems()
            // Get actual indices in the full data list
            val actualIndices = checkedIndices.map { (currentPage - 1) * itemsPerPage + it }

            // Create a new list without the selected items
            val updatedTransactions = currentDisplayedData.filterIndexed { index, _ ->
                !actualIndices.contains(index)
            }

            // Update the displayed data and reset pagination if needed
            filterAndDisplayTransactions(updatedTransactions)

            // Hide delete button after deletion
            isCheckboxVisible = false
            toggleDeleteButtonVisibility()
        }
    }

    // Fungsi untuk memeriksa apakah sentuhan berada di dalam area etSearch
    private fun isTouchInsideEditText(event: MotionEvent): Boolean {
        val location = IntArray(2)
        binding.etSearch.getLocationOnScreen(location)
        val x = event.rawX
        val y = event.rawY
        val etSearchLeft = location[0]
        val etSearchTop = location[1]
        val etSearchRight = etSearchLeft + binding.etSearch.width
        val etSearchBottom = etSearchTop + binding.etSearch.height

        return x >= etSearchLeft && x <= etSearchRight && y >= etSearchTop && y <= etSearchBottom
    }

    // Fungsi untuk mereset tampilan etSearch ke keadaan semula
    private fun resetSearchView() {
        // Mengosongkan teks pada etSearch
        binding.etSearch.text.clear()

        // Mengatur ulang lebar etSearch ke ukuran semula
        val params = binding.etSearch.layoutParams
        params.width = resources.getDimensionPixelSize(R.dimen.search_collapsed_width)
        binding.etSearch.layoutParams = params

        // Sembunyikan semua checkbox
        isCheckboxVisible = false
        checkBoxList.forEach { it.visibility = View.GONE }

        // Pastikan tombol dan etSearch tetap terlihat
        binding.btnStartDate.visibility = View.VISIBLE
        binding.btnEndDate.visibility = View.VISIBLE
        binding.ivActiveCheckbox.visibility = View.VISIBLE
        binding.ivDownload.visibility = View.VISIBLE
        binding.ivClearSearch.visibility = View.GONE
        binding.etSearch.visibility = View.VISIBLE // Pastikan etSearch tetap terlihat

        // Reset tampilan tabel transaksi ke kondisi default
        filterAndDisplayTransactions(transactions)

        // Mengembalikan warna teks dan hint menjadi default
        binding.etSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.etSearch.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        // Mengembalikan drawable ke posisi semula (drawableTop)
        binding.etSearch.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icon_search, 0, 0)

        // Menghilangkan fokus dari etSearch
        binding.etSearch.clearFocus()

        toggleDeleteButtonVisibility()
    }

    private fun setupDateFilter() {
        binding.btnStartDate.setOnClickListener {
            showDatePicker(isStartDate = true) { date ->
                startDate = date
                binding.btnStartDate.text = dateFormat.format(date.time)
                filterTransactions()
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(isStartDate = false) { date ->
                endDate = date
                binding.btnEndDate.text = dateFormat.format(date.time)
                filterTransactions()
            }
        }

        binding.ivClearSearch.setOnClickListener {
            clearFilters()
        }
    }

    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }

                if (isStartDate) {
                    if (endDate != null && selectedDate.after(endDate)) {
                        Toast.makeText(
                            requireContext(),
                            "Tanggal mulai tidak boleh setelah tanggal selesai!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@DatePickerDialog
                    }
                } else {
                    if (startDate != null && selectedDate.before(startDate)) {
                        Toast.makeText(
                            requireContext(),
                            "Tanggal selesai tidak boleh sebelum tanggal mulai!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@DatePickerDialog
                    }
                }

                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun Calendar.stripTime(): Calendar {
        this.set(Calendar.HOUR_OF_DAY, 0)
        this.set(Calendar.MINUTE, 0)
        this.set(Calendar.SECOND, 0)
        this.set(Calendar.MILLISECOND, 0)
        return this
    }

    private fun filterTransactions() {
        val filteredTransactions = if (startDate == null || endDate == null) {
            transactions
        } else {
            transactions.filter { transaction ->
                val transactionDate = Calendar.getInstance().apply {
                    time = dateFormat.parse(transaction.date)!!
                }.stripTime()

                val strippedStartDate = startDate?.stripTime()
                val strippedEndDate = endDate?.stripTime()

                !transactionDate.before(strippedStartDate) && !transactionDate.after(strippedEndDate)
            }
        }

        // Reset to first page when applying new filters
        currentPage = 1
        filterAndDisplayTransactions(filteredTransactions)

        binding.ivClearSearch.visibility = if (startDate != null || endDate != null) View.VISIBLE else View.GONE
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim().lowercase()

            if (query.isNotEmpty()) {
                // Hanya tampilkan etSearch
                binding.btnStartDate.visibility = View.GONE
                binding.btnEndDate.visibility = View.GONE
                binding.ivActiveCheckbox.visibility = View.GONE
                binding.ivDownload.visibility = View.GONE
                binding.ivClearSearch.visibility = View.GONE

                // Sembunyikan semua checkbox
                isCheckboxVisible = false
                checkBoxList.forEach { it.visibility = View.GONE }

                // Filter data berdasarkan pencarian dan reset ke halaman pertama
                val filteredTransactions = transactions.filter { transaction ->
                    transaction.employeeName.lowercase().contains(query)
                }

                currentPage = 1
                filterAndDisplayTransactions(filteredTransactions)
            } else {
                // Kembalikan tampilan default
                resetSearchView()
            }
        }
    }

    private fun filterAndDisplayTransactions(transactionsToDisplay: List<Transaction>) {
        // Update the current displayed data
        currentDisplayedData = transactionsToDisplay

        // Calculate total pages
        totalPages = calculateTotalPages(transactionsToDisplay.size)

        // Ensure current page is valid
        currentPage = minOf(currentPage, totalPages)

        // Update page display
        updatePageDisplay()

        // Display current page items
        displayTransactions(getPageItems())

        // Show/hide no data message
        if (transactionsToDisplay.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.tableView.visibility = View.GONE
            binding.paginationLayout.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.tableView.visibility = View.VISIBLE
            binding.paginationLayout.visibility = View.VISIBLE
        }
    }

    private fun displayTransactions(transactionsToDisplay: List<Transaction>) {
        binding.tableView.removeAllViews()
        checkBoxList.clear() // Bersihkan list saat tabel diperbarui

        // Create header row
        val headerRow = TableRow(context).apply {
            setPadding(16, 16, 16, 16)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))

            // Add checkbox header if checkboxes are visible
            if (isCheckboxVisible) {
                addView(TextView(context).apply {
                    layoutParams = TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    )
                })
            }

            // Add headers
            addView(createTextView("Nama Karyawan", isHeader = true))
            addView(createTextView("Tanggal", isHeader = true))
            addView(createTextView("Tipe Pembayaran", isHeader = true))
            addView(createTextView("Harga", isHeader = true))
            addView(createTextView("Kembalian", isHeader = true))
        }
        binding.tableView.addView(headerRow)

        // Add data rows
        for ((index, transaction) in transactionsToDisplay.withIndex()) {
            val row = TableRow(context).apply {
                setPadding(16, 16, 16, 16)
                setBackgroundColor(
                    if (index % 2 == 0)
                        ContextCompat.getColor(requireContext(), R.color.white)
                    else
                        ContextCompat.getColor(requireContext(), R.color.secondary)
                )

                // Add checkbox if enabled
                if (isCheckboxVisible) {
                    val checkBox = CheckBox(context).apply {
                        visibility = View.VISIBLE
                        setOnCheckedChangeListener { _, _ ->
                            // Periksa jika ada checkbox yang dicentang
                            toggleDeleteButtonVisibility()
                        }
                    }
                    checkBoxList.add(checkBox) // Simpan checkbox ke dalam list
                    addView(checkBox)
                }

                // Add data columns
                val employeeNameView = createTextView(transaction.employeeName).apply {
                    setOnClickListener {
                        // Pindah ke Activity detail
                        val intent = Intent(requireContext(), RecapDetailActivity::class.java).apply {
                            putExtra("employeeName", transaction.employeeName)
                        }
                        startActivity(intent)
                    }
                }

                addView(employeeNameView)
                addView(createTextView(transaction.date))
                addView(createTextView(transaction.paymentType))
                addView(createTextView("Rp${transaction.totalPrice}"))
                addView(createTextView("Rp${transaction.change}"))
            }
            binding.tableView.addView(row)
        }

        toggleDeleteButtonVisibility()
    }

    private fun toggleDeleteButtonVisibility() {
        val isAnyChecked = checkBoxList.any { it.isChecked }

        if (isAnyChecked) {
            // Tampilkan hanya tombol delete
            binding.ivDelete.visibility = View.VISIBLE

            // Sembunyikan tombol lainnya
            binding.ivClearSearch.visibility = View.GONE
            binding.ivDownload.visibility = View.GONE
            binding.btnStartDate.visibility = View.GONE
            binding.btnEndDate.visibility = View.GONE
            binding.etSearch.visibility = View.GONE
            binding.paginationLayout.visibility = View.GONE
        } else {
            // Sembunyikan tombol delete
            binding.ivDelete.visibility = View.GONE

            // Tampilkan kembali tombol lainnya jika sedang tidak dalam mode search
            if (binding.etSearch.text.toString().isEmpty()) {
                binding.ivDownload.visibility = View.VISIBLE
                binding.btnStartDate.visibility = View.VISIBLE
                binding.btnEndDate.visibility = View.VISIBLE
                binding.etSearch.visibility = View.VISIBLE
                if (currentDisplayedData.isNotEmpty()) {
                    binding.paginationLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            // Use consistent layout params for all columns
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                // Ensure columns are evenly distributed
                width = 0
                weight = 1f
            }
        }
    }

    private fun clearFilters() {
        startDate = null
        endDate = null
        binding.btnStartDate.text = "dd/mm/yyyy"
        binding.btnEndDate.text = "dd/mm/yyyy"
        currentPage = 1
        filterAndDisplayTransactions(transactions)
        binding.ivClearSearch.visibility = View.GONE
        binding.etSearch.text.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Dummy transactions
    private val transactions = listOf(
        Transaction("Alice", "01/12/2024", "QRIS", 20000, 30000),
        Transaction("Bob", "02/12/2024", "QRIS", 20000, 20000),
        Transaction("Charlie", "03/12/2024", "Cash", 20000, 54000),
        Transaction("Dave", "04/12/2024", "QRIS", 20000, 44000),
        Transaction("Eve", "05/12/2024", "Cash", 20000, 30000),
        Transaction("Faythe", "06/12/2024", "Cash", 20000, 30000),
        Transaction("Grace", "07/12/2024", "QRIS", 20000, 20000),
        Transaction("Heidi", "08/12/2024", "Cash", 20000, 30000),
        Transaction("Ivan", "09/12/2024", "QRIS", 20000, 20000),
        Transaction("Judy", "10/12/2024", "Cash", 20000, 30000),
        Transaction("Karl", "11/12/2024", "QRIS", 20000, 20000),
        Transaction("Lily", "12/12/2024", "Cash", 20000, 30000),
        Transaction("Mallory", "13/12/2024", "QRIS", 20000, 20000),
        Transaction("Nia", "14/12/2024", "Cash", 20000, 30000),
        Transaction("Oscar", "15/12/2024", "QRIS", 20000, 20000),
        Transaction("Peggy", "16/12/2024", "Cash", 20000, 30000),
    )
}