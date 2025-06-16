package com.example.eatstedi.activity

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eatstedi.databinding.ViewItemScheduleBinding
import com.example.eatstedi.model.Schedule

class ScheduleAdapter(
    private val schedules: MutableList<Schedule>,
    private val isAdmin: Boolean = false,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(private val binding: ViewItemScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(schedule: Schedule, position: Int) {
            // Log data untuk debugging
            Log.d("ScheduleAdapter", "Binding schedule: id=${schedule.id}, day=${schedule.day}, id_shifts=${schedule.id_shifts}, id_cashiers=${schedule.id_cashiers}")

            // Tampilkan hari dalam bahasa Indonesia
            val dayDisplayMap = mapOf(
                "MONDAY" to "Senin",
                "TUESDAY" to "Selasa",
                "WEDNESDAY" to "Rabu",
                "THURSDAY" to "Kamis",
                "FRIDAY" to "Jumat",
                "SATURDAY" to "Sabtu",
                "SUNDAY" to "Minggu"
            )
            binding.tvDay.text = dayDisplayMap[schedule.day] ?: schedule.day

            // Mapping id_shifts ke teks shift dengan waktu
            val shiftTimeMap = mapOf(
                1 to "Shift 1 (07.00 - 09.00)",
                2 to "Shift 2 (09.00 - 12.00)",
                3 to "Shift 3 (12.00 - 14.00)",
                4 to "Shift 4 (14.00 - 16.00)"
            )
            binding.tvShift.text = shiftTimeMap[schedule.id_shifts] ?: "Shift Unknown"

            // Kontrol visibilitas tombol hapus berdasarkan peran pengguna
            binding.ivDelete.visibility = if (isAdmin) View.VISIBLE else View.GONE

            // Menangani klik pada tombol hapus (hanya untuk admin)
            if (isAdmin) {
                binding.ivDelete.setOnClickListener {
                    Log.d("ScheduleAdapter", "Delete clicked for position: $adapterPosition")
                    onDelete(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ViewItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(schedules[position], position)
    }

    override fun getItemCount(): Int = schedules.size

    fun addSchedule(schedule: Schedule) {
        schedules.add(schedule)
        notifyItemInserted(schedules.size - 1)
    }
}