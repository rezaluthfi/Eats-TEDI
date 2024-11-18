package com.example.eatstedi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.databinding.ViewItemMenuBinding
import com.example.eatstedi.model.MenuItem

// Add a callback interface
class MenuAdapter(
    private val menuList: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(val binding: ViewItemMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ViewItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = menuList[position]
        holder.binding.tvMenuName.text = menuItem.menuName
        holder.binding.tvOwnerName.text = menuItem.ownerName
        holder.binding.tvPrice.text = menuItem.price

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(menuItem.imageUrl)
            .into(holder.binding.ivMenu)

        // Set click listener to trigger callback
        holder.itemView.setOnClickListener {
            onItemClick(menuItem)
        }

        // Set listener for iv_more
        holder.binding.ivMore.setOnClickListener { view ->
            showPopupMenu(view, menuItem)
        }
    }

    override fun getItemCount() = menuList.size

    // Display the popup menu
    private fun showPopupMenu(view: View, menuItem: MenuItem) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.menu_item_options, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItemSelected ->
            when (menuItemSelected.itemId) {
                R.id.action_edit_menu -> {
                    // Handle edit action for this menu item
                    // Add edit logic here
                    true
                }
                R.id.action_delete_menu -> {
                    // Handle delete action for this menu item
                    // Add delete logic here
                    true
                }
                R.id.action_set_stock_menu -> {
                    // Handle share action for this menu item
                    // Add share logic here
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
}


