package com.example.eatstedi.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eatstedi.R
import com.example.eatstedi.adapter.ScheduleAdapter
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.CreateScheduleRequest
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.ScheduleResponse
import com.example.eatstedi.databinding.ActivityAddScheduleEmployeeBinding
import com.example.eatstedi.databinding.ViewModalScheduleBinding
import com.example.eatstedi.model.Schedule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max

class AddScheduleEmployeeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAddScheduleEmployeeBinding.inflate(layoutInflater) }
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val schedules = mutableListOf<Schedule>()
    private var cashierId: Int = -1

    private val dayTranslationMap = mapOf(
        "Senin" to "MONDAY", "Selasa" to "TUESDAY", "Rabu" to "WEDNESDAY",
        "Kamis" to "THURSDAY", "Jumat" to "FRIDAY", "Sabtu" to "SATURDAY", "Minggu" to "SUNDAY"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cashierId = intent.getIntExtra("EMPLOYEE_ID", -1)
        Log.d("AddScheduleEmployeeActivity", "Received EMPLOYEE_ID: $cashierId")

        if (cashierId == -1) {
            Toast.makeText(this, "ID Karyawan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = sharedPreferences.getString("user_role", null)
        if (userRole != "admin") {
            Toast.makeText(this, "Hanya admin yang dapat mengelola jadwal", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inisialisasi adapter
        scheduleAdapter = ScheduleAdapter(schedules, true, 0) { position ->
            removeSchedule(position)
        }

        // Set layout manager SEMENTARA untuk mencegah error
        binding.rvSchedule.layoutManager = GridLayoutManager(this, 2)
        binding.rvSchedule.adapter = scheduleAdapter

        // Panggil fungsi untuk membuat grid dinamis
        setupDynamicGrid()

        with(binding) {
            btnAddSchedule.visibility = View.VISIBLE
            btnAddSchedule.setOnClickListener {
                showScheduleModal()
            }
            ivArrowBack.setOnClickListener {
                finish()
            }
        }

        fetchSchedules()
    }

    private fun setupDynamicGrid() {
        binding.rvSchedule.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Hapus listener agar tidak berjalan berulang kali
                binding.rvSchedule.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val recyclerViewWidth = binding.rvSchedule.width

                // Lebar ideal per kolom dalam DP. Angka ini sedikit lebih besar dari
                // lebar item untuk memberikan sedikit ruang
                val idealColumnWidthDp = 360
                val idealColumnWidthPx = (idealColumnWidthDp * resources.displayMetrics.density).toInt()

                if (idealColumnWidthPx > 0 && recyclerViewWidth > 0) {
                    // Hitung jumlah kolom yang muat, minimal 1
                    val spanCount = max(1, recyclerViewWidth / idealColumnWidthPx)

                    // Terapkan LayoutManager baru dengan span count yang sudah dihitung
                    binding.rvSchedule.layoutManager = GridLayoutManager(this@AddScheduleEmployeeActivity, spanCount)

                    Log.d("AddScheduleActivity", "Dynamic grid setup. RVWidth: $recyclerViewWidth, SpanCount: $spanCount")
                }
            }
        })
    }

    private fun fetchSchedules() {
        showLoading(true)
        val apiService = RetrofitClient.getInstance(this)
        apiService.getScheduleByIdCashier(cashierId).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        schedules.clear()
                        schedules.addAll(body.data)
                        scheduleAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal mengambil jadwal", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal mengambil jadwal: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AddScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("AddScheduleEmployeeActivity", "Fetch schedules failure", t)
            }
        })
    }

    private fun showScheduleModal() {
        val modalBinding = ViewModalScheduleBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(modalBinding.root)
            .create()

        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val daysAdapter = ArrayAdapter.createFromResource(this, R.array.days_array, android.R.layout.simple_spinner_item)
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spDay.adapter = daysAdapter

        val shiftList = listOf(
            "Shift 1 (07.00 - 09.00)", "Shift 2 (09.00 - 12.00)",
            "Shift 3 (12.00 - 14.00)", "Shift 4 (14.00 - 16.00)"
        )
        val shiftAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shiftList)
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spShift.adapter = shiftAdapter

        modalBinding.btnSaveSchedule.setOnClickListener {
            val selectedDay = modalBinding.spDay.selectedItem?.toString() ?: ""
            val idShifts = modalBinding.spShift.selectedItemPosition + 1
            val translatedDay = dayTranslationMap[selectedDay] ?: ""

            if (translatedDay.isEmpty()) {
                Toast.makeText(this, "Hari tidak valid!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (schedules.any { it.day.equals(translatedDay, ignoreCase = true) && it.id_shifts == idShifts }) {
                Toast.makeText(this, "Jadwal untuk $selectedDay shift $idShifts sudah ada!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val requestBody = CreateScheduleRequest(id_shifts = idShifts, day = translatedDay)
            createSchedule(cashierId, requestBody)
            dialog.dismiss()
        }

        modalBinding.btnCancelSchedule.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createSchedule(idCashier: Int, requestBody: CreateScheduleRequest) {
        showLoading(true)
        val apiService = RetrofitClient.getInstance(this)
        apiService.createSchedule(idCashier, requestBody).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchSchedules()
                    Toast.makeText(this@AddScheduleEmployeeActivity, "Jadwal berhasil ditambahkan!", Toast.LENGTH_LONG).show()
                } else {
                    val errorMessage = response.body()?.message?.toString() ?: "Gagal menambahkan jadwal"
                    Toast.makeText(this@AddScheduleEmployeeActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AddScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun removeSchedule(position: Int) {
        if (position in schedules.indices) {
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi Hapus")
                .setMessage("Yakin ingin menghapus jadwal ${schedules[position].day} Shift ${schedules[position].id_shifts}?")
                .setPositiveButton("Hapus") { _, _ ->
                    val scheduleId = schedules[position].id
                    showLoading(true)
                    val apiService = RetrofitClient.getInstance(this)
                    apiService.deleteSchedule(scheduleId).enqueue(object : Callback<GenericResponse> {
                        override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                            showLoading(false)
                            if (response.isSuccessful && response.body()?.success == true) {
                                fetchSchedules()
                                Toast.makeText(this@AddScheduleEmployeeActivity, "Jadwal berhasil dihapus!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal menghapus jadwal: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                            showLoading(false)
                            Toast.makeText(this@AddScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                    })
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}