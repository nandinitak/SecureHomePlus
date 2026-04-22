package com.example.securehomeplus.view.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securehomeplus.R
import com.example.securehomeplus.data.api.ForecastItem
import com.example.securehomeplus.databinding.ItemForecastBinding
import java.text.SimpleDateFormat
import java.util.*

class ForecastAdapter : ListAdapter<ForecastItem, ForecastAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ForecastItem) {
            // Format time
            val date = Date(item.dt * 1000)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            
            binding.tvTime.text = timeFormat.format(date)
            binding.tvDay.text = dayFormat.format(date)
            binding.tvTemp.text = "${item.main.temp.toInt()}°"

            // Weather icon
            val iconRes = when {
                item.weather.firstOrNull()?.main?.contains("Rain", true) == true -> R.drawable.ic_weather_rain
                item.weather.firstOrNull()?.main?.contains("Cloud", true) == true -> R.drawable.ic_weather_cloudy
                item.weather.firstOrNull()?.main?.contains("Clear", true) == true -> R.drawable.ic_weather_sunny
                else -> R.drawable.ic_weather_sunny
            }
            binding.ivIcon.setImageResource(iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<ForecastItem>() {
        override fun areItemsTheSame(oldItem: ForecastItem, newItem: ForecastItem): Boolean {
            return oldItem.dt == newItem.dt
        }

        override fun areContentsTheSame(oldItem: ForecastItem, newItem: ForecastItem): Boolean {
            return oldItem == newItem
        }
    }
}
