package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.AnnouncementItem
import com.second_year.hkroadmap.Api.Models.AnnouncementResponse
import com.second_year.hkroadmap.databinding.AnnouncementItemBinding
import java.text.SimpleDateFormat
import java.util.Locale

class AnnouncementAdapter : RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {
    private var announcements: List<AnnouncementItem> = emptyList()
    private var onItemClickListener: ((AnnouncementItem) -> Unit)? = null

    inner class AnnouncementViewHolder(
        private val binding: AnnouncementItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(announcement: AnnouncementItem) {
            try {
                binding.apply {
                    // Match the IDs from your layout file
                    tvAnnouncementTitle.text = announcement.title
                    tvAnnouncementContent.text = announcement.content
                    tvAuthorName.text = "Posted by ${announcement.authorName}"

                    // Update date format to match the API response
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val date = inputFormat.parse(announcement.createdAt)
                    tvCreatedAt.text = date?.let { outputFormat.format(it) } ?: announcement.createdAt

                    // Set read status (isRead is now Int)
                    root.alpha = if (announcement.isRead == 1) 0.7f else 1.0f

                    // Set click listener
                    root.setOnClickListener {
                        onItemClickListener?.invoke(announcement)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to showing the raw date if parsing fails
                binding.tvCreatedAt.text = announcement.createdAt
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val binding = AnnouncementItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        holder.bind(announcements[position])
    }

    override fun getItemCount(): Int = announcements.size

    fun submitList(response: AnnouncementResponse) {
        announcements = response.announcements ?: emptyList()
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (AnnouncementItem) -> Unit) {
        onItemClickListener = listener
    }
}