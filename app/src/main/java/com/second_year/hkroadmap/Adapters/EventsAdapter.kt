package com.second_year.hkroadmap.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdapter : ListAdapter<EventResponse, EventsAdapter.EventViewHolder>(EventDiffCallback()) {
    private var onEventClickListener: ((EventResponse) -> Unit)? = null

    fun setOnEventClickListener(listener: (EventResponse) -> Unit) {
        onEventClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding, onEventClickListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onEventClickListener: ((EventResponse) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            isLenient = false // Strict date parsing
        }
        private val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        fun bind(event: EventResponse) {
            binding.apply {
                tvEventTitle.text = event.title
                tvEventLocation.text = event.location
                tvEventDate.text = formatEventDate(event.date)

                // Set click listener
                root.setOnClickListener {
                    onEventClickListener?.invoke(event)
                }
            }
        }

        private fun formatEventDate(dateString: String): String {
            return try {
                when {
                    dateString.isBlank() || dateString == "0000-00-00 00:00:00" -> "Date TBD"
                    else -> {
                        val date = inputFormat.parse(dateString)
                        if (date != null) {
                            outputFormat.format(date)
                        } else {
                            Log.w("EventsAdapter", "Null date parsed from: $dateString")
                            "Invalid Date"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EventsAdapter", "Error parsing date: $dateString", e)
                "Invalid Date"
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<EventResponse>() {
        override fun areItemsTheSame(oldItem: EventResponse, newItem: EventResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EventResponse, newItem: EventResponse): Boolean {
            return oldItem == newItem
        }
    }
}