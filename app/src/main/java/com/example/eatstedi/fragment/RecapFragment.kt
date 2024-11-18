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
import com.example.eatstedi.databinding.FragmentRecapBinding
import com.example.eatstedi.model.Transaction

class RecapFragment : Fragment() {

    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Data dummy untuk tabel rekap transaksi
        val transactions = listOf(
            Transaction("Alice", "Nasi Goreng", "Warung Makan", 15000, 2, 30000),
            Transaction("Bob", "Ayam Bakar", "Dapoer Ayu", 20000, 1, 20000),
            Transaction("Charlie", "Mie Ayam", "Bakso Pak Slamet", 18000, 3, 54000),
            Transaction("Dave", "Sate Ayam", "Sate Cak Udin", 22000, 2, 44000)
        )

        // Membuat header tabel
        val headerRow = TableRow(context)
        headerRow.setPadding(8, 16, 8, 16)
        headerRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))

        headerRow.addView(createTextView("Nama Employee", isHeader = true))
        headerRow.addView(createTextView("Menu", isHeader = true))
        headerRow.addView(createTextView("Supplier", isHeader = true))
        headerRow.addView(createTextView("Harga", isHeader = true))
        headerRow.addView(createTextView("Kuantitas", isHeader = true))
        headerRow.addView(createTextView("Total Harga", isHeader = true))

        binding.tableView.addView(headerRow)

        // Menambah data dummy ke dalam tabel
        for (transaction in transactions) {
            val row = TableRow(context)
            row.setPadding(8, 16, 8, 16)
            row.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))

            row.addView(createTextView(transaction.employeeName))
            row.addView(createTextView(transaction.menu))
            row.addView(createTextView(transaction.supplier))
            row.addView(createTextView("Rp${transaction.price}"))
            row.addView(createTextView(transaction.quantity.toString()))
            row.addView(createTextView("Rp${transaction.totalPrice}"))

            binding.tableView.addView(row)
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
