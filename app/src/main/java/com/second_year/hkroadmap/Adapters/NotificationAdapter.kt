package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.NotificationResponse
import com.second_year.hkroadmap.R
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private val onNotificationClick: (NotificationResponse) -> Unit
) : ListAdapter<NotificationResponse, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view, onNotificationClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        itemView: View,
        private val onNotificationClick: (NotificationResponse) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.notificationMessage)
        private val timeText: TextView = itemView.findViewById(R.id.notificationTime)
        private val readIndicator: View = itemView.findViewById(R.id.readIndicator)
        private val verticalIndicator: View = itemView.findViewById(R.id.verticalIndicator)

        fun bind(notification: NotificationResponse) {
            // Set text content
            messageText.text = notification.notification_body

            // Format the timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(notification.created_at)
            timeText.text = date?.let { formatTimeAgo(it.time) }

            // Determine if notification is read - force evaluation to boolean
            val isRead = notification.read_notif == 1

            // Update read indicator visibility
            readIndicator.visibility = if (!isRead) View.VISIBLE else View.GONE

            // Set a more noticeable opacity difference for read items
            val alpha = if (isRead) 0.5f else 1.0f

            // Apply alpha directly to the item view
            itemView.alpha = alpha

            // Apply alpha to individual elements to ensure consistent appearance
            verticalIndicator.alpha = alpha
            messageText.alpha = alpha
            timeText.alpha = alpha

            // Set click listener
            itemView.setOnClickListener {
                onNotificationClick(notification)
            }
        }

        private fun formatTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000} minutes ago"
                diff < 86400_000 -> "${diff / 3600_000} hours ago"
                else -> "${diff / 86400_000} days ago"
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationResponse>() {
        override fun areItemsTheSame(oldItem: NotificationResponse, newItem: NotificationResponse): Boolean {
            return oldItem.notification_id == newItem.notification_id
        }

        override fun areContentsTheSame(oldItem: NotificationResponse, newItem: NotificationResponse): Boolean {
            return oldItem == newItem
        }
    }
}