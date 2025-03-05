package com.second_year.hkroadmap.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.second_year.hkroadmap.Adapters.RequirementsTabAdapter
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.Views.DocumentSubmissionActivity
import com.second_year.hkroadmap.databinding.FragmentSubmittedRequirementsBinding
import kotlinx.coroutines.launch

class SubmittedRequirementsFragment : Fragment() {
    private var _binding: FragmentSubmittedRequirementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var requirementsAdapter: RequirementsTabAdapter
    private lateinit var apiService: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubmittedRequirementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDependencies()
        setupRecyclerView()
        fetchSubmittedRequirements()
    }

    private fun setupDependencies() {
        apiService = RetrofitInstance.createApiService()
    }

    private fun setupRecyclerView() {
        requirementsAdapter = RequirementsTabAdapter().apply {
            setOnItemClickListener { requirement ->
                Intent(requireContext(), DocumentSubmissionActivity::class.java).also { intent ->
                    intent.putExtra("event_id", requirement.event_id)
                    intent.putExtra("requirement_id", requirement.requirement_id)
                    intent.putExtra("requirement_title", requirement.requirement_name)
                    intent.putExtra("requirement_due_date", requirement.due_date)
                    startActivity(intent)
                }
            }
        }

        binding.requirementsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = requirementsAdapter
        }
    }

    private fun fetchSubmittedRequirements() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val token = "Bearer ${TokenManager.getToken(requireContext())}"

                // First get the documents
                val documents = apiService.getStudentDocuments(token).body()?.documents ?: emptyList()
                val submittedDocs = documents.filter { it.is_submitted == 1 }

                if (submittedDocs.isEmpty()) {
                    binding.emptyStateText.text = "No submitted requirements"
                    binding.emptyStateText.visibility = View.VISIBLE
                } else {
                    // Group documents by event_id and take the first document from each event
                    val uniqueEventDocs = submittedDocs.groupBy { it.event_id }
                        .map { (_, docs) -> docs.first() }

                    // Get all requirements for these events
                    val eventId = uniqueEventDocs.firstOrNull()?.event_id
                    val requirements = if (eventId != null) {
                        apiService.getRequirementsByEventId(token, eventId)
                    } else emptyList()

                    // Create a map of requirement details by requirement_id
                    val requirementMap = requirements.associateBy { it.requirement_id }

                    // Convert documents to requirements with descriptions
                    val requirementItems = uniqueEventDocs.map { doc ->
                        val requirement = requirementMap[doc.requirement_id]
                        RequirementItem(
                            requirement_id = doc.requirement_id,
                            event_id = doc.event_id,
                            requirement_name = doc.requirement_title ?: "",
                            requirement_desc = requirement?.requirement_desc ?: "",
                            due_date = doc.requirement_due_date ?: ""
                        )
                    }
                    requirementsAdapter.setRequirements(requirementItems)
                }
            } catch (e: Exception) {
                binding.emptyStateText.text = "Error loading submitted requirements"
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