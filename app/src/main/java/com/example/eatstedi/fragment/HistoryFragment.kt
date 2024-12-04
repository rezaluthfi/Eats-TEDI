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
            Attendance("Alice", "2024-11-01", "Shift 1", "07:00-09:00", "Hadir"),
            Attendance("Bob", "2024-11-01", "Shift 2", "09:00-12:00", "Hadir"),
            Attendance("Charlie", "2024-11-02", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Dave", "2024-11-02", "Shift 1", "07:00-09:00", "Tidak Hadir"),
            Attendance("Eve", "2024-11-03", "Shift 3", "12:00-14:00", "Tidak Hadir"),
            Attendance("Frank", "2024-11-03", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Grace", "2024-11-04", "Shift 1", "07:00-09:00", "Hadir"),
            Attendance("Hank", "2024-11-04", "Shift 3", "12:00-14:00", "Hadir"),
            Attendance("Ivy", "2024-11-05", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Jack", "2024-11-05", "Shift 1", "07:00-09:00", "Tidak Hadir"),
            Attendance("Kathy", "2024-11-06", "Shift 3", "12:00-14:00", "Tidak Hadir"),
            Attendance("Liam", "2024-11-06", "Shift 4", "14:00-16:00", "Hadir"),
            Attendance("Mia", "2024-11-07", "Shift 1", "07:00-09:00", "Hadir"),
            Attendance("Nathan", "2024-11-07", "Shift 3", "12:00-14:00", "Hadir"),
            Attendance("Olivia", "2024-11-08", "Shift 4", "14:00-16:00", "Hadir")
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
            showDatePickerDialog { date ->
                startDate = date
                binding.btnStartDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                updateActiveFilter(activeFilter, attendanceRecords)
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePickerDialog { date ->
                endDate = date
                binding.btnEndDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                updateActiveFilter(activeFilter, attendanceRecords)
            }
        }
    }

    private fun showDatePickerDialog(onDateSet: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time

                // Validasi jika startDate atau endDate sudah diisi
                if ((startDate != null && selectedDate < startDate!!) ||
                    (endDate != null && selectedDate > endDate!!)) {
                    Toast.makeText(
                        requireContext(),
                        "Tanggal tidak valid! Pastikan tanggal mulai sebelum tanggal selesai.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onDateSet(selectedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
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

    private fun filterAndDisplayData(attendanceRecords: List<Attendance>, filter: String) {
        var filteredRecords = when (filter) {
            "Hadir" -> attendanceRecords.filter { it.attendance == "Hadir" }
            "Tidak Hadir" -> attendanceRecords.filter { it.attendance == "Tidak Hadir" }
            else -> attendanceRecords
        }

        // Filter berdasarkan rentang tanggal jika ada
        if (startDate != null && endDate != null) {
            filteredRecords = filteredRecords.filter { attendance ->
                val attendanceDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(attendance.date)
                attendanceDate != null && !attendanceDate.before(startDate) && !attendanceDate.after(endDate)
            }
        }

        // Perbarui hitungan statistik
        updateFilteredCounts(filteredRecords)

        // Perbarui tabel
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

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
        for (attendance in attendanceRecords) {
            val formattedDate = try {
                val date = dateFormat.parse(attendance.date)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                attendance.date // Fallback jika format gagal
            }

            val row = TableRow(context)
            row.setPadding(8, 16, 8, 16)
            row.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))

            row.addView(createTextView(attendance.employeeName))
            row.addView(createTextView(formattedDate))
            row.addView(createTextView(attendance.shift))
            row.addView(createTextView(attendance.time))
            row.addView(createTextView(attendance.attendance))

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
