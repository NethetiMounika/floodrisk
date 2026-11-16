package com.example.floodrisk.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.floodrisk.databinding.ItemForecastDayBinding
import com.example.floodrisk.model.FloodForecastDay
import com.example.floodrisk.model.RiskLevel
import java.util.Locale

class ForecastAdapter : ListAdapter<FloodForecastDay, ForecastAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemForecastDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FloodForecastDay) {
            binding.tvDate.text = item.date

            val dischargeStr = item.riverDischarge?.let { String.format(Locale.US, "%.2f m³/s", it) } ?: "N/A"
            binding.tvDischarge.text = dischargeStr

            val meanStr = item.riverDischargeMean?.let { String.format(Locale.US, "Mean: %.2f m³/s", it) } ?: "Mean: N/A"
            binding.tvMeanDischarge.text = meanStr

            val maxStr = item.riverDischargeMax?.let { String.format(Locale.US, "Max: %.2f m³/s", it) } ?: "Max: N/A"
            binding.tvMaxDischarge.text = maxStr

            binding.tvRiskLabel.text = item.riskLevel.displayName
            val color = when (item.riskLevel) {
                RiskLevel.NORMAL -> Color.parseColor("#2E7D32")
                RiskLevel.WATCH -> Color.parseColor("#F57F17")
                RiskLevel.ALERT -> Color.parseColor("#C62828")
                RiskLevel.UNAVAILABLE -> Color.parseColor("#757575")
            }
            binding.tvRiskLabel.setTextColor(color)
            binding.cardRiskIndicator.setCardBackgroundColor(color)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemForecastDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<FloodForecastDay>() {
        override fun areItemsTheSame(oldItem: FloodForecastDay, newItem: FloodForecastDay): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: FloodForecastDay, newItem: FloodForecastDay): Boolean {
            return oldItem == newItem
        }
    }
}
