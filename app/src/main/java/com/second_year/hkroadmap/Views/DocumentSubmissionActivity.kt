package com.second_year.hkroadmap.Views

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Adapters.DocumentAdapter
import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Repository.DocumentRepository
import com.second_year.hkroadmap.Utils.FileUtils
import com.second_year.hkroadmap.Utils.SessionManager
import com.second_year.hkroadmap.ViewModel.DocumentViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.databinding.ActivityDocumentSubmissionBinding
import java.io.File

class DocumentSubmissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocumentSubmissionBinding
    private lateinit var viewModel: DocumentViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var documentAdapter: DocumentAdapter

    private var eventId: Int = 0
    private var requirementId: Int = 0

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentSubmissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get IDs from intent extras
        eventId = intent.getIntExtra("event_id", 0)
        requirementId = intent.getIntExtra("requirement_id", 0)

        if (eventId == 0 || requirementId == 0) {
            showError(getString(R.string.error_invalid_ids))
            finish()
            return
        }

        setupDependencies()
        setupViews()
        observeViewModel()
        loadDocuments()
    }

    private fun setupDependencies() {
        sessionManager = SessionManager(this)
        val apiService = RetrofitInstance.createApiService()
        val documentRepository = DocumentRepository(apiService)
        val factory = ViewModelFactory(documentRepository)
        viewModel = ViewModelProvider(this, factory)[DocumentViewModel::class.java]
    }

    private fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_submit_documents)
        }
    }

    private fun setupRecyclerView() {
        documentAdapter = DocumentAdapter { document ->
            deleteDocument(document)
        }

        binding.rvDocuments.apply {
            layoutManager = LinearLayoutManager(this@DocumentSubmissionActivity)
            adapter = documentAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddDocument.setOnClickListener {
            getContent.launch("*/*")
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            val file = FileUtils.getFileFromUri(this, uri)
            file?.let {
                uploadDocument(it)
            } ?: showError(getString(R.string.error_processing_file))
        } catch (e: Exception) {
            showError(e.message ?: getString(R.string.error_unknown))
        }
    }

    private fun uploadDocument(file: File) {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            showError(getString(R.string.error_auth_required))
            return
        }

        viewModel.uploadDocument(
            token = token,
            file = file,
            eventId = eventId,
            requirementId = requirementId
        )
    }

    private fun deleteDocument(document: DocumentResponse) {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            showError(getString(R.string.error_auth_required))
            return
        }

        viewModel.deleteDocument(
            token = token,
            eventId = eventId,
            requirementId = requirementId,
            documentId = document.id
        )
    }

    private fun loadDocuments() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            showError(getString(R.string.error_auth_required))
            return
        }
        viewModel.getStudentDocuments(token)
    }

    private fun observeViewModel() {
        viewModel.studentDocuments.observe(this) { documents ->
            val filteredDocs = documents.filter {
                it.event_id == eventId && it.requirement_id == requirementId
            }
            documentAdapter.submitList(filteredDocs)
            updateEmptyState(filteredDocs.isEmpty())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let { showError(it) }
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let { showSuccess(it) }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            layoutNoAttachments.visibility = if (isEmpty) View.VISIBLE else View.GONE
            rvDocuments.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}