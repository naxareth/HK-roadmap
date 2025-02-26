package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.Api.Models.RequirementResponse
import com.second_year.hkroadmap.databinding.ItemRequirementBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RequirementsAdapter : RecyclerView.Adapter<RequirementsAdapter.RequirementViewHolder>() {
    private var requirements = listOf<RequirementItem>()

    inner class RequirementViewHolder(private val binding: ItemRequirementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(requirement: RequirementItem) {
            binding.apply {
                tvRequirementName.text = requirement.requirement_name

                try {
                    if (requirement.due_date == "0000-00-00 00:00:00") {
                        tvDueDate.text = "Due date: TBD"
                    } else {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        val date = inputFormat.parse(requirement.due_date)
                        if (date != null) {
                            tvDueDate.text = "Due date: ${outputFormat.format(date)}"
                        } else {
                            tvDueDate.text = "Due date: Not available"
                        }
                    }
                } catch (e: Exception) {
                    tvDueDate.text = "Due date: Not available"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequirementViewHolder {
        val binding = ItemRequirementBinding.inflate(
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

    fun setRequirements(newRequirements: List<RequirementItem>) {
        requirements = newRequirements
        notifyDataSetChanged()
    }
}