package com.second_year.hkroadmap.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdapter : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {
    private var events = listOf<EventResponse>()
    private var onEventClickListener: ((EventResponse) -> Unit)? = null

    fun setOnEventClickListener(listener: (EventResponse) -> Unit) {
        onEventClickListener = listener
    }

    fun setEvents(newEvents: List<EventResponse>) {
        events = newEvents
        notifyDataSetChanged()
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
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onEventClickListener: ((EventResponse) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: EventResponse) {
            binding.apply {
                tvEventTitle.text = event.title
                tvEventLocation.text = event.location

                // Handle date formatting with proper error handling
                try {
                    if (event.date == "0000-00-00 00:00:00") {
                        tvEventDate.text = "Date TBD"
                        return@apply
                    }

                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

                    val date = inputFormat.parse(event.date)
                    if (date != null) {
                        tvEventDate.text = outputFormat.format(date)
                    } else {
                        tvEventDate.text = "Invalid Date"
                    }
                } catch (e: Exception) {
                    tvEventDate.text = "Invalid Date"
                    Log.e("EventsAdapter", "Error parsing date: ${event.date}", e)
                }

                // Set click listener
                root.setOnClickListener {
                    onEventClickListener?.invoke(event)
                }
            }
        }
    }
}