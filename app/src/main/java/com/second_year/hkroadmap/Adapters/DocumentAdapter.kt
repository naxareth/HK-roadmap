package com.second_year.hkroadmap.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentAdapter(
    private val onDeleteClick: (DocumentResponse) -> Unit
) : ListAdapter<DocumentResponse, DocumentAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    companion object {
        private const val TAG = "DocumentAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        Log.d(TAG, "Creating new ViewHolder")
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = getItem(position)
        Log.d(TAG, "Binding document at position $position: ${document.file_path}")
        holder.bind(document)
    }

    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(document: DocumentResponse) {
            binding.apply {
                try {
                    // Extract filename from file path
                    val fileName = document.file_path.split("/").lastOrNull()
                        ?: "Unknown File"
                    tvFileName.text = fileName
                    Log.d(TAG, "Set filename: $fileName")

                    // Format the date
                    try {
                        val formattedDate = dateFormatter.format(document.upload_at)
                        tvFileDate.text = "Uploaded: $formattedDate"
                        Log.d(TAG, "Set upload date: $formattedDate")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error formatting date: ${document.upload_at}", e)
                        tvFileDate.text = "Uploaded: ${document.upload_at}"
                    }

                    // Set status
                    tvStatus.text = "Status: ${document.status}"
                    Log.d(TAG, "Set status: ${document.status}")

                    // Set delete button click listener
                    btnDelete.setOnClickListener {
                        Log.d(TAG, "Delete clicked for document: ${document.id}")
                        onDeleteClick(document)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error binding document view", e)
                    // Set fallback values
                    tvFileName.text = "Error loading file"
                    tvFileDate.text = "Date unavailable"
                    tvStatus.text = "Status unavailable"
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