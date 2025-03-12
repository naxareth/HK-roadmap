package com.second_year.hkroadmap.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.databinding.ItemEventWithRequirementsBinding

class ExpandableEventAdapter(
    private val onRequirementClick: (RequirementItem) -> Unit
) : RecyclerView.Adapter<ExpandableEventAdapter.EventViewHolder>() {

    private var events: List<EventWithRequirements> = emptyList()

    data class EventWithRequirements(
        val event: EventResponse,
        val requirements: List<RequirementItem>,
        var isExpanded: Boolean = false
    )

    inner class EventViewHolder(
        private val binding: ItemEventWithRequirementsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val requirementsAdapter = RequirementsTabAdapter().apply {
            setOnItemClickListener { requirement ->
                onRequirementClick(requirement)
            }
        }

        init {
            binding.rvRequirements.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = requirementsAdapter
            }

            binding.layoutEventHeader.setOnClickListener {
                val position = adapterPosition // Use this instead of absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val event = events[position]
                    event.isExpanded = !event.isExpanded
                    notifyItemChanged(position)
                }
            }
        }

        fun bind(eventWithReqs: EventWithRequirements) {
            binding.apply {
                tvEventTitle.text = eventWithReqs.event.title

                // Update expand icon
                ivExpandIcon.setImageResource(
                    if (eventWithReqs.isExpanded) R.drawable.ic_expand_less
                    else R.drawable.ic_expand_more
                )

                // Show/hide requirements
                rvRequirements.isVisible = eventWithReqs.isExpanded
                if (eventWithReqs.isExpanded) {
                    requirementsAdapter.setRequirements(eventWithReqs.requirements)
                }
            }
        }
    } // Added missing brace for EventViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventWithRequirementsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    fun setEvents(newEvents: List<EventWithRequirements>) {
        events = newEvents
        notifyDataSetChanged()
    }
}