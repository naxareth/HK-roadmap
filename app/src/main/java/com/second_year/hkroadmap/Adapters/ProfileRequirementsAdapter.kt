package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.data.models.RequirementGroup

class ProfileRequirementsAdapter : RecyclerView.Adapter<ProfileRequirementsAdapter.RequirementGroupViewHolder>() {

    private var requirementGroups: List<RequirementGroup> = emptyList()

    fun submitList(newList: List<RequirementGroup>) {
        requirementGroups = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequirementGroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_requirement_group, parent, false)
        return RequirementGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequirementGroupViewHolder, position: Int) {
        holder.bind(requirementGroups[position])
    }

    override fun getItemCount(): Int = requirementGroups.size

    inner class RequirementGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        private val rvRequirements: RecyclerView = itemView.findViewById(R.id.rvRequirements)
        private val requirementItemAdapter = ProfileRequirementItemAdapter()

        fun bind(requirementGroup: RequirementGroup) {
            tvEventName.text = requirementGroup.eventName

            rvRequirements.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = requirementItemAdapter
            }

            requirementItemAdapter.submitList(requirementGroup.requirements)
        }
    }
}