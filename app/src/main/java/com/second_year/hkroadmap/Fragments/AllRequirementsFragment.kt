package com.second_year.hkroadmap.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonParseException
import com.second_year.hkroadmap.Adapters.ExpandableEventAdapter
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.EventItem
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Views.DocumentSubmissionActivity
import com.second_year.hkroadmap.Views.LoginActivity
import com.second_year.hkroadmap.databinding.FragmentAllRequirementsBinding
import kotlinx.coroutines.launch
import java.io.EOFException

class AllRequirementsFragment : Fragment() {
    private var _binding: FragmentAllRequirementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: ExpandableEventAdapter
    private lateinit var apiService: ApiService
    private var studentId: Int = 0

    companion object {
        private const val TAG = "AllRequirementsFragment"
    }

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
        getStudentId()
    }

    private fun setupDependencies() {
        try {
            apiService = RetrofitInstance.createApiService()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up dependencies", e)
            showError(getString(R.string.error_setup_failed))
            redirectToLogin()
        }
    }

    private fun getStudentId() {
        val token = TokenManager.getToken(requireContext()) ?: run {
            Log.d(TAG, "No token found, redirecting to login")
            redirectToLogin()
            return
        }

        binding.progressBar.isVisible = true
        lifecycleScope.launch {
            try {
                val response = apiService.getStudentProfile("Bearer $token")
                if (response.id == 0) {
                    Log.e(TAG, "Invalid student ID received")
                    showError(getString(R.string.error_invalid_student_id))
                    redirectToLogin()
                    return@launch
                }

                studentId = response.id  // Use id instead of student_id
                Log.d(TAG, "Retrieved student ID: $studentId")

                // Only proceed with setup after getting student ID
                setupRecyclerView()
                fetchAllRequirements()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting student profile", e)
                when (e) {
                    is EOFException -> {
                        Log.e(TAG, "Empty response from server")
                        showError(getString(R.string.error_empty_response))
                        redirectToLogin()
                    }
                    is JsonParseException -> {
                        Log.e(TAG, "Invalid JSON response")
                        showError(getString(R.string.error_invalid_response))
                        redirectToLogin()
                    }
                    else -> {
                        showError(getString(R.string.error_loading_profile))
                        redirectToLogin()
                    }
                }
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun setupRecyclerView() {
        try {
            eventAdapter = ExpandableEventAdapter { requirement ->
                navigateToDocumentSubmission(requirement)
            }

            binding.requirementsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = eventAdapter
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
            showError(getString(R.string.error_setup_failed))
        }
    }

    private fun navigateToDocumentSubmission(requirement: RequirementItem) {
        try {
            if (studentId == 0) {
                Log.e(TAG, "Attempting to navigate with invalid student ID")
                showError(getString(R.string.error_student_id_not_found))
                return
            }

            Log.d(TAG, """
                Navigating to DocumentSubmission:
                - Event ID: ${requirement.event_id}
                - Requirement ID: ${requirement.requirement_id}
                - Student ID: $studentId
                - Requirement Title: ${requirement.requirement_name}
            """.trimIndent())

            Intent(requireContext(), DocumentSubmissionActivity::class.java).also { intent ->
                intent.putExtra("event_id", requirement.event_id)
                intent.putExtra("requirement_id", requirement.requirement_id)
                intent.putExtra("student_id", studentId)
                intent.putExtra("requirement_title", requirement.requirement_name)
                intent.putExtra("requirement_desc", requirement.requirement_desc)
                intent.putExtra("requirement_due_date", requirement.due_date)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to DocumentSubmissionActivity", e)
            showError(getString(R.string.error_navigation_failed))
        }
    }

    fun fetchAllRequirements() {
        binding.progressBar.isVisible = true
        binding.emptyStateText.isVisible = false
        binding.requirementsRecyclerView.isVisible = false

        lifecycleScope.launch {
            try {
                val token = TokenManager.getToken(requireContext()) ?: run {
                    Log.e(TAG, "Token not found during requirements fetch")
                    showError(getString(R.string.error_auth_required))
                    redirectToLogin()
                    return@launch
                }

                // Get all events and their requirements first
                val events = apiService.getEvents("Bearer $token")
                val allDocuments = apiService.getStudentDocuments("Bearer $token").body()?.documents ?: emptyList()

                // Group documents by requirement ID for quick lookup
                val documentsByRequirement = allDocuments.groupBy { it.requirement_id }

                val eventsWithReqs = events.mapNotNull { event ->
                    // Get requirements for this specific event only
                    val requirements = apiService.getRequirementsByEventId("Bearer $token", event.event_id)

                    // Filter requirements that either:
                    // 1. Have no documents OR
                    // 2. Only have documents in draft status (is_submitted = 0)
                    // 3. Ensure the requirement belongs to this event
                    val pendingRequirements = requirements.filter { requirement ->
                        // First verify this requirement belongs to this event
                        requirement.event_id == event.event_id &&
                                documentsByRequirement[requirement.requirement_id]?.let { docs ->
                                    // Only include if all documents are in draft status
                                    // Explicitly exclude any that have 'missing' status
                                    docs.isNotEmpty() &&
                                            docs.all { doc -> doc.status != "missing" } &&
                                            docs.all { doc -> doc.is_submitted == 0 }
                                } ?: true  // If no documents exist (null case), include the requirement
                    }

                    // Only create EventWithRequirements if there are pending requirements for this event
                    if (pendingRequirements.isEmpty()) {
                        null
                    } else {
                        ExpandableEventAdapter.EventWithRequirements(
                            event = EventResponse(
                                id = event.event_id,
                                title = event.event_name,
                                description = "",
                                date = event.date,
                                location = "",
                                created_at = "",
                                updated_at = ""
                            ),
                            requirements = pendingRequirements,
                            isExpanded = false
                        )
                    }
                }

                if (eventsWithReqs.isEmpty()) {
                    binding.emptyStateText.text = getString(R.string.no_pending_requirements)
                    binding.emptyStateText.isVisible = true
                } else {
                    eventAdapter.setEvents(eventsWithReqs)
                    binding.requirementsRecyclerView.isVisible = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching requirements", e)
                binding.emptyStateText.text = getString(R.string.error_loading_requirements)
                binding.emptyStateText.isVisible = true
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun showError(message: String) {
        try {
            if (isAdded) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error message", e)
        }
    }

    private fun redirectToLogin() {
        try {
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error redirecting to login", e)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}