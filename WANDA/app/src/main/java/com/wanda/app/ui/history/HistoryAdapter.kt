package com.wanda.app.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wanda.app.R
import com.wanda.app.data.database.entity.GpsRecord
import com.wanda.app.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter :
    ListAdapter<GpsRecord, HistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))

    companion object {
        private val COLOR_ENTRADA = Color.parseColor("#2E7D32") // Green
        private val COLOR_SALIDA = Color.parseColor("#E65100")  // Orange
    }

    inner class ViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: GpsRecord) {
            val isEntrada = record.type == "ENTRADA"

            // Set event type text with emoji
            binding.tvEventType.text = if (isEntrada) {
                "🏠 Entrada"
            } else {
                "🚶 Salida"
            }

            // Set icon
            binding.ivIcon.setImageResource(
                if (isEntrada) R.drawable.ic_enter else R.drawable.ic_exit
            )

            // Set formatted timestamp
            binding.tvEventTime.text = dateFormat.format(Date(record.timestamp))

            // Set text color based on event type
            val textColor = if (isEntrada) COLOR_ENTRADA else COLOR_SALIDA
            binding.tvEventType.setTextColor(textColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<GpsRecord>() {
        override fun areItemsTheSame(oldItem: GpsRecord, newItem: GpsRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GpsRecord, newItem: GpsRecord): Boolean {
            return oldItem == newItem
        }
    }
}
