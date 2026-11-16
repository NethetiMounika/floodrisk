package com.example.floodrisk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.floodrisk.databinding.ItemMonitoredPlaceBinding
import com.example.floodrisk.model.MonitoredPlace
import java.util.Locale

class MonitoredPlacesAdapter(
    private val onSelect: (MonitoredPlace) -> Unit,
    private val onEdit: (MonitoredPlace) -> Unit,
    private val onDelete: (MonitoredPlace) -> Unit
) : ListAdapter<MonitoredPlace, MonitoredPlacesAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemMonitoredPlaceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMonitoredPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.tvPlaceName.text = item.name
        holder.binding.tvCoordinates.text = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", item.latitude, item.longitude)
        holder.binding.tvDefaultBadge.visibility = if (item.isDefault) View.VISIBLE else View.GONE

        holder.binding.root.setOnClickListener { onSelect(item) }
        holder.binding.btnEdit.setOnClickListener { onEdit(item) }
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<MonitoredPlace>() {
        override fun areItemsTheSame(oldItem: MonitoredPlace, newItem: MonitoredPlace): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MonitoredPlace, newItem: MonitoredPlace): Boolean {
            return oldItem == newItem
        }
    }
}
