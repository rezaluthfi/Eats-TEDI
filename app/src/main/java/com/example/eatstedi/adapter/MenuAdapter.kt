package com.example.eatstedi.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.eatstedi.R
import com.example.eatstedi.activity.ManageStockMenuActivity
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.databinding.ViewItemMenuBinding
import com.example.eatstedi.model.MenuItem
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuAdapter(
    private var menuList: MutableList<MenuItem>,
    private val userRole: String,
    private val onItemClick: (MenuItem) -> Unit,
    private val onEditMenu: (MenuItem) -> Unit,
    private val onDeleteMenu: (MenuItem) -> Unit,
    // Callback for setting stock
    private val onSetStock: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(val binding: ViewItemMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ViewItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun getItemCount(): Int = menuList.size

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = menuList[position]
        with(holder.binding) {
            tvMenuName.text = menuItem.menuName
            tvOwnerName.text = menuItem.ownerName
            tvPrice.text = menuItem.formattedPrice
            tvStock.text = "Sisa: ${menuItem.stock}"

            ivMenuImage.setImageResource(R.drawable.image_menu)

            val apiService = RetrofitClient.getInstance(holder.itemView.context)
            apiService.getMenuPhoto(menuItem.id).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (holder.absoluteAdapterPosition == RecyclerView.NO_POSITION) return

                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val imageBytes = response.body()!!.bytes()

                            Glide.with(holder.itemView.context)
                                .load(imageBytes)
                                .placeholder(R.drawable.image_menu)
                                .error(R.drawable.ic_launcher_background)
                                .centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(ivMenuImage)

                        } catch (e: Exception) {
                            Log.e("MenuAdapter", "Error reading image bytes for menu ID ${menuItem.id}", e)
                            ivMenuImage.setImageResource(R.drawable.ic_launcher_background)
                        }
                    } else {
                        Log.w("MenuAdapter", "Failed to load image for menu ID ${menuItem.id}. Code: ${response.code()}")
                        ivMenuImage.setImageResource(R.drawable.ic_launcher_background)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    if (holder.absoluteAdapterPosition == RecyclerView.NO_POSITION) return
                    Log.e("MenuAdapter", "Network failure loading image for menu ID ${menuItem.id}", t)
                    ivMenuImage.setImageResource(R.drawable.ic_launcher_background)
                }
            })

            if (userRole.equals("cashier", ignoreCase = true)) {
                root.isClickable = true
                root.setOnClickListener {
                    if (menuItem.stock > 0) {
                        onItemClick(menuItem)
                    } else {
                        Toast.makeText(it.context, "Maaf, menu sudah habis", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                root.isClickable = false
            }

            if (menuItem.stock <= 0) {
                root.alpha = 0.5f
                tvStock.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
            } else {
                root.alpha = 1.0f
                tvStock.setTextColor(root.context.getColor(android.R.color.black))
            }

            if (userRole.equals("admin", ignoreCase = true) || userRole.equals("cashier", ignoreCase = true)) {
                ivMore.visibility = View.VISIBLE
                ivMore.setOnClickListener { showPopupMenu(it, menuItem) }
            } else {
                ivMore.visibility = View.GONE
            }
        }
    }

    fun updateList(newList: List<MenuItem>) {
        menuList.clear()
        menuList.addAll(newList)
        notifyDataSetChanged()
    }

    private fun showPopupMenu(view: View, menuItem: MenuItem) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.menu_item_options, popupMenu.menu)

        when (userRole.lowercase()) {
            "admin" -> {
                popupMenu.menu.findItem(R.id.action_edit_menu).isVisible = true
                popupMenu.menu.findItem(R.id.action_delete_menu).isVisible = true
                popupMenu.menu.findItem(R.id.action_set_stock_menu).isVisible = false
            }
            "cashier" -> {
                popupMenu.menu.findItem(R.id.action_edit_menu).isVisible = false
                popupMenu.menu.findItem(R.id.action_delete_menu).isVisible = false
                popupMenu.menu.findItem(R.id.action_set_stock_menu).isVisible = true
            }
            else -> {
                popupMenu.menu.findItem(R.id.action_edit_menu).isVisible = false
                popupMenu.menu.findItem(R.id.action_delete_menu).isVisible = false
                popupMenu.menu.findItem(R.id.action_set_stock_menu).isVisible = false
            }
        }

        popupMenu.setOnMenuItemClickListener { menuItemSelected ->
            when (menuItemSelected.itemId) {
                R.id.action_edit_menu -> {
                    onEditMenu(menuItem)
                    true
                }
                R.id.action_delete_menu -> {
                    onDeleteMenu(menuItem)
                    true
                }
                R.id.action_set_stock_menu -> {
                    onSetStock(menuItem)
                    true
                }
                else -> false
            }
        }

        if (popupMenu.menu.hasVisibleItems()) {
            popupMenu.show()
        }
    }
}