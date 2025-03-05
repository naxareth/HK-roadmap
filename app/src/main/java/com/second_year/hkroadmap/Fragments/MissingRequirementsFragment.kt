package com.second_year.hkroadmap.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.second_year.hkroadmap.Adapters.ExpandableEventAdapter
import com.second_year.hkroadmap.Adapters.RequirementsTabAdapter
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.Views.DocumentSubmissionActivity
import com.second_year.hkroadmap.databinding.FragmentMissingRequirementsBinding
import kotlinx.coroutines.launch

class MissingRequirementsFragment : Fragment() {
    private var _binding: FragmentMissingRequirementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: ExpandableEventAdapter
    private lateinit var apiService: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMissingRequirementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDependencies()
        setupRecyclerView()
        fetchMissingRequirements()
    }

    private fun setupDependencies() {
        apiService = RetrofitInstance.createApiService()
    }

    private fun setupRecyclerView() {
        eventAdapter = ExpandableEventAdapter { requirement ->
            Intent(requireContext(), DocumentSubmissionActivity::class.java).also { intent ->
                intent.putExtra("event_id", requirement.event_id)
                intent.putExtra("requirement_id", requirement.requirement_id)
                intent.putExtra("requirement_title", requirement.requirement_name)
                intent.putExtra("requirement_desc", requirement.requirement_desc)
                intent.putExtra("requirement_due_date", requirement.due_date)
                startActivity(intent)
            }
        }

        binding.requirementsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    fun fetchMissingRequirements() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val token = "Bearer ${TokenManager.getToken(requireContext())}"
                val documents = apiService.getStudentDocuments(token).body()?.documents ?: emptyList()

                // Filter for missing documents with valid data
                val missingDocs = documents.filter { doc ->
                    doc.status == "missing" &&
                            doc.requirement_id != 0 &&
                            !doc.requirement_title.isNullOrEmpty() &&
                            !doc.requirement_due_date.isNullOrEmpty()
                }

                if (missingDocs.isEmpty()) {
                    binding.emptyStateText.text = "No missing requirements"
                    binding.emptyStateText.visibility = View.VISIBLE
                } else {
                    // First group by event_id
                    val eventGroups = missingDocs.groupBy { it.event_id }

                    // Get all requirements for these events
                    val eventId = eventGroups.keys.firstOrNull()
                    val requirements = if (eventId != null) {
                        apiService.getRequirementsByEventId(token, eventId)
                    } else emptyList()

                    // Create a map of requirement details by requirement_id
                    val requirementMap = requirements.associateBy { it.requirement_id }

                    // Convert to EventWithRequirements objects
                    val eventsWithReqs = eventGroups.mapNotNull { (eventId, docs) ->
                        // Skip if no valid documents for this event
                        if (docs.isEmpty()) return@mapNotNull null

                        val firstDoc = docs.first()
                        // Skip if event title is missing
                        if (firstDoc.event_title.isNullOrEmpty()) return@mapNotNull null

                        // Group documents by requirement_id and take first of each group
                        val uniqueRequirements = docs.groupBy { it.requirement_id }
                            .mapNotNull { (reqId, reqDocs) ->
                                // Skip if requirement_id is 0 or invalid
                                if (reqId == 0) return@mapNotNull null
                                reqDocs.first()
                            }

                        // Skip if no valid requirements
                        if (uniqueRequirements.isEmpty()) return@mapNotNull null

                        ExpandableEventAdapter.EventWithRequirements(
                            event = EventResponse(
                                id = eventId,
                                title = firstDoc.event_title ?: "Unknown Event",
                                description = "",
                                date = firstDoc.requirement_due_date ?: "",
                                location = "",
                                created_at = "",
                                updated_at = ""
                            ),
                            requirements = uniqueRequirements.mapNotNull { doc ->
                                val requirement = requirementMap[doc.requirement_id]
                                // Only create RequirementItem if all required fields exist
                                if (doc.requirement_id != 0 &&
                                    !doc.requirement_title.isNullOrEmpty() &&
                                    !doc.requirement_due_date.isNullOrEmpty()
                                ) {
                                    RequirementItem(
                                        requirement_id = doc.requirement_id,
                                        event_id = doc.event_id,
                                        requirement_name = doc.requirement_title,
                                        requirement_desc = requirement?.requirement_desc ?: "",
                                        due_date = doc.requirement_due_date
                                    )
                                } else null
                            }
                        )
                    }.filter { it.requirements.isNotEmpty() } // Only include events that have valid requirements

                    if (eventsWithReqs.isEmpty()) {
                        binding.emptyStateText.text = "No valid missing requirements found"
                        binding.emptyStateText.visibility = View.VISIBLE
                    } else {
                        eventAdapter.setEvents(eventsWithReqs)
                        binding.requirementsRecyclerView.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                binding.emptyStateText.text = "Error loading missing requirements"
                binding.emptyStateText.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}