package com.second_year.hkroadmap.Views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.second_year.hkroadmap.Adapters.DocumentAdapter
import com.second_year.hkroadmap.Api.Interfaces.RetrofitInstance
import com.second_year.hkroadmap.Api.Interfaces.TokenManager
import com.second_year.hkroadmap.Api.Models.DocumentResponse
import com.second_year.hkroadmap.R
import com.second_year.hkroadmap.Repository.DocumentRepository
import com.second_year.hkroadmap.Utils.DocumentDownloadUtils
import com.second_year.hkroadmap.Utils.DocumentLoggingUtils
import com.second_year.hkroadmap.Utils.DocumentUploadUtils
import com.second_year.hkroadmap.Utils.DocumentViewerUtils
import com.second_year.hkroadmap.Utils.FileUtils
import com.second_year.hkroadmap.ViewModel.DocumentViewModel
import com.second_year.hkroadmap.ViewModel.ViewModelFactory
import com.second_year.hkroadmap.databinding.ActivityDocumentSubmissionBinding
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        binding.layoutRequirementDetails.isVisible =
            requirementTitle.isNotEmpty() || requirementDueDate.isNotEmpty()

        if (binding.layoutRequirementDetails.isVisible) {
            binding.tvRequirementTitle.text = requirementTitle
            binding.tvRequirementDueDate.text = getString(R.string.due_date_format, requirementDueDate)
        }
    }

    private fun setupRecyclerView() {
        documentAdapter = DocumentAdapter(
            onDeleteClick = { document -> showDeleteConfirmation(document) },
            onViewClick = { document -> viewDocument(document) }
        ).apply {
            setOnDocumentStatusChangedListener {
                updateButtonVisibility()
            }
        }

        binding.rvDocuments.apply {
            layoutManager = LinearLayoutManager(this@DocumentSubmissionActivity)
            adapter = documentAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun updateButtonVisibility() {
        binding.apply {
            val hasDraftDocs = documentAdapter.getDraftCount() > 0
            val hasPendingDocs = documentAdapter.getPendingCount() > 0

            btnSubmit.isVisible = hasDraftDocs
            btnUnsubmit.isVisible = hasPendingDocs

            Log.d(TAG, """
                Button Visibility Update:
                - Draft docs: $hasDraftDocs
                - Pending docs: $hasPendingDocs
            """.trimIndent())
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnUploadFile.setOnClickListener {
                if (isRequirementOverdue()) {
                    showError(getString(R.string.error_past_due))
                    return@setOnClickListener
                }
                getContent.launch("*/*")
            }

            btnAddLink.setOnClickListener {
                if (isRequirementOverdue()) {
                    showError(getString(R.string.error_past_due))
                    return@setOnClickListener
                }
                showAddLinkDialog()
            }

            btnSubmit.setOnClickListener {
                val draftIds = documentAdapter.getDraftDocumentIds()
                if (draftIds.isEmpty()) {
                    showError("No draft documents available to submit")
                    return@setOnClickListener
                }
                showSubmitMultipleConfirmation(draftIds)
            }

            btnUnsubmit.setOnClickListener {
                val pendingIds = documentAdapter.getPendingDocumentIds()
                if (pendingIds.isEmpty()) {
                    showError("No pending documents available to unsubmit")
                    return@setOnClickListener
                }
                showUnsubmitMultipleConfirmation(pendingIds)
            }

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun showSubmitMultipleConfirmation(documentIds: List<Int>) {
        if (isRequirementOverdue()) {
            showError(getString(R.string.error_past_due))
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Submit Documents")
            .setMessage("Are you sure you want to submit ${documentIds.size} documents?")
            .setPositiveButton("Submit") { dialog, _ ->
                submitMultipleDocuments(documentIds)
                documentAdapter.clearDraftDocumentIds()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showUnsubmitMultipleConfirmation(documentIds: List<Int>) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Unsubmit Documents")
            .setMessage("Are you sure you want to unsubmit ${documentIds.size} documents?")
            .setPositiveButton("Unsubmit") { dialog, _ ->
                unsubmitMultipleDocuments(documentIds)
                documentAdapter.clearPendingDocumentIds()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun submitMultipleDocuments(documentIds: List<Int>) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }
        Log.d(TAG, """
        Submitting documents:
        - IDs: $documentIds
        - Total count: ${documentIds.size}
        - Draft IDs available: ${documentAdapter.getDraftDocumentIds()}
    """.trimIndent())
        viewModel.submitMultipleDocuments(token, documentIds)
    }

    private fun unsubmitMultipleDocuments(documentIds: List<Int>) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }
        Log.d(TAG, """
        Unsubmitting documents:
        - IDs: $documentIds
        - Total count: ${documentIds.size}
        - Pending IDs available: ${documentAdapter.getPendingDocumentIds()}
    """.trimIndent())
        viewModel.unsubmitMultipleDocuments(token, documentIds)
    }

    private fun showAddLinkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.link_dialog, null)
        val linkInput = dialogView.findViewById<TextInputEditText>(R.id.et_link)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_link))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                val link = linkInput.text.toString()
                if (link.isNotEmpty()) {
                    if (isValidUrl(link)) {
                        uploadLink(link)
                    } else {
                        showError(getString(R.string.invalid_url))
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun uploadLink(link: String) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }
        viewModel.uploadLinkDocument(token, link, eventId, requirementId)
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            val file = FileUtils.getFileFromUri(this, uri)
            if (file != null) {
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
            Log.e(TAG, "Error handling selected file", e)
            showError(getString(R.string.error_processing_file))
        }
    }

    private fun uploadDocument(file: File) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }
        viewModel.uploadDocument(token, file, eventId, requirementId)
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

    private fun deleteDocument(document: DocumentResponse) {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }
        documentAdapter.removeDocumentId(document.document_id)
        viewModel.deleteDocument(token, document.document_id)
    }

    private fun viewDocument(document: DocumentResponse) {
        if (document.document_type == "link") {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(document.link_url))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening link", e)
                showError(getString(R.string.unable_to_open_link))
            }
            return
        }

        // Show loading indicator
        binding.progressBar.isVisible = true

        // Launch coroutine to handle file download and viewing
        lifecycleScope.launch {
            try {
                DocumentViewerUtils.createViewIntent(this@DocumentSubmissionActivity, document).fold(
                    onSuccess = { intent ->
                        startActivity(Intent.createChooser(intent, getString(R.string.view_document)))
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error viewing document", e)
                        showError(getString(R.string.error_opening_document))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error viewing document", e)
                showError(getString(R.string.error_opening_document))
            } finally {
                binding.progressBar.isVisible = false
            }
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

    override fun onDestroy() {
        super.onDestroy()
        DocumentDownloadUtils.clearCache(this)
    }

    private fun isRequirementOverdue(): Boolean {
        if (requirementDueDate.isEmpty()) return false
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dueDate = dateFormat.parse(requirementDueDate)
            val currentDate = Date()
            return dueDate?.before(currentDate) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing due date", e)
            return false
        }
    }

    private fun observeViewModel() {
        viewModel.studentDocuments.observe(this) { documents ->
            try {
                Log.d(TAG, "Received ${documents?.size} documents")
                val filteredDocs = documents?.filter { doc ->
                    doc.event_id == eventId && doc.requirement_id == requirementId
                } ?: emptyList()

                Log.d(TAG, "Filtered to ${filteredDocs.size} documents for current requirement")
                documentAdapter.submitList(filteredDocs)
                updateEmptyState(filteredDocs.isEmpty())
            } catch (e: Exception) {
                Log.e(TAG, "Error processing documents", e)
                showError(getString(R.string.error_processing_documents))
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showError(it)
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let {
                showSuccess(it)
                viewModel.clearMessages()
            }
        }
    }

    private fun loadDocuments() {
        val token = TokenManager.getToken(this) ?: run {
            redirectToLogin()
            return
        }
        viewModel.getStudentDocuments(token)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            layoutNoAttachments.isVisible = isEmpty
            rvDocuments.isVisible = !isEmpty
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}