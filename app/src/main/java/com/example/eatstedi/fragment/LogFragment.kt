package com.example.eatstedi.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eatstedi.adapter.LogActivityAdapter
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.LogByDateRequest
import com.example.eatstedi.api.service.LogByNameRequest
import com.example.eatstedi.api.service.LogResponse
import com.example.eatstedi.databinding.FragmentLogBinding
import com.example.eatstedi.model.LogActivity as ModelLogActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private lateinit var originalLogActivities: List<ModelLogActivity>
    private lateinit var adapter: LogActivityAdapter

    // Pagination parameters
    private val pageSize = 20
    private var currentPage = 0
    private var isLastPage = false
    private var isLoading = false
    private var filteredLogs = listOf<ModelLogActivity>()

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ApiService using RetrofitClient
        val apiService = RetrofitClient.getInstance(requireContext())

        // Set up RecyclerView
        adapter = LogActivityAdapter(mutableListOf())
        binding.rvLogActivity.layoutManager = LinearLayoutManager(context)
        binding.rvLogActivity.adapter = adapter

        setupPagination()
        setupSearchFilter()
        setupDateFilter()

        // Load initial data
        fetchLogs(apiService)
    }

    private fun fetchLogs(apiService: com.example.eatstedi.api.service.ApiService) {
        isLoading = true
        showLoading(true)
        apiService.getAllLogs().enqueue(object : Callback<LogResponse> {
            override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                isLoading = false
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("LogFragment", "API Response: ${response.body()}")
                    val logs = response.body()!!.data.orEmpty() // Handle null data
                    originalLogActivities = processLogData(logs)
                    filteredLogs = originalLogActivities
                    loadItems()
                    updateUiState()
                } else {
                    Log.e("LogFragment", "API Error: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Failed to fetch logs: ${response.message()}", Toast.LENGTH_SHORT).show()
                    showNoData()
                }
            }

            override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                isLoading = false
                showLoading(false)
                Log.e("LogFragment", "Network Error: ${t.message}", t)
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                showNoData()
            }
        })
    }

    private fun filterLogs(apiService: com.example.eatstedi.api.service.ApiService) {
        val query = binding.etSearch.text.toString().trim().lowercase(Locale.getDefault())

        if (query.isEmpty() && startDate == null && endDate == null) {
            filteredLogs = originalLogActivities
            resetPagination()
            loadItems()
            updateUiState()
            return
        }

        isLoading = true
        showLoading(true)
        if (query.isNotEmpty()) {
            // Search by name
            apiService.getLogByName(LogByNameRequest(query)).enqueue(object : Callback<LogResponse> {
                override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                    handleFilterResponse(response)
                }

                override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                    handleFilterError(t)
                }
            })
        } else if (startDate != null && endDate != null) {
            // Filter by date
            val start = apiDateFormat.format(startDate!!.time)
            val end = apiDateFormat.format(endDate!!.time)
            apiService.filterLogByDate(LogByDateRequest(start, end)).enqueue(object : Callback<LogResponse> {
                override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                    handleFilterResponse(response)
                }

                override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                    handleFilterError(t)
                }
            })
        }
    }

    private fun handleFilterResponse(response: Response<LogResponse>) {
        isLoading = false
        showLoading(false)
        if (response.isSuccessful && response.body()?.success == true) {
            Log.d("LogFragment", "Filter Response: ${response.body()}")
            val logs = response.body()!!.data.orEmpty() // Handle null data
            filteredLogs = processLogData(logs)
            if (filteredLogs.isEmpty()) {
                Toast.makeText(context, "No logs found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("LogFragment", "Filter Error: ${response.code()} - ${response.errorBody()?.string()}")
            filteredLogs = emptyList()
            Toast.makeText(context, "No logs found", Toast.LENGTH_SHORT).show()
        }
        resetPagination()
        loadItems()
        updateUiState()
    }

    private fun handleFilterError(t: Throwable) {
        isLoading = false
        showLoading(false)
        Log.e("LogFragment", "Filter Network Error: ${t.message}", t)
        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
        filteredLogs = emptyList()
        resetPagination()
        loadItems()
        updateUiState()
    }

    private fun processLogData(logs: List<com.example.eatstedi.api.service.LogActivity>): List<ModelLogActivity> {
        val logList = logs.mapNotNull { log ->
            try {
                if (log.createdAt == null || log.action == null) {
                    Log.w("LogFragment", "Skipping log with null createdAt or action: $log")
                    return@mapNotNull null
                }
                val date = isoFormat.parse(log.createdAt)
                if (date != null) {
                    val user = when {
                        log.idCashier != null -> fetchCashierName(log.idCashier)
                        log.idAdmin != null -> fetchAdminName(log.idAdmin)
                        else -> "Unknown"
                    }
                    Pair(
                        date, // Store date for sorting
                        ModelLogActivity(
                            user = user,
                            activity = log.action,
                            time = timeFormat.format(date),
                            date = dateFormat.format(date)
                        )
                    )
                } else {
                    Log.w("LogFragment", "Invalid date format for log: $log")
                    null
                }
            } catch (e: Exception) {
                Log.w("LogFragment", "Error parsing log: $log", e)
                null // Skip malformed entries
            }
        }
        // Sort by date in descending order (most recent first)
        return logList.sortedByDescending { it.first }.map { it.second }
    }

    private fun fetchCashierName(id: Int): String {
        // Placeholder: Implement actual API call to get cashier name
        return "Cashier $id"
    }

    private fun fetchAdminName(id: Int): String {
        // Placeholder: Implement actual API call to get admin name
        return "Admin $id"
    }

    private fun setupPagination() {
        binding.rvLogActivity.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0 && totalItemCount >= pageSize
                ) {
                    loadMoreItems()
                }
            }
        })

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                loadItems()
                updatePaginationControls()
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (!isLastPage) {
                currentPage++
                loadItems()
                updatePaginationControls()
            }
        }

        updatePaginationControls()
    }

    private fun updatePaginationControls() {
        // Perbaikan: Bungkus semua akses ke binding.
        _binding?.let { binding ->
            val totalPages = (filteredLogs.size + pageSize - 1) / pageSize
            binding.tvPageInfo.text = "Halaman ${currentPage + 1}/$totalPages"
            binding.btnPrevPage.isEnabled = currentPage > 0
            binding.btnNextPage.isEnabled = !isLastPage
            binding.btnPrevPage.alpha = if (currentPage > 0) 1.0f else 0.5f
            binding.btnNextPage.alpha = if (!isLastPage) 1.0f else 0.5f
        }
    }

    private fun loadMoreItems() {
        isLoading = true
        val startPosition = currentPage * pageSize
        if (startPosition >= filteredLogs.size) {
            isLastPage = true
            isLoading = false
            return
        }

        val endPosition = minOf(startPosition + pageSize, filteredLogs.size)
        val pageItems = filteredLogs.subList(startPosition, endPosition)
        adapter.updateData(pageItems)
        isLoading = false
        isLastPage = endPosition >= filteredLogs.size
        updatePaginationControls()
    }

    private fun loadItems() {
        // Perbaikan: Pastikan view masih ada sebelum berinteraksi dengan adapter/UI.
        _binding?.let {
            adapter.clearItems()
            val startPosition = currentPage * pageSize
            val endPosition = minOf(startPosition + pageSize, filteredLogs.size)

            if (startPosition < filteredLogs.size) {
                val pageItems = filteredLogs.subList(startPosition, endPosition)
                adapter.updateData(pageItems)
            }

            isLastPage = (currentPage + 1) * pageSize >= filteredLogs.size
            updatePaginationControls() // Panggilan ini sekarang aman
        }
    }

    private fun setupSearchFilter() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLogs(RetrofitClient.getInstance(requireContext()))
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
                filterLogs(RetrofitClient.getInstance(requireContext()))
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(isStartDate = false) { date ->
                endDate = date
                binding.btnEndDate.text = dateFormat.format(date.time)
                filterLogs(RetrofitClient.getInstance(requireContext()))
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
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

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

    private fun resetPagination() {
        currentPage = 0
        isLastPage = false
    }

    private fun showNoData() {
        // Perbaikan: Bungkus semua perubahan visibilitas.
        _binding?.let { binding ->
            binding.tvNoData.visibility = View.VISIBLE
            binding.rvLogActivity.visibility = View.GONE
            binding.paginationLayout.visibility = View.GONE
        }
    }

    private fun updateUiState() {
        // Perbaikan: Bungkus seluruh logika state UI.
        _binding?.let { binding ->
            if (filteredLogs.isEmpty()) {
                showNoData() // Panggilan ini sudah aman
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.rvLogActivity.visibility = View.VISIBLE
                binding.paginationLayout.visibility = View.VISIBLE
            }
            binding.ivClearSearch.visibility = if (binding.etSearch.text.isNotEmpty() || startDate != null || endDate != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        // Perbaikan: Amankan akses ke progress bar.
        _binding?.let { binding ->
            // Add a ProgressBar in fragment_log.xml and reference it here
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun clearFilters() {
        // Perbaikan: Bungkus akses ke view.
        _binding?.let { binding ->
            binding.etSearch.text.clear()
            startDate = null
            endDate = null
            binding.btnStartDate.text = "dd/mm/yyyy"
            binding.btnEndDate.text = "dd/mm/yyyy"
            filteredLogs = originalLogActivities
            resetPagination()
            loadItems()
            updateUiState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}