package com.wanda.app.ui.shopping

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wanda.app.data.database.entity.ShoppingItem
import com.wanda.app.databinding.ItemShoppingBinding

class ShoppingAdapter(
    private val onToggleChecked: (ShoppingItem) -> Unit,
    private val onDelete: (ShoppingItem) -> Unit
) : ListAdapter<ShoppingItem, ShoppingAdapter.ViewHolder>(ShoppingDiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemShoppingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShoppingItem) {
            binding.tvItemName.text = item.name

            // Apply or remove strikethrough based on checked state
            if (item.isChecked) {
                binding.tvItemName.paintFlags =
                    binding.tvItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvItemName.setTextColor(Color.GRAY)
            } else {
                binding.tvItemName.paintFlags =
                    binding.tvItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvItemName.setTextColor(Color.BLACK)
            }

            // Use setOnClickListener instead of setOnCheckedChangeListener
            // to avoid recycling issues with CheckBox
            binding.cbItem.setOnCheckedChangeListener(null)
            binding.cbItem.isChecked = item.isChecked
            binding.cbItem.setOnClickListener {
                onToggleChecked(item)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ShoppingDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {
        override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
            return oldItem == newItem
        }
    }
}
