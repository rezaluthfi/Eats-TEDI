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
import com.example.eatstedi.api.service.AdminProfileResponse
import com.example.eatstedi.api.service.ApiService
import com.example.eatstedi.api.service.CashierResponse
import com.example.eatstedi.api.service.LogByDateRequest
import com.example.eatstedi.api.service.LogByNameRequest
import com.example.eatstedi.api.service.LogResponse
import com.example.eatstedi.databinding.FragmentLogBinding
import com.example.eatstedi.model.LogActivity as ModelLogActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    // Akses aman untuk menghindari NPE, hanya digunakan saat view pasti ada.
    private val binding get() = _binding!!

    private lateinit var apiService: ApiService
    private lateinit var adapter: LogActivityAdapter
    private var originalLogActivities: List<ModelLogActivity> = emptyList()
    private var filteredLogs = listOf<ModelLogActivity>()

    // Maps untuk menyimpan nama kasir dan admin berdasarkan ID
    private val cashierNameMap = mutableMapOf<Int, String>()
    private val adminNameMap = mutableMapOf<Int, String>()

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Pagination parameters
    private val pageSize = 20
    private var currentPage = 0
    private var isLastPage = false
    private var isLoading = false

    // --- Lifecycle Methods ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apiService = RetrofitClient.getInstance(requireContext())
        setupRecyclerView()
        setupUI()
        showShimmer()
        fetchAllUserDataThenLogs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Shimmer Control ---

    private fun showShimmer() {
        // Menggunakan _binding?.let memastikan tidak ada akses jika view sudah hancur.
        _binding?.let {
            it.shimmerRecycler.visibility = View.VISIBLE
            it.shimmerPagination.visibility = View.VISIBLE
            it.contentContainer.visibility = View.GONE
            it.paginationLayout.visibility = View.GONE
            it.tvNoData.visibility = View.GONE
            it.shimmerRecycler.startShimmer()
            it.shimmerPagination.startShimmer()
        }
    }

    private fun hideShimmer() {
        // Perbaikan utama: Menggunakan _binding?.let untuk semua operasi UI.
        _binding?.let {
            it.shimmerRecycler.stopShimmer()
            it.shimmerPagination.stopShimmer()
            it.shimmerRecycler.visibility = View.GONE
            it.shimmerPagination.visibility = View.GONE
            it.contentContainer.visibility = View.VISIBLE
        }
    }

    // --- UI Setup ---

    private fun setupUI() {
        setupSearchFilter()
        setupDateFilter()
        setupPagination()
    }

    private fun setupRecyclerView() {
        adapter = LogActivityAdapter(mutableListOf())
        _binding?.rvLogActivity?.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
    }

    private fun setupSearchFilter() {
        _binding?.etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLogs()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        _binding?.ivClearSearch?.setOnClickListener { clearFilters() }
    }

    private fun setupDateFilter() {
        _binding?.let { binding ->
            binding.btnStartDate.setOnClickListener {
                showDatePicker(isStartDate = true) { date ->
                    startDate = date
                    // Cek _binding lagi sebelum akses
                    _binding?.btnStartDate?.text = dateFormat.format(date.time)
                    filterLogs()
                }
            }
            binding.btnEndDate.setOnClickListener {
                showDatePicker(isStartDate = false) { date ->
                    endDate = date
                    _binding?.btnEndDate?.text = dateFormat.format(date.time)
                    filterLogs()
                }
            }
        }
    }

    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Calendar) -> Unit) {
        if (!isAdded) return // Guard
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
                if (isStartDate && endDate != null && selectedDate.after(endDate)) {
                    showToast("Tanggal mulai tidak boleh setelah tanggal selesai!")
                    return@DatePickerDialog
                }
                if (!isStartDate && startDate != null && selectedDate.before(startDate)) {
                    showToast("Tanggal selesai tidak boleh sebelum tanggal mulai!")
                    return@DatePickerDialog
                }
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // --- Data Fetching ---

    private fun fetchAllUserDataThenLogs() {
        // Ambil data Admin
        apiService.getAdminProfile().enqueue(object : Callback<AdminProfileResponse> {
            override fun onResponse(call: Call<AdminProfileResponse>, response: Response<AdminProfileResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val admin = response.body()?.data
                    if (admin != null) {
                        adminNameMap[admin.id] = admin.name
                    }
                }
                // Setelah admin selesai, ambil data Kasir
                fetchCashierData()
            }

            override fun onFailure(call: Call<AdminProfileResponse>, t: Throwable) {
                Log.e("LogFragment", "Gagal mengambil profil admin", t)
                // Tetap lanjutkan ke pengambilan data kasir
                fetchCashierData()
            }
        })
    }

    private fun fetchCashierData() {
        apiService.getCashiers().enqueue(object : Callback<CashierResponse> {
            override fun onResponse(call: Call<CashierResponse>, response: Response<CashierResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.forEach { cashier ->
                        cashierNameMap[cashier.id] = cashier.name
                    }
                }
                // Setelah semua data pengguna terkumpul, baru ambil data log
                fetchLogs()
            }

            override fun onFailure(call: Call<CashierResponse>, t: Throwable) {
                Log.e("LogFragment", "Gagal mengambil data kasir", t)
                // Tetap lanjutkan ke pengambilan log, meskipun mungkin hanya nama admin yang tampil
                fetchLogs()
            }
        })
    }

    private fun fetchLogs() {
        if (isLoading) return
        isLoading = true
        showShimmer()
        apiService.getAllLogs().enqueue(object : Callback<LogResponse> {
            override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                // Perbaikan: Pindahkan semua logika UI ke dalam blok aman
                _binding?.let {
                    isLoading = false
                    hideShimmer()
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        originalLogActivities = processLogData(body.data.orEmpty())
                        filteredLogs = originalLogActivities
                        resetAndLoadItems()
                    } else {
                        showToast("Gagal mengambil log: ${response.message()}")
                        showNoData()
                    }
                }
            }

            override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                // Perbaikan: Pindahkan semua logika UI ke dalam blok aman
                _binding?.let {
                    isLoading = false
                    hideShimmer()
                    showToast("Error jaringan: ${t.message}")
                    showNoData()
                }
            }
        })
    }

    private fun filterLogs() {
        // Akses binding di awal dan simpan dalam variabel lokal
        val query = _binding?.etSearch?.text.toString().trim().lowercase(Locale.getDefault())

        if (query.isNullOrEmpty() && startDate == null && endDate == null) {
            filteredLogs = originalLogActivities
            resetAndLoadItems()
            return
        }

        if (isLoading) return
        isLoading = true
        showShimmer()

        val callback = object : Callback<LogResponse> {
            override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                // Cek _binding di sini, karena ini adalah titik masuk utama dari callback
                _binding?.let { handleFilterResponse(response) }
            }
            override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                _binding?.let { handleFilterError(t) }
            }
        }

        when {
            !query.isNullOrEmpty() -> apiService.getLogByName(LogByNameRequest(query)).enqueue(callback)
            startDate != null && endDate != null -> {
                val start = apiDateFormat.format(startDate!!.time)
                val end = apiDateFormat.format(endDate!!.time)
                apiService.filterLogByDate(LogByDateRequest(start, end)).enqueue(callback)
            }
            else -> {
                isLoading = false
                hideShimmer()
                filteredLogs = originalLogActivities
                resetAndLoadItems()
            }
        }
    }

    private fun handleFilterResponse(response: Response<LogResponse>) {
        isLoading = false
        hideShimmer()
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            filteredLogs = processLogData(body.data.orEmpty())
            if (filteredLogs.isEmpty()) showToast("Tidak ada log ditemukan")
        } else {
            filteredLogs = emptyList()
            showToast("Tidak ada log ditemukan")
        }
        resetAndLoadItems()
    }

    private fun handleFilterError(t: Throwable) {
        isLoading = false
        hideShimmer()
        showToast("Error jaringan: ${t.message}")
        filteredLogs = emptyList()
        resetAndLoadItems()
    }

    private fun processLogData(logs: List<com.example.eatstedi.api.service.LogActivity>): List<ModelLogActivity> {
        return logs.mapNotNull { log ->
            try {
                if (log.createdAt == null || log.action == null) return@mapNotNull null
                val date = isoFormat.parse(log.createdAt) ?: return@mapNotNull null

                val user = when {
                    log.idCashier != null -> {
                        // Cari nama kasir di map, jika tidak ada, tampilkan ID
                        cashierNameMap[log.idCashier] ?: "Kasir (ID: ${log.idCashier})"
                    }
                    log.idAdmin != null -> {
                        // Cari nama admin di map, jika tidak ada, tampilkan "Admin"
                        adminNameMap[log.idAdmin] ?: "Admin"
                    }
                    else -> "Pengguna Tidak Dikenal"
                }

                Pair(
                    date,
                    ModelLogActivity(
                        user = user,
                        activity = log.action,
                        time = timeFormat.format(date),
                        date = dateFormat.format(date)
                    )
                )
            } catch (e: Exception) {
                Log.e("LogFragment", "Error parsing log: $log", e)
                null
            }
        }.sortedByDescending { it.first }.map { it.second }
    }

    // --- Pagination ---

    private fun setupPagination() {
        _binding?.let { binding ->
            binding.rvLogActivity.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && !isLastPage && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0 && totalItemCount >= pageSize) {
                        loadMoreItems()
                    }
                }
            })

            binding.btnPrevPage.setOnClickListener {
                if (currentPage > 0) {
                    currentPage--
                    loadItems()
                }
            }
            binding.btnNextPage.setOnClickListener {
                if (!isLastPage) {
                    currentPage++
                    loadItems()
                }
            }
        }
        updatePaginationControls()
    }

    private fun updatePaginationControls() {
        _binding?.let {
            val totalPages = if (filteredLogs.isEmpty()) 1 else (filteredLogs.size + pageSize - 1) / pageSize
            it.tvPageInfo.text = "Halaman ${currentPage + 1}/$totalPages"
            it.btnPrevPage.isEnabled = currentPage > 0
            it.btnNextPage.isEnabled = !isLastPage
            it.btnPrevPage.alpha = if (it.btnPrevPage.isEnabled) 1.0f else 0.5f
            it.btnNextPage.alpha = if (it.btnNextPage.isEnabled) 1.0f else 0.5f
        }
    }

    private fun loadMoreItems() {
        currentPage++
        loadItems(true)
    }

    private fun loadItems(isAppending: Boolean = false) {
        if (!isAppending) adapter.clearItems()

        val startPosition = currentPage * pageSize
        if (startPosition >= filteredLogs.size) {
            isLastPage = true
            updatePaginationControls()
            return
        }

        val endPosition = minOf(startPosition + pageSize, filteredLogs.size)
        val pageItems = filteredLogs.subList(startPosition, endPosition)

        if (isAppending) adapter.addItems(pageItems) else adapter.updateData(pageItems)

        isLastPage = endPosition >= filteredLogs.size
        updatePaginationControls()
    }

    private fun resetAndLoadItems() {
        currentPage = 0
        isLastPage = false
        loadItems()
        updateUiState()
    }

    // --- UI State Management ---

    private fun updateUiState() {
        _binding?.let {
            val isEmpty = filteredLogs.isEmpty()
            it.tvNoData.visibility = if (isEmpty) View.VISIBLE else View.GONE
            it.rvLogActivity.visibility = if (isEmpty) View.GONE else View.VISIBLE
            it.paginationLayout.visibility = if (isEmpty) View.GONE else View.VISIBLE

            val isFilterActive = it.etSearch.text.isNotEmpty() || startDate != null || endDate != null
            it.ivClearSearch.visibility = if (isFilterActive) View.VISIBLE else View.GONE
        }
    }

    private fun showNoData() {
        _binding?.let {
            it.tvNoData.visibility = View.VISIBLE
            it.rvLogActivity.visibility = View.GONE
            it.paginationLayout.visibility = View.GONE
        }
    }

    // --- Filters ---

    private fun clearFilters() {
        _binding?.let {
            it.etSearch.text.clear()
            startDate = null
            endDate = null
            it.btnStartDate.text = "dd/mm/yyyy"
            it.btnEndDate.text = "dd/mm/yyyy"
            filteredLogs = originalLogActivities
            resetAndLoadItems()
        }
    }

    // --- Utility ---

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (isAdded) { // Guard
            Toast.makeText(context, message, duration).show()
        }
    }
}