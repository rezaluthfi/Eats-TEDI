package com.example.eatstedi.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eatstedi.adapter.ScheduleAdapter
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.ScheduleResponse
import com.example.eatstedi.databinding.ActivityScheduleEmployeeBinding
import com.example.eatstedi.model.Schedule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max

class ScheduleEmployeeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityScheduleEmployeeBinding.inflate(layoutInflater) }
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val schedules = mutableListOf<Schedule>()
    private var cashierId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cashierId = intent.getIntExtra("EMPLOYEE_ID", -1)
        Log.d("ScheduleEmployeeActivity", "Received EMPLOYEE_ID: $cashierId")

        if (cashierId == -1) {
            Toast.makeText(this, "ID Karyawan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inisialisasi adapter
        scheduleAdapter = ScheduleAdapter(schedules, false, 0) {
            // Cashier tidak bisa menghapus
        }

        // Set layout manager SEMENTARA
        binding.rvSchedule.layoutManager = GridLayoutManager(this, 2)
        binding.rvSchedule.adapter = scheduleAdapter

        // Panggil fungsi untuk membuat grid dinamis
        setupDynamicGrid()

        with(binding) {
            btnAddSchedule.visibility = View.GONE
            ivArrowBack.setOnClickListener {
                finish()
            }
        }

        fetchSchedules()
    }

    private fun setupDynamicGrid() {
        binding.rvSchedule.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.rvSchedule.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val recyclerViewWidth = binding.rvSchedule.width
                val idealColumnWidthDp = 360
                val idealColumnWidthPx = (idealColumnWidthDp * resources.displayMetrics.density).toInt()

                if (idealColumnWidthPx > 0 && recyclerViewWidth > 0) {
                    val spanCount = max(1, recyclerViewWidth / idealColumnWidthPx)
                    binding.rvSchedule.layoutManager = GridLayoutManager(this@ScheduleEmployeeActivity, spanCount)
                    Log.d("ScheduleActivity", "Dynamic grid setup. RVWidth: $recyclerViewWidth, SpanCount: $spanCount")
                }
            }
        })
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

}