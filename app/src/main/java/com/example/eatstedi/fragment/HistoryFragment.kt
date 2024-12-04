package com.example.eatstedi.fragment

import android.app.DatePickerDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.eatstedi.R
import com.example.eatstedi.databinding.FragmentHistoryBinding
import com.example.eatstedi.model.Attendance
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private var activeFilter: String = "Total"
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Data dummy untuk history kehadiran
        val attendanceRecords = listOf(
            Attendance("Alice", "01/11/2024", "Shift 1", "07:00-09:00", "Hadir"),
            Attendance("Bob", "01/11/2024", "Shift 2", "09:00-12:00", "Hadir"),
            Attendance("Charlie", "02/11/2024", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Dave", "02/11/2024", "Shift 1", "07:00-09:00", "Tidak Hadir"),
            Attendance("Eve", "03/11/2024", "Shift 3", "12:00-14:00", "Tidak Hadir"),
            Attendance("Frank", "03/11/2024", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Grace", "04/11/2024", "Shift 1", "07:00-09:00", "Hadir"),
            Attendance("Hank", "04/11/2024", "Shift 3", "12:00-14:00", "Hadir"),
            Attendance("Ivy", "05/11/2024", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Jack", "05/11/2024", "Shift 1", "07:00-09:00", "Tidak Hadir"),
            Attendance("Kathy", "06/11/2024", "Shift 3", "12:00-14:00", "Tidak Hadir"),
            Attendance("Liam", "06/11/2024", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Mia", "07/11/2024", "Shift 1", "07:00-09:00", "Hadir"),
            Attendance("Nathan", "07/11/2024", "Shift 3", "12:00-14:00", "Hadir"),
            Attendance("Olivia", "08/11/2024", "Shift 4", "14:00-16:00", "Hadir")
        )


        // Hitung jumlah awal
        updateFilterCounts(attendanceRecords)

        // Setup filter event handlers
        setupFilters(attendanceRecords)

        // Tampilkan data awal dengan filter "Total" aktif
        updateActiveFilter("Total", attendanceRecords)

        // Setup date pickers for start and end date
        setupDatePickers(attendanceRecords)

        // Setup Clear Search button click
        setupClearSearchButton(attendanceRecords)
    }

    private fun updateFilterCounts(attendanceRecords: List<Attendance>) {
        // Menghitung jumlah kehadiran, ketidakhadiran, dan total
        val total = attendanceRecords.size
        val present = attendanceRecords.count { it.attendance == "Hadir" }
        val absent = attendanceRecords.count { it.attendance == "Tidak Hadir" }

        // Update UI dengan jumlah yang dihitung
        binding.tvTotal.text = total.toString() // Menampilkan jumlah total
        binding.tvPresent.text = present.toString() // Menampilkan jumlah hadir
        binding.tvAbsent.text = absent.toString() // Menampilkan jumlah tidak hadir
    }

    private fun setupFilters(attendanceRecords: List<Attendance>) {
        binding.llSummaryTotal.setOnClickListener {
            updateActiveFilter("Total", attendanceRecords)
        }
        binding.llSummaryPresent.setOnClickListener {
            updateActiveFilter("Hadir", attendanceRecords)
        }
        binding.llSummaryAbsent.setOnClickListener {
            updateActiveFilter("Tidak Hadir", attendanceRecords)
        }
    }

    private fun setupDatePickers(attendanceRecords: List<Attendance>) {
        binding.btnStartDate.setOnClickListener {
            showDatePicker(isStartDate = true) { date ->
                startDate = date
                binding.btnStartDate.text = dateFormat.format(date)
                updateActiveFilter(activeFilter, attendanceRecords)
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(isStartDate = false) { date ->
                endDate = date
                binding.btnEndDate.text = dateFormat.format(date)
                updateActiveFilter(activeFilter, attendanceRecords)
            }
        }
    }


    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time

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

    private fun setupClearSearchButton(attendanceRecords: List<Attendance>) {
        // Menyembunyikan tombol Clear Search secara default
        binding.ivClearSearch.visibility = View.GONE

        binding.ivClearSearch.setOnClickListener {
            // Reset start date dan end date
            startDate = null
            endDate = null
            binding.btnStartDate.text = "dd/mm/yyyy"
            binding.btnEndDate.text = "dd/mm/yyyy"

            // Reset filter ke "Total"
            updateActiveFilter("Total", attendanceRecords)

            // Sembunyikan tombol Clear Search setelah reset
            binding.ivClearSearch.visibility = View.GONE
        }
    }

    private fun updateActiveFilter(filter: String, attendanceRecords: List<Attendance>) {
        activeFilter = filter

        // Update UI untuk setiap LinearLayout sesuai status aktif/tidak aktif
        updateFilterUI(binding.llSummaryTotal, filter == "Total", binding.tvTotal, binding.tvTotalLabel)
        updateFilterUI(binding.llSummaryPresent, filter == "Hadir", binding.tvPresent, binding.tvPresentLabel)
        updateFilterUI(binding.llSummaryAbsent, filter == "Tidak Hadir", binding.tvAbsent, binding.tvAbsentLabel)

        // Menampilkan tombol Clear Search jika ada filter yang diterapkan
        if (startDate != null || endDate != null || filter != "Total") {
            binding.ivClearSearch.visibility = View.VISIBLE
        } else {
            binding.ivClearSearch.visibility = View.GONE
        }

        // Tampilkan data sesuai filter dan rentang tanggal
        filterAndDisplayData(attendanceRecords, filter)
    }

    private fun updateFilterUI(
        layout: View,
        isActive: Boolean,
        valueTextView: TextView,
        labelTextView: TextView
    ) {
        // Atur warna latar belakang
        val backgroundColor = if (isActive) R.color.red else R.color.secondary
        val textColor = if (isActive) R.color.white else R.color.black

        // Tambahkan sudut membulat (corner radius)
        val drawable = GradientDrawable().apply {
            cornerRadius = 8f
            setColor(ContextCompat.getColor(requireContext(), backgroundColor))
        }
        layout.background = drawable

        // Atur warna teks
        valueTextView.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        labelTextView.setTextColor(ContextCompat.getColor(requireContext(), textColor))
    }

    private fun Date.stripTime(): Date {
        val calendar = Calendar.getInstance().apply {
            time = this@stripTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    private fun filterAndDisplayData(attendanceRecords: List<Attendance>, filter: String) {
        var filteredRecords = when (filter) {
            "Hadir" -> attendanceRecords.filter { it.attendance == "Hadir" }
            "Tidak Hadir" -> attendanceRecords.filter { it.attendance == "Tidak Hadir" }
            else -> attendanceRecords
        }

        // Filter berdasarkan rentang tanggal jika start date dan end date tidak null
        // Menggunakan `stripTime()` untuk menghapus waktu dari tanggal
        val strippedStartDate = startDate?.stripTime()
        val strippedEndDate = endDate?.stripTime()
        if (strippedStartDate != null && strippedEndDate != null) {
            filteredRecords = filteredRecords.filter { attendance ->
                val attendanceDate = dateFormat.parse(attendance.date)!!.stripTime()
                !attendanceDate.before(strippedStartDate) && !attendanceDate.after(strippedEndDate)
            }
        }

        updateFilteredCounts(filteredRecords)
        displayData(filteredRecords)
    }

    private fun updateFilteredCounts(filteredRecords: List<Attendance>) {
        val total = filteredRecords.size
        val present = filteredRecords.count { it.attendance == "Hadir" }
        val absent = filteredRecords.count { it.attendance == "Tidak Hadir" }

        // Perbarui `TextView` untuk total, hadir, dan tidak hadir
        binding.tvTotal.text = total.toString()
        binding.tvPresent.text = present.toString()
        binding.tvAbsent.text = absent.toString()
    }

    private fun displayData(attendanceRecords: List<Attendance>) {
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Periksa apakah ada data
        if (attendanceRecords.isEmpty()) {
            // Jika tidak ada data, tampilkan tvNoData dan sembunyikan tabel
            binding.tvNoData.visibility = View.VISIBLE
            binding.attendanceTableView.visibility = View.GONE
            return
        } else {
            // Jika ada data, sembunyikan tvNoData dan tampilkan tabel
            binding.tvNoData.visibility = View.GONE
            binding.attendanceTableView.visibility = View.VISIBLE
        }

        // Hapus semua baris di tabel (kecuali header)
        binding.attendanceTableView.removeAllViews()

        // Buat header tabel
        val headerRow = TableRow(context)
        headerRow.setPadding(8, 16, 8, 16)
        headerRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))

        headerRow.addView(createTextView("Nama Karyawan", isHeader = true))
        headerRow.addView(createTextView("Tanggal", isHeader = true))
        headerRow.addView(createTextView("Shift", isHeader = true))
        headerRow.addView(createTextView("Waktu", isHeader = true))
        headerRow.addView(createTextView("Kehadiran", isHeader = true))

        binding.attendanceTableView.addView(headerRow)

        // Tambahkan data ke tabel
        // Add data to the table
        for ((index, attendance) in attendanceRecords.withIndex()) {
            val formattedDate = try {
                val date = dateFormat.parse(attendance.date)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                attendance.date // Fallback if parsing fails
            }

            val row = TableRow(context)
            row.setPadding(8, 16, 8, 16)
            // Alternate row colors based on the index (even = white, odd = secondary color)
            row.setBackgroundColor(
                if (index % 2 == 0) {
                    ContextCompat.getColor(requireContext(), R.color.white)  // Even rows
                } else {
                    ContextCompat.getColor(requireContext(), R.color.secondary)  // Odd rows
                }
            )

            // Add text views to the row
            row.addView(createTextView(attendance.employeeName))
            row.addView(createTextView(formattedDate))
            row.addView(createTextView(attendance.shift))
            row.addView(createTextView(attendance.time))
            row.addView(createTextView(attendance.attendance))

            // Add the row to the table
            binding.attendanceTableView.addView(row)
        }

    }


    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(8, 8, 8, 8)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
