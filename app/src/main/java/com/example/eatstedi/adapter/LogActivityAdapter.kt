package com.example.eatstedi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eatstedi.databinding.ViewItemLogActivityBinding
import com.example.eatstedi.model.LogActivity

class LogActivityAdapter(private val logActivities: MutableList<LogActivity>) :
    RecyclerView.Adapter<LogActivityAdapter.LogActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogActivityViewHolder {
        val binding = ViewItemLogActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogActivityViewHolder, position: Int) {
        val logActivity = logActivities[position]
        holder.bind(logActivity)
    }

    override fun getItemCount(): Int = logActivities.size

    inner class LogActivityViewHolder(private val binding: ViewItemLogActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(logActivity: LogActivity) {
            binding.tvLogMessage.text = "Sistem mencatat user ${logActivity.user} ${logActivity.activity} pada pukul ${logActivity.time}"
        }
    }

    fun updateData(newLogActivities: List<LogActivity>) {
        logActivities.clear()
        logActivities.addAll(newLogActivities)
        notifyDataSetChanged()
    }
}
