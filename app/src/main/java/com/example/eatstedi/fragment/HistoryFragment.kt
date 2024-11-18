package com.example.eatstedi.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.eatstedi.R
import com.example.eatstedi.databinding.FragmentHistoryBinding
import com.example.eatstedi.model.Attendance

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

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
            Attendance("Alice", "2024-11-01", "Pagi", "08:00 - 12:00"),
            Attendance("Bob", "2024-11-01", "Siang", "12:00 - 16:00"),
            Attendance("Charlie", "2024-11-02", "Malam", "16:00 - 20:00"),
            Attendance("Dave", "2024-11-02", "Pagi", "08:00 - 12:00"),
            Attendance("Eve", "2024-11-03", "Siang", "12:00 - 16:00"),
        )

        // Membuat header tabel
        val headerRow = TableRow(context)
        headerRow.setPadding(8, 16, 8, 16)
        headerRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))

        headerRow.addView(createTextView("Nama Employee", isHeader = true))
        headerRow.addView(createTextView("Tanggal", isHeader = true))
        headerRow.addView(createTextView("Shift", isHeader = true))
        headerRow.addView(createTextView("Waktu", isHeader = true))

        binding.attendanceTableView.addView(headerRow)

        // Menambah data dummy ke dalam tabel
        for (attendance in attendanceRecords) {
            val row = TableRow(context)
            row.setPadding(8, 16, 8, 16)
            row.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))

            row.addView(createTextView(attendance.employeeName))
            row.addView(createTextView(attendance.date))
            row.addView(createTextView(attendance.shift))
            row.addView(createTextView(attendance.time))

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
