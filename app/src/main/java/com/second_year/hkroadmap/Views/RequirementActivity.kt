package com.second_year.hkroadmap.Views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Adapters.RequirementsAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Models.RequirementItem
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Repository.RequirementRepository
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.ViewModel.RequirementViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.databinding.ActivityRequirementBinding
import kotlinx.coroutines.launch

class RequirementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequirementBinding
    private lateinit var viewModel: RequirementViewModel
    private lateinit var requirementsAdapter: RequirementsAdapter

    companion object {
        private const val TAG = "RequirementActivity"
    }

    private var eventId: Int = -1
    private var eventTitle: String = ""
    private var eventDate: String = ""
    private var eventLocation: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequirementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Activity created")

        // Get event details from intent
        intent.extras?.let { extras ->
            eventId = extras.getInt("event_id", -1)
            eventTitle = extras.getString("event_title", "")
            eventDate = extras.getString("event_date", "")
            eventLocation = extras.getString("event_location", "TBD")

            Log.d(TAG, """
                Received event details:
                - ID: $eventId
                - Title: $eventTitle
                - Date: $eventDate
                - Location: $eventLocation
            """.trimIndent())
        }

        if (eventId == -1) {
            Log.e(TAG, "Invalid event ID received")
            showError(getString(R.string.error_invalid_event))
            finish()
            return
        }

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
        setupToolbar()
        setupEventDetails()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = eventTitle
        }
        Log.d(TAG, "Toolbar setup completed with title: $eventTitle")
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
            Intent(this, DocumentSubmissionActivity::class.java).also { intent ->
                intent.putExtra("event_id", eventId)
                intent.putExtra("requirement_id", requirement.requirement_id)
                intent.putExtra("requirement_title", requirement.requirement_name)
                intent.putExtra("requirement_desc", requirement.requirement_desc) // Add this line
                intent.putExtra("requirement_due_date", requirement.due_date)
                startActivity(intent)
            }
        }

        binding.requirementsRecyclerView.apply {
            adapter = requirementsAdapter
            layoutManager = LinearLayoutManager(this@RequirementActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun navigateToDocumentSubmission(requirement: RequirementItem) {
        Log.d(TAG, """
        Navigating to DocumentSubmission:
        - Event ID: $eventId
        - Requirement ID: ${requirement.requirement_id}
        - Requirement Name: ${requirement.requirement_name}
        - Requirement Description: ${requirement.requirement_desc}
        - Due Date: ${requirement.due_date}
    """.trimIndent())

        Intent(this, DocumentSubmissionActivity::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("requirement_id", requirement.requirement_id)
            putExtra("requirement_title", requirement.requirement_name)
            putExtra("requirement_desc", requirement.requirement_desc) // Add this line
            putExtra("requirement_due_date", requirement.due_date)
            startActivity(this)
        }
    }

    private fun fetchRequirements() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Log.e(TAG, "No authentication token found")
            showError(getString(R.string.error_auth_required))
            finish()
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
        viewModel.requirements.observe(this) { requirements ->
            val validRequirements = requirements.filter { it.event_id == eventId }
            Log.d(TAG, "Requirements received: ${requirements.size}, Valid: ${validRequirements.size}")
            requirementsAdapter.submitList(validRequirements)
            updateEmptyState(validRequirements.isEmpty())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading state changed: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.d(TAG, "Back navigation selected")
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}