package com.example.eatstedi.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eatstedi.R
import com.example.eatstedi.adapter.LogActivityAdapter
import com.example.eatstedi.databinding.FragmentLogBinding
import com.example.eatstedi.model.LogActivity

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Data dummy untuk log aktivitas
        val logActivities = listOf(
            LogActivity("Reza", "update profile", "10.00"),
            LogActivity("Mira", "edit menu", "11.30"),
            LogActivity("John", "add transaction", "14.00"),
            LogActivity("Nina", "delete item", "16.15"),
            LogActivity("Rudi", "add user", "17.45"),
            LogActivity("Sari", "delete user", "19.30"),
            LogActivity("Arga", "update profile", "20.00"),
            LogActivity("Dina", "edit menu", "21.30"),
            LogActivity("Rian", "add transaction", "22.00"),
            LogActivity("Nina", "delete item", "23.15"),
            LogActivity("Rudi", "add user", "23.45"),
            LogActivity("Sari", "delete user", "23.30"),
            LogActivity("Arga", "update profile", "23.00"),
            LogActivity("Dina", "edit menu", "23.30"),
            LogActivity("Rian", "add transaction", "23.00"),
            LogActivity("Nina", "delete item", "23.15"),
            LogActivity("Rudi", "add user", "23.45"),
            LogActivity("Sari", "delete user", "23.30"),
        )

        // Set up RecyclerView
        val adapter = LogActivityAdapter(logActivities)
        binding.rvLogActivity.layoutManager = LinearLayoutManager(context)
        binding.rvLogActivity.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
