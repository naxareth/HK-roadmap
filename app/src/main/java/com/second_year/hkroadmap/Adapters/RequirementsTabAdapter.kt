package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.databinding.ItemRequirementTabBinding
import java.text.SimpleDateFormat
import java.util.*

class RequirementsTabAdapter : RecyclerView.Adapter<RequirementsTabAdapter.RequirementViewHolder>() {
    private var requirements = listOf<RequirementItem>()
    private var onItemClick: ((RequirementItem) -> Unit)? = null

    fun setRequirements(newRequirements: List<RequirementItem>) {
        requirements = newRequirements
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (RequirementItem) -> Unit) {
        onItemClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequirementViewHolder {
        val binding = ItemRequirementTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequirementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequirementViewHolder, position: Int) {
        holder.bind(requirements[position])
    }

    override fun getItemCount() = requirements.size

    inner class RequirementViewHolder(
        private val binding: ItemRequirementTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        private val displayFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(requirement: RequirementItem) {
            binding.apply {
                tvRequirementName.text = requirement.requirement_name

                try {
                    val date = dateFormatter.parse(requirement.due_date)
                    tvDueDate.text = "Due: ${displayFormatter.format(date!!)}"
                } catch (e: Exception) {
                    tvDueDate.text = "Due: ${requirement.due_date}"
                }

                root.setOnClickListener {
                    onItemClick?.invoke(requirement)
                }
            }
        }
    }
}