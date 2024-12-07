package com.example.eatstedi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatstedi.databinding.ViewItemOrderMenuBinding
import com.example.eatstedi.model.MenuItem

class OrderAdapter(
    private val orderList: MutableList<MenuItem>,
    private val onQuantityChange: () -> Unit // Callback untuk perubahan kuantitas
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ViewItemOrderMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ViewItemOrderMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val menuItem = orderList[position]
        holder.binding.tvMenuName.text = menuItem.menuName
        holder.binding.tvPrice.text = menuItem.price
        holder.binding.tvQuantity.text = menuItem.quantity.toString() // Tampilkan kuantitas

        Glide.with(holder.itemView.context)
            .load(menuItem.imageUrl)
            .into(holder.binding.ivMenu)

        // Listener untuk tombol minus
        holder.binding.btnMinusOrder.setOnClickListener {
            if (menuItem.quantity > 1) {
                menuItem.quantity-- // Kurangi jumlah
                holder.binding.tvQuantity.text = menuItem.quantity.toString() // Update tampilan
                onQuantityChange() // Panggil callback untuk memperbarui total pembayaran
            } else {
                // Jika kuantitas sudah 1, bisa menghapus item dari pesanan
                orderList.removeAt(position)
                notifyItemRemoved(position) // Hapus item dari adapter
                onQuantityChange() // Panggil callback untuk memperbarui total pembayaran
                notifyItemRangeChanged(position, orderList.size) // Perbarui tampilan
            }
        }

        // Listener untuk tombol plus
        holder.binding.btnPlusOrder.setOnClickListener {
            menuItem.quantity++ // Tambah jumlah
            holder.binding.tvQuantity.text = menuItem.quantity.toString() // Update tampilan
            onQuantityChange() // Panggil callback untuk memperbarui total pembayaran
        }
    }

    override fun getItemCount() = orderList.size
}


