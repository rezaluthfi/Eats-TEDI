package com.example.eatstedi.fragment

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
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

@RequiresApi(Build.VERSION_CODES.O)
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
    private var allReceipts = listOf<Receipt>() // Simpan semua data asli
    private var currentDisplayedData = listOf<Receipt>()
    private var totalPages = 1
    private var cashierList: List<Employee> = emptyList()

    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private var pendingDownload: (() -> Unit)? = null

    // --- Lifecycle Methods ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi launcher untuk izin penyimpanan
        requestPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Izin penyimpanan diberikan.", Toast.LENGTH_SHORT).show()
                pendingDownload?.invoke()
                pendingDownload = null
            } else {
                Toast.makeText(requireContext(), "Izin penyimpanan ditolak. Tidak dapat mengunduh file.", Toast.LENGTH_SHORT).show()
            }
        }

        apiService = RetrofitClient.getInstance(requireContext())
        setupUI()
        showShimmer()
        fetchCashiers { fetchReceipts() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Shimmer Control ---

    private fun showShimmer() {
        _binding?.let {
            it.shimmerTable.visibility = View.VISIBLE
            it.shimmerPagination.visibility = View.VISIBLE
            it.contentContainer.visibility = View.GONE
            it.paginationLayout.visibility = View.GONE
            it.tvNoData.visibility = View.GONE
            it.shimmerTable.startShimmer()
            it.shimmerPagination.startShimmer()
        }
    }

    private fun hideShimmer() {
        _binding?.let {
            it.shimmerTable.stopShimmer()
            it.shimmerPagination.stopShimmer()
            it.shimmerTable.visibility = View.GONE
            it.shimmerPagination.visibility = View.GONE
            it.contentContainer.visibility = View.VISIBLE
        }
    }

    // --- UI Setup ---

    private fun setupUI() {
        setupDateFilter()
        setupSearchListener()
        setupPagination()
        setupSearchBarInteractions()
        setupCheckboxToggle()
        setupDeleteButton()
        setupDownloadButton()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchBarInteractions() {
        _binding?.let { binding ->
            binding.etSearch.setOnClickListener {
                if (binding.btnStartDate.visibility == View.VISIBLE) {
                    hideHeaderControls()
                    expandSearchBar()
                }
            }
            binding.root.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !isTouchInsideEditText(event)) {
                    resetSearchView()
                }
                false
            }
            binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) resetSearchView()
            }
        }
    }

    private fun hideHeaderControls() {
        _binding?.let {
            it.btnStartDate.visibility = View.GONE
            it.btnEndDate.visibility = View.GONE
            it.ivActiveCheckbox.visibility = View.GONE
            it.ivDownload.visibility = View.GONE
            it.ivClearSearch.visibility = View.GONE
        }
    }

    private fun expandSearchBar() {
        _binding?.let {
            val params = it.etSearch.layoutParams
            params.width = resources.getDimensionPixelSize(R.dimen.search_expanded_width)
            it.etSearch.layoutParams = params
            it.etSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            it.etSearch.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            it.etSearch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_search, 0, 0, 0)
        }
    }

    private fun setupCheckboxToggle() {
        _binding?.ivActiveCheckbox?.setOnClickListener {
            isCheckboxVisible = !isCheckboxVisible
            displayTransactions(getPageItems())
            toggleDeleteButtonVisibility()
        }
    }

    private fun setupDeleteButton() {
        _binding?.ivDelete?.setOnClickListener { deleteSelectedItems() }
    }

    private fun setupDownloadButton() {
        _binding?.ivDownload?.setOnClickListener { exportReceipts() }
    }

    // --- Data Fetching ---

    private fun fetchCashiers(onComplete: () -> Unit) {
        apiService.getCashiers().enqueue(object : Callback<CashierResponse> {
            override fun onResponse(call: Call<CashierResponse>, response: Response<CashierResponse>) {
                if (!isAdded) return // Guard
                if (response.isSuccessful && response.body()?.success == true) {
                    cashierList = response.body()?.data ?: emptyList()
                } else {
                    cashierList = emptyList()
                    showToast("Gagal mengambil daftar kasir")
                }
                onComplete()
            }
            override fun onFailure(call: Call<CashierResponse>, t: Throwable) {
                if (!isAdded) return // Guard
                cashierList = emptyList()
                showToast("Error jaringan: Gagal mengambil kasir")
                onComplete()
            }
        })
    }

    private fun fetchReceipts() {
        showShimmer()
        apiService.getReceipts().enqueue(object : Callback<ReceiptResponse> {
            override fun onResponse(call: Call<ReceiptResponse>, response: Response<ReceiptResponse>) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    allReceipts = body.data ?: emptyList()
                    filterAndDisplayTransactions(allReceipts)
                } else {
                    showToast("Gagal mengambil transaksi: ${body?.message}")
                    filterAndDisplayTransactions(emptyList())
                }
            }
            override fun onFailure(call: Call<ReceiptResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                showToast("Error jaringan: ${t.message}")
                filterAndDisplayTransactions(emptyList())
            }
        })
    }

    // --- Date Filtering ---

    private fun setupDateFilter() {
        _binding?.let { binding ->
            binding.btnStartDate.setOnClickListener {
                showDatePicker(isStartDate = true) { date ->
                    startDate = date
                    binding.btnStartDate.text = dateFormat.format(date.time)
                    filterTransactionsByDate()
                }
            }
            binding.btnEndDate.setOnClickListener {
                showDatePicker(isStartDate = false) { date ->
                    endDate = date
                    binding.btnEndDate.text = dateFormat.format(date.time)
                    filterTransactionsByDate()
                }
            }
            binding.ivClearSearch.setOnClickListener { clearFilters() }
        }
    }

    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Calendar) -> Unit) {
        if (!isAdded) return // Guard
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                    stripTime()
                }
                if (isStartDate && endDate != null && selectedDate.after(endDate)) {
                    showToast("Tanggal mulai tidak boleh setelah tanggal selesai!")
                    return@DatePickerDialog
                }
                if (!isStartDate && startDate != null && selectedDate.before(startDate)) {
                    showToast("Tanggal selesai tidak boleh sebelum tanggal mulai!")
                    return@DatePickerDialog
                }
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun Calendar.stripTime(): Calendar {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        return this
    }

    private fun filterTransactionsByDate() {
        val safeStartDate = startDate
        val safeEndDate = endDate
        if (safeStartDate == null || safeEndDate == null) {
            return
        }
        showShimmer()
        val request = SearchReceiptByDateRequest(
            apiDateFormat.format(safeStartDate.time),
            apiDateFormat.format(safeEndDate.time)
        )
        apiService.searchReceiptsByDate(request).enqueue(object : Callback<ReceiptResponse> {
            override fun onResponse(call: Call<ReceiptResponse>, response: Response<ReceiptResponse>) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    allReceipts = body.data ?: emptyList()
                    filterAndDisplayTransactions(allReceipts)
                    _binding?.ivClearSearch?.visibility = View.VISIBLE
                } else {
                    showToast("Gagal memfilter tanggal: ${body?.message}")
                    filterAndDisplayTransactions(emptyList())
                }
            }
            override fun onFailure(call: Call<ReceiptResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                showToast("Error jaringan: ${t.message}")
                filterAndDisplayTransactions(emptyList())
            }
        })
    }

    private fun clearFilters() {
        startDate = null
        endDate = null
        _binding?.let {
            it.btnStartDate.text = "dd/mm/yyyy"
            it.btnEndDate.text = "dd/mm/yyyy"
            it.ivClearSearch.visibility = View.GONE
            it.etSearch.text.clear()
        }
        fetchReceipts()
    }

    // --- Search ---

    private fun setupSearchListener() {
        _binding?.etSearch?.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isNotEmpty()) {
                searchTransactions(query)
            } else if(!_binding?.etSearch!!.isFocused) {
                // Jika query kosong dan tidak sedang difokus, reset
                resetSearchView()
            }
        }
    }

    private fun searchTransactions(query: String) {
        showShimmer()
        apiService.searchReceipts(SearchReceiptRequest(query)).enqueue(object : Callback<ReceiptResponse> {
            override fun onResponse(call: Call<ReceiptResponse>, response: Response<ReceiptResponse>) {
                if (!isAdded || _binding == null) return
                hideShimmer()

                val body = response.body()

                // Kondisi 1: Respons sukses dari server
                if (response.isSuccessful && body?.success == true) {
                    val searchResult = body.data
                    if (searchResult != null && searchResult.isNotEmpty()) {
                        // Sukses dan ada data
                        filterAndDisplayTransactions(searchResult)
                    } else {
                        // Sukses tapi tidak ada data (kasus kasir 3, 4, 5)
                        filterAndDisplayTransactions(emptyList())
                        showToast("Tidak ada transaksi untuk '$query'")
                    }
                }
                // Kondisi 2: Respons GAGAL, kita periksa alasannya
                else {
                    // Kita perlu membaca errorBody, tapi karena response.body() mungkin juga berisi pesan,
                    // errorBody() hanya ada jika kode HTTP bukan 2xx.
                    val errorBodyString = response.errorBody()?.string() ?: body?.message ?: ""

                    // Cek apakah pesan error mengandung frasa kunci dari backend bahwa model Cashier tidak ditemukan
                    if (errorBodyString.contains("No query results for model [App\\\\Models\\\\Cashier]")) {

                        // Ini kasus kasir 2. Kita anggap sebagai "kasir tidak ditemukan", bukan error
                        Log.w("RecapFragment", "Search failed because cashier model was not found. Treating as 'not found'.")
                        filterAndDisplayTransactions(emptyList())
                        showToast("Kasir dengan nama '$query' tidak ditemukan")

                    } else {
                        // Ini adalah error server yang sebenarnya
                        val errorMessage = body?.message ?: "Terjadi kesalahan pada server"
                        showToast("Pencarian gagal: $errorMessage")
                        filterAndDisplayTransactions(emptyList())
                    }
                    // =============================================================
                }
            }

            override fun onFailure(call: Call<ReceiptResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return
                hideShimmer()
                showToast("Error jaringan saat mencari: ${t.message}")
                filterAndDisplayTransactions(emptyList())
            }
        })
    }

    private fun resetSearchView() {
        _binding?.let { binding ->
            binding.etSearch.text.clear()
            val params = binding.etSearch.layoutParams
            params.width = resources.getDimensionPixelSize(R.dimen.search_collapsed_width)
            binding.etSearch.layoutParams = params

            binding.btnStartDate.visibility = View.VISIBLE
            binding.btnEndDate.visibility = View.VISIBLE
            binding.ivActiveCheckbox.visibility = View.VISIBLE
            binding.ivDownload.visibility = View.VISIBLE
            binding.etSearch.visibility = View.VISIBLE
            binding.etSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.etSearch.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.etSearch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_search, 0, 0, 0)
            binding.etSearch.clearFocus()

            // Jika filter tanggal aktif, jangan fetch semua, tapi tampilkan hasil filter terakhir
            if (startDate != null && endDate != null) {
                filterAndDisplayTransactions(currentDisplayedData) // tampilkan hasil filter terakhir
            } else {
                filterAndDisplayTransactions(allReceipts) // tampilkan semua data
            }
            toggleDeleteButtonVisibility()
        }
    }

    private fun isTouchInsideEditText(event: MotionEvent): Boolean {
        val et = _binding?.etSearch ?: return false
        val location = IntArray(2)
        et.getLocationOnScreen(location)
        val x = event.rawX
        val y = event.rawY
        val etLeft = location[0]
        val etTop = location[1]
        val etRight = etLeft + et.width
        val etBottom = etTop + et.height
        return x >= etLeft && x <= etRight && y >= etTop && y <= etBottom
    }

    // --- Pagination ---

    private fun setupPagination() {
        _binding?.let { binding ->
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
    }

    private fun updatePageDisplay() {
        _binding?.let {
            totalPages = if (currentDisplayedData.isEmpty()) 1 else (currentDisplayedData.size + itemsPerPage - 1) / itemsPerPage
            it.tvPageInfo.text = "Halaman $currentPage / $totalPages"
            it.btnPrevPage.isEnabled = currentPage > 1
            it.btnPrevPage.alpha = if (it.btnPrevPage.isEnabled) 1.0f else 0.5f
            it.btnNextPage.isEnabled = currentPage < totalPages
            it.btnNextPage.alpha = if (it.btnNextPage.isEnabled) 1.0f else 0.5f
        }
    }

    private fun getPageItems(): List<Receipt> {
        val startIndex = (currentPage - 1) * itemsPerPage
        if (startIndex >= currentDisplayedData.size) return emptyList()
        val endIndex = minOf(startIndex + itemsPerPage, currentDisplayedData.size)
        return currentDisplayedData.subList(startIndex, endIndex)
    }

    // --- Display Logic ---

    private fun filterAndDisplayTransactions(receiptsToDisplay: List<Receipt>) {
        currentDisplayedData = receiptsToDisplay
        totalPages = calculateTotalPages(receiptsToDisplay.size)
        currentPage = 1 // Selalu reset ke halaman pertama setelah filter/pencarian

        updatePageDisplay()
        displayTransactions(getPageItems())

        _binding?.let {
            val isEmpty = receiptsToDisplay.isEmpty()
            it.tvNoData.visibility = if (isEmpty) View.VISIBLE else View.GONE
            it.tableView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            it.paginationLayout.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun calculateTotalPages(dataSize: Int): Int {
        return if (dataSize == 0) 1 else (dataSize + itemsPerPage - 1) / itemsPerPage
    }

    private fun displayTransactions(receiptsToDisplay: List<Receipt>) {
        if (!isAdded) return // Guard
        _binding?.let { binding ->
            binding.tableView.removeAllViews()
            checkBoxList.clear()

            val headerRow = TableRow(context).apply {
                setPadding(16, 16, 16, 16)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                if (isCheckboxVisible) addView(TextView(context)) // Placeholder for checkbox
                addView(createTextView("Nama Karyawan", isHeader = true))
                addView(createTextView("Tanggal", isHeader = true))
                addView(createTextView("Tipe Pembayaran", isHeader = true))
                addView(createTextView("Harga", isHeader = true))
                addView(createTextView("Kembalian", isHeader = true))
            }
            binding.tableView.addView(headerRow)

            receiptsToDisplay.forEachIndexed { index, receipt ->
                val row = TableRow(context).apply {
                    setPadding(16, 16, 16, 16)
                    val bgColor = if (index % 2 == 0) R.color.white else R.color.secondary
                    setBackgroundColor(ContextCompat.getColor(requireContext(), bgColor))
                    setOnClickListener {
                        val intent = Intent(requireContext(), RecapDetailActivity::class.java).apply {
                            putExtra("receiptId", receipt.id)
                        }
                        startActivity(intent)
                    }
                }

                if (isCheckboxVisible) {
                    val checkBox = CheckBox(context).apply { setOnCheckedChangeListener { _, _ -> toggleDeleteButtonVisibility() } }
                    checkBoxList.add(checkBox)
                    row.addView(checkBox)
                }

                row.addView(createTextView(getCashierName(receipt.cashier_id)))
                row.addView(createTextView(formatWibDateTime(receipt.created_at)))
                row.addView(createTextView(receipt.payment_type.replaceFirstChar { it.uppercase() }))
                row.addView(createTextView("Rp${formatPrice(receipt.total)}"))
                row.addView(createTextView("Rp${formatPrice(receipt.returns)}"))
                binding.tableView.addView(row)
            }
            toggleDeleteButtonVisibility()
        }
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = if (isHeader) 16f else 14f
            setPadding(16, 8, 16, 8)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun getCashierName(cashierId: Int): String {
        return cashierList.find { it.id == cashierId }?.name ?: "Kasir Tidak Dikenal"
    }

    private fun toggleDeleteButtonVisibility() {
        _binding?.let { binding ->
            // Cek apakah ada checkbox yang tercentang.
            val anyChecked = checkBoxList.any { it.isChecked }

            // Atur visibilitas tombol delete berdasarkan kondisi di atas.
            binding.ivDelete.visibility = if (anyChecked) View.VISIBLE else View.GONE

            // Atur visibilitas semua kontrol header lainnya.
            if (anyChecked) {
                // Jika ada yang tercentang, SEMBUNYIKAN semua kontrol header lain.
                binding.btnStartDate.visibility = View.GONE
                binding.btnEndDate.visibility = View.GONE
                binding.ivActiveCheckbox.visibility = View.GONE
                binding.ivDownload.visibility = View.GONE
                binding.etSearch.visibility = View.GONE
                binding.ivClearSearch.visibility = View.GONE
                // Sembunyikan juga pagination agar tidak mengganggu
                binding.paginationLayout.visibility = View.GONE
            } else {
                // Jika TIDAK ada yang tercentang:
                // Cek apakah kita sedang dalam mode search atau tidak.
                val isSearching = binding.etSearch.isFocused || binding.etSearch.text.isNotEmpty()
                if (!isSearching) {
                    // Jika tidak sedang search, TAMPILKAN KEMBALI semua kontrol header.
                    binding.btnStartDate.visibility = View.VISIBLE
                    binding.btnEndDate.visibility = View.VISIBLE

                    binding.ivActiveCheckbox.visibility = View.VISIBLE

                    binding.ivDownload.visibility = View.VISIBLE
                    binding.etSearch.visibility = View.VISIBLE

                    // Tampilkan tombol clear hanya jika filter tanggal aktif
                    binding.ivClearSearch.visibility = if (startDate != null || endDate != null) View.VISIBLE else View.GONE

                    // Tampilkan kembali pagination jika ada data
                    if (currentDisplayedData.isNotEmpty()) {
                        binding.paginationLayout.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // --- Deletion ---

    private fun deleteSelectedItems() {
        val checkedIndices = checkBoxList.mapIndexedNotNull { index, checkBox -> if (checkBox.isChecked) index else null }
        if (checkedIndices.isEmpty()) {
            isCheckboxVisible = false
            toggleDeleteButtonVisibility()
            displayTransactions(getPageItems())
            return
        }

        showShimmer()
        val pagedItems = getPageItems()
        val idsToDelete = checkedIndices.mapNotNull { pagedItems.getOrNull(it)?.id }
        if (idsToDelete.isEmpty()) {
            hideShimmer()
            return
        }

        apiService.deleteReceipts(DeleteReceiptRequest(idsToDelete)).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                if (response.isSuccessful && response.body()?.success == true) {
                    showToast("Berhasil menghapus transaksi")
                    fetchReceipts() // Refresh data dari server
                } else {
                    showToast("Gagal menghapus: ${response.body()?.message}")
                    displayTransactions(getPageItems()) // Tampilkan kembali data lama jika gagal
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                showToast("Error jaringan: ${t.message}")
                displayTransactions(getPageItems()) // Tampilkan kembali data lama jika gagal
            }
        })
    }

    // --- Export ---

    private fun exportReceipts() {
        // UBAH: Logika disederhanakan, hanya memanggil API
        showToast("Mulai mengunduh...")
        apiService.exportReceipts().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    response.body()?.let {
                        val fileName = "receipts_export_${System.currentTimeMillis()}.pdf"
                        // Panggil router penyimpanan file yang baru
                        savePdfFile(it, fileName)
                    } ?: showToast("Gagal mengunduh: Respons kosong")
                } else {
                    showToast("Gagal mengunduh PDF: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (!isAdded) return
                showToast("Error jaringan: ${t.message}")
            }
        })
    }

    private fun savePdfFile(body: ResponseBody, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Metode modern untuk Android 10+
            savePdfUsingMediaStore(body, fileName)
        } else {
            // Metode lama untuk Android 9 ke bawah
            if (checkAndRequestStoragePermission()) {
                savePdfLegacy(body, fileName)
            } else {
                // Simpan aksi download, akan dijalankan jika user memberikan izin
                pendingDownload = { savePdfLegacy(body, fileName) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun savePdfUsingMediaStore(body: ResponseBody, fileName: String) {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    outputStream?.write(body.bytes())
                    showToast("PDF disimpan di Download/$fileName", Toast.LENGTH_LONG)
                }
            } catch (e: IOException) {
                showToast("Gagal menyimpan PDF: ${e.message}")
            }
        }
    }

    private fun savePdfLegacy(body: ResponseBody, fileName: String) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        val file = File(downloadDir, fileName)

        try {
            FileOutputStream(file).use { it.write(body.bytes()) }
            showToast("PDF disimpan di Download/$fileName", Toast.LENGTH_LONG)
        } catch (e: IOException) {
            showToast("Gagal menyimpan PDF: ${e.message}")
        }
    }

    // --- Permission Handling ---

    private fun checkAndRequestStoragePermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            false
        }
    }

    // --- Formatting ---

    private fun formatWibDateTime(utcDateStr: String): String {
        return try {
            val utcDateTime = LocalDateTime.parse(utcDateStr, DateTimeFormatter.ISO_DATE_TIME)
            val wibDateTime = utcDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("Asia/Jakarta")).toLocalDateTime()
            wibDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun formatPrice(price: Int): String {
        return java.text.NumberFormat.getInstance(Locale("id", "ID")).format(price)
    }

    // --- Utility ---

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (isAdded) { // Cek sebelum menampilkan Toast
            Toast.makeText(context, message, duration).show()
        }
    }
}