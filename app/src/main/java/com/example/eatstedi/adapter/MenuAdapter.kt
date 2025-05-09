package com.example.eatstedi.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatstedi.activity.ManageStockMenuActivity
import com.example.eatstedi.R
import com.example.eatstedi.databinding.ViewItemMenuBinding
import com.example.eatstedi.model.MenuItem

class MenuAdapter(
    private var menuList: List<MenuItem>,
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
            tvMenuName.text = menuItem.menuName
            tvOwnerName.text = menuItem.ownerName
            tvPrice.text = menuItem.formattedPrice
            tvStock.text = "Sisa: ${menuItem.stock}"

            Glide.with(holder.itemView.context)
                .load(menuItem.imageUrl)
                .placeholder(R.drawable.image_menu)
                .error(R.drawable.ic_launcher_background)
                .into(ivMenuImage)

            root.setOnClickListener { onItemClick(menuItem) }
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
        popupMenu.show()
    }
}