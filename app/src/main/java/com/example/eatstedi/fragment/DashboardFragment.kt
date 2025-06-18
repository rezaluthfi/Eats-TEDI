package com.example.eatstedi.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.activity.AllEmployeeActivity
import com.example.eatstedi.activity.AllSupplierActivity
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.*
import com.example.eatstedi.databinding.FragmentDashboardBinding
import com.example.eatstedi.databinding.ViewItemEmployeeBinding
import com.example.eatstedi.databinding.ViewItemSupplierBinding
import com.example.eatstedi.model.*
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Data API
    private val cashiers = mutableListOf<Employee>()
    private val schedules = mutableListOf<Schedule>()
    private var dailyStatsForAdmin = mutableListOf<DailyStatistics>()
    private val weeklyStats = mutableMapOf<String, List<DailyStatistics>>()
    private val monthlyStats = mutableMapOf<String, List<DailyStatistics>>()

    // Data Pengguna
    private var userRole: String? = null
    private var userId: Int = -1

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        userRole = sharedPrefs.getString("user_role", null)
        userId = sharedPrefs.getInt("user_id", -1)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Izin penyimpanan diberikan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUiForRole()
        setupClickListeners()
        fetchDataForRole()
    }

    private fun setupUiForRole() {
        _binding?.let { binding ->
            if (userRole == "cashier") {
                binding.llEmployee.visibility = View.GONE
                binding.llSupplier.visibility = View.GONE
                binding.tvPayments.visibility = View.VISIBLE
                binding.rlPayments.visibility = View.VISIBLE
                binding.tvStatistics.visibility = View.GONE
                binding.llStatisticsDailyWeekly.visibility = View.GONE
                binding.llStatisticsMonthly.visibility = View.GONE
                binding.tvScheduleTitle.text = "Jadwal Saya"
            } else { // Admin
                binding.llEmployee.visibility = View.VISIBLE
                binding.llSupplier.visibility = View.VISIBLE
                binding.tvPayments.visibility = View.VISIBLE
                binding.rlPayments.visibility = View.VISIBLE
                binding.tvStatistics.visibility = View.VISIBLE
                binding.llStatisticsDailyWeekly.visibility = View.VISIBLE
                binding.llStatisticsMonthly.visibility = View.VISIBLE
                binding.tvScheduleTitle.text = "Jadwal Karyawan"
            }
        }
    }

    private fun setupClickListeners() {
        _binding?.let { binding ->
            binding.tvViewAllEmployee.setOnClickListener {
                startActivity(Intent(requireContext(), AllEmployeeActivity::class.java))
            }
            binding.tvViewAllSupplier.setOnClickListener {
                startActivity(Intent(requireContext(), AllSupplierActivity::class.java))
            }
            binding.tvDownloadDaily.setOnClickListener { handleDownload("daily") }
            binding.tvDownloadWeekly.setOnClickListener { handleDownload("weekly") }
            binding.tvDownloadMonthly.setOnClickListener { handleDownload("monthly") }
        }
    }

    private fun fetchDataForRole() {
        fetchSchedulesForRole()
        if (userRole == "admin") {
            fetchCashiers()
            fetchSuppliers()
            // Panggil data rekap mingguan KHUSUS untuk Pie Chart
            fetchWeeklyRecapForAdminPieChart()
            // Panggil data statistik untuk Horizontal Bar Charts
            fetchDailyStatisticsForAdmin()
            fetchWeeklyStatistics()
            fetchMonthlyStatistics()
        } else if (userRole == "cashier" && userId != -1) {
            fetchCashierPaymentRecap(userId)
        }
    }

    /**
     * Fungsi baru untuk mengambil rekap pembayaran mingguan
     * dan menggunakannya untuk mengisi Pie Chart Admin.
     */
    private fun fetchWeeklyRecapForAdminPieChart() {
        val apiService = RetrofitClient.getInstance(requireContext())
        // Pastikan endpoint ini sudah ditambahkan di ApiService.kt
        apiService.getWeeklyPaymentRecap().enqueue(object : Callback<CashierPaymentRecapResponse> {
            override fun onResponse(call: Call<CashierPaymentRecapResponse>, response: Response<CashierPaymentRecapResponse>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful && response.body()?.success == true) {
                    val weeklyRecapData = response.body()!!.data

                    val cashCount = weeklyRecapData.count { it.payment_type.lowercase() == "cash" }.toFloat()
                    val qrisCount = weeklyRecapData.count { it.payment_type.lowercase() == "qris" }.toFloat()

                    Log.d("DashboardFragment", "Weekly Recap -> Cash: $cashCount, QRIS: $qrisCount")
                    // Panggil fungsi setup pie chart dengan data yang sudah dihitung
                    setupPieChartForAdmin(cashCount, qrisCount)
                } else {
                    handleApiError(response, "Gagal mengambil rekap mingguan")
                    // Tampilkan chart kosong jika gagal
                    setupPieChartForAdmin(0f, 0f)
                }
            }

            override fun onFailure(call: Call<CashierPaymentRecapResponse>, t: Throwable) {
                if (!isAdded) return
                handleApiFailure(t)
                // Tampilkan chart kosong jika gagal
                setupPieChartForAdmin(0f, 0f)
            }
        })
    }

    private fun fetchSchedulesForRole() {
        val apiService = RetrofitClient.getInstance(requireContext())
        val call: Call<ScheduleResponse> = if (userRole == "admin") {
            apiService.getSchedules()
        } else if (userRole == "cashier" && userId != -1) {
            apiService.getScheduleByIdCashier(userId)
        } else {
            return
        }

        call.enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    schedules.clear()
                    schedules.addAll(response.body()!!.data)
                    updateScheduleTableForRole()
                } else {
                    handleApiError(response, "Gagal mengambil jadwal")
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                handleApiFailure(t)
            }
        })
    }

    private fun fetchCashierPaymentRecap(cashierId: Int) {
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getCashierPaymentRecap(cashierId).enqueue(object : Callback<CashierPaymentRecapResponse> {
            override fun onResponse(call: Call<CashierPaymentRecapResponse>, response: Response<CashierPaymentRecapResponse>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful && response.body()?.success == true) {
                    val paymentDataList = response.body()!!.data
                    val cashCount = paymentDataList.count { it.payment_type.lowercase() == "cash" }.toFloat()
                    val qrisCount = paymentDataList.count { it.payment_type.lowercase() == "qris" }.toFloat()

                    Log.d("DashboardFragment", "Cashier Recap -> Cash: $cashCount, QRIS: $qrisCount")
                    setupPieChartForCashier(cashCount, qrisCount)

                } else {
                    Log.e("DashboardFragment", "Response not successful or body is null")
                    handleApiError(response, "Gagal mengambil rekap pembayaran")
                    setupPieChartForCashier(0f, 0f)
                }
            }

            override fun onFailure(call: Call<CashierPaymentRecapResponse>, t: Throwable) {
                Log.e("DashboardFragment", "API call failed", t)
                handleApiFailure(t)
                setupPieChartForCashier(0f, 0f)
            }
        })
    }

    private fun fetchCashiers() {
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getCashiers().enqueue(object : Callback<CashierResponse> {
            override fun onResponse(call: Call<CashierResponse>, response: Response<CashierResponse>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    val cashierList = response.body()!!.data
                    cashiers.clear()
                    cashiers.addAll(cashierList)
                    val employeeList = cashierList.map { EmployeePreview(it.name, it.profile_picture) }
                    addEmployeeProfiles(employeeList)
                    updateScheduleTableForRole()
                } else {
                    handleApiError(response, "Gagal mengambil data karyawan")
                }
            }
            override fun onFailure(call: Call<CashierResponse>, t: Throwable) {
                handleApiFailure(t)
            }
        })
    }

    private fun fetchSuppliers() {
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getSuppliers().enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    val supplierList = response.body()!!.data
                    val supplierPreviewList = supplierList.map { SupplierPreview(it.name, it.profile_picture) }
                    addSupplierProfiles(supplierPreviewList)
                } else {
                    handleApiError(response, "Gagal mengambil data pemasok")
                }
            }
            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                handleApiFailure(t)
            }
        })
    }

    private fun fetchDailyStatisticsForAdmin() {
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getDailyStatistics().enqueue(object : Callback<DailyStatisticsResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<DailyStatisticsResponse>, response: Response<DailyStatisticsResponse>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    dailyStatsForAdmin.clear()
                    dailyStatsForAdmin.addAll(response.body()!!.data)
                    // Panggilan ke Pie Chart DIHAPUS dari sini
                    // setupPieChartForAdmin() // <-- DIHAPUS
                    setupHorizontalBarCharts() // <-- Biarkan ini untuk mengisi bar chart harian
                } else {
                    handleApiError(response, "Gagal mengambil statistik harian")
                }
            }
            override fun onFailure(call: Call<DailyStatisticsResponse>, t: Throwable) {
                handleApiFailure(t)
            }
        })
    }

    private fun fetchWeeklyStatistics() {
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getWeeklyStatistics().enqueue(object : Callback<WeeklyStatisticsResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<WeeklyStatisticsResponse>, response: Response<WeeklyStatisticsResponse>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    weeklyStats.clear()
                    weeklyStats.putAll(response.body()!!.data)
                    setupHorizontalBarCharts()
                }
            }
            override fun onFailure(call: Call<WeeklyStatisticsResponse>, t: Throwable) { handleApiFailure(t) }
        })
    }

    private fun fetchMonthlyStatistics() {
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getMonthlyStatistics().enqueue(object : Callback<MonthlyStatisticsResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<MonthlyStatisticsResponse>, response: Response<MonthlyStatisticsResponse>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    monthlyStats.clear()
                    monthlyStats.putAll(response.body()!!.data)
                    setupHorizontalBarCharts()
                }
            }
            override fun onFailure(call: Call<MonthlyStatisticsResponse>, t: Throwable) { handleApiFailure(t) }
        })
    }

    private fun updateScheduleTableForRole() {
        val shiftTimeMap = mapOf(1 to "07:00-09:00", 2 to "09:00-12:00", 3 to "12:00-14:00", 4 to "14:00-16:00")
        val scheduleList = if (userRole == "admin") {
            schedules.mapNotNull { schedule ->
                cashiers.find { it.id == schedule.id_cashiers }?.let { cashier ->
                    EmployeeSchedule(
                        name = cashier.name,
                        day = schedule.day.capitalizeFirst(),
                        shift = "Shift ${schedule.id_shifts}",
                        time = shiftTimeMap[schedule.id_shifts] ?: "N/A"
                    )
                }
            }
        } else {
            val cashierName = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("user_name", "Saya") ?: "Saya"
            schedules.map { schedule ->
                EmployeeSchedule(
                    name = cashierName,
                    day = schedule.day.capitalizeFirst(),
                    shift = "Shift ${schedule.id_shifts}",
                    time = shiftTimeMap[schedule.id_shifts] ?: "N/A"
                )
            }
        }
        addScheduleToTable(scheduleList)
    }

    private fun addScheduleToTable(scheduleList: List<EmployeeSchedule>) {
        _binding?.let { binding ->
            val tableLayout = binding.tableView
            tableLayout.removeAllViews()
            val headerRow = TableRow(requireContext()).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                if (userRole == "admin") {
                    addView(createTextView("Nama Karyawan", isHeader = true))
                }
                addView(createTextView("Hari", isHeader = true))
                addView(createTextView("Shift", isHeader = true))
                addView(createTextView("Waktu", isHeader = true))
            }
            tableLayout.addView(headerRow)
            for (schedule in scheduleList) {
                val row = TableRow(requireContext()).apply { setPadding(8, 16, 8, 16) }
                if (userRole == "admin") {
                    row.addView(createTextView(schedule.name))
                }
                row.addView(createTextView(schedule.day))
                row.addView(createTextView(schedule.shift))
                row.addView(createTextView(schedule.time))
                tableLayout.addView(row)
            }
        }
    }

    private fun addEmployeeProfiles(employeeList: List<EmployeePreview>) {
        _binding?.let { binding ->
            val container = binding.employeeContainer
            container.removeAllViews()
            employeeList.forEach { employee ->
                val itemBinding = ViewItemEmployeeBinding.inflate(layoutInflater, container, false)
                Glide.with(this).load(employee.imageUrl).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).circleCrop().into(itemBinding.ivEmployee)
                itemBinding.tvEmployee.text = employee.name
                container.addView(itemBinding.root)
            }
        }
    }

    private fun addSupplierProfiles(supplierList: List<SupplierPreview>) {
        _binding?.let { binding ->
            val container = binding.supplierContainer
            container.removeAllViews()
            supplierList.forEach { supplier ->
                val itemBinding = ViewItemSupplierBinding.inflate(layoutInflater, container, false)
                Glide.with(this).load(supplier.imageUrl).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).circleCrop().into(itemBinding.ivSupplier)
                itemBinding.tvSupplier.text = supplier.name
                container.addView(itemBinding.root)
            }
        }
    }

    private fun setupPieChartForAdmin(totalCashTransactions: Float, totalQRISTransactions: Float) {
        _binding?.let { binding ->
            val pieChart = binding.piechartPayments
            val totalTransactions = totalCashTransactions + totalQRISTransactions

            if (totalTransactions == 0f) {
                // ... (Logika untuk menampilkan "Tidak ada data" tetap sama)
                setupEmptyPieChart(pieChart)
                return@let
            }

            val cashPercentage = (totalCashTransactions / totalTransactions) * 100
            val qrisPercentage = (totalQRISTransactions / totalTransactions) * 100

            val entries = listOf(
                PieEntry(totalCashTransactions, "Tunai"),
                PieEntry(totalQRISTransactions, "QRIS")
            )

            val dataSet = PieDataSet(entries, "Metode Pembayaran").apply {
                colors = listOf(Color.parseColor("#FFB74D"), Color.parseColor("#64B5F6"))
                sliceSpace = 5f
                selectionShift = 10f
                valueTextColor = Color.BLACK
                valueTextSize = 14f
                setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
                setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
                valueLineColor = Color.BLACK
                valueLineWidth = 1.5f
                valueLinePart1Length = 0.6f
                valueLinePart2Length = 0.8f
            }

            val data = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Perbaikan logika agar label persen benar
                        return if (value == totalCashTransactions) {
                            "Tunai: ${value.toInt()} (${String.format("%.1f", cashPercentage)}%)"
                        } else {
                            "QRIS: ${value.toInt()} (${String.format("%.1f", qrisPercentage)}%)"
                        }
                    }
                })
            }

            pieChart.apply {
                this.data = data
                // ... (Konfigurasi PieChart tetap sama)
                description.isEnabled = false
                setDrawHoleEnabled(true)
                setHoleColor(Color.WHITE)
                setTransparentCircleRadius(80f)
                setHoleRadius(20f)
                setUsePercentValues(false)
                legend.isEnabled = true
                legend.textColor = Color.BLACK
                legend.textSize = 12f
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.yOffset = 12f
                legend.formSize = 10f
                legend.formToTextSpace = 10f
                legend.xEntrySpace = 15f
                setExtraOffsets(8f, 8f, 8f, 20f)
                invalidate()
                animateY(1000)
            }
        }
    }

    private fun setupPieChartForCashier(cashCount: Float, qrisCount: Float) {
        _binding?.let { binding ->
            val pieChart = binding.piechartPayments
            val totalTransactions = cashCount + qrisCount

            if (totalTransactions == 0f) {
                setupEmptyPieChart(pieChart)
                return@let
            }

            val cashPercentage = (cashCount / totalTransactions) * 100
            val qrisPercentage = (qrisCount / totalTransactions) * 100

            val entries = mutableListOf<PieEntry>()
            if (cashCount > 0) entries.add(PieEntry(cashCount, "Tunai"))
            if (qrisCount > 0) entries.add(PieEntry(qrisCount, "QRIS"))

            val dataSet = PieDataSet(entries, "Metode Pembayaran").apply {
                colors = listOf(Color.parseColor("#FFB74D"), Color.parseColor("#64B5F6"))
                sliceSpace = 5f
                selectionShift = 10f
                valueTextColor = Color.BLACK
                valueTextSize = 14f
                setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
                setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
                valueLineColor = Color.BLACK
                valueLineWidth = 1.5f
                valueLinePart1Length = 0.6f
                valueLinePart2Length = 0.8f
            }

            val data = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value == cashCount) {
                            "Tunai: ${value.toInt()} (${String.format("%.1f", cashPercentage)}%)"
                        } else {
                            "QRIS: ${value.toInt()} (${String.format("%.1f", qrisPercentage)}%)"
                        }
                    }
                })
            }

            pieChart.apply {
                this.data = data
                description.isEnabled = false
                setDrawHoleEnabled(true)
                setHoleColor(Color.WHITE)
                setTransparentCircleRadius(80f)
                setHoleRadius(20f)
                setUsePercentValues(false)
                legend.isEnabled = true
                legend.textColor = Color.BLACK
                legend.textSize = 12f
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.yOffset = 12f
                legend.formSize = 10f
                legend.formToTextSpace = 10f
                legend.xEntrySpace = 15f
                setExtraOffsets(8f, 8f, 8f, 20f)
                invalidate()
                animateY(1000)
            }
        }
    }

    private fun setupEmptyPieChart(pieChart: PieChart) {
        val entries = listOf(PieEntry(1f, "Tidak ada data"))
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.LTGRAY)
            valueTextColor = Color.BLACK
            valueTextSize = 16f
        }
        val data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = "Tidak ada data"
            })
        }
        pieChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            isRotationEnabled = false
            isHighlightPerTapEnabled = false
            invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupHorizontalBarCharts() {
        _binding?.let { binding ->
            val days = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
            val weeks = arrayOf("week1", "week2", "week3", "week4")
            val last6Months = generateLast6MonthsOriginalFormat()

            val dailyStatsByDay = groupStatsByDay(dailyStatsForAdmin)
            val dailyTunaiEntries = mutableListOf<BarEntry>()
            val dailyQRISEntries = mutableListOf<BarEntry>()
            val dailyTotalEntries = mutableListOf<BarEntry>()

            days.forEachIndexed { index, day ->
                val stats = dailyStatsByDay[day] ?: emptyList()
                val tunaiTotal = stats.filter { it.payment_type == "cash" }.sumOf { it.total ?: 0 }.toFloat()
                val qrisTotal = stats.filter { it.payment_type == "qris" }.sumOf { it.total ?: 0 }.toFloat()
                val total = tunaiTotal + qrisTotal
                dailyTunaiEntries.add(BarEntry(index.toFloat(), tunaiTotal))
                dailyQRISEntries.add(BarEntry(index.toFloat(), qrisTotal))
                dailyTotalEntries.add(BarEntry(index.toFloat(), total))
            }
            setupGroupedBarChart(
                binding.horizontalBarChartDaily, dailyTunaiEntries, dailyQRISEntries, dailyTotalEntries, "Harian", days
            )

            val weeklyTunaiEntries = mutableListOf<BarEntry>()
            val weeklyQRISEntries = mutableListOf<BarEntry>()
            val weeklyTotalEntries = mutableListOf<BarEntry>()

            weeks.forEachIndexed { index, week ->
                val stats = weeklyStats[week] ?: emptyList()
                val tunaiTotal = stats.filter { it.payment_type == "cash" }.sumOf { it.total ?: 0 }.toFloat()
                val qrisTotal = stats.filter { it.payment_type == "qris" }.sumOf { it.total ?: 0 }.toFloat()
                weeklyTunaiEntries.add(BarEntry(index.toFloat(), tunaiTotal))
                weeklyQRISEntries.add(BarEntry(index.toFloat(), qrisTotal))
                weeklyTotalEntries.add(BarEntry(index.toFloat(), tunaiTotal + qrisTotal))
            }
            setupGroupedBarChart(
                binding.horizontalBarChartWeekly, weeklyTunaiEntries, weeklyQRISEntries, weeklyTotalEntries, "Mingguan",
                weeks.mapIndexed { index, _ -> "Minggu ${index + 1}" }.toTypedArray()
            )

            val monthlyTunaiEntries = mutableListOf<BarEntry>()
            val monthlyQRISEntries = mutableListOf<BarEntry>()
            val monthlyTotalEntries = mutableListOf<BarEntry>()

            last6Months.forEachIndexed { index, month ->
                val fullMonthName = monthToFullName(month)
                val stats = monthlyStats[fullMonthName] ?: monthlyStats[month] ?: emptyList()
                val tunaiTotal = stats.filter { it.payment_type == "cash" }.sumOf { it.total ?: 0 }.toFloat()
                val qrisTotal = stats.filter { it.payment_type == "qris" }.sumOf { it.total ?: 0 }.toFloat()
                monthlyTunaiEntries.add(BarEntry(index.toFloat(), tunaiTotal))
                monthlyQRISEntries.add(BarEntry(index.toFloat(), qrisTotal))
                monthlyTotalEntries.add(BarEntry(index.toFloat(), tunaiTotal + qrisTotal))
            }
            setupGroupedBarChart(
                binding.horizontalBarChartMonthly, monthlyTunaiEntries, monthlyQRISEntries, monthlyTotalEntries, "Bulanan", last6Months
            )
        }
    }

    private fun setupGroupedBarChart(
        chart: HorizontalBarChart,
        tunaiEntries: List<BarEntry>,
        qrisEntries: List<BarEntry>,
        totalEntries: List<BarEntry>,
        label: String,
        xLabels: Array<String>
    ) {
        val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

        val tunaiDataSet = BarDataSet(tunaiEntries, "Tunai").apply {
            color = Color.parseColor("#FFB74D")
            valueTextColor = Color.BLACK
            valueTextSize = 16f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "Rp 0" else "Rp ${numberFormat.format(value.toInt())}"
                }
            }
        }

        val qrisDataSet = BarDataSet(qrisEntries, "QRIS").apply {
            color = Color.parseColor("#64B5F6")
            valueTextColor = Color.BLACK
            valueTextSize = 16f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "Rp 0" else "Rp ${numberFormat.format(value.toInt())}"
                }
            }
        }

        val totalDataSet = BarDataSet(totalEntries, "Total Uang").apply {
            color = Color.parseColor("#81C784")
            valueTextColor = Color.BLACK
            valueTextSize = 16f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "Rp 0" else "Rp ${numberFormat.format(value.toInt())}"
                }
            }
        }

        val barData = BarData(tunaiDataSet, qrisDataSet, totalDataSet)
        barData.barWidth = 0.25f
        val groupSpace = 0.2f
        val barSpace = 0.03f
        barData.groupBars(-0.5f, groupSpace, barSpace)

        chart.apply {
            data = barData
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            xAxis.granularity = 1f
            xAxis.textColor = Color.BLACK
            xAxis.setCenterAxisLabels(true)
            xAxis.axisMinimum = -0.1f
            xAxis.axisMaximum = xLabels.size - 0.1f
            xAxis.setDrawGridLines(false)
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setExtraOffsets(16f, 24f, 16f, 24f)
            setVisibleXRangeMaximum(xLabels.size.toFloat())
            setVisibleXRangeMinimum(1f)
            setPinchZoom(true)
            setDoubleTapToZoomEnabled(true)
            setScaleYEnabled(false)
            setFitBars(true)
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.textColor = Color.BLACK
            legend.textSize = 14f
            notifyDataSetChanged()
            invalidate()
            animateY(1800)
        }
    }

    private fun handleDownload(type: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !checkStoragePermission()) {
            Toast.makeText(requireContext(), "Izin penyimpanan diperlukan", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = RetrofitClient.getInstance(requireContext())
        val call = when (type) {
            "daily" -> apiService.exportDailyStatistics()
            "weekly" -> apiService.exportWeeklyStatistics()
            "monthly" -> apiService.exportMonthlyStatistics()
            else -> return
        }

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    response.body()?.let { saveToCsv(it.string(), "Rekap_${type.capitalizeFirst()}_${System.currentTimeMillis()}.csv") }
                } else {
                    handleApiError(response, "Gagal mengunduh data")
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                handleApiFailure(t)
            }
        })
    }

    private fun saveToCsv(csvContent: String, fileName: String) {
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use { it.write(csvContent.toByteArray()) }
            Toast.makeText(requireContext(), "File $fileName berhasil disimpan di Downloads", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            false
        } else true
    }

    private fun handleApiError(response: Response<*>, defaultMessage: String) {
        if (!isAdded) return
        Toast.makeText(context, defaultMessage, Toast.LENGTH_SHORT).show()
        Log.e("DashboardFragment", "API Error: ${response.code()} - ${response.errorBody()?.string()}")
    }

    private fun handleApiFailure(t: Throwable) {
        if (!isAdded) return
        Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
        Log.e("DashboardFragment", "Network Failure", t)
    }

    private fun String.capitalizeFirst(): String = this.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = if (isHeader) 16f else 14f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun groupStatsByDay(stats: List<DailyStatistics>): Map<String, List<DailyStatistics>> {
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("id", "ID"))
        return stats.groupBy { stat ->
            try {
                dayFormatter.format(Instant.parse(stat.created_at).atZone(ZoneId.of("UTC")))
            } catch (e: Exception) { "" }
        }
    }

    private fun generateLast6MonthsOriginalFormat(): Array<String> {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Des")
        val calendar = Calendar.getInstance()
        return Array(6) { i ->
            val tempCalendar = calendar.clone() as Calendar
            tempCalendar.add(Calendar.MONTH, -5 + i)
            months[tempCalendar.get(Calendar.MONTH)]
        }
    }

    private fun monthToFullName(shortMonth: String): String = when (shortMonth) {
        "Jan" -> "January"; "Feb" -> "February"; "Mar" -> "March"; "Apr" -> "April"; "Mei" -> "May"; "Jun" -> "June"
        "Jul" -> "July"; "Aug" -> "August"; "Sep" -> "September"; "Okt" -> "October"; "Nov" -> "November"; "Des" -> "December"
        else -> shortMonth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}