package com.example.eatstedi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatstedi.databinding.ViewItemOrderMenuBinding
import com.example.eatstedi.model.MenuItem

class OrderAdapter(private val orderList: List<MenuItem>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ViewItemOrderMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ViewItemOrderMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val menuItem = orderList[position]
        holder.binding.tvMenuName.text = menuItem.menuName
        holder.binding.tvPrice.text = menuItem.price

        Glide.with(holder.itemView.context)
            .load(menuItem.imageUrl)
            .into(holder.binding.ivMenu)
    }

    override fun getItemCount() = orderList.size
}
