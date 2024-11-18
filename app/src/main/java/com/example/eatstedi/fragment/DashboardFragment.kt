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
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
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


        // Set adapter ke Spinner menggunakan binding
        binding.spShift.adapter = spinnerAdapter

        // Set listener untuk menangani pemilihan shift
        binding.spShift.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedShift = shifts[position]
                // Gunakan selectedShift untuk memfilter data
                filterDataByShift(selectedShift)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Optional: tindakan jika tidak ada yang dipilih
            }
        }

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
            EmployeeSchedule("Alice Johnson", "Senin", "Shift 1", "08:00 - 16:00"),
            EmployeeSchedule("Bob Smith", "Selasa", "Shift 1", "12:00 - 20:00"),
            EmployeeSchedule("Charlie Lee", "Rabu", "Shift 1", "20:00 - 04:00"),
            EmployeeSchedule("David Kim", "Kamis", "Shift 1", "08:00 - 16:00"),
            EmployeeSchedule("Eve Brown", "Jumat", "Shift 1", "12:00 - 20:00"),
            EmployeeSchedule("Frank White", "Sabtu", "Shift 1", "20:00 - 04:00"),
            EmployeeSchedule("Grace Davis", "Minggu", "Shift 1", "08:00 - 16:00"),
            EmployeeSchedule("Henry Wilson", "Senin", "Shift 2", "12:00 - 20:00"),
            EmployeeSchedule("Ivy Thomas", "Selasa", "Shift 2", "20:00 - 04:00"),
            EmployeeSchedule("Jack Harris", "Rabu", "Shift 2", "08:00 - 16:00"),
            EmployeeSchedule("Katie Clark", "Kamis", "Shift 2", "12:00 - 20:00"),
            EmployeeSchedule("Laura Miller", "Jumat", "Shift 2", "20:00 - 04:00"),
            EmployeeSchedule("Michael Brown", "Sabtu", "Shift 2", "08:00 - 16:00")
        )

        // Data dummy employee
        val employeeList = listOf(
            EmployeePreview("Alice Johnson", R.drawable.user_profile),
            EmployeePreview("Bob Smith", R.drawable.user_profile),
            EmployeePreview("Charlie Lee", R.drawable.user_profile),
            EmployeePreview("David Kim", R.drawable.user_profile),
            EmployeePreview("Eve Brown", R.drawable.user_profile),
            EmployeePreview("Frank White", R.drawable.user_profile),
            EmployeePreview("Grace Davis", R.drawable.user_profile),
            EmployeePreview("Henry Wilson", R.drawable.user_profile),
            EmployeePreview("Ivy Thomas", R.drawable.user_profile),
            EmployeePreview("Jack Harris", R.drawable.user_profile),
            EmployeePreview("Katie Clark", R.drawable.user_profile)
        )

        // Data dummy supplier
        val supplierList = listOf(
            SupplierPreview("Supplier A", R.drawable.user_profile),
            SupplierPreview("Supplier B", R.drawable.user_profile),
            SupplierPreview("Supplier C", R.drawable.user_profile),
            SupplierPreview("Supplier D", R.drawable.user_profile),
            SupplierPreview("Supplier E", R.drawable.user_profile),
            SupplierPreview("Supplier F", R.drawable.user_profile),
            SupplierPreview("Supplier G", R.drawable.user_profile),
            SupplierPreview("Supplier H", R.drawable.user_profile),
            SupplierPreview("Supplier I", R.drawable.user_profile),
            SupplierPreview("Supplier J", R.drawable.user_profile),
            SupplierPreview("Supplier K", R.drawable.user_profile)
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

    // Fungfi setupPieChart untuk menyiapkan data dan menampilkan PieChart
    private fun setupPieChart() {
        val pieChart = binding.piechartPayments

        // Data dummy untuk PieChart
        val entries = listOf(
            PieEntry(40f, "Cash"),
            PieEntry(30f, "QRIS"),
        )

        // Membuat PieDataSet
        val dataSet = PieDataSet(entries, "Metode Pembayaran (dalam %)")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() // Warna slice yang digunakan
        dataSet.sliceSpace = 5f  // Mengurangi sedikit spasi antar slice
        dataSet.selectionShift = 10f  // Membuat slice yang terpilih lebih besar

        // Membuat PieData
        val data = PieData(dataSet)
        data.setValueTextSize(16f)  // Ukuran teks nilai dalam chart
        data.setValueTextColor(Color.WHITE)  // Warna teks dalam chart

        // Menetapkan data ke PieChart dan mengatur beberapa konfigurasi
        pieChart.data = data
        pieChart.invalidate()
        pieChart.description.isEnabled = false
        pieChart.setDrawHoleEnabled(true)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleRadius(80f) // Membuat hole lebih kecil
        pieChart.setHoleRadius(40f) // Membuat hole lebih besar
        pieChart.animateY(1000)
        pieChart.setUsePercentValues(true)
    }

    // Fungsi untuk Horizontal Bar Chart
    private fun setupHorizontalBarCharts() {
        // Data dummy untuk Daily Bar Chart
        val dailyEntries = listOf(
            BarEntry(0f, 150f),
            BarEntry(1f, 200f),
            BarEntry(2f, 180f),
            BarEntry(3f, 130f),
            BarEntry(4f, 160f)
        )

        // Membuat BarDataSet untuk daily chart
        val dailyDataSet = BarDataSet(dailyEntries, "Daily Sales")
        dailyDataSet.color = Color.rgb(0, 128, 0) // Green color
        dailyDataSet.valueTextColor = Color.WHITE

        // Membuat BarData untuk daily chart
        val dailyBarData = BarData(dailyDataSet)

        // Setup Horizontal Bar Chart for Daily
        val dailyBarChart = binding.horizontalBarChartDaily
        dailyBarChart.data = dailyBarData
        dailyBarChart.invalidate()
        dailyBarChart.setFitBars(true) // Make sure the bars fit within the chart width
        dailyBarChart.animateY(1000)

        // Data dummy untuk Weekly Bar Chart
        val weeklyEntries = listOf(
            BarEntry(0f, 300f),
            BarEntry(1f, 350f),
            BarEntry(2f, 280f),
            BarEntry(3f, 320f),
            BarEntry(4f, 370f)
        )

        // Membuat BarDataSet untuk weekly chart
        val weeklyDataSet = BarDataSet(weeklyEntries, "Weekly Sales")
        weeklyDataSet.color = Color.rgb(255, 165, 0) // Orange color
        weeklyDataSet.valueTextColor = Color.WHITE

        // Membuat BarData untuk weekly chart
        val weeklyBarData = BarData(weeklyDataSet)

        // Setup Horizontal Bar Chart for Weekly
        val weeklyBarChart = binding.horizontalBarChartWeekly
        weeklyBarChart.data = weeklyBarData
        weeklyBarChart.invalidate()
        weeklyBarChart.setFitBars(true)
        weeklyBarChart.animateY(1000)

        // Data dummy untuk Monthly Bar Chart
        val monthlyEntries = listOf(
            BarEntry(0f, 1000f),
            BarEntry(1f, 1200f),
            BarEntry(2f, 1500f),
            BarEntry(3f, 1300f),
            BarEntry(4f, 1100f)
        )

        // Membuat BarDataSet untuk monthly chart
        val monthlyDataSet = BarDataSet(monthlyEntries, "Monthly Sales")
        monthlyDataSet.color = Color.rgb(0, 0, 255) // Blue color
        monthlyDataSet.valueTextColor = Color.WHITE

        // Membuat BarData untuk monthly chart
        val monthlyBarData = BarData(monthlyDataSet)

        // Setup Horizontal Bar Chart for Monthly
        val monthlyBarChart = binding.horizontalBarChartMonthly
        monthlyBarChart.data = monthlyBarData
        monthlyBarChart.invalidate()
        monthlyBarChart.setFitBars(true)
        monthlyBarChart.animateY(1000)
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

        headerRow.addView(createTextView("Nama Employee", isHeader = true))
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
