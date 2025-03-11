package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.second_year.hkroadmap.Api.Models.Comment
import com.second_year.hkroadmap.R
import java.text.SimpleDateFormat
import java.util.Locale

class CommentAdapter(
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chipUserType: Chip = itemView.findViewById(R.id.chip_user_type)
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btn_more)

        fun bind(comment: Comment) {
            // Set user type chip with appropriate styling
            chipUserType.text = comment.userType?.uppercase()
            chipUserType.setChipBackgroundColorResource(
                when (comment.userType) {
                    Comment.USER_TYPE_STUDENT -> R.color.student_chip_background
                    Comment.USER_TYPE_ADMIN -> R.color.admin_chip_background
                    else -> R.color.default_chip_background
                }
            )

            // Set username and comment
            tvUsername.text = comment.userName
            tvComment.text = comment.body

            // Format and set timestamp
            tvTimestamp.text = formatTimestamp(comment.createdAt)

            // Show more button only for comment owner
            btnMore.visibility = if (comment.isOwner) View.VISIBLE else View.GONE

            // Set up more button click listener
            btnMore.setOnClickListener { view ->
                showPopupMenu(view, comment)
            }
        }

        private fun showPopupMenu(view: View, comment: Comment) {
            PopupMenu(view.context, view).apply {
                inflate(R.menu.menu_comment_options)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            onEditClick(comment)
                            true
                        }
                        R.id.action_delete -> {
                            onDeleteClick(comment)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        private fun formatTimestamp(timestamp: String): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            return try {
                val date = inputFormat.parse(timestamp)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                timestamp
            }
        }
    }

    private class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            // Compare all fields except documentId (which might be null)
            return oldItem.copy(documentId = null) == newItem.copy(documentId = null)
        }
    }
}