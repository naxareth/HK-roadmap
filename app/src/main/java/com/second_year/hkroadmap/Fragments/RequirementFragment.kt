package com.second_year.hkroadmap.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Adapters.RequirementsAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.Api.Repository.RequirementRepository
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.ViewModel.RequirementViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.Views.DocumentSubmissionActivity
import com.second_year.hkroadmap.databinding.FragmentRequirementBinding
import kotlinx.coroutines.launch

class RequirementFragment : Fragment() {
    private var _binding: FragmentRequirementBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RequirementViewModel
    private lateinit var requirementsAdapter: RequirementsAdapter

    private var eventId: Int = -1
    private var eventTitle: String = ""
    private var eventDate: String = ""
    private var eventLocation: String = ""

    companion object {
        private const val TAG = "RequirementFragment"

        fun newInstance(
            eventId: Int,
            eventTitle: String,
            eventDate: String,
            eventLocation: String
        ): RequirementFragment {
            return RequirementFragment().apply {
                arguments = Bundle().apply {
                    putInt("event_id", eventId)
                    putString("event_title", eventTitle)
                    putString("event_date", eventDate)
                    putString("event_location", eventLocation)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            eventId = it.getInt("event_id", -1)
            eventTitle = it.getString("event_title", "")
            eventDate = it.getString("event_date", "")
            eventLocation = it.getString("event_location", "TBD")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequirementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDependencies()
        setupViews()
        observeViewModel()
        fetchRequirements()
    }

    private fun setupDependencies() {
        Log.d(TAG, "Setting up dependencies")
        val apiService = RetrofitInstance.createApiService()
        val repository = RequirementRepository(apiService)
        val factory = ViewModelFactory(requirementRepository = repository)
        viewModel = ViewModelProvider(this, factory)[RequirementViewModel::class.java]
    }

    private fun setupViews() {
        Log.d(TAG, "Setting up views")
        setupEventDetails()
        setupRecyclerView()
    }

    private fun setupEventDetails() {
        binding.apply {
            tvEventTitle.text = eventTitle
            tvEventDate.text = eventDate
            tvEventLocation.text = eventLocation
        }
        Log.d(TAG, "Event details displayed")
    }

    private fun setupRecyclerView() {
        requirementsAdapter = RequirementsAdapter { requirement ->
            navigateToDocumentSubmission(requirement)
        }

        binding.requirementsRecyclerView.apply {
            adapter = requirementsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun navigateToDocumentSubmission(requirement: RequirementItem) {
        Log.d(TAG, "Navigating to DocumentSubmission for requirement: ${requirement.requirement_id}")

        Intent(requireContext(), DocumentSubmissionActivity::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("requirement_id", requirement.requirement_id)
            putExtra("requirement_title", requirement.requirement_name)
            putExtra("requirement_desc", requirement.requirement_desc)
            putExtra("requirement_due_date", requirement.due_date)
            startActivity(this)
        }
    }

    private fun fetchRequirements() {
        val token = TokenManager.getToken(requireContext())
        if (token == null) {
            Log.e(TAG, "No authentication token found")
            showError(getString(R.string.error_auth_required))
            requireActivity().finish()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching requirements for event: $eventId")
                val authToken = "Bearer $token"
                viewModel.getRequirementsByEventId(authToken, eventId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch requirements", e)
                showError(getString(R.string.error_loading_requirements))
            }
        }
    }

    private fun observeViewModel() {
        viewModel.requirements.observe(viewLifecycleOwner) { requirements ->
            val validRequirements = requirements.filter { it.event_id == eventId }
            Log.d(TAG, "Requirements received: ${requirements.size}, Valid: ${validRequirements.size}")
            requirementsAdapter.submitList(validRequirements)
            updateEmptyState(validRequirements.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Loading state changed: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "Error received: $it")
                showError(it)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        Log.d(TAG, "Updating empty state: $isEmpty")
        binding.apply {
            emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            requirementsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}