package com.example.eatstedi.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.eatstedi.AllEmployeeActivity
import com.example.eatstedi.AllSupplierActivity
import com.example.eatstedi.R
import com.example.eatstedi.databinding.FragmentDashboardBinding
import com.example.eatstedi.databinding.ViewItemEmployeeBinding
import com.example.eatstedi.databinding.ViewItemSupplierBinding
import com.example.eatstedi.model.EmployeePreview
import com.example.eatstedi.model.EmployeeSchedule
import com.example.eatstedi.model.SupplierPreview
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using ViewBinding
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data shift dari resources
        val shifts = resources.getStringArray(R.array.shift_array)

        // Buat ArrayAdapter untuk Spinner
        spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, shifts)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)



        with(binding) {
            // Set listener untuk tombol View All Employee
            tvViewAllEmployee.setOnClickListener {
                val intent = Intent(requireContext(), AllEmployeeActivity::class.java)
                startActivity(intent)
            }
            // Set listener untuk tombol View All Supplier
            tvViewAllSupplier.setOnClickListener {
                val intent = Intent(requireContext(), AllSupplierActivity::class.java)
                startActivity(intent)
            }

        }

        // Data dummy jadwal
        val scheduleList = listOf(
            EmployeeSchedule("Alice Johnson", "Senin", "1", "07:00-09:00"),
            EmployeeSchedule("Bob Smith", "Selasa", "1", "07:00-09:00"),
            EmployeeSchedule("Charlie Lee", "Rabu", "1", "07:00-09:00"),
            EmployeeSchedule("David Kim", "Kamis", "1", "07:00-09:00"),
            EmployeeSchedule("Eve Brown", "Jumat", "2", "09:00-12:00"),
            EmployeeSchedule("Frank White", "Sabtu", "2", "09:00-12:00"),
            EmployeeSchedule("Grace Davis", "Minggu", "2", "09:00-12:00"),
            EmployeeSchedule("Henry Wilson", "Senin", "2", "09:00-12:00"),
            EmployeeSchedule("Ivy Thomas", "Selasa", "3", "12:00-14:00"),
            EmployeeSchedule("Jack Harris", "Rabu", "3", "12:00-14:00"),
            EmployeeSchedule("Katie Clark", "Kamis", "3", "12:00-14:00"),
            EmployeeSchedule("Laura Miller", "Jumat", "4", "14:00-16:00"),
            EmployeeSchedule("Michael Brown", "Sabtu", "4", "14:00-16:00")
        )

        // Data dummy employee
        val employeeList = listOf(
            EmployeePreview("Alice Johnson", R.drawable.img_avatar),
            EmployeePreview("Bob Smith", R.drawable.img_avatar),
            EmployeePreview("Charlie Lee", R.drawable.img_avatar),
            EmployeePreview("David Kim", R.drawable.img_avatar),
            EmployeePreview("Eve Brown", R.drawable.img_avatar),
            EmployeePreview("Frank White", R.drawable.img_avatar),
            EmployeePreview("Grace Davis", R.drawable.img_avatar),
            EmployeePreview("Henry Wilson", R.drawable.img_avatar),
            EmployeePreview("Ivy Thomas", R.drawable.img_avatar),
            EmployeePreview("Jack Harris", R.drawable.img_avatar),
            EmployeePreview("Katie Clark", R.drawable.img_avatar)
        )

        // Data dummy supplier
        val supplierList = listOf(
            SupplierPreview("Supplier A", R.drawable.img_avatar),
            SupplierPreview("Supplier B", R.drawable.img_avatar),
            SupplierPreview("Supplier C", R.drawable.img_avatar),
            SupplierPreview("Supplier D", R.drawable.img_avatar),
            SupplierPreview("Supplier E", R.drawable.img_avatar),
            SupplierPreview("Supplier F", R.drawable.img_avatar),
            SupplierPreview("Supplier G", R.drawable.img_avatar),
            SupplierPreview("Supplier H", R.drawable.img_avatar),
            SupplierPreview("Supplier I", R.drawable.img_avatar),
            SupplierPreview("Supplier J", R.drawable.img_avatar),
            SupplierPreview("Supplier K", R.drawable.img_avatar)
        )

        // Memanggil fungsi untuk menambahkan profil karyawan secara dinamis
        addEmployeeProfiles(employeeList)
        // Memanggil fungsi untuk menambahkan profil supplier secara dinamis
        addSupplierProfiles(supplierList)

        // Memanggil fungsi setupPieChart
        setupPieChart()
        // Memanggil fungsi setupHorizontalBarCharts
        setupHorizontalBarCharts()

        // Menambahkan jadwal ke TableLayout
        addScheduleToTable(scheduleList)

    }

    // Fungsi untuk memfilter data berdasarkan shift yang dipilih
    private fun filterDataByShift(shift: String) {
        // Logika untuk memfilter data berdasarkan shift
        println("Data difilter berdasarkan shift: $shift")
    }

    private fun addEmployeeProfiles(employeeList: List<EmployeePreview>) {
        // Ambil container di fragment_dashboard.xml melalui binding
        val employeeContainer = binding.employeeContainer

        // Tambahkan setiap employee secara dinamis
        for (employee in employeeList) {
            // Gunakan binding untuk item_employee.xml
            val itemBinding = ViewItemEmployeeBinding.inflate(layoutInflater, employeeContainer, false)

            // Setel data employee ke item binding
            itemBinding.ivEmployee.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), employee.imageResId)
            )
            itemBinding.tvEmployee.text = employee.name

            // Tambahkan item view ke dalam container
            employeeContainer.addView(itemBinding.root)
        }
    }

    // Fungsi untuk menambahkan profil supplier secara dinamis
    private fun addSupplierProfiles(supplierList: List<SupplierPreview>) {
        // Ambil container di fragment_dashboard.xml melalui binding
        val supplierContainer = binding.supplierContainer

        // Tambahkan setiap supplier secara dinamis
        for (supplier in supplierList) {
            // Gunakan binding untuk item_supplier.xml
            val itemBinding = ViewItemSupplierBinding.inflate(layoutInflater, supplierContainer, false)

            // Setel data supplier ke item binding
            itemBinding.ivSupplier.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), supplier.imageResId)
            )
            itemBinding.tvSupplier.text = supplier.name

            // Tambahkan item view ke dalam container
            supplierContainer.addView(itemBinding.root)
        }
    }

    private fun setupPieChart() {
        val pieChart = binding.piechartPayments

        // Data dummy untuk PieChart
        val totalCashTransactions = 324  // Jumlah transaksi Cash
        val totalQRITransactions = 768   // Jumlah transaksi QRIS
        val totalTransactions = totalCashTransactions + totalQRITransactions

        val cashPercentage = (totalCashTransactions.toFloat() / totalTransactions) * 100
        val qrisPercentage = (totalQRITransactions.toFloat() / totalTransactions) * 100

        val entries = listOf(
            PieEntry(totalCashTransactions.toFloat(), "Tunai"),
            PieEntry(totalQRITransactions.toFloat(), "QRIS"),
        )

        // Membuat PieDataSet
        val dataSet = PieDataSet(entries, "Metode Pembayaran")
        dataSet.colors = listOf(
            Color.parseColor("#FFB74D"), // Oranye untuk Cash
            Color.parseColor("#64B5F6")  // Biru untuk QRIS
        )
        dataSet.sliceSpace = 5f  // Mengurangi sedikit spasi antar slice
        dataSet.selectionShift = 10f  // Membuat slice yang terpilih lebih besar
        dataSet.valueTextColor = Color.BLACK  // Warna teks nilai di dalam slice
        dataSet.valueTextSize = 14f  // Ukuran teks nilai di dalam slice

        // Menampilkan garis indikator dan label di luar slice
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
        dataSet.valueLineColor = Color.BLACK  // Warna garis indikator label
        dataSet.valueLineWidth = 1.5f  // Ketebalan garis indikator
        dataSet.valueLinePart1Length = 0.4f  // Panjang bagian pertama garis
        dataSet.valueLinePart2Length = 0.6f  // Panjang bagian kedua garis


        // Membuat PieData
        val data = PieData(dataSet)

        // Menambahkan Value Formatter untuk menampilkan transaksi dan persentase
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Menentukan label berdasarkan nilai
                return if (value == totalCashTransactions.toFloat()) {
                    "Tunai: $totalCashTransactions (${String.format("%.1f", cashPercentage)}%)"
                } else {
                    "QRIS: $totalQRITransactions (${String.format("%.1f", qrisPercentage)}%)"
                }
            }
        })

        // Menetapkan data ke PieChart dan mengatur beberapa konfigurasi
        pieChart.data = data
        pieChart.invalidate()
        pieChart.description.isEnabled = false
        pieChart.setDrawHoleEnabled(true)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleRadius(80f) // Membuat hole lebih kecil
        pieChart.setHoleRadius(20f) // Membuat hole lebih besar
        pieChart.animateY(1000)

        // Nonaktifkan mode persentase bawaan untuk menampilkan nilai kustom
        pieChart.setUsePercentValues(false)

        // Menambahkan legenda
        pieChart.legend.isEnabled = true
        pieChart.legend.textColor = Color.BLACK
        pieChart.legend.textSize = 12f
    }

    // Fungsi untuk Horizontal Bar Chart
    private fun setupHorizontalBarCharts() {
        // Label untuk Harian, Mingguan, Bulanan
        val days = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
        val weeks = arrayOf("Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4")
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei")

        // Data Harian
        val dailyTunaiEntries = listOf(
            BarEntry(0f, 100f), BarEntry(1f, 120f), BarEntry(2f, 110f), BarEntry(3f, 130f), BarEntry(4f, 140f)
        )
        val dailyQRISEntries = listOf(
            BarEntry(0f, 150f), BarEntry(1f, 140f), BarEntry(2f, 160f), BarEntry(3f, 170f), BarEntry(4f, 180f)
        )
        val dailyTotalEntries = listOf(
            BarEntry(0f, 250f), BarEntry(1f, 260f), BarEntry(2f, 270f), BarEntry(3f, 300f), BarEntry(4f, 320f)
        )

        setupGroupedBarChart(
            binding.horizontalBarChartDaily, dailyTunaiEntries, dailyQRISEntries, dailyTotalEntries, "Harian", days
        )

        // Data Mingguan (4 minggu saja)
        val weeklyTunaiEntries = listOf(
            BarEntry(0f, 500f), BarEntry(1f, 600f), BarEntry(2f, 550f), BarEntry(3f, 700f)
        )
        val weeklyQRISEntries = listOf(
            BarEntry(0f, 700f), BarEntry(1f, 750f), BarEntry(2f, 800f), BarEntry(3f, 850f)
        )
        val weeklyTotalEntries = listOf(
            BarEntry(0f, 1200f), BarEntry(1f, 1350f), BarEntry(2f, 1350f), BarEntry(3f, 1550f)
        )

        setupGroupedBarChart(
            binding.horizontalBarChartWeekly, weeklyTunaiEntries, weeklyQRISEntries, weeklyTotalEntries, "Mingguan", weeks
        )

        // Data Bulanan
        val monthlyTunaiEntries = listOf(
            BarEntry(0f, 2000f), BarEntry(1f, 2200f), BarEntry(2f, 2100f), BarEntry(3f, 2300f), BarEntry(4f, 2400f)
        )
        val monthlyQRISEntries = listOf(
            BarEntry(0f, 2500f), BarEntry(1f, 2700f), BarEntry(2f, 2600f), BarEntry(3f, 2800f), BarEntry(4f, 2900f)
        )
        val monthlyTotalEntries = listOf(
            BarEntry(0f, 4500f), BarEntry(1f, 4900f), BarEntry(2f, 4700f), BarEntry(3f, 5100f), BarEntry(4f, 5300f)
        )

        setupGroupedBarChart(
            binding.horizontalBarChartMonthly, monthlyTunaiEntries, monthlyQRISEntries, monthlyTotalEntries, "Bulanan", months
        )
    }

    // Fungsi untuk Setup Grouped Bar Chart
    private fun setupGroupedBarChart(
        chart: HorizontalBarChart,
        tunaiEntries: List<BarEntry>,
        qrisEntries: List<BarEntry>,
        totalEntries: List<BarEntry>,
        label: String,
        xLabels: Array<String>
    ) {
        // Buat DataSet untuk masing-masing kategori
        val tunaiDataSet = BarDataSet(tunaiEntries, "Tunai")
        tunaiDataSet.color = Color.parseColor("#FFB74D") // Oranye terang
        tunaiDataSet.valueTextColor = Color.BLACK
        tunaiDataSet.valueTextSize = 14f

        val qrisDataSet = BarDataSet(qrisEntries, "QRIS")
        qrisDataSet.color = Color.parseColor("#64B5F6") // Biru muda
        qrisDataSet.valueTextColor = Color.BLACK
        qrisDataSet.valueTextSize = 14f

        val totalDataSet = BarDataSet(totalEntries, "Total Uang")
        totalDataSet.color = Color.parseColor("#81C784") // Hijau terang
        totalDataSet.valueTextColor = Color.BLACK
        totalDataSet.valueTextSize = 14f

        // Gabungkan DataSet ke dalam BarData
        val barData = BarData(tunaiDataSet, qrisDataSet, totalDataSet)
        barData.barWidth = 0.25f // Lebar setiap batang

        // Atur posisi batang agar terpisah
        val groupSpace = 0.4f // Jarak antar grup
        val barSpace = 0.05f // Jarak antar batang dalam grup
        barData.groupBars(0f, groupSpace, barSpace) // Mengelompokkan batang

        // Setup Chart
        chart.data = barData
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        chart.xAxis.granularity = 1f
        chart.xAxis.textColor = Color.BLACK
        chart.axisLeft.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.description.text = label
        chart.description.textColor = Color.BLACK
        chart.setDrawValueAboveBar(true) // Angka muncul di atas batang
        chart.setFitBars(true)
        chart.invalidate()
        chart.animateY(1000)
    }

    // Fungsi untuk menambahkan jadwal ke dalam TableLayout
    private fun addScheduleToTable(scheduleList: List<EmployeeSchedule>) {
        val tableLayout = binding.tableView

        // Hapus semua baris yang ada jika diperlukan
        tableLayout.removeAllViews()

        // Menambahkan header untuk tabel
        val headerRow = TableRow(requireContext()).apply {
            setPadding(8, 16, 8, 16)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary)) // Sesuaikan warna
        }

        headerRow.addView(createTextView("Nama Karyawan", isHeader = true))
        headerRow.addView(createTextView("Hari", isHeader = true))
        headerRow.addView(createTextView("Shift", isHeader = true))
        headerRow.addView(createTextView("Waktu", isHeader = true))

        tableLayout.addView(headerRow)

        // Menambahkan data jadwal karyawan ke dalam table
        for (schedule in scheduleList) {
            val row = TableRow(requireContext()).apply {
                setPadding(8, 16, 8, 16)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            row.addView(createTextView(schedule.name))
            row.addView(createTextView(schedule.day))
            row.addView(createTextView(schedule.shift))
            row.addView(createTextView(schedule.time))

            tableLayout.addView(row)
        }
    }

    // Fungsi untuk membuat TextView dengan gaya header atau konten biasa
    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(requireContext(), if (isHeader) R.color.black else R.color.black))
            setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f) // Menyediakan bobot agar lebar kolom sama
        }
    }


    // Fungsi untuk membersihkan binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
