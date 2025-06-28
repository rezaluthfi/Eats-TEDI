package com.example.eatstedi.fragment

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.ApiService
import com.example.eatstedi.api.service.AttendanceApiResponse
import com.example.eatstedi.api.service.AttendanceRecord
import com.example.eatstedi.api.service.DateFilterRequest
import com.example.eatstedi.databinding.FragmentHistoryBinding
import com.example.eatstedi.login.LoginActivity
import com.facebook.shimmer.ShimmerFrameLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    // Variabel filter
    private var activeFilter: String = "Total"
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var apiService: ApiService

    // Dua variabel ini untuk menangani izin dan download
    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private var pendingDownload: (() -> Unit)? = null

    // Variabel data
    private var allAttendanceRecords: List<AttendanceRecord> = emptyList()
    private var currentDisplayedData: List<AttendanceRecord> = emptyList()

    // Variabel paginasi
    private val itemsPerPage = 20
    private var currentPage = 1
    private var totalPages = 1

    // Variabel peran pengguna
    private var userRole: String? = null
    private var cashierId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        userRole = sharedPreferences.getString("user_role", null)
        val id = sharedPreferences.getInt("user_id", -1)
        cashierId = if (id == -1) null else id

        // Inisialisasi ActivityResultLauncher untuk meminta izin
        requestPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Izin penyimpanan diberikan.", Toast.LENGTH_SHORT).show()
                // Jalankan download yang tertunda setelah izin diberikan
                pendingDownload?.invoke()
                pendingDownload = null
            } else {
                Toast.makeText(requireContext(), "Izin penyimpanan ditolak. Tidak dapat mengunduh file.", Toast.LENGTH_SHORT).show()
            }
        }

        // Validasi role dan ID
        if (userRole.isNullOrEmpty() || (userRole == "cashier" && cashierId == null)) {
            redirectToLogin()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiService = RetrofitClient.getInstance(requireContext())
        setupFilters()
        setupDatePickers()
        setupClearSearchButton()
        setupExportButton()
        setupPagination()

        updateActiveFilter("Total")
        fetchAttendance()
    }

    // --- FUNGSI SHIMMER ---

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

    // --- FUNGSI PAGINASI ---

    private fun setupPagination() {
        _binding?.let { binding ->
            binding.btnPrevPage.setOnClickListener {
                if (currentPage > 1) {
                    currentPage--
                    displayDataForCurrentPage()
                }
            }
            binding.btnNextPage.setOnClickListener {
                if (currentPage < totalPages) {
                    currentPage++
                    displayDataForCurrentPage()
                }
            }
        }
    }

    private fun updatePageDisplay() {
        _binding?.let {
            it.tvPageInfo.text = "Halaman $currentPage / $totalPages"
            it.btnPrevPage.isEnabled = currentPage > 1
            it.btnNextPage.isEnabled = currentPage < totalPages
            it.btnPrevPage.alpha = if (it.btnPrevPage.isEnabled) 1.0f else 0.5f
            it.btnNextPage.alpha = if (it.btnNextPage.isEnabled) 1.0f else 0.5f
        }
    }

    private fun getPageItems(data: List<AttendanceRecord>): List<AttendanceRecord> {
        val startIndex = (currentPage - 1) * itemsPerPage
        if (startIndex >= data.size) {
            return emptyList()
        }
        val endIndex = minOf(startIndex + itemsPerPage, data.size)
        return data.subList(startIndex, endIndex)
    }

    // --- FUNGSI FETCH DATA ---

    private fun fetchAttendance() {
        showShimmer()
        when (userRole) {
            "admin" -> apiService.getAllAttendance().enqueue(createAttendanceCallback())
            "cashier" -> {
                cashierId?.let { id ->
                    apiService.getAttendanceByCashier(id).enqueue(createAttendanceCallback())
                } ?: redirectToLogin() // Jika ID kasir null, kembali ke login
            }
            else -> {
                hideShimmer()
                Toast.makeText(context, "Role tidak valid.", Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }
        }
    }

    private fun fetchAttendanceByDate() {
        val safeStartDate = startDate
        val safeEndDate = endDate
        if (safeStartDate == null || safeEndDate == null) {
            Toast.makeText(context, "Pilih tanggal mulai dan selesai", Toast.LENGTH_SHORT).show()
            return
        }
        showShimmer()
        val request = DateFilterRequest(
            date_awal = apiDateFormat.format(safeStartDate),
            date_akhir = apiDateFormat.format(safeEndDate)
        )
        when (userRole) {
            "admin" -> apiService.filterByDateAttendance(request).enqueue(createAttendanceCallback())
            "cashier" -> {
                cashierId?.let { id ->
                    apiService.filterAttendanceByDateCashier(id, request).enqueue(createAttendanceCallback())
                } ?: redirectToLogin()
            }
        }
    }

    private fun fetchAttendanceByStatus() {
        showShimmer()
        val status = if (activeFilter == "Hadir") 1 else 0
        when (userRole) {
            "admin" -> apiService.getAttendanceByAbsent(status).enqueue(createAttendanceCallback())
            "cashier" -> {
                cashierId?.let { id ->
                    apiService.getAttendanceByAbsentCashier(id, status == 1).enqueue(createAttendanceCallback())
                } ?: redirectToLogin()
            }
        }
    }

    // Callback terpusat untuk response data kehadiran
    private fun createAttendanceCallback(): Callback<AttendanceApiResponse> {
        return object : Callback<AttendanceApiResponse> {
            override fun onResponse(call: Call<AttendanceApiResponse>, response: Response<AttendanceApiResponse>) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    allAttendanceRecords = body.data ?: emptyList()
                    filterAndDisplayData()
                } else {
                    handleApiError(response, "Gagal mengambil data: ${body?.activity}")
                    filterAndDisplayData(emptyList())
                }
            }

            override fun onFailure(call: Call<AttendanceApiResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return // Guard
                hideShimmer()
                handleApiFailure(t)
                filterAndDisplayData(emptyList())
            }
        }
    }

    private fun exportAttendance() {
        // Mulai dengan validasi izin penyimpanan
        Toast.makeText(requireContext(), "Mulai mengunduh...", Toast.LENGTH_SHORT).show()

        val callback = object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val fileName = "attendance_${userRole}_${System.currentTimeMillis()}.pdf"
                    // Panggil router penyimpanan file yang baru
                    response.body()?.let { savePdfFile(it, fileName) }
                } else {
                    handleApiError(response, "Gagal mengunduh PDF")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (!isAdded) return
                handleApiFailure(t)
            }
        }

        when (userRole) {
            "admin" -> apiService.exportAttendance().enqueue(callback)
            "cashier" -> cashierId?.let { apiService.exportAttendanceCashier(it).enqueue(callback) } ?: redirectToLogin()
        }
    }

    private fun savePdfFile(body: ResponseBody, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Metode modern untuk Android 10+
            savePdfUsingMediaStore(body, fileName)
        } else {
            // Metode lama untuk Android 9 ke bawah (dengan permintaan izin)
            if (checkAndRequestStoragePermission()) {
                savePdfLegacy(body, fileName)
            } else {
                // Simpan aksi download, akan dijalankan jika user memberikan izin
                pendingDownload = { savePdfLegacy(body, fileName) }
            }
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
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
                    Toast.makeText(requireContext(), "File disimpan di Downloads", Toast.LENGTH_LONG).show()
                }
            } catch (e: java.io.IOException) {
                Log.e("HistoryFragment", "Gagal menyimpan PDF dengan MediaStore", e)
                Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "File disimpan di Downloads", Toast.LENGTH_LONG).show()
        } catch (e: java.io.IOException) {
            Log.e("HistoryFragment", "Gagal menyimpan PDF (legacy)", e)
            Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestStoragePermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            false
        }
    }

    // --- FUNGSI TAMPILAN DAN LOGIKA ---

    private fun filterAndDisplayData(data: List<AttendanceRecord>? = null) {
        if (data != null) {
            allAttendanceRecords = data
        }
        currentDisplayedData = allAttendanceRecords
        updateFilteredCounts(allAttendanceRecords)

        currentPage = 1
        totalPages = if (currentDisplayedData.isEmpty()) 1 else (currentDisplayedData.size + itemsPerPage - 1) / itemsPerPage
        displayDataForCurrentPage()
    }

    private fun displayDataForCurrentPage() {
        val pageItems = getPageItems(currentDisplayedData)
        displayData(pageItems)
        updatePageDisplay()
    }

    private fun updateFilteredCounts(records: List<AttendanceRecord>) {
        _binding?.let { binding ->
            binding.tvTotal.text = records.size.toString()
            binding.tvPresent.text = records.count { it.attendance == 1 }.toString()
            binding.tvAbsent.text = records.count { it.attendance == 0 }.toString()
        }
    }

    private fun displayData(paginatedRecords: List<AttendanceRecord>) {
        if (!isAdded) return // Cek sebelum akses context
        _binding?.let { binding ->
            binding.attendanceTableView.removeAllViews()

            if (currentDisplayedData.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.attendanceTableView.visibility = View.GONE
                binding.paginationLayout.visibility = View.GONE
                return@let
            }

            binding.tvNoData.visibility = View.GONE
            binding.attendanceTableView.visibility = View.VISIBLE
            binding.paginationLayout.visibility = View.VISIBLE

            val headerRow = TableRow(context).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                addView(createTextView("Nama Karyawan", isHeader = true))
                addView(createTextView("Tanggal", isHeader = true))
                addView(createTextView("Shift", isHeader = true))
                addView(createTextView("Waktu", isHeader = true))
                addView(createTextView("Kehadiran", isHeader = true))
            }
            binding.attendanceTableView.addView(headerRow)

            for ((index, record) in paginatedRecords.withIndex()) {
                val row = TableRow(context).apply {
                    setPadding(8, 8, 8, 8)
                    val bgColor = if (index % 2 == 0) R.color.white else R.color.secondary
                    setBackgroundColor(ContextCompat.getColor(requireContext(), bgColor))
                }

                val shiftInfo = mapShiftIdToTime(record.id_shift)
                val attendanceStatus = if (record.attendance == 1) "Hadir" else "Tidak Hadir"
                val formattedDate = try {
                    apiDateFormat.parse(record.date)?.let { dateFormatDisplay.format(it) } ?: record.date
                } catch (e: Exception) { record.date }

                row.addView(createTextView(record.name))
                row.addView(createTextView(formattedDate))
                row.addView(createTextView(shiftInfo.first))
                row.addView(createTextView(shiftInfo.second))
                row.addView(createTextView(attendanceStatus))

                binding.attendanceTableView.addView(row)
            }
        }
    }

    private fun mapShiftIdToTime(shiftId: Int): Pair<String, String> {
        return when (shiftId) {
            1 -> "Shift 1" to "07:00-09:00"
            2 -> "Shift 2" to "09:00-12:00"
            3 -> "Shift 3" to "12:00-14:00"
            4 -> "Shift 4" to "14:00-16:00"
            else -> "Unknown" to "N/A"
        }
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = if (isHeader) 16f else 14f
            setPadding(12, 12, 12, 12)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    // --- FUNGSI SETUP UI DAN HELPER ---

    private fun setupFilters() {
        _binding?.let { binding ->
            binding.llSummaryTotal.setOnClickListener { updateActiveFilter("Total") }
            binding.llSummaryPresent.setOnClickListener { updateActiveFilter("Hadir") }
            binding.llSummaryAbsent.setOnClickListener { updateActiveFilter("Tidak Hadir") }
        }
    }

    private fun updateActiveFilter(filter: String) {
        activeFilter = filter
        _binding?.let { binding ->
            updateFilterUI(binding.llSummaryTotal, filter == "Total", binding.tvTotal, binding.tvTotalLabel)
            updateFilterUI(binding.llSummaryPresent, filter == "Hadir", binding.tvPresent, binding.tvPresentLabel)
            updateFilterUI(binding.llSummaryAbsent, filter == "Tidak Hadir", binding.tvAbsent, binding.tvAbsentLabel)
        }

        if (filter == "Total") {
            fetchAttendance()
        } else {
            fetchAttendanceByStatus()
        }
    }

    private fun updateFilterUI(layout: View, isActive: Boolean, valueTextView: TextView, labelTextView: TextView) {
        if (!isAdded) return
        _binding?.let {
            val backgroundColor = if (isActive) R.color.red else R.color.secondary
            val textColor = if (isActive) R.color.white else R.color.black
            val drawable = GradientDrawable().apply {
                cornerRadius = 16f // Disesuaikan agar lebih rounded
                setColor(ContextCompat.getColor(requireContext(), backgroundColor))
            }
            layout.background = drawable
            valueTextView.setTextColor(ContextCompat.getColor(requireContext(), textColor))
            labelTextView.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        }
    }

    private fun setupDatePickers() {
        _binding?.let { binding ->
            binding.btnStartDate.setOnClickListener {
                showDatePicker(isStartDate = true) { date ->
                    startDate = date
                    binding.btnStartDate.text = dateFormatDisplay.format(date)
                    binding.ivClearSearch.visibility = View.VISIBLE
                    if (endDate != null) fetchAttendanceByDate()
                }
            }
            binding.btnEndDate.setOnClickListener {
                showDatePicker(isStartDate = false) { date ->
                    endDate = date
                    binding.btnEndDate.text = dateFormatDisplay.format(date)
                    binding.ivClearSearch.visibility = View.VISIBLE
                    if (startDate != null) fetchAttendanceByDate()
                }
            }
        }
    }

    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Date) -> Unit) {
        if (!isAdded) return
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.time
                if (isStartDate) {
                    if (endDate != null && selectedDate.after(endDate)) {
                        Toast.makeText(context, "Tanggal mulai tidak boleh setelah tanggal selesai", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                } else {
                    if (startDate != null && selectedDate.before(startDate)) {
                        Toast.makeText(context, "Tanggal selesai tidak boleh sebelum tanggal mulai", Toast.LENGTH_SHORT).show()
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

    private fun setupClearSearchButton() {
        _binding?.ivClearSearch?.setOnClickListener {
            startDate = null
            endDate = null
            _binding?.btnStartDate?.text = "dd/mm/yyyy"
            _binding?.btnEndDate?.text = "dd/mm/yyyy"
            it.visibility = View.GONE
            updateActiveFilter("Total")
        }
    }

    private fun setupExportButton() {
        _binding?.ivDownload?.setOnClickListener { exportAttendance() }
    }

    private fun handleApiError(response: Response<*>, defaultMessage: String) {
        if (!isAdded) return
        Toast.makeText(context, defaultMessage, Toast.LENGTH_SHORT).show()
        Log.e("HistoryFragment", "API Error: ${response.code()} - ${response.errorBody()?.string()}")
    }

    private fun handleApiFailure(t: Throwable) {
        if (!isAdded) return
        Toast.makeText(context, "Error Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
        Log.e("HistoryFragment", "Network Failure", t)
    }

    private fun redirectToLogin() {
        if (!isAdded) return
        val intent = Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}