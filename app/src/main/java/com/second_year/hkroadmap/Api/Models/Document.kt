package com.second_year.hkroadmap.Api.Models

import com.google.gson.annotations.SerializedName

/**
 * Main document response model matching backend response structure
 */
data class DocumentResponse(
    @SerializedName("student_id")
    val student_id: String,
    @SerializedName("document_id")
    val document_id: Int,
    @SerializedName("event_id")
    val event_id: Int,
    @SerializedName("requirement_id")
    val requirement_id: Int,
    @SerializedName("file_path")
    val file_path: String,
    @SerializedName("upload_at")
    val upload_at: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("is_submitted")
    val is_submitted: Int,
    @SerializedName("submitted_at")
    val submitted_at: String?,
    @SerializedName("event_title")
    val event_title: String?,
    @SerializedName("requirement_title")
    val requirement_title: String?,
    @SerializedName("requirement_due_date")
    val requirement_due_date: String?
)

/**
 * Response wrapper for list of documents
 */
data class DocumentListResponse(
    @SerializedName("documents")
    val documents: List<DocumentResponse>
)

/**
 * Document upload request model
 */
data class DocumentUploadRequest(
    @SerializedName("event_id")
    val event_id: Int,
    @SerializedName("requirement_id")
    val requirement_id: Int
)

/**
 * Document submit request model
 */
data class DocumentSubmitRequest(
    @SerializedName("document_id")
    val document_id: Int
)

/**
 * Document unsubmit request model
 */
data class DocumentUnsubmitRequest(
    @SerializedName("document_id")
    val document_id: Int
)

/**
 * Document delete request model
 */
data class DocumentDeleteRequest(
    @SerializedName("document_id")
    val document_id: Int
)

/**
 * Document status response model
 */
data class DocumentStatusResponse(
    @SerializedName("document_id")
    val document_id: Int,
    @SerializedName("status")
    val status: String
)

/**
 * Document status constants
 */
object DocumentStatus {
    const val DRAFT = "draft"
    const val PENDING = "pending"
    const val MISSING = "missing"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}

/**
 * File upload constants matching backend constraints
 */
object FileConstants {
    const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    val ALLOWED_MIME_TYPES = listOf(
        "application/pdf",
        "image/jpeg",
        "image/png"
    )
    const val UPLOAD_DIR = "uploads/"
}

/**
 * Error response model
 */
data class ErrorResponse(
    @SerializedName("message")
    val message: String
)