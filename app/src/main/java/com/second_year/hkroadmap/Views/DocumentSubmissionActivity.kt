package com.second_year.hkroadmap.Views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonParseException
import com.second_year.hkroadmap.Adapters.DocumentAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Repository.DocumentRepository
import com.second_year.hkroadmap.Utils.FileUtils
import com.second_year.hkroadmap.ViewModel.DocumentViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.databinding.ActivityDocumentSubmissionBinding
import java.io.File

class DocumentSubmissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocumentSubmissionBinding
    private lateinit var viewModel: DocumentViewModel
    private lateinit var documentAdapter: DocumentAdapter

    companion object {
        private const val TAG = "DocumentSubmissionActivity"
    }

    private var eventId: Int = 0
    private var requirementId: Int = 0

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Log.d(TAG, "File selected: $uri")
            handleSelectedFile(it)
        } ?: Log.w(TAG, "No file selected")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentSubmissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkToken()
        Log.d(TAG, "Activity created")

        eventId = intent.getIntExtra("event_id", 0)
        requirementId = intent.getIntExtra("requirement_id", 0)

        Log.d(TAG, "Received IDs - eventId: $eventId, requirementId: $requirementId")

        if (eventId == 0 || requirementId == 0) {
            Log.e(TAG, "Invalid IDs received. Finishing activity")
            showError(getString(R.string.error_invalid_ids))
            finish()
            return
        }

        setupDependencies()
        setupViews()
        observeViewModel()
        loadDocuments()
    }

    private fun checkToken() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Log.e(TAG, "No valid token found - redirecting to login")
            redirectToLogin()
            return
        }
        Log.d(TAG, "Token validation successful")
    }

    private fun redirectToLogin() {
        Log.d(TAG, "Redirecting to login activity")
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupDependencies() {
        Log.d(TAG, "Setting up dependencies")
        val token = TokenManager.getToken(this)
        Log.d(TAG, "Current token status: ${if (token.isNullOrEmpty()) "MISSING" else "PRESENT"}")

        val apiService = RetrofitInstance.createApiService()
        val documentRepository = DocumentRepository(apiService)
        val factory = ViewModelFactory(documentRepository)
        viewModel = ViewModelProvider(this, factory)[DocumentViewModel::class.java]

        Log.d(TAG, "Dependencies setup completed")
    }

    private fun setupViews() {
        Log.d(TAG, "Setting up views")
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
        Log.d(TAG, "Toolbar setup completed")
    }

    private fun setupRecyclerView() {
        documentAdapter = DocumentAdapter { document ->
            Log.d(TAG, "Delete requested for document: ${document.id}")
            deleteDocument(document)
        }

        binding.rvDocuments.apply {
            layoutManager = LinearLayoutManager(this@DocumentSubmissionActivity)
            adapter = documentAdapter
        }
        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun setupClickListeners() {
        binding.fabAddDocument.setOnClickListener {
            Log.d(TAG, "FAB clicked - launching file picker")
            getContent.launch("*/*")
        }

        binding.toolbar.setNavigationOnClickListener {
            Log.d(TAG, "Navigation back clicked")
            onBackPressed()
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            Log.d(TAG, "Processing file from URI: $uri")
            val file = FileUtils.getFileFromUri(this, uri)

            if (file != null) {
                Log.d(TAG, "File created successfully - Name: ${file.name}, Size: ${file.length()} bytes")
                uploadDocument(file)
            } else {
                Log.e(TAG, "Failed to create file from URI")
                showError(getString(R.string.error_processing_file))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling file: ${e.message}", e)
            showError(e.message ?: getString(R.string.error_unknown))
        }
    }

    private fun uploadDocument(file: File) {
        val token = TokenManager.getToken(this)

        Log.d(TAG, """
            Attempting document upload:
            - Token status: ${if (token.isNullOrEmpty()) "MISSING" else "PRESENT"}
            - File name: ${file.name}
            - File size: ${file.length()} bytes
            - Event ID: $eventId
            - Requirement ID: $requirementId
        """.trimIndent())

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Upload failed: No authentication token")
            redirectToLogin()
            return
        }

        try {
            viewModel.uploadDocument(
                token = token,
                file = file,
                eventId = eventId,
                requirementId = requirementId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during upload: ${e.message}", e)
            showError(getString(R.string.error_upload_failed))
        }
    }

    private fun deleteDocument(document: DocumentResponse) {
        val token = TokenManager.getToken(this)

        Log.d(TAG, """
            Attempting document deletion:
            - Document ID: ${document.id}
            - Token status: ${if (token.isNullOrEmpty()) "MISSING" else "PRESENT"}
            - Event ID: $eventId
            - Requirement ID: $requirementId
        """.trimIndent())

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Deletion failed: No authentication token")
            redirectToLogin()
            return
        }

        try {
            viewModel.deleteDocument(
                token = token,
                eventId = eventId,
                requirementId = requirementId,
                documentId = document.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during deletion: ${e.message}", e)
            showError(getString(R.string.error_delete_failed))
        }
    }

    private fun loadDocuments() {
        val token = TokenManager.getToken(this)

        Log.d(TAG, "Loading documents - Token status: ${if (token.isNullOrEmpty()) "MISSING" else "PRESENT"}")

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Document loading failed: No authentication token")
            redirectToLogin()
            return
        }

        try {
            viewModel.getStudentDocuments(token)
        } catch (e: JsonParseException) {
            Log.e(TAG, "JSON parsing error: ${e.message}", e)
            handleJsonParseError(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading documents: ${e.message}", e)
            showError(e.message ?: getString(R.string.error_unknown))
        }
    }

    private fun handleJsonParseError(error: JsonParseException) {
        Log.e(TAG, "JSON Parse Error Details:", error)
        showError(getString(R.string.error_loading_documents))
    }

    private fun observeViewModel() {
        viewModel.studentDocuments.observe(this) { documents ->
            try {
                // Handle the array response directly
                val filteredDocs = documents?.filter {
                    it.event_id == eventId && it.requirement_id == requirementId
                } ?: emptyList()

                Log.d(TAG, "Documents received: ${documents?.size ?: 0}, Filtered: ${filteredDocs.size}")
                documentAdapter.submitList(filteredDocs)
                updateEmptyState(filteredDocs.isEmpty())
            } catch (e: Exception) {
                Log.e(TAG, "Error processing documents: ${e.message}", e)
                showError(getString(R.string.error_processing_documents))
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading state changed: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Log.e(TAG, "Error received: $it")
                showError(it)
            }
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Log.d(TAG, "Success: $it")
                showSuccess(it)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        Log.d(TAG, "Updating empty state: $isEmpty")
        binding.apply {
            layoutNoAttachments.visibility = if (isEmpty) View.VISIBLE else View.GONE
            rvDocuments.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Log.d(TAG, "Showing success: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}