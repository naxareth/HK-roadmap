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
import com.second_year.hkroadmap.databinding.FragmentMissingRequirementsBinding
import kotlinx.coroutines.launch

class MissingRequirementsFragment : Fragment() {
    private var _binding: FragmentMissingRequirementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var requirementsAdapter: RequirementsTabAdapter
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

    private fun fetchMissingRequirements() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val token = "Bearer ${TokenManager.getToken(requireContext())}"
                val documents = apiService.getStudentDocuments(token).body()?.documents ?: emptyList()

                // Filter for missing documents
                val missingDocs = documents.filter { it.status == "missing" }

                if (missingDocs.isEmpty()) {
                    binding.emptyStateText.text = "No missing requirements"
                    binding.emptyStateText.visibility = View.VISIBLE
                } else {
                    // Convert documents to requirements
                    val requirements = missingDocs.map { doc ->
                        RequirementItem(
                            requirement_id = doc.requirement_id,
                            event_id = doc.event_id,
                            requirement_name = doc.requirement_title ?: "",
                            due_date = doc.requirement_due_date ?: ""
                        )
                    }
                    requirementsAdapter.setRequirements(requirements)
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