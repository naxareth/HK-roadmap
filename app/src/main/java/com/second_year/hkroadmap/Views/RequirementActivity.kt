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
import com.second_year.hkroadmap.ViewModel.RequirementViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.databinding.ActivityRequirementBinding
import kotlinx.coroutines.launch

class RequirementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequirementBinding
    private lateinit var viewModel: RequirementViewModel
    private lateinit var requirementsAdapter: RequirementsAdapter

    private var eventId: Int = -1
    private var eventTitle: String = ""
    private val TAG = "RequirementActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequirementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get event details from intent
        eventId = intent.getIntExtra("event_id", -1)
        eventTitle = intent.getStringExtra("event_title") ?: "Requirements"

        if (eventId == -1) {
            showError("Invalid event ID")
            finish()
            return
        }

        setupDependencies()
        setupViews()
        observeViewModel()
        fetchRequirements()
    }

    private fun setupDependencies() {
        val apiService = RetrofitInstance.createApiService()
        val repository = RequirementRepository(apiService)
        val factory = ViewModelFactory(requirementRepository = repository)
        viewModel = ViewModelProvider(this, factory)[RequirementViewModel::class.java]
    }

    private fun setupViews() {
        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = eventTitle
        }
    }

    private fun setupRecyclerView() {
        requirementsAdapter = RequirementsAdapter { requirement ->
            navigateToDocumentSubmission(requirement)
        }

        binding.requirementsRecyclerView.apply {
            adapter = requirementsAdapter
            layoutManager = LinearLayoutManager(this@RequirementActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun navigateToDocumentSubmission(requirement: RequirementItem) {
        Intent(this, DocumentSubmissionActivity::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("requirement_id", requirement.requirement_id)
            startActivity(this)
        }
    }

    private fun fetchRequirements() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            showError("Authentication required")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val authToken = "Bearer $token"
                viewModel.getRequirementsByEventId(authToken, eventId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch requirements", e)
                showError("Failed to load requirements: ${e.message}")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.requirements.observe(this) { requirements ->
            val validRequirements = requirements.filter { it.event_id == eventId }
            requirementsAdapter.submitList(validRequirements)
            updateEmptyState(validRequirements.isEmpty())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let { showError(it) }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            requirementsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}