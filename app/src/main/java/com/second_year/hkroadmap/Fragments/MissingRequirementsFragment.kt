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
import com.second_year.hkroadmap.Api.Models.EventResponse
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Views.DocumentSubmissionActivity
import com.second_year.hkroadmap.Views.LoginActivity
import com.second_year.hkroadmap.databinding.FragmentMissingRequirementsBinding
import kotlinx.coroutines.launch
import java.io.EOFException


class MissingRequirementsFragment : Fragment() {
    private var _binding: FragmentMissingRequirementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: ExpandableEventAdapter
    private lateinit var apiService: ApiService
    private var studentId: Int = 0

    companion object {
        private const val TAG = "MissingReqFragment"
    }

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

                studentId = response.id
                Log.d(TAG, "Retrieved student ID: $studentId")

                setupRecyclerView()
                fetchMissingRequirements()
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
            }
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = ExpandableEventAdapter { requirement ->
            navigateToDocumentSubmission(requirement)
        }

        binding.requirementsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
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

    fun fetchMissingRequirements() {
        binding.progressBar.isVisible = true
        binding.emptyStateText.isVisible = false

        lifecycleScope.launch {
            try {
                val token = "Bearer ${TokenManager.getToken(requireContext())}"
                val documents = apiService.getStudentDocuments(token).body()?.documents ?: emptyList()

                val missingDocs = documents.filter { doc ->
                    doc.status == "missing" &&
                            doc.requirement_id != 0 &&
                            !doc.requirement_title.isNullOrEmpty() &&
                            !doc.requirement_due_date.isNullOrEmpty()
                }

                if (missingDocs.isEmpty()) {
                    binding.emptyStateText.text = "No missing requirements"
                    binding.emptyStateText.isVisible = true
                } else {
                    val eventGroups = missingDocs.groupBy { it.event_id }
                    val eventId = eventGroups.keys.firstOrNull()
                    val requirements = if (eventId != null) {
                        apiService.getRequirementsByEventId(token, eventId)
                    } else emptyList()

                    val requirementMap = requirements.associateBy { it.requirement_id }

                    val eventsWithReqs = eventGroups.mapNotNull { (eventId, docs) ->
                        if (docs.isEmpty()) return@mapNotNull null

                        val firstDoc = docs.first()
                        if (firstDoc.event_title.isNullOrEmpty()) return@mapNotNull null

                        val uniqueRequirements = docs.groupBy { it.requirement_id }
                            .mapNotNull { (reqId, reqDocs) ->
                                if (reqId == 0) return@mapNotNull null
                                reqDocs.first()
                            }

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
                    }.filter { it.requirements.isNotEmpty() }

                    if (eventsWithReqs.isEmpty()) {
                        binding.emptyStateText.text = "No valid missing requirements found"
                        binding.emptyStateText.isVisible = true
                    } else {
                        eventAdapter.setEvents(eventsWithReqs)
                        binding.requirementsRecyclerView.isVisible = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching missing requirements", e)
                binding.emptyStateText.text = "Error loading missing requirements"
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