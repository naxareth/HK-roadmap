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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Adapters.StatusAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Repository.DocumentRepository
import com.second_year.hkroadmap.ViewModel.DocumentViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.Views.DocumentSubmissionActivity
import com.second_year.hkroadmap.Views.LoginActivity
import com.second_year.hkroadmap.databinding.FragmentStatusBinding
import kotlinx.coroutines.launch

class StatusFragment : Fragment() {
    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DocumentViewModel
    private lateinit var statusAdapter: StatusAdapter

    private var eventId: Int = -1
    private var studentId: Int = 0

    companion object {
        private const val TAG = "StatusFragment"

        fun newInstance(eventId: Int): StatusFragment {
            return StatusFragment().apply {
                arguments = Bundle().apply {
                    putInt("event_id", eventId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            eventId = it.getInt("event_id", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDependencies()
        getStudentId() // Add this function call
        setupViews()
        observeViewModel()
        fetchDocumentStatus()
    }

    private fun setupDependencies() {
        Log.d(TAG, "Setting up dependencies")
        val apiService = RetrofitInstance.createApiService()
        val repository = DocumentRepository(apiService)
        val factory = ViewModelFactory(documentRepository = repository)
        viewModel = ViewModelProvider(this, factory)[DocumentViewModel::class.java]
    }

    private fun setupViews() {
        Log.d(TAG, "Setting up views")
        setupRecyclerView()
        setupChipGroup()
    }

    private fun setupRecyclerView() {
        statusAdapter = StatusAdapter { document ->
            showDocumentDetails(document)
        }

        binding.statusRecyclerView.apply {
            adapter = statusAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun setupChipGroup() {
        binding.statusFilterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            when (checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it) }) {
                binding.chipAll -> {
                    Log.d(TAG, "Filter: All selected")
                    // Filter out draft and missing statuses
                    viewModel.filterDocuments("submitted")  // This will be handled in ViewModel
                }
                binding.chipPending -> {
                    Log.d(TAG, "Filter: Pending selected")
                    viewModel.filterDocuments("pending")
                }
                binding.chipApproved -> {
                    Log.d(TAG, "Filter: Approved selected")
                    viewModel.filterDocuments("approved")
                }
                binding.chipRejected -> {
                    Log.d(TAG, "Filter: Rejected selected")
                    viewModel.filterDocuments("rejected")
                }
                null -> {
                    // If no chip is selected, select "All" by default
                    binding.chipAll.isChecked = true
                }
            }
        }

        // Set initial selection
        binding.chipAll.isChecked = true
    }

    private fun fetchDocumentStatus() {
        val token = TokenManager.getToken(requireContext())
        if (token == null) {
            Log.e(TAG, "No authentication token found")
            showError(getString(R.string.error_auth_required))
            requireActivity().finish()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching document status for event: $eventId")
                viewModel.getDocumentsByEventId(token, eventId)  // Remove the "Bearer" prefix
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch document status", e)
                showError(getString(R.string.error_loading_status))
            }
        }
    }

    private fun observeViewModel() {
        // Observe filtered documents instead of all documents
        viewModel.filteredDocuments.observe(viewLifecycleOwner) { documents ->
            Log.d(TAG, "Filtered documents received: ${documents.size}")
            statusAdapter.submitList(documents)
            updateEmptyState(documents.isEmpty())
            updateChipCounts(viewModel.getDocumentStatistics())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Loading state changed: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "Error received: $it")
                showError(it)
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                showMessage(it)
                viewModel.clearMessages()
            }
        }
    }

    private fun updateChipCounts(statistics: Map<String, Int>) {
        // Filter only submitted documents (pending, approved, rejected)
        val submittedStatuses = listOf("pending", "approved", "rejected")
        val filteredStats = statistics.filterKeys { it in submittedStatuses }

        binding.apply {
            chipAll.text = getString(R.string.all_documents_count, filteredStats.values.sum())
            chipPending.text = getString(R.string.pending_count, filteredStats["pending"] ?: 0)
            chipApproved.text = getString(R.string.approved_count, filteredStats["approved"] ?: 0)
            chipRejected.text = getString(R.string.rejected_count, filteredStats["rejected"] ?: 0)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        Log.d(TAG, "Updating empty state: $isEmpty")
        binding.apply {
            emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            statusRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun getStudentId() {
        val token = TokenManager.getToken(requireContext()) ?: run {
            showError(getString(R.string.error_auth_required))
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val apiService = RetrofitInstance.createApiService()
                val response = apiService.getStudentProfile("Bearer $token")
                studentId = response.id
                Log.d(TAG, "Retrieved student ID: $studentId")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting student profile", e)
                showError(getString(R.string.error_loading_profile))
                redirectToLogin()
            }
        }
    }


    private fun showDocumentDetails(document: DocumentResponse) {
        if (studentId == 0) {
            showError(getString(R.string.error_student_id_not_found))
            return
        }

        Log.d(TAG, """
        Navigating to DocumentSubmission:
        - Document ID: ${document.document_id}
        - Event ID: ${document.event_id}
        - Requirement ID: ${document.requirement_id}
        - Requirement Title: ${document.requirement_title}
        - Due Date: ${document.requirement_due_date}
        - Student ID: $studentId
        """.trimIndent())

        Intent(requireContext(), DocumentSubmissionActivity::class.java).apply {
            putExtra("event_id", document.event_id)
            putExtra("requirement_id", document.requirement_id)
            putExtra("student_id", studentId)  // Add student_id
            putExtra("requirement_title", document.requirement_title)
            putExtra("requirement_desc", "") // If you have requirement description in DocumentResponse, add it here
            putExtra("requirement_due_date", document.requirement_due_date)
            startActivity(this)
        }
    }

    // Add helper function for login redirection
    private fun redirectToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }
    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showMessage(message: String) {
        Log.d(TAG, "Showing message: $message")
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}