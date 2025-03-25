package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.data.models.RequirementGroup
import com.second_year.hkroadmap.data.models.RequirementItem

class ProfileRequirementItemAdapter : RecyclerView.Adapter<ProfileRequirementItemAdapter.RequirementItemViewHolder>() {

    private var requirements: List<RequirementItem> = emptyList()

    fun submitList(newList: List<RequirementItem>) {
        requirements = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequirementItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_requirement, parent, false)
        return RequirementItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequirementItemViewHolder, position: Int) {
        holder.bind(requirements[position])

        // Add alternating row colors for better readability
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.gray_50))
        }

        // Add bottom divider for all rows except the last one
        if (position < requirements.size - 1) {
            holder.itemView.findViewById<View>(R.id.rowDivider).visibility = View.VISIBLE
        } else {
            holder.itemView.findViewById<View>(R.id.rowDivider).visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = requirements.size

    inner class RequirementItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRequirementName: TextView = itemView.findViewById(R.id.tvRequirementName)
        private val tvSubmissionDate: TextView = itemView.findViewById(R.id.tvSubmissionDate)
        private val tvApprovedBy: TextView = itemView.findViewById(R.id.tvApprovedBy)

        fun bind(requirement: RequirementItem) {
            tvRequirementName.text = requirement.requirement

            // Simplified submission date display
            tvSubmissionDate.text = if (requirement.submissionDate != null) {
                formatDate(requirement.submissionDate)
            } else {
                "N/A"
            }

            // Simplified approval display
            tvApprovedBy.text = requirement.approvedBy ?: "N/A"
        }

        private fun formatDate(dateString: String): String {
            return try {
                // Assuming format is "yyyy-MM-dd HH:mm:ss"
                val parts = dateString.split(" ")
                if (parts.size >= 1) parts[0] else dateString
            } catch (e: Exception) {
                dateString
            }
        }
    }
}