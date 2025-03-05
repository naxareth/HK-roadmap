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
import com.second_year.hkroadmap.Views.DocumentSubmissionActivity
import com.second_year.hkroadmap.databinding.FragmentAllRequirementsBinding
import kotlinx.coroutines.launch

class AllRequirementsFragment : Fragment() {
    private var _binding: FragmentAllRequirementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var requirementsAdapter: RequirementsTabAdapter
    private lateinit var apiService: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllRequirementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDependencies()
        setupRecyclerView()
        fetchAllRequirements()
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
                    intent.putExtra("requirement_desc", requirement.requirement_desc) // Add this line
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

    private fun fetchAllRequirements() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val token = "Bearer ${TokenManager.getToken(requireContext())}"
                val requirements = apiService.getRequirements(token)

                if (requirements.isEmpty()) {
                    binding.emptyStateText.text = "No requirements found"
                    binding.emptyStateText.visibility = View.VISIBLE
                } else {
                    requirementsAdapter.setRequirements(requirements)
                }
            } catch (e: Exception) {
                binding.emptyStateText.text = "Error loading requirements"
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