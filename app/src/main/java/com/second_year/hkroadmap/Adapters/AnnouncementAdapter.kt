package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.AnnouncementItem
import com.second_year.hkroadmap.databinding.AnnouncementItemBinding
import java.text.SimpleDateFormat
import java.util.Locale

class AnnouncementAdapter : RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {
    private var announcements = listOf<AnnouncementItem>()

    inner class AnnouncementViewHolder(
        private val binding: AnnouncementItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(announcement: AnnouncementItem) {
            try {
                binding.apply {
                    tvAnnouncementTitle.text = announcement.title
                    tvAnnouncementContent.text = announcement.content
                    tvAuthorName.text = "Posted by ${announcement.authorName}"

                    // Update date format to match the API response
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val date = inputFormat.parse(announcement.createdAt)
                    tvCreatedAt.text = date?.let { outputFormat.format(it) } ?: announcement.createdAt
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

    fun submitList(newAnnouncements: List<AnnouncementItem>) {
        announcements = newAnnouncements
        notifyDataSetChanged()
    }
}