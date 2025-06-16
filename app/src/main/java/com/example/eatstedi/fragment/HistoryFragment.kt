package com.example.eatstedi.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
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

    // Variabel data
    private var allAttendanceRecords: List<AttendanceRecord> = emptyList()
    private var currentDisplayedData: List<AttendanceRecord> = emptyList()

    // Variabel Paginasi
    private val itemsPerPage = 20
    private var currentPage = 1
    private var totalPages = 1

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
        setupPagination() // Panggil fungsi setup paginasi

        fetchAllAttendance()
    }

    // --- FUNGSI PAGINASI BARU ---

    private fun setupPagination() {
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

    private fun updatePageDisplay() {
        _binding?.let {
            it.tvPageInfo.text = "Halaman $currentPage / $totalPages"
            it.btnPrevPage.isEnabled = currentPage > 1
            it.btnNextPage.isEnabled = currentPage < totalPages
            it.btnPrevPage.alpha = if (currentPage > 1) 1.0f else 0.5f
            it.btnNextPage.alpha = if (currentPage < totalPages) 1.0f else 0.5f
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

    private fun fetchAllAttendance() {
        apiService.getAllAttendance().enqueue(object : Callback<AttendanceApiResponse> {
            override fun onResponse(call: Call<AttendanceApiResponse>, response: Response<AttendanceApiResponse>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    allAttendanceRecords = response.body()?.data ?: emptyList()
                    filterAndDisplayData()
                } else {
                    handleApiError(response, "Failed to fetch attendance data")
                }
            }
            override fun onFailure(call: Call<AttendanceApiResponse>, t: Throwable) {
                handleApiFailure(t)
            }
        })
    }

    private fun fetchAttendanceByDate() {
        if (startDate != null && endDate != null) {
            val request = DateFilterRequest(
                date_awal = apiDateFormat.format(startDate!!),
                date_akhir = apiDateFormat.format(endDate!!)
            )
            apiService.filterByDateAttendance(request).enqueue(object : Callback<AttendanceApiResponse> {
                override fun onResponse(call: Call<AttendanceApiResponse>, response: Response<AttendanceApiResponse>) {
                    if (!isAdded || _binding == null) return
                    if (response.isSuccessful && response.body()?.success == true) {
                        allAttendanceRecords = response.body()?.data ?: emptyList()
                        filterAndDisplayData()
                    } else {
                        handleApiError(response, "Failed to fetch filtered attendance")
                    }
                }
                override fun onFailure(call: Call<AttendanceApiResponse>, t: Throwable) {
                    handleApiFailure(t)
                }
            })
        }
    }

    // --- FUNGSI TAMPILAN DAN LOGIKA ---

    private fun filterAndDisplayData() {
        currentDisplayedData = when (activeFilter) {
            "Hadir" -> allAttendanceRecords.filter { it.attendance == 1 }
            "Tidak Hadir" -> allAttendanceRecords.filter { it.attendance == 0 }
            else -> allAttendanceRecords
        }

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
            val total = records.size
            val present = records.count { it.attendance == 1 }
            val absent = records.count { it.attendance == 0 }

            binding.tvTotal.text = total.toString()
            binding.tvPresent.text = present.toString()
            binding.tvAbsent.text = absent.toString()
        }
    }

    private fun displayData(paginatedRecords: List<AttendanceRecord>) {
        _binding?.let { binding ->
            binding.attendanceTableView.removeAllViews()

            if (currentDisplayedData.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.attendanceTableView.visibility = View.GONE
                binding.paginationLayout.visibility = View.GONE
                return@let
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.attendanceTableView.visibility = View.VISIBLE
                binding.paginationLayout.visibility = View.VISIBLE
            }

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
                    setBackgroundColor(
                        if (index % 2 == 0) ContextCompat.getColor(requireContext(), R.color.white)
                        else ContextCompat.getColor(requireContext(), R.color.secondary)
                    )
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
            textSize = if (isHeader) 18f else 16f
            setPadding(8, 8, 8, 8)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    // --- FUNGSI SETUP UI DAN HELPER ---

    private fun setupFilters() {
        binding.llSummaryTotal.setOnClickListener { updateActiveFilter("Total") }
        binding.llSummaryPresent.setOnClickListener { updateActiveFilter("Hadir") }
        binding.llSummaryAbsent.setOnClickListener { updateActiveFilter("Tidak Hadir") }
    }

    private fun updateActiveFilter(filter: String) {
        if (activeFilter == filter && startDate == null) return

        activeFilter = filter

        _binding?.let { binding ->
            updateFilterUI(binding.llSummaryTotal, filter == "Total", binding.tvTotal, binding.tvTotalLabel)
            updateFilterUI(binding.llSummaryPresent, filter == "Hadir", binding.tvPresent, binding.tvPresentLabel)
            updateFilterUI(binding.llSummaryAbsent, filter == "Tidak Hadir", binding.tvAbsent, binding.tvAbsentLabel)
        }

        filterAndDisplayData()
    }

    private fun updateFilterUI(layout: View, isActive: Boolean, valueTextView: TextView, labelTextView: TextView) {
        _binding?.let {
            val backgroundColor = if (isActive) R.color.red else R.color.secondary
            val textColor = if (isActive) R.color.white else R.color.black
            val drawable = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(ContextCompat.getColor(requireContext(), backgroundColor))
            }
            layout.background = drawable
            valueTextView.setTextColor(ContextCompat.getColor(requireContext(), textColor))
            labelTextView.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        }
    }

    private fun setupDatePickers() {
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

    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.time
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

    private fun setupClearSearchButton() {
        binding.ivClearSearch.setOnClickListener {
            startDate = null
            endDate = null
            binding.btnStartDate.text = "dd/mm/yyyy"
            binding.btnEndDate.text = "dd/mm/yyyy"
            binding.ivClearSearch.visibility = View.GONE

            updateActiveFilter("Total")
            fetchAllAttendance()
        }
    }

    private fun setupExportButton() {
        binding.ivDownload.setOnClickListener {
            exportAttendance()
        }
    }

    private fun exportAttendance() {
        apiService.exportAttendance().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        try {
                            val fileName = "attendance_${System.currentTimeMillis()}.pdf"
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val file = File(downloadsDir, fileName)

                            FileOutputStream(file).use { outputStream ->
                                outputStream.write(body.bytes())
                            }

                            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            intent.data = android.net.Uri.fromFile(file)
                            requireContext().sendBroadcast(intent)

                            Toast.makeText(requireContext(), "PDF berhasil disimpan di Download/$fileName", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Log.e("HistoryFragment", "Error saving PDF", e)
                            Toast.makeText(context, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    handleApiError(response, "Gagal mengunduh PDF")
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                handleApiFailure(t)
            }
        })
    }

    private fun handleApiError(response: Response<*>, defaultMessage: String) {
        if (!isAdded) return
        Toast.makeText(context, defaultMessage, Toast.LENGTH_SHORT).show()
        Log.e("HistoryFragment", "API Error: ${response.code()} - ${response.errorBody()?.string()}")
    }

    private fun handleApiFailure(t: Throwable) {
        if (!isAdded) return
        Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
        Log.e("HistoryFragment", "Network Failure", t)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}