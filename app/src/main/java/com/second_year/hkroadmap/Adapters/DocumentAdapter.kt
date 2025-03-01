package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.databinding.ItemDocumentBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentAdapter(
    private val onDeleteClick: (DocumentResponse) -> Unit
) : ListAdapter<DocumentResponse, DocumentAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(document: DocumentResponse) {
            binding.apply {
                // Extract filename from file path
                val fileName = document.file_path.split("/").lastOrNull()
                    ?: "Unknown File"
                tvFileName.text = fileName

                // Format the date
                try {
                    val formattedDate = dateFormatter.format(document.created_at)
                    tvFileDate.text = "Uploaded: $formattedDate"
                } catch (e: Exception) {
                    tvFileDate.text = "Uploaded: ${document.created_at}"
                }

                // Set delete button click listener
                btnDelete.setOnClickListener {
                    onDeleteClick(document)
                }
            }
        }
    }

    private class DocumentDiffCallback : DiffUtil.ItemCallback<DocumentResponse>() {
        override fun areItemsTheSame(oldItem: DocumentResponse, newItem: DocumentResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DocumentResponse, newItem: DocumentResponse): Boolean {
            return oldItem == newItem
        }
    }
}