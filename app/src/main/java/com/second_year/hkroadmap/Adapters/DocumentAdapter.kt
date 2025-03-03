package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentAdapter(
    private val onDeleteClick: (DocumentResponse) -> Unit,
    private val onViewClick: (DocumentResponse) -> Unit,
    private val onSubmitClick: (DocumentResponse) -> Unit,
    private val onUnsubmitClick: (DocumentResponse) -> Unit
) : ListAdapter<DocumentResponse, DocumentAdapter.DocumentViewHolder>(DocumentDiffCallback()) {
    // ... existing code ...
    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: DocumentResponse) {
            binding.apply {
                // Format date
                val uploadDate = formatDate(document.upload_at)

                // Set text fields
                tvFileName.text = if (document.file_path.isNotEmpty()) {
                    document.file_path.substringAfterLast("/")
                } else {
                    root.context.getString(R.string.no_file_uploaded)
                }

                tvFileDate.text = root.context.getString(R.string.uploaded_at, uploadDate)

                // Setup file icon based on file type
                ivFileIcon.setImageResource(
                    when {
                        document.file_path.isEmpty() -> R.drawable.ic_no_file
                        document.file_path.lowercase().endsWith(".pdf", true) -> R.drawable.ic_pdf
                        document.file_path.lowercase().let { path ->
                            path.endsWith(".jpg", true) ||
                                    path.endsWith(".jpeg", true) ||
                                    path.endsWith(".png", true)
                        } -> R.drawable.ic_image
                        else -> R.drawable.ic_file
                    }
                )

                // Setup status chip and menu
                setupStatusChip(document.status)
                setupMenu(document)

                // Handle item click for viewing document
                root.setOnClickListener {
                    if (document.file_path.isNotEmpty()) {
                        onViewClick(document)
                    }
                }
            }
        }

        private fun setupStatusChip(status: String) {
            binding.chipStatus.apply {
                text = when (status.lowercase()) {
                    "draft" -> context.getString(R.string.status_draft)
                    "pending" -> context.getString(R.string.status_pending)
                    "missing" -> context.getString(R.string.status_missing)
                    "approved" -> context.getString(R.string.status_approved)
                    "rejected" -> context.getString(R.string.status_rejected)
                    else -> status
                }

                setChipBackgroundColorResource(
                    when (status.lowercase()) {
                        "draft" -> R.color.status_draft
                        "pending" -> R.color.status_pending
                        "missing" -> R.color.status_missing
                        "approved" -> R.color.status_approved
                        "rejected" -> R.color.status_rejected
                        else -> R.color.status_draft
                    }
                )
            }
        }

        private fun setupMenu(document: DocumentResponse) {
            binding.btnMenu.apply {
                when {
                    document.file_path.isEmpty() -> {
                        visibility = View.GONE
                    }
                    document.status.lowercase() == "draft" -> {
                        visibility = View.VISIBLE
                        setOnClickListener { showDraftMenu(it, document) }
                    }
                    document.status.lowercase() == "pending" -> {
                        visibility = View.VISIBLE
                        setOnClickListener { showPendingMenu(it, document) }
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
        }

        private fun showDraftMenu(anchor: View, document: DocumentResponse) {
            PopupMenu(anchor.context, anchor).apply {
                inflate(R.menu.menu_document_draft)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_submit -> {
                            onSubmitClick(document)
                            true
                        }
                        R.id.action_delete -> {
                            onDeleteClick(document)
                            true
                        }
                        R.id.action_view -> {
                            onViewClick(document)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        private fun showPendingMenu(anchor: View, document: DocumentResponse) {
            PopupMenu(anchor.context, anchor).apply {
                inflate(R.menu.menu_document_pending)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_unsubmit -> {
                            onUnsubmitClick(document)
                            true
                        }
                        R.id.action_view -> {
                            onViewClick(document)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        private fun formatDate(dateString: String): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return try {
                val date = inputFormat.parse(dateString)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString
            }
        }
    }

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

    private class DocumentDiffCallback : DiffUtil.ItemCallback<DocumentResponse>() {
        override fun areItemsTheSame(oldItem: DocumentResponse, newItem: DocumentResponse): Boolean {
            return oldItem.document_id == newItem.document_id
        }

        override fun areContentsTheSame(oldItem: DocumentResponse, newItem: DocumentResponse): Boolean {
            return oldItem == newItem
        }
    }
}