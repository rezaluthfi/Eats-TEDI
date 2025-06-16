package com.example.eatstedi.fragment

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.eatstedi.R
import com.example.eatstedi.activity.RecapDetailActivity
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.*
import com.example.eatstedi.databinding.FragmentRecapBinding
import com.example.eatstedi.model.Employee
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class RecapFragment : Fragment() {

    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding!!

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private lateinit var apiService: ApiService

    private var isCheckboxVisible = false
    private val checkBoxList = mutableListOf<CheckBox>()

    private var currentPage = 1
    private val itemsPerPage = 20
    private var currentDisplayedData = listOf<Receipt>()
    private var totalPages = 1
    private var cashierList: List<Employee> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apiService = RetrofitClient.getInstance(requireContext())
        setupDateFilter()
        setupPagination()

        fetchCashiers {
            fetchReceipts()
        }

        setupSearchListener()

        binding.etSearch.setOnClickListener {
            if (binding.btnStartDate.visibility == View.VISIBLE) {
                binding.btnStartDate.visibility = View.GONE
                binding.btnEndDate.visibility = View.GONE
                binding.ivActiveCheckbox.visibility = View.GONE
                binding.ivDownload.visibility = View.GONE
                binding.ivClearSearch.visibility = View.GONE

                val params = binding.etSearch.layoutParams
                params.width = resources.getDimensionPixelSize(R.dimen.search_expanded_width)
                binding.etSearch.layoutParams = params

                binding.etSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.etSearch.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.etSearch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_search, 0, 0, 0)
            }
        }

        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!isTouchInsideEditText(event)) {
                    resetSearchView()
                }
            }
            false
        }

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                resetSearchView()
            }
        }

        binding.ivActiveCheckbox.setOnClickListener {
            isCheckboxVisible = !isCheckboxVisible
            displayTransactions(getPageItems())
            toggleDeleteButtonVisibility()
        }

        binding.ivDelete.setOnClickListener {
            deleteSelectedItems()
        }

        binding.ivDownload.setOnClickListener {
            exportReceipts()
        }
    }

    private fun fetchCashiers(onComplete: () -> Unit) {
        apiService.getCashiers().enqueue(object : Callback<CashierResponse> {
            override fun onResponse(call: Call<CashierResponse>, response: Response<CashierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    cashierList = response.body()?.data ?: emptyList()
                    Log.d("RecapFragment", "Fetched ${cashierList.size} cashiers: ${cashierList.map { "${it.id} -> ${it.name}" }}")
                } else {
                    cashierList = emptyList()
                    Log.e("RecapFragment", "Failed to fetch cashiers: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Gagal mengambil daftar kasir", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }

            override fun onFailure(call: Call<CashierResponse>, t: Throwable) {
                cashierList = emptyList()
                Log.e("RecapFragment", "Error fetching cashiers: ${t.message}", t)
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupPagination() {
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePageDisplay()
                displayTransactions(getPageItems())
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePageDisplay()
                displayTransactions(getPageItems())
            }
        }
    }

    private fun updatePageDisplay() {
        _binding?.let { binding -> // Menggunakan 'binding' dari 'let' block
            // Hitung total halaman, pastikan tidak 0 jika data kosong
            totalPages = if (currentDisplayedData.isEmpty()) 1 else (currentDisplayedData.size + itemsPerPage - 1) / itemsPerPage

            // Tampilkan teks halaman
            binding.tvPageInfo.text = "Halaman $currentPage / $totalPages"

            // Cek kondisi untuk tombol "Sebelumnya"
            val canGoPrev = currentPage > 1
            binding.btnPrevPage.isEnabled = canGoPrev
            binding.btnPrevPage.alpha = if (canGoPrev) 1.0f else 0.5f // <--- TAMBAHKAN BARIS INI

            // Cek kondisi untuk tombol "Selanjutnya"
            val canGoNext = currentPage < totalPages
            binding.btnNextPage.isEnabled = canGoNext
            binding.btnNextPage.alpha = if (canGoNext) 1.0f else 0.5f // <--- TAMBAHKAN BARIS INI
        }
    }

    private fun calculateTotalPages(dataSize: Int): Int {
        return if (dataSize == 0) 1 else (dataSize + itemsPerPage - 1) / itemsPerPage
    }

    private fun getPageItems(): List<Receipt> {
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, currentDisplayedData.size)
        return if (currentDisplayedData.isEmpty()) {
            emptyList()
        } else if (startIndex >= currentDisplayedData.size) {
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
            val pageItems = getPageItems()
            val actualIndices = checkedIndices.map { (currentPage - 1) * itemsPerPage + it }
            val idsToDelete = actualIndices.mapNotNull { currentDisplayedData.getOrNull(it)?.id }

            if (idsToDelete.isNotEmpty()) {
                val request = DeleteReceiptRequest(idsToDelete)
                apiService.deleteReceipts(request).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(requireContext(), "Berhasil menghapus transaksi", Toast.LENGTH_SHORT).show()
                            fetchReceipts()
                        } else {
                            Toast.makeText(requireContext(), "Gagal menghapus transaksi: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                            Log.e("RecapFragment", "Delete error: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        Log.e("RecapFragment", "Delete failure: ${t.message}", t)
                    }
                })
            }

            isCheckboxVisible = false
            toggleDeleteButtonVisibility()
        }
    }

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun resetSearchView() {
        // Perbaikan: Bungkus semua manipulasi search view.
        _binding?.let { binding ->
            binding.etSearch.text.clear()
            val params = binding.etSearch.layoutParams
            params.width = resources.getDimensionPixelSize(R.dimen.search_collapsed_width)
            binding.etSearch.layoutParams = params

            isCheckboxVisible = false
            checkBoxList.forEach { it.visibility = View.GONE }

            binding.btnStartDate.visibility = View.VISIBLE
            binding.btnEndDate.visibility = View.VISIBLE
            binding.ivActiveCheckbox.visibility = View.VISIBLE
            binding.ivDownload.visibility = View.VISIBLE
            binding.ivClearSearch.visibility = View.GONE
            binding.etSearch.visibility = View.VISIBLE

            filterAndDisplayTransactions(currentDisplayedData)

            binding.etSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.etSearch.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.etSearch.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icon_search, 0, 0)
            binding.etSearch.clearFocus()

            toggleDeleteButtonVisibility()
        }
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
                        Toast.makeText(requireContext(), "Tanggal mulai tidak boleh setelah tanggal selesai!", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                } else {
                    if (startDate != null && selectedDate.before(startDate)) {
                        Toast.makeText(requireContext(), "Tanggal selesai tidak boleh sebelum tanggal mulai!", Toast.LENGTH_SHORT).show()
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
        if (startDate != null && endDate != null) {
            val startDateStr = apiDateFormat.format(startDate!!.time)
            val endDateStr = apiDateFormat.format(endDate!!.time)
            val request = SearchReceiptByDateRequest(startDateStr, endDateStr)
            apiService.searchReceiptsByDate(request).enqueue(object : Callback<ReceiptResponse> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ReceiptResponse>, response: Response<ReceiptResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            currentPage = 1
                            filterAndDisplayTransactions(body.data ?: emptyList())
                            binding.ivClearSearch.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(requireContext(), "Gagal memfilter tanggal: ${body?.message}", Toast.LENGTH_SHORT).show()
                            Log.e("RecapFragment", "Filter date error: ${response.errorBody()?.string()}")
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal memfilter tanggal", Toast.LENGTH_SHORT).show()
                        Log.e("RecapFragment", "Filter date error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ReceiptResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RecapFragment", "Filter date failure: ${t.message}", t)
                }
            })
        } else {
            fetchReceipts()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isNotEmpty()) {
                Log.d("RecapFragment", "Search triggered with query: '$query'")

                binding.btnStartDate.visibility = View.GONE
                binding.btnEndDate.visibility = View.GONE
                binding.ivActiveCheckbox.visibility = View.GONE
                binding.ivDownload.visibility = View.GONE
                binding.ivClearSearch.visibility = View.GONE

                isCheckboxVisible = false
                checkBoxList.forEach { it.visibility = View.GONE }

                val request = SearchReceiptRequest(query)
                Log.d("RecapFragment", "SearchReceiptRequest created: $request")

                apiService.searchReceipts(request).enqueue(object : Callback<ReceiptResponse> {
                    override fun onResponse(call: Call<ReceiptResponse>, response: Response<ReceiptResponse>) {
                        Log.d("RecapFragment", "Search Response - Code: ${response.code()}, URL: ${call.request().url}")

                        when (response.code()) {
                            200 -> {
                                val body = response.body()
                                if (body?.success == true) {
                                    Log.d("RecapFragment", "Search successful, data count: ${body.data.size}")
                                    currentPage = 1
                                    filterAndDisplayTransactions(body.data)
                                } else {
                                    Log.w("RecapFragment", "Search response not successful: ${body?.message}")
                                    val errorMessage = body?.message ?: "Response tidak berhasil"
                                    Toast.makeText(requireContext(), "Pencarian gagal: $errorMessage", Toast.LENGTH_SHORT).show()
                                    filterAndDisplayTransactions(emptyList())
                                }
                            }
                            404 -> {
                                Log.e("RecapFragment", "404 - Endpoint not found: ${call.request().url}")
                                Toast.makeText(requireContext(),
                                    "Endpoint tidak ditemukan. URL: ${call.request().url}\nPastikan server Laravel memiliki route untuk search-receipts",
                                    Toast.LENGTH_LONG).show()
                                filterAndDisplayTransactions(emptyList())
                            }
                            401 -> {
                                Log.e("RecapFragment", "401 - Unauthorized")
                                Toast.makeText(requireContext(), "Tidak memiliki akses. Token mungkin expired", Toast.LENGTH_SHORT).show()
                                filterAndDisplayTransactions(emptyList())
                            }
                            500 -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e("RecapFragment", "500 - Server Error: $errorBody")
                                Toast.makeText(requireContext(), "Server error. Cek log Laravel", Toast.LENGTH_SHORT).show()
                                filterAndDisplayTransactions(emptyList())
                            }
                            else -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e("RecapFragment", "HTTP ${response.code()}: $errorBody")
                                Toast.makeText(requireContext(), "HTTP Error ${response.code()}: ${response.message()}", Toast.LENGTH_SHORT).show()
                                filterAndDisplayTransactions(emptyList())
                            }
                        }
                    }

                    override fun onFailure(call: Call<ReceiptResponse>, t: Throwable) {
                        Log.e("RecapFragment", "Search network failure", t)

                        val errorMessage = when {
                            t is java.net.UnknownHostException -> "Tidak dapat resolve hostname. Cek koneksi internet atau alamat server"
                            t is java.net.ConnectException -> "Tidak dapat connect ke server. Pastikan server berjalan di http://10.0.2.2:8000"
                            t is java.net.SocketTimeoutException -> "Timeout connecting to server"
                            t.message?.contains("CLEARTEXT communication") == true -> "HTTP tidak diizinkan. Gunakan HTTPS atau tambahkan android:usesCleartextTraffic=\"true\""
                            else -> "Network error: ${t.message}"
                        }

                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        filterAndDisplayTransactions(emptyList())
                    }
                })
            } else {
                resetSearchView()
            }
        }
    }

    private fun fetchReceipts() {
        apiService.getReceipts().enqueue(object : Callback<ReceiptResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ReceiptResponse>, response: Response<ReceiptResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        currentPage = 1
                        filterAndDisplayTransactions(body.data ?: emptyList())
                    } else {
                        Toast.makeText(requireContext(), "Gagal mengambil transaksi: ${body?.message}", Toast.LENGTH_SHORT).show()
                        Log.e("RecapFragment", "Fetch error: ${response.errorBody()?.string()}")
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mengambil transaksi", Toast.LENGTH_SHORT).show()
                    Log.e("RecapFragment", "Fetch error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ReceiptResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("RecapFragment", "Fetch failure: ${t.message}", t)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterAndDisplayTransactions(receiptsToDisplay: List<Receipt>) {
        // Perbaikan: Bungkus logika yang berinteraksi dengan view.
        _binding?.let { binding ->
            currentDisplayedData = receiptsToDisplay
            totalPages = calculateTotalPages(receiptsToDisplay.size)
            currentPage = minOf(currentPage, totalPages)

            // Panggilan ini sekarang aman karena di dalam blok 'let'.
            updatePageDisplay()
            displayTransactions(getPageItems())

            if (receiptsToDisplay.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.tableView.visibility = View.GONE
                binding.paginationLayout.visibility = View.GONE
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.tableView.visibility = View.VISIBLE
                binding.paginationLayout.visibility = View.VISIBLE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayTransactions(receiptsToDisplay: List<Receipt>) {
        // Perbaikan: Bungkus semua manipulasi tabel dan view.
        _binding?.let { binding ->
            binding.tableView.removeAllViews()
            checkBoxList.clear()

            val headerRow = TableRow(context).apply {
                setPadding(16, 16, 16, 16)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))

                if (isCheckboxVisible) {
                    addView(TextView(context).apply {
                        layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
                    })
                }

                addView(createTextView("Nama Karyawan", isHeader = true))
                addView(createTextView("Tanggal", isHeader = true))
                addView(createTextView("Tipe Pembayaran", isHeader = true))
                addView(createTextView("Harga", isHeader = true))
                addView(createTextView("Kembalian", isHeader = true))
            }
            binding.tableView.addView(headerRow)

            for ((index, receipt) in receiptsToDisplay.withIndex()) {
                val cashierName = getCashierName(receipt.cashier_id)
                Log.d("RecapFragment", "Displaying receipt ID ${receipt.id}, Cashier ID ${receipt.cashier_id}, Cashier Name: $cashierName")

                val row = TableRow(context).apply {
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(
                        if (index % 2 == 0) ContextCompat.getColor(requireContext(), R.color.white)
                        else ContextCompat.getColor(requireContext(), R.color.secondary)
                    )

                    if (isCheckboxVisible) {
                        val checkBox = CheckBox(context).apply {
                            visibility = View.VISIBLE
                            setOnCheckedChangeListener { _, _ -> toggleDeleteButtonVisibility() }
                        }
                        checkBoxList.add(checkBox)
                        addView(checkBox)
                    }

                    val employeeNameView = createTextView(cashierName).apply {
                        setOnClickListener {
                            val intent = Intent(requireContext(), RecapDetailActivity::class.java).apply {
                                putExtra("receiptId", receipt.id)
                            }
                            startActivity(intent)
                        }
                    }

                    addView(employeeNameView)
                    addView(createTextView(formatWibDate(receipt.created_at)))
                    addView(createTextView(receipt.payment_type.replaceFirstChar { it.uppercase() }))
                    addView(createTextView("Rp${formatPrice(receipt.total)}"))
                    addView(createTextView("Rp${formatPrice(receipt.returns)}"))
                }
                binding.tableView.addView(row)
            }

            // Panggilan ini juga sekarang aman.
            toggleDeleteButtonVisibility()
        }
    }

    private fun getCashierName(cashierId: Int): String {
        val cashier = cashierList.find { it.id == cashierId }
        return cashier?.name ?: run {
            Log.w("RecapFragment", "Cashier ID $cashierId not found in cashierList")
            "Kasir Tidak Diketahui"
        }
    }

    private fun toggleDeleteButtonVisibility() {
        // Perbaikan: Bungkus semua perubahan visibilitas view.
        _binding?.let { binding ->
            val isAnyChecked = checkBoxList.any { it.isChecked }

            if (isAnyChecked) {
                binding.ivDelete.visibility = View.VISIBLE
                binding.ivClearSearch.visibility = View.GONE
                binding.ivDownload.visibility = View.GONE
                binding.btnStartDate.visibility = View.GONE
                binding.btnEndDate.visibility = View.GONE
                binding.etSearch.visibility = View.GONE
                binding.paginationLayout.visibility = View.GONE
            } else {
                binding.ivDelete.visibility = View.GONE
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
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                width = 0
                weight = 1f
            }
        }
    }

    private fun clearFilters() {
        _binding?.let { binding ->
            startDate = null
            endDate = null
            binding.btnStartDate.text = "dd/mm/yyyy"
            binding.btnEndDate.text = "dd/mm/yyyy"
            currentPage = 1
            fetchReceipts()
            binding.ivClearSearch.visibility = View.GONE
            binding.etSearch.text.clear()
        }
    }

    private fun formatPrice(price: Int): String {
        val numberFormat = java.text.NumberFormat.getInstance(Locale("id", "ID"))
        return numberFormat.format(price)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatWibDate(utcDateStr: String): String {
        return try {
            val utcDateTime = LocalDateTime.parse(utcDateStr, DateTimeFormatter.ISO_DATE_TIME)
            val wibZone = ZoneId.of("Asia/Jakarta")
            val wibDateTime = utcDateTime.atZone(wibZone).toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            wibDateTime.format(formatter)
        } catch (e: Exception) {
            Log.e("RecapFragment", "Error parsing date: $utcDateStr, ${e.message}")
            "N/A"
        }
    }

    private fun exportReceipts() {
        apiService.exportReceipts().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        try {
                            val fileName = "receipts_export_${System.currentTimeMillis()}.pdf"
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val file = File(downloadsDir, fileName)

                            FileOutputStream(file).use { outputStream ->
                                outputStream.write(body.bytes())
                                outputStream.flush()
                            }

                            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            intent.data = android.net.Uri.fromFile(file)
                            requireContext().sendBroadcast(intent)

                            Toast.makeText(requireContext(), "PDF berhasil disimpan di Download/$fileName", Toast.LENGTH_LONG).show()
                        } catch (e: IOException) {
                            Log.e("RecapFragment", "Error saving PDF: ${e.message}", e)
                            Toast.makeText(requireContext(), "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mengunduh PDF: ${response.message()}", Toast.LENGTH_SHORT).show()
                    Log.e("RecapFragment", "Export error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("RecapFragment", "Export failure: ${t.message}", t)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}