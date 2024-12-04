package com.example.eatstedi.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
            LogActivity("Banu", "delete user", "16.00", "13/12/2024"),
            LogActivity("Banu", "add user", "15.58", "13/12/2024"),
            LogActivity("Banu", "delete user", "15.56", "13/12/2024"),
            LogActivity("Rudi", "add user", "13.10", "13/12/2024"),
            LogActivity("Nina", "delete item", "12.48", "12/12/2024"),
            LogActivity("Reza", "update profile", "11.42", "11/12/2024"),
            LogActivity("Tayo", "edit menu", "12.12", "10/12/2024"),
            LogActivity("John", "add transaction", "11.52", "10/12/2024"),
            LogActivity("Sari", "delete user", "16.40", "08/12/2024"),
            LogActivity("Rudi", "add user", "15.50", "07/12/2024"),
            LogActivity("Sari", "delete user", "15.40", "06/12/2024"),
            LogActivity("Rudi", "add user", "14.50", "05/12/2024"),
            LogActivity("Nina", "delete item", "14.21", "04/12/2024"),
            LogActivity("John", "add transaction", "12.20", "03/12/2024"),
            LogActivity("Mira", "edit menu", "11.12", "02/12/2024"),
            LogActivity("Reza", "edit menu", "13.42", "01/12/2024"),
            LogActivity("Reza", "update profile", "10.20", "01/12/2024")
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
        binding.btnStartDate.setOnClickListener {
            showDatePicker(isStartDate = true) { date ->
                startDate = date
                binding.btnStartDate.text = dateFormat.format(date.time)
                filterLogs()
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(isStartDate = false) { date ->
                endDate = date
                binding.btnEndDate.text = dateFormat.format(date.time)
                filterLogs()
            }
        }
    }

    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }

                // Validasi jika tanggal mulai lebih besar dari tanggal selesai atau sebaliknya
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

    // Fungsi untuk menghapus waktu dan hanya menyimpan tanggal
    private fun Calendar.stripTime(): Calendar {
        this.set(Calendar.HOUR_OF_DAY, 0)
        this.set(Calendar.MINUTE, 0)
        this.set(Calendar.SECOND, 0)
        this.set(Calendar.MILLISECOND, 0)
        return this
    }

    // Filter untuk log activity berdasarkan query pencarian dan rentang tanggal
    private fun filterLogs() {
        val query = binding.etSearch.text.toString().lowercase(Locale.getDefault())
        val filteredLogs = originalLogActivities.filter { log ->
            // Memeriksa kecocokan query pencarian dengan pengguna atau aktivitas
            val matchesQuery = log.user.lowercase(Locale.getDefault()).contains(query) ||
                    log.activity.lowercase(Locale.getDefault()).contains(query)

            // Memeriksa apakah log berada dalam rentang tanggal yang ditentukan
            val matchesDate = if (startDate != null && endDate != null) {
                val logDate = Calendar.getInstance().apply {
                    time = dateFormat.parse(log.date)!!
                }.stripTime() // Menghapus waktu pada tanggal log

                val strippedStartDate = startDate?.stripTime()
                val strippedEndDate = endDate?.stripTime()

                !logDate.before(strippedStartDate) && !logDate.after(strippedEndDate)
            } else {
                true
            }

            matchesQuery && matchesDate
        }

        // Update data pada adapter
        adapter.updateData(filteredLogs)

        // Menyembunyikan atau menampilkan pesan jika tidak ada data yang sesuai
        if (filteredLogs.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.rvLogActivity.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.rvLogActivity.visibility = View.VISIBLE
        }

        // Menampilkan ikon clear search jika ada filter yang aktif
        binding.ivClearSearch.visibility = if (query.isNotEmpty() || startDate != null || endDate != null) View.VISIBLE else View.GONE
    }

    // Membersihkan filter pencarian dan tanggal
    private fun clearFilters() {
        binding.etSearch.text.clear()
        startDate = null
        endDate = null

        binding.btnStartDate.text = "dd/mm/yyyy" // Teks default untuk start date
        binding.btnEndDate.text = "dd/mm/yyyy" // Teks default untuk end date

        // Mengupdate data dengan log activities asli
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
