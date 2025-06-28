package com.example.eatstedi.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.eatstedi.model.MenuItem

class MenuDiffCallback(
    private val oldList: List<MenuItem>,
    private val newList: List<MenuItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.menuName == newItem.menuName &&
                oldItem.price == newItem.price &&
                oldItem.stock == newItem.stock &&
                oldItem.imageUrl == newItem.imageUrl &&
                oldItem.supplierName == newItem.supplierName
    }

}