package com.example.eatstedi.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.ScheduleResponse
import com.example.eatstedi.databinding.ActivityScheduleEmployeeBinding
import com.example.eatstedi.model.Schedule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScheduleEmployeeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityScheduleEmployeeBinding.inflate(layoutInflater) }
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val schedules = mutableListOf<Schedule>()
    private var cashierId: Int = -1

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
        Log.d("ScheduleEmployeeActivity", "Received EMPLOYEE_ID: $cashierId")

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

        // Inisialisasi RecyclerView
        scheduleAdapter = ScheduleAdapter(schedules, isAdmin) { position ->
            removeSchedule(position)
        }
        binding.rvSchedule.apply {
            layoutManager = GridLayoutManager(this@ScheduleEmployeeActivity, 3)
            adapter = scheduleAdapter
        }

        with(binding) {
            // Sembunyikan tombol tambah jadwal untuk semua pengguna
            btnAddSchedule.visibility = View.GONE
            btnAddSchedule.isEnabled = false

            ivArrowBack.setOnClickListener {
                finish()
            }
        }

        // Ambil data jadwal saat aktivitas dimulai
        fetchSchedules()
    }

    private fun fetchSchedules() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getScheduleByIdCashier(cashierId).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        schedules.clear()
                        schedules.addAll(body.data)
                        // Log data untuk debugging
                        schedules.forEach { schedule ->
                            Log.d("ScheduleEmployeeActivity", "Schedule: id=${schedule.id}, day=${schedule.day}, id_shifts=${schedule.id_shifts}, id_cashiers=${schedule.id_cashiers}")
                        }
                        scheduleAdapter.notifyDataSetChanged()
                        Log.d("ScheduleEmployeeActivity", "Schedules fetched: ${schedules.size} entries")
                    } else {
                        Toast.makeText(this@ScheduleEmployeeActivity, "Gagal mengambil jadwal", Toast.LENGTH_LONG).show()
                        Log.e("ScheduleEmployeeActivity", "Fetch schedules error: ${response.errorBody()?.string()}")
                    }
                } else {
                    Toast.makeText(this@ScheduleEmployeeActivity, "Gagal mengambil jadwal: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                    Log.e("ScheduleEmployeeActivity", "Fetch schedules HTTP error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                Toast.makeText(this@ScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("ScheduleEmployeeActivity", "Fetch schedules failure: ${t.message}", t)
            }
        })
    }

    private fun removeSchedule(position: Int) {
        if (position >= 0 && position < schedules.size) {
            val schedule = schedules[position]
            val apiService = RetrofitClient.getInstance(this)
            apiService.deleteSchedule(schedule.id).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            schedules.removeAt(position)
                            scheduleAdapter.notifyItemRemoved(position)
                            Toast.makeText(this@ScheduleEmployeeActivity, "Jadwal berhasil dihapus", Toast.LENGTH_SHORT).show()
                            Log.d("ScheduleEmployeeActivity", "Schedule deleted: ${schedule.id}")
                        } else {
                            Toast.makeText(this@ScheduleEmployeeActivity, "Gagal menghapus jadwal: ${body?.message}", Toast.LENGTH_LONG).show()
                            Log.e("ScheduleEmployeeActivity", "Delete schedule error: ${response.errorBody()?.string()}")
                        }
                    } else {
                        Toast.makeText(this@ScheduleEmployeeActivity, "Gagal menghapus jadwal: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                        Log.e("ScheduleEmployeeActivity", "Delete schedule HTTP error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@ScheduleEmployeeActivity, "Error jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("ScheduleEmployeeActivity", "Delete schedule failure: ${t.message}", t)
                }
            })
        } else {
            Log.e("ScheduleEmployeeActivity", "Invalid position: $position")
            Toast.makeText(this@ScheduleEmployeeActivity, "Posisi jadwal tidak valid!", Toast.LENGTH_LONG).show()
        }
    }
}