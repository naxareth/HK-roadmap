package com.second_year.hkroadmap.Adapters

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentAdapter(
    private val onDeleteClick: (DocumentResponse) -> Unit,
    private val onViewClick: (DocumentResponse) -> Unit
) : ListAdapter<DocumentResponse, DocumentAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    private val draftDocumentIds = mutableListOf<Int>()
    private val pendingDocumentIds = mutableListOf<Int>()
    private var onDocumentStatusChanged: () -> Unit = {}

    companion object {
        private const val BASE_URL = "http://192.168.0.12:8000/uploads/"
        private const val THUMBNAIL_SIZE = 40
        private const val ICON_SIZE = 24
    }

    fun setOnDocumentStatusChangedListener(listener: () -> Unit) {
        onDocumentStatusChanged = listener
    }

    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: DocumentResponse) {
            binding.apply {
                // Track ALL document IDs based on status, regardless of type
                when (document.status.lowercase()) {
                    "draft" -> {
                        if (!draftDocumentIds.contains(document.document_id)) {
                            Log.d("DocumentAdapter", "Adding draft document ID: ${document.document_id}, Type: ${document.document_type}")
                            draftDocumentIds.add(document.document_id)
                            onDocumentStatusChanged()
                        }
                    }
                    "pending" -> {
                        if (!pendingDocumentIds.contains(document.document_id)) {
                            Log.d("DocumentAdapter", "Adding pending document ID: ${document.document_id}, Type: ${document.document_type}")
                            pendingDocumentIds.add(document.document_id)
                            onDocumentStatusChanged()
                        }
                    }
                    else -> {
                        // Remove from both lists if status changes
                        val wasInDraft = draftDocumentIds.remove(document.document_id)
                        val wasInPending = pendingDocumentIds.remove(document.document_id)
                        if (wasInDraft || wasInPending) {
                            Log.d("DocumentAdapter", "Removing document ID: ${document.document_id} due to status change to: ${document.status}")
                            onDocumentStatusChanged()
                        }
                    }
                }

                // Format date
                val uploadDate = formatDate(document.upload_at)

                // Set text fields based on document type
                tvFileName.text = when {
                    document.document_type == "link" -> document.link_url
                    document.file_path.isNotEmpty() -> document.file_path.substringAfterLast("/")
                    else -> root.context.getString(R.string.no_file_uploaded)
                }
                tvFileDate.text = root.context.getString(R.string.uploaded_at, uploadDate)

                // Handle image loading or icon setting
                setupDocumentImage(document)

                // Setup status chip and menu
                setupStatusChip(document.status)
                setupMenu(document)
            }
        }

        private fun setupDocumentImage(document: DocumentResponse) {
            binding.ivFileIcon.apply {
                if (document.file_path.isNotEmpty() && isImageFile(document.file_path)) {
                    // Configure image view for thumbnails
                    val params = layoutParams
                    params.width = THUMBNAIL_SIZE.dp
                    params.height = THUMBNAIL_SIZE.dp
                    layoutParams = params
                    scaleType = ImageView.ScaleType.CENTER_CROP

                    // Load image using Coil
                    val imageUrl = BASE_URL + document.file_path.removePrefix("uploads/")
                    load(imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_image)
                        error(R.drawable.ic_image)
                        transformations(RoundedCornersTransformation(8f))
                        size(THUMBNAIL_SIZE.dp, THUMBNAIL_SIZE.dp)
                        listener(
                            onSuccess = { _, _ ->
                                Log.d("DocumentAdapter", "Image loaded successfully: $imageUrl")
                            },
                            onError = { _, error ->
                                Log.e("DocumentAdapter", "Error loading image: $imageUrl", error.throwable)
                            }
                        )
                    }
                } else {
                    // Reset image view for icons
                    val params = layoutParams
                    params.width = ICON_SIZE.dp
                    params.height = ICON_SIZE.dp
                    layoutParams = params
                    scaleType = ImageView.ScaleType.CENTER_INSIDE

                    // Set appropriate icon
                    setImageResource(
                        when {
                            document.document_type == "link" -> R.drawable.ic_link
                            document.file_path.isEmpty() -> R.drawable.ic_no_file
                            document.file_path.lowercase().endsWith(".pdf") -> R.drawable.ic_pdf
                            else -> R.drawable.ic_file
                        }
                    )
                }
            }
        }

        private fun isImageFile(filePath: String): Boolean {
            return filePath.lowercase().let { path ->
                path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                        path.endsWith(".png") || path.endsWith(".webp")
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
                // Show menu button for all states except "missing"
                visibility = when (document.status.lowercase()) {
                    "missing" -> View.GONE
                    else -> View.VISIBLE
                }

                setOnClickListener { anchor ->
                    showDocumentMenu(anchor, document)
                }
            }
        }

        private fun showDocumentMenu(anchor: View, document: DocumentResponse) {
            PopupMenu(anchor.context, anchor).apply {
                when (document.status.lowercase()) {
                    "draft" -> {
                        // For draft documents, show both delete and view options
                        menu.add(0, R.id.action_delete, 0, R.string.delete)
                        menu.add(0, R.id.action_view, 1, R.string.view_document)
                    }
                    "pending", "approved", "rejected" -> {
                        // For these states, only show view option
                        menu.add(0, R.id.action_view, 0, R.string.view_document)
                    }
                    // For "missing" status, don't show any options
                }

                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
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

                // Only show the menu if there are items
                if (menu.size() > 0) {
                    show()
                }
            }
        }

        private fun handleDocumentView(anchor: View, document: DocumentResponse) {
            if (document.document_type == "link") {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(document.link_url))
                    anchor.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        anchor.context,
                        anchor.context.getString(R.string.unable_to_open_link),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                onViewClick(document)
            }
        }

        // Extension property for converting dp to pixels
        private val Int.dp: Int
            get() = (this * itemView.context.resources.displayMetrics.density).toInt()
    }

    // Get document IDs for API requests
    fun getDraftDocumentIds(): List<Int> = draftDocumentIds.toList()
    fun getPendingDocumentIds(): List<Int> = pendingDocumentIds.toList()

    // Get counts
    fun getDraftCount(): Int = draftDocumentIds.size
    fun getPendingCount(): Int = pendingDocumentIds.size

    // Clear IDs after successful operations
    fun clearDraftDocumentIds() {
        draftDocumentIds.clear()
        onDocumentStatusChanged()
    }

    fun clearPendingDocumentIds() {
        pendingDocumentIds.clear()
        onDocumentStatusChanged()
    }

    // Remove specific document ID
    fun removeDocumentId(documentId: Int) {
        val wasInDraft = draftDocumentIds.remove(documentId)
        val wasInPending = pendingDocumentIds.remove(documentId)
        if (wasInDraft || wasInPending) {
            onDocumentStatusChanged()
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

    override fun submitList(list: List<DocumentResponse>?) {
        Log.d("DocumentAdapter", "Submitting new list with ${list?.size} items")
        draftDocumentIds.clear()
        pendingDocumentIds.clear()
        super.submitList(list)

        // Re-populate IDs after list update
        list?.forEach { document ->
            when (document.status.lowercase()) {
                "draft" -> draftDocumentIds.add(document.document_id)
                "pending" -> pendingDocumentIds.add(document.document_id)
            }
        }
        Log.d(
            "DocumentAdapter",
            "After update - Draft IDs: $draftDocumentIds, Pending IDs: $pendingDocumentIds"
        )
        notifyDataSetChanged() // Force refresh
        onDocumentStatusChanged() // Notify status change
    }

    private class DocumentDiffCallback : DiffUtil.ItemCallback<DocumentResponse>() {
        override fun areItemsTheSame(
            oldItem: DocumentResponse,
            newItem: DocumentResponse
        ): Boolean {
            return oldItem.document_id == newItem.document_id
        }

        override fun areContentsTheSame(
            oldItem: DocumentResponse,
            newItem: DocumentResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}