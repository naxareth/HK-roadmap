package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ItemDocumentStatusBinding

class StatusAdapter(
    private val onItemClick: (DocumentResponse) -> Unit
) : ListAdapter<DocumentResponse, StatusAdapter.StatusViewHolder>(StatusDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val binding = ItemDocumentStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StatusViewHolder(
        private val binding: ItemDocumentStatusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(document: DocumentResponse) {
            binding.apply {
                // Set document title/name
                tvDocumentTitle.text = document.requirement_title ?: "Untitled Document"

                // Set event title
                tvEventTitle.text = document.event_title ?: "Unknown Event"

                // Set submission date
                tvSubmissionDate.text = document.submitted_at ?: "Not submitted"

                // Set status chip appearance based on document status
                chipStatus.apply {
                    text = document.status.capitalize()
                    setChipBackgroundColorResource(
                        when (document.status.lowercase()) {
                            "pending" -> R.color.status_pending
                            "approved" -> R.color.status_approved
                            "rejected" -> R.color.status_rejected
                            else -> R.color.status_draft
                        }
                    )
                }

                // Set document type icon
                ivDocumentType.setImageResource(
                    when (document.document_type) {
                        "link" -> R.drawable.ic_link
                        else -> R.drawable.ic_file
                    }
                )
            }
        }
    }

    private class StatusDiffCallback : DiffUtil.ItemCallback<DocumentResponse>() {
        override fun areItemsTheSame(oldItem: DocumentResponse, newItem: DocumentResponse): Boolean {
            return oldItem.document_id == newItem.document_id
        }

        override fun areContentsTheSame(oldItem: DocumentResponse, newItem: DocumentResponse): Boolean {
            return oldItem == newItem
        }
    }
}