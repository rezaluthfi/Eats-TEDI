package com.example.eatstedi.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.databinding.ViewItemOrderMenuBinding
import com.example.eatstedi.model.MenuItem
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        // Hindari crash jika posisi tidak valid karena penghapusan item
        if (position >= orderList.size || position < 0) return

        val menuItem = orderList[position]
        with(holder.binding) {
            tvMenuName.text = menuItem.menuName
            tvPrice.text = menuItem.formattedPrice
            tvQuantity.text = menuItem.quantity.toString()

            ivMenu.setImageResource(R.drawable.ic_launcher_background) // Set default image

            // Panggil API untuk mengambil gambar menggunakan Retrofit (yang sudah punya token)
            val apiService = RetrofitClient.getInstance(holder.itemView.context)
            apiService.getMenuPhoto(menuItem.id).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    // Pastikan item view masih ada dan terikat pada adapter sebelum memanipulasi UI
                    if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return

                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val imageBytes = response.body()!!.bytes()

                            // Muat gambar dari byte array yang diterima
                            Glide.with(holder.itemView.context)
                                .load(imageBytes)
                                .placeholder(R.drawable.image_menu)
                                .error(R.drawable.ic_launcher_background)
                                .centerCrop()
                                .into(ivMenu)
                        } catch (e: Exception) {
                            Log.e("OrderAdapter", "Error reading image bytes for menu ID ${menuItem.id}", e)
                            ivMenu.setImageResource(R.drawable.ic_launcher_background)
                        }
                    } else {
                        Log.w("OrderAdapter", "Failed to load image for menu ID ${menuItem.id}. Code: ${response.code()}")
                        ivMenu.setImageResource(R.drawable.ic_launcher_background)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return
                    Log.e("OrderAdapter", "Network failure loading image for menu ID ${menuItem.id}", t)
                    ivMenu.setImageResource(R.drawable.ic_launcher_background)
                }
            })

            // Logika untuk tombol kurang
            btnMinusOrder.setOnClickListener {
                val currentPosition = holder.bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val item = orderList[currentPosition]
                if (item.quantity > 1) {
                    item.quantity--
                    notifyItemChanged(currentPosition)
                } else {
                    orderList.removeAt(currentPosition)
                    notifyItemRemoved(currentPosition)
                }
                onQuantityChange()
            }

            // Logika untuk tombol tambah
            btnPlusOrder.setOnClickListener {
                val currentPosition = holder.bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val item = orderList[currentPosition]
                item.quantity++
                notifyItemChanged(currentPosition)
                onQuantityChange()
            }
        }
    }

    override fun getItemCount() = orderList.size
}