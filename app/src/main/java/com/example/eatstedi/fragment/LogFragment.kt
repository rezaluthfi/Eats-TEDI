package com.example.eatstedi.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eatstedi.adapter.LogActivityAdapter
import com.example.eatstedi.databinding.FragmentLogBinding
import com.example.eatstedi.model.LogActivity
import java.text.SimpleDateFormat
import java.util.*

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private lateinit var originalLogActivities: List<LogActivity>
    private lateinit var adapter: LogActivityAdapter

    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Data dummy
        originalLogActivities = listOf(
            LogActivity("Reza", "update profile", "10.00"),
            LogActivity("Mira", "edit menu", "11.30"),
            LogActivity("John", "add transaction", "14.00"),
            LogActivity("Nina", "delete item", "16.15"),
            LogActivity("Rudi", "add user", "17.45"),
            LogActivity("Sari", "delete user", "19.30"),
            LogActivity("Tayo", "update profile", "20.00"),
            LogActivity("Rina", "edit menu", "21.30"),
            LogActivity("Joko", "add transaction", "22.00"),
            LogActivity("Nana", "delete item", "23.15"),
            LogActivity("Rudi", "add user", "23.45"),
            LogActivity("Sari", "delete user", "23.30"),
            LogActivity("Tayo", "update profile", "23.00"),
            LogActivity("Rina", "edit menu", "23.30"),
            LogActivity("Joko", "add transaction", "23.00"),
            LogActivity("Nana", "delete item", "23.15"),
            LogActivity("Rudi", "add user", "23.45"),
            LogActivity("Sari", "delete user", "23.30"),
        )

        // Set up RecyclerView
        adapter = LogActivityAdapter(originalLogActivities.toMutableList())
        binding.rvLogActivity.layoutManager = LinearLayoutManager(context)
        binding.rvLogActivity.adapter = adapter

        setupSearchFilter()
        setupDateFilter()
    }

    private fun setupSearchFilter() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLogs()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivClearSearch.setOnClickListener {
            clearFilters()
        }
    }

    private fun setupDateFilter() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        binding.btnStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                binding.btnStartDate.text = dateFormat.format(date)
                filterLogs()
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                binding.btnEndDate.text = dateFormat.format(date)
                filterLogs()
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                onDateSelected(selectedDate.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterLogs() {
        val query = binding.etSearch.text.toString().lowercase(Locale.getDefault())
        val filteredLogs = originalLogActivities.filter { log ->
            val matchesQuery = log.user.lowercase(Locale.getDefault()).contains(query) ||
                    log.activity.lowercase(Locale.getDefault()).contains(query)

            val matchesDate = if (startDate != null || endDate != null) {
                val logDate = SimpleDateFormat("HH.mm", Locale.getDefault()).parse(log.time)
                val afterStartDate = startDate?.let { logDate!! >= it } ?: true
                val beforeEndDate = endDate?.let { logDate!! <= it } ?: true
                afterStartDate && beforeEndDate
            } else true

            matchesQuery && matchesDate
        }

        adapter.updateData(filteredLogs)

        // Atur visibilitas TextView "tv_no_data"
        if (filteredLogs.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.rvLogActivity.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.rvLogActivity.visibility = View.VISIBLE
        }

        // Toggle Clear Search visibility
        val isFilterApplied = query.isNotEmpty() || startDate != null || endDate != null
        binding.ivClearSearch.visibility = if (isFilterApplied) View.VISIBLE else View.GONE
    }

    private fun clearFilters() {
        // Reset filters
        binding.etSearch.text.clear()
        startDate = null
        endDate = null

        // Reset date filter buttons
        binding.btnStartDate.text = "dd/mm/yyyy"
        binding.btnEndDate.text = "dd/mm/yyyy"

        // Reset data and UI
        adapter.updateData(originalLogActivities)
        binding.tvNoData.visibility = View.GONE
        binding.rvLogActivity.visibility = View.VISIBLE
        binding.ivClearSearch.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
