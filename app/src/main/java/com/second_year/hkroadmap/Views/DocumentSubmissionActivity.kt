package com.second_year.hkroadmap.Views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.second_year.hkroadmap.Adapters.DocumentAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Repository.DocumentRepository
import com.second_year.hkroadmap.Utils.DocumentUploadUtils
import com.second_year.hkroadmap.Utils.FileUtils
import com.second_year.hkroadmap.ViewModel.DocumentViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.databinding.ActivityDocumentSubmissionBinding
import java.io.File

class DocumentSubmissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocumentSubmissionBinding
    private lateinit var viewModel: DocumentViewModel
    private lateinit var documentAdapter: DocumentAdapter

    private var eventId: Int = 0
    private var requirementId: Int = 0
    private var requirementTitle: String = ""
    private var requirementDueDate: String = ""

    companion object {
        private const val TAG = "DocumentActivity"
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentSubmissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkToken()
        setupIntentExtras()
        setupDependencies()
        setupViews()
        observeViewModel()
        loadDocuments()
    }

    private fun setupIntentExtras() {
        intent.extras?.let { extras ->
            eventId = extras.getInt("event_id", 0)
            requirementId = extras.getInt("requirement_id", 0)
            requirementTitle = extras.getString("requirement_title", "")
            requirementDueDate = extras.getString("requirement_due_date", "")

            Log.d(TAG, """
                Received Extras:
                - Event ID: $eventId
                - Requirement ID: $requirementId
                - Title: $requirementTitle
                - Due Date: $requirementDueDate
            """.trimIndent())
        }

        if (eventId == 0 || requirementId == 0) {
            showError(getString(R.string.error_invalid_ids))
            finish()
        }
    }

    private fun checkToken() {
        if (TokenManager.getToken(this) == null) {
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun setupDependencies() {
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
            title = requirementTitle.ifEmpty {
                getString(R.string.title_submit_documents)
            }
        }

        if (requirementTitle.isNotEmpty() || requirementDueDate.isNotEmpty()) {
            binding.layoutRequirementDetails.visibility = View.VISIBLE
            binding.tvRequirementTitle.text = requirementTitle
            binding.tvRequirementDueDate.text = getString(R.string.due_date_format, requirementDueDate)
        } else {
            binding.layoutRequirementDetails.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        documentAdapter = DocumentAdapter(
            onDeleteClick = { document -> showDeleteConfirmation(document) },
            onViewClick = { document -> viewDocument(document) },
            onSubmitClick = { document -> showSubmitConfirmation(document) },
            onUnsubmitClick = { document -> showUnsubmitConfirmation(document) }
        )

        binding.rvDocuments.apply {
            layoutManager = LinearLayoutManager(this@DocumentSubmissionActivity)
            adapter = documentAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnUploadFile.setOnClickListener {
            getContent.launch("*/*")
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewModel.studentDocuments.observe(this) { documents ->
            try {
                Log.d(TAG, "All documents received: ${documents?.size}")

                val filteredDocs = documents?.filter { doc ->
                    doc.event_id == eventId && doc.requirement_id == requirementId
                } ?: emptyList()

                Log.d(TAG, """
                    Filtering Results:
                    - Total documents: ${filteredDocs.size}
                    - Filtered documents: ${filteredDocs.size}
                    - For Event ID: $eventId
                    - For Requirement ID: $requirementId
                """.trimIndent())

                documentAdapter.submitList(filteredDocs)
                updateEmptyState(filteredDocs.isEmpty())
            } catch (e: Exception) {
                Log.e(TAG, "Error processing documents", e)
                showError(getString(R.string.error_processing_documents))
            }
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

    private fun loadDocuments() {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }

        try {
            viewModel.getStudentDocuments(token)
        } catch (e: Exception) {
            showError(getString(R.string.error_loading_documents))
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            val file = FileUtils.getFileFromUri(this, uri)
            if (file != null) {
                // Validate before upload
                val error = DocumentUploadUtils.getFileError(file)
                if (error != null) {
                    showError(error)
                    return
                }
                uploadDocument(file)
            } else {
                showError(getString(R.string.error_processing_file))
            }
        } catch (e: Exception) {
            showError(getString(R.string.error_processing_file))
        }
    }

    private fun uploadDocument(file: File) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }

        try {
            viewModel.uploadDocument(token, file, eventId, requirementId)
        } catch (e: Exception) {
            showError(getString(R.string.error_upload_failed))
        }
    }

    private fun showDeleteConfirmation(document: DocumentResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_confirmation_title))
            .setMessage(getString(R.string.delete_confirmation_message))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                deleteDocument(document)
                dialog.dismiss()
            }
            .show()
    }

    private fun showSubmitConfirmation(document: DocumentResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.submit_confirmation_title))
            .setMessage(getString(R.string.submit_confirmation_message))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                submitDocument(document)
                dialog.dismiss()
            }
            .show()
    }

    private fun showUnsubmitConfirmation(document: DocumentResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.unsubmit_confirmation_title))
            .setMessage(getString(R.string.unsubmit_confirmation_message))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                unsubmitDocument(document)
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteDocument(document: DocumentResponse) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }

        try {
            viewModel.deleteDocument(token, document.document_id)
        } catch (e: Exception) {
            showError(getString(R.string.error_delete_failed))
        }
    }

    private fun submitDocument(document: DocumentResponse) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }

        try {
            viewModel.submitDocument(token, document.document_id)
        } catch (e: Exception) {
            showError(getString(R.string.error_submit_failed))
        }
    }

    private fun unsubmitDocument(document: DocumentResponse) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }

        try {
            viewModel.unsubmitDocument(token, document.document_id)
        } catch (e: Exception) {
            showError(getString(R.string.error_unsubmit_failed))
        }
    }

    private fun viewDocument(document: DocumentResponse) {
        try {
            val file = File(document.file_path)
            if (!file.exists()) {
                showError(getString(R.string.error_file_not_found))
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, getString(R.string.view_document)))
        } catch (e: Exception) {
            showError(getString(R.string.error_opening_document))
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "*/*"
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