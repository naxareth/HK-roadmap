package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.databinding.ItemRequirementBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RequirementsAdapter(
    private val onRequirementClick: (RequirementItem) -> Unit
) : ListAdapter<RequirementItem, RequirementsAdapter.RequirementViewHolder>(RequirementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequirementViewHolder {
        val binding = ItemRequirementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequirementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequirementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequirementViewHolder(
        private val binding: ItemRequirementBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(requirement: RequirementItem) {
            binding.apply {
                tvRequirementName.text = requirement.requirement_name

                try {
                    val formattedDate = dateFormatter.format(requirement.due_date)
                    tvDueDate.text = "Due: $formattedDate"
                } catch (e: Exception) {
                    tvDueDate.text = "Due: ${requirement.due_date}"
                }

                root.setOnClickListener {
                    onRequirementClick(requirement)
                }
            }
        }
    }

    private class RequirementDiffCallback : DiffUtil.ItemCallback<RequirementItem>() {
        override fun areItemsTheSame(oldItem: RequirementItem, newItem: RequirementItem): Boolean {
            return oldItem.requirement_id == newItem.requirement_id
        }

        override fun areContentsTheSame(oldItem: RequirementItem, newItem: RequirementItem): Boolean {
            return oldItem == newItem
        }
    }
}