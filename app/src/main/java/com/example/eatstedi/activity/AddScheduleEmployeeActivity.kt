package com.example.eatstedi.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eatstedi.R
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

class AddScheduleEmployeeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAddScheduleEmployeeBinding.inflate(layoutInflater) }
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val schedules = mutableListOf<Schedule>()
    private var cashierId: Int = -1

    // Mapping hari dari bahasa Indonesia ke bahasa Inggris
    private val dayTranslationMap = mapOf(
        "Senin" to "MONDAY",
        "Selasa" to "TUESDAY",
        "Rabu" to "WEDNESDAY",
        "Kamis" to "THURSDAY",
        "Jumat" to "FRIDAY",
        "Sabtu" to "SATURDAY",
        "Minggu" to "SUNDAY"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ambil cashierId dari Intent
        cashierId = intent.getIntExtra("EMPLOYEE_ID", -1)
        Log.d("AddScheduleEmployeeActivity", "Received EMPLOYEE_ID: $cashierId")

        // Validasi cashierId
        if (cashierId == -1) {
            Toast.makeText(this, "ID Karyawan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Periksa peran pengguna
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = sharedPreferences.getString("user_role", "cashier") ?: "cashier"
        val isAdmin = userRole == "admin"

        // Hanya admin yang dapat mengakses halaman ini
        if (!isAdmin) {
            Toast.makeText(this, "Hanya admin yang dapat mengelola jadwal", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inisialisasi RecyclerView
        scheduleAdapter = ScheduleAdapter(schedules, isAdmin) { position ->
            removeSchedule(position)
        }
        binding.rvSchedule.apply {
            layoutManager = GridLayoutManager(this@AddScheduleEmployeeActivity, 3)
            adapter = scheduleAdapter
        }

        with(binding) {
            // Pastikan tombol Tambah Jadwal terlihat untuk admin
            btnAddSchedule.visibility = View.VISIBLE
            btnAddSchedule.isEnabled = true
            btnAddSchedule.setOnClickListener {
                showScheduleModal()
            }

            ivArrowBack.setOnClickListener {
                finish()
            }
        }

        // Ambil data jadwal saat aktivitas dimulai
        fetchSchedules()
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
                        // Log data untuk debugging
                        schedules.forEach { schedule ->
                            Log.d("AddScheduleEmployeeActivity", "Schedule: id=${schedule.id}, day=${schedule.day}, id_shifts=${schedule.id_shifts}, id_cashiers=${schedule.id_cashiers}")
                        }
                        scheduleAdapter.notifyDataSetChanged()
                        Log.d("AddScheduleEmployeeActivity", "Schedules fetched: ${schedules.size} entries")
                    } else {
                        Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal mengambil jadwal", Toast.LENGTH_LONG).show()
                        Log.e("AddScheduleEmployeeActivity", "Fetch schedules error: ${response.errorBody()?.string()}")
                    }
                } else {
                    Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal mengambil jadwal: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                    Log.e("AddScheduleEmployeeActivity", "Fetch schedules HTTP error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AddScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("AddScheduleEmployeeActivity", "Fetch schedules failure: ${t.message}", t)
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

        // Set up days spinner
        val daysAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.days_array,
            android.R.layout.simple_spinner_item
        )
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spDay.adapter = daysAdapter
        modalBinding.spDay.setSelection(0) // Set default ke Senin

        // Set up shift spinner
        val shiftList = listOf(
            "Shift 1 (07.00 - 09.00)",
            "Shift 2 (09.00 - 12.00)",
            "Shift 3 (12.00 - 14.00)",
            "Shift 4 (14.00 - 16.00)"
        )
        val shiftAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shiftList)
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spShift.adapter = shiftAdapter
        modalBinding.spShift.setSelection(0) // Set default ke Shift 1

        modalBinding.btnSaveSchedule.setOnClickListener {
            val selectedDay = modalBinding.spDay.selectedItem?.toString() ?: ""
            val selectedShiftPosition = modalBinding.spShift.selectedItemPosition
            val idShifts = selectedShiftPosition + 1 // Shift 1 = index 0, Shift 2 = index 1, dst.

            // Log untuk debugging
            Log.d("AddScheduleEmployeeActivity", "Selected day: $selectedDay, shift position: $selectedShiftPosition, id_shifts: $idShifts")

            // Validasi input
            if (selectedDay.isEmpty()) {
                Toast.makeText(this@AddScheduleEmployeeActivity, "Pilih hari terlebih dahulu!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (selectedShiftPosition == -1) {
                Toast.makeText(this@AddScheduleEmployeeActivity, "Pilih shift terlebih dahulu!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Terjemahkan hari ke format server
            val translatedDay = dayTranslationMap[selectedDay] ?: ""
            if (translatedDay.isEmpty()) {
                Toast.makeText(this@AddScheduleEmployeeActivity, "Hari tidak valid!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Cek duplikat jadwal
            if (schedules.any { it.day == translatedDay && it.id_shifts == idShifts }) {
                Toast.makeText(this@AddScheduleEmployeeActivity, "Jadwal untuk $selectedDay shift $idShifts sudah ada!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Log request body untuk debugging
            Log.d("AddScheduleEmployeeActivity", "Creating schedule: id_cashier=$cashierId, id_shifts=$idShifts, day=$translatedDay")

            val requestBody = CreateScheduleRequest(
                id_shifts = idShifts,
                day = translatedDay
            )

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
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        fetchSchedules()
                        Toast.makeText(this@AddScheduleEmployeeActivity, "Jadwal berhasil ditambahkan!", Toast.LENGTH_LONG).show()
                        Log.d("AddScheduleEmployeeActivity", "Schedule created: ${body.message}")
                    } else {
                        val errorMessage = when (body?.message) {
                            is String -> body.message as String
                            is Map<*, *> -> (body.message as Map<*, *>).toString()
                            else -> "Unknown error"
                        }
                        Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal menambahkan jadwal: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e("AddScheduleEmployeeActivity", "Create schedule error: $errorMessage, raw: ${response.errorBody()?.string()}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown HTTP error"
                    Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal menambahkan jadwal: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                    Log.e("AddScheduleEmployeeActivity", "Create schedule HTTP error: $errorBody")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AddScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("AddScheduleEmployeeActivity", "Create schedule failure: ${t.message}", t)
            }
        })
    }

    private fun removeSchedule(position: Int) {
        if (position >= 0 && position < schedules.size) {
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus jadwal ${schedules[position].day} Shift ${schedules[position].id_shifts}?")
                .setPositiveButton("Hapus") { _, _ ->
                    val schedule = schedules[position]
                    showLoading(true)
                    val apiService = RetrofitClient.getInstance(this)
                    apiService.deleteSchedule(schedule.id).enqueue(object : Callback<GenericResponse> {
                        override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                            showLoading(false)
                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body?.success == true) {
                                    schedules.removeAt(position)
                                    scheduleAdapter.notifyItemRemoved(position)
                                    Toast.makeText(this@AddScheduleEmployeeActivity, "Jadwal berhasil dihapus!", Toast.LENGTH_LONG).show()
                                    Log.d("AddScheduleEmployeeActivity", "Schedule deleted: ${schedule.id}")
                                } else {
                                    Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal menghapus jadwal: ${body?.message}", Toast.LENGTH_LONG).show()
                                    Log.e("AddScheduleEmployeeActivity", "Delete schedule error: ${response.errorBody()?.string()}")
                                }
                            } else {
                                Toast.makeText(this@AddScheduleEmployeeActivity, "Gagal menghapus jadwal: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                                Log.e("AddScheduleEmployeeActivity", "Delete schedule HTTP error: ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                            showLoading(false)
                            Toast.makeText(this@AddScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                            Log.e("AddScheduleEmployeeActivity", "Delete schedule failure: ${t.message}", t)
                        }
                    })
                }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            Log.e("AddScheduleEmployeeActivity", "Invalid position: $position")
            Toast.makeText(this@AddScheduleEmployeeActivity, "Posisi jadwal tidak valid!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}