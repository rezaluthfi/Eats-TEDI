package com.example.eatstedi.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.eatstedi.R
import com.example.eatstedi.activity.ManageStockMenuActivity
import com.example.eatstedi.databinding.ViewItemMenuBinding
import com.example.eatstedi.model.MenuItem
import android.util.Log
import android.widget.Toast

class MenuAdapter(
    private var menuList: List<MenuItem>,
    private val userRole: String,
    private val onItemClick: (MenuItem) -> Unit,
    private val onEditMenu: (MenuItem) -> Unit,
    private val onDeleteMenu: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private var filteredMenuList: List<MenuItem> = menuList

    inner class MenuViewHolder(val binding: ViewItemMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ViewItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = filteredMenuList[position]
        with(holder.binding) {
            Log.d("MenuAdapter", "Binding menu: ID=${menuItem.id}, Name=${menuItem.menuName}, Supplier=${menuItem.ownerName}, Stock=${menuItem.stock}, ImageUrl=${menuItem.imageUrl}")
            tvMenuName.text = menuItem.menuName
            tvOwnerName.text = menuItem.ownerName
            tvPrice.text = menuItem.formattedPrice
            tvStock.text = "Sisa: ${menuItem.stock}"

            Glide.with(holder.itemView.context)
                .load(menuItem.imageUrl)
                .apply(RequestOptions()
                    .placeholder(R.drawable.image_menu)
                    .error(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                .circleCrop()
                .into(ivMenuImage)

            // Hanya izinkan klik untuk menambah ke order jika role adalah cashier
            if (userRole.lowercase() == "cashier") {
                root.setOnClickListener {
                    if (menuItem.stock > 0) {
                        Log.d("MenuAdapter", "Clicked menu: ID=${menuItem.id}, Name=${menuItem.menuName}, Stock=${menuItem.stock}")
                        onItemClick(menuItem)
                    } else {
                        Toast.makeText(
                            it.context,
                            "Maaf, menu sudah habis",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                // Nonaktifkan klik untuk Admin
                root.isClickable = false
                root.isFocusable = false
                Log.d("MenuAdapter", "Click disabled for menu: ID=${menuItem.id}, Role=$userRole")
            }

            if (menuItem.stock <= 0) {
                root.alpha = 0.5f
                tvStock.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
            } else {
                root.alpha = 1.0f
                tvStock.setTextColor(root.context.getColor(android.R.color.black))
            }

            ivMore.setOnClickListener { showPopupMenu(it, menuItem) }
        }
    }

    override fun getItemCount() = filteredMenuList.size

    fun updateList(newList: List<MenuItem>) {
        menuList = newList
        filteredMenuList = newList
        notifyDataSetChanged()
    }

    fun filterMenus(query: String) {
        filteredMenuList = menuList.filter { it.menuName.contains(query, ignoreCase = true) }
        notifyDataSetChanged()
    }

    private fun showPopupMenu(view: View, menuItem: MenuItem) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.menu_item_options, popupMenu.menu)

        // Adjust menu visibility based on user role
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
                Log.w("MenuAdapter", "Unknown user role: $userRole, hiding all options")
                popupMenu.menu.findItem(R.id.action_edit_menu).isVisible = false
                popupMenu.menu.findItem(R.id.action_delete_menu).isVisible = false
                popupMenu.menu.findItem(R.id.action_set_stock_menu).isVisible = false
            }
        }

        popupMenu.setOnMenuItemClickListener { menuItemSelected ->
            when (menuItemSelected.itemId) {
                R.id.action_edit_menu -> {
                    Log.d("MenuAdapter", "Edit menu selected for: ${menuItem.menuName}")
                    onEditMenu(menuItem)
                    true
                }
                R.id.action_delete_menu -> {
                    Log.d("MenuAdapter", "Delete menu selected for: ${menuItem.menuName}")
                    onDeleteMenu(menuItem)
                    true
                }
                R.id.action_set_stock_menu -> {
                    Log.d("MenuAdapter", "Set stock selected for: ${menuItem.menuName}")
                    val context = view.context
                    val intent = Intent(context, ManageStockMenuActivity::class.java).apply {
                        putExtra("MENU_ITEM_ID", menuItem.id)
                        putExtra("MENU_ITEM_NAME", menuItem.menuName)
                        putExtra("MENU_ITEM_IMAGE_URL", menuItem.imageUrl)
                        putExtra("MENU_ITEM_STOCK", menuItem.stock)
                    }
                    context.startActivity(intent)
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