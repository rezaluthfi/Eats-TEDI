package com.example.eatstedi.activity

import ScheduleAdapter
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eatstedi.R
import com.example.eatstedi.databinding.ActivityScheduleEmployeeBinding
import com.example.eatstedi.databinding.ViewModalScheduleBinding
import com.example.eatstedi.model.Schedule

class ScheduleEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityScheduleEmployeeBinding.inflate(layoutInflater)
    }

    private lateinit var scheduleAdapter: ScheduleAdapter
    private val schedules = mutableListOf<Schedule>() // Daftar untuk menyimpan jadwal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Menerima data dari Intent
        //val employeeName = intent.getStringExtra("EMPLOYEE_NAME")
        //binding.tvEmployeeName.text = employeeName // Menampilkan nama karyawan

        // Inisialisasi RecyclerView
        scheduleAdapter = ScheduleAdapter(schedules) { position ->
            removeSchedule(position) // Panggil fungsi untuk menghapus jadwal
        }
        binding.rvSchedule.apply {
            layoutManager = GridLayoutManager(this@ScheduleEmployeeActivity, 3)
            adapter = scheduleAdapter
        }

        with(binding) {
            btnAddSchedule.setOnClickListener {
                showScheduleModal()
            }

            ivArrowBack.setOnClickListener {
                finish()
            }
        }
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

        val daysAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.days_array,
            android.R.layout.simple_spinner_item
        )
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spDay.adapter = daysAdapter

        val shiftAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.shift_array_with_hours,
            android.R.layout.simple_spinner_item
        )
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spShift.adapter = shiftAdapter

        modalBinding.btnSaveSchedule.setOnClickListener {
            val selectedDay = modalBinding.spDay.selectedItem.toString()
            val selectedShift = modalBinding.spShift.selectedItem.toString()
            val newSchedule = Schedule(selectedDay, selectedShift)

            scheduleAdapter.addSchedule(newSchedule) // Tambahkan ke adapter
            dialog.dismiss()
        }

        modalBinding.btnCancelSchedule.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun removeSchedule(position: Int) {
        if (position >= 0 && position < schedules.size) { // Pastikan indeks valid
            schedules.removeAt(position)
            scheduleAdapter.notifyItemRemoved(position)
        } else {
            println("Invalid position: $position") // Tambahkan log untuk debugging
        }
    }

}
