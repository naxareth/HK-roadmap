package com.second_year.hkroadmap.Api.Models

import com.google.gson.annotations.SerializedName


data class DocumentUploadRequest(
    val event_id: Int,
    val requirement_id: Int
)

// Document delete request model
data class DocumentDeleteRequest(
    val event_id: Int,
    val requirement_id: Int,
    val document_id: Int
)

data class ErrorResponse(
    @SerializedName("message")
    val message: String
)

// Document response models
data class DocumentResponse(
    @SerializedName("document_id")
    val id: Int,
    @SerializedName("event_id")
    val event_id: Int,
    @SerializedName("requirement_id")
    val requirement_id: Int,
    @SerializedName("student_id")
    val student_id: String,
    @SerializedName("file_path")
    val file_path: String,
    @SerializedName("upload_at")
    val upload_at: String,
    @SerializedName("status")
    val status: String
)

// For responses that include metadata
data class DocumentListResponse(
    val documents: List<DocumentResponse>,
    val message: String
)

// Document upload response
data class DocumentUploadResponse(
    val document: DocumentResponse,
    val message: String
)

// Document delete response
data class DocumentDeleteResponse(
    val message: String
)
