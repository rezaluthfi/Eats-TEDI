package com.example.eatstedi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eatstedi.databinding.ItemOnboardingBinding
import com.example.eatstedi.model.OnboardingItem

class OnboardingAdapter(private val items: List<OnboardingItem>) : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = com.example.eatstedi.databinding.ItemOnboardingBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false)
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

class OnboardingViewHolder(private val binding: com.example.eatstedi.databinding.ItemOnboardingBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
    fun bind(item: OnboardingItem) {
        binding.onboardingImage.setImageResource(item.imageRes)
        binding.onboardingTitle.text = item.title
        binding.onboardingDescription.text = item.description
    }
}