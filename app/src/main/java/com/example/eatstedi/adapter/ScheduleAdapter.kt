package com.example.eatstedi.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eatstedi.databinding.ViewItemScheduleBinding
import com.example.eatstedi.model.Schedule

class ScheduleAdapter(
    private val schedules: MutableList<Schedule>,
    private val isAdmin: Boolean,
    private var columnWidth: Int,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(val binding: ViewItemScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(schedule: Schedule) {
            // Logika mapping hari dan shift
            val dayDisplayMap = mapOf(
                "MONDAY" to "Senin", "TUESDAY" to "Selasa", "WEDNESDAY" to "Rabu",
                "THURSDAY" to "Kamis", "FRIDAY" to "Jumat", "SATURDAY" to "Sabtu", "SUNDAY" to "Minggu"
            )
            binding.tvDay.text = dayDisplayMap[schedule.day.uppercase()] ?: schedule.day

            val shiftTimeMap = mapOf(
                1 to "Shift 1 (07.00 - 09.00)", 2 to "Shift 2 (09.00 - 12.00)",
                3 to "Shift 3 (12.00 - 14.00)", 4 to "Shift 4 (14.00 - 16.00)"
            )
            binding.tvShift.text = shiftTimeMap[schedule.id_shifts] ?: "Shift Unknown"

            binding.ivDelete.visibility = if (isAdmin) View.VISIBLE else View.GONE
            if (isAdmin) {
                binding.ivDelete.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onDelete(adapterPosition)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ViewItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        // Paksa lebar item view sesuai perhitungan dari Activity
        if (columnWidth > 0) {
            val layoutParams = holder.itemView.layoutParams
            // Hanya set jika lebarnya berbeda untuk efisiensi
            if (layoutParams.width != columnWidth) {
                layoutParams.width = columnWidth
                holder.itemView.layoutParams = layoutParams
            }
        }
        // ================================================

        holder.bind(schedules[position])
    }

    override fun getItemCount(): Int = schedules.size

    // Fungsi baru untuk mengupdate lebar kolom dari Activity
    fun setColumnWidth(width: Int) {
        // Cek jika lebarnya benar-benar berubah untuk menghindari refresh yang tidak perlu
        if (this.columnWidth != width) {
            this.columnWidth = width
            // Beri tahu RecyclerView untuk menggambar ulang semua item yang terlihat
            // dengan ukuran baru. Ini penting.
            notifyDataSetChanged()
        }
    }
}