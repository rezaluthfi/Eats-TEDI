package com.example.eatstedi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.databinding.ViewItemOrderMenuBinding
import com.example.eatstedi.model.MenuItem

class OrderAdapter(
    private val orderList: MutableList<MenuItem>,
    private val onQuantityChange: () -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ViewItemOrderMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ViewItemOrderMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val menuItem = orderList[position]
        with(holder.binding) {
            tvMenuName.text = menuItem.menuName
            tvPrice.text = menuItem.formattedPrice
            tvQuantity.text = menuItem.quantity.toString()

            Glide.with(holder.itemView.context)
                .load(menuItem.imageUrl)
                .placeholder(R.drawable.image_menu)
                .error(R.drawable.ic_launcher_background)
                .into(ivMenu)

            btnMinusOrder.setOnClickListener {
                if (menuItem.quantity > 1) {
                    menuItem.quantity--
                    tvQuantity.text = menuItem.quantity.toString()
                    onQuantityChange()
                } else {
                    orderList.removeAt(position)
                    notifyItemRemoved(position)
                    onQuantityChange()
                    notifyItemRangeChanged(position, orderList.size)
                }
            }

            btnPlusOrder.setOnClickListener {
                menuItem.quantity++
                tvQuantity.text = menuItem.quantity.toString()
                onQuantityChange()
            }
        }
    }

    override fun getItemCount() = orderList.size
}