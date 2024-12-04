package com.example.eatstedi

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityAddScheduleEmployeeBinding
import com.example.eatstedi.databinding.ViewModalScheduleBinding

class AddScheduleEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAddScheduleEmployeeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {
            btnAddSchedule.setOnClickListener {
                showScheduleModal()
            }

            ivArrowBack.setOnClickListener {
                val intent = Intent(this@AddScheduleEmployeeActivity, AllEmployeeActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showScheduleModal() {
        // Inisialisasi binding untuk modal
        val modalBinding = ViewModalScheduleBinding.inflate(layoutInflater)

        // Buat AlertDialog dengan custom view
        val dialog = AlertDialog.Builder(this)
            .setView(modalBinding.root)
            .create()

        // Atur tampilan dialog di tengah layar
        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Menambahkan adapter untuk dropdown hari
        val daysAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.days_array,
            android.R.layout.simple_spinner_item
        )
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spDay.adapter = daysAdapter

        // Menambahkan adapter untuk dropdown shift
        val shiftAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.shift_array_with_hours,
            android.R.layout.simple_spinner_item
        )
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modalBinding.spShift.adapter = shiftAdapter

        // Set listener untuk tombol simpan menggunakan binding
        modalBinding.btnSaveSchedule.setOnClickListener {
            // Logika untuk menyimpan jadwal berdasarkan pilihan hari dan shift
            val selectedDay = modalBinding.spDay.selectedItem.toString()
            val selectedShift = modalBinding.spShift.selectedItem.toString()
            // Tambahkan logika penyimpanan dengan data yang dipilih
            dialog.dismiss()
        }

        // Set listener untuk tombol batal
        modalBinding.btnCancelSchedule.setOnClickListener {
            dialog.dismiss()
        }

        // Tampilkan modal di tengah
        dialog.show()
    }
}