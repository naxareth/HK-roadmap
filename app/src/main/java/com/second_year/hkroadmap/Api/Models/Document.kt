package com.second_year.hkroadmap.Api.Models


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

// Document response models
data class DocumentResponse(
    val id: Int,
    val event_id: Int,
    val requirement_id: Int,
    val student_id: Int,
    val file_path: String,
    val created_at: String,
    val updated_at: String
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
