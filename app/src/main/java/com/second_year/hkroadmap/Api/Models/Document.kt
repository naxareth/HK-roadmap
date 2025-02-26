package com.second_year.hkroadmap.Api.Models

// Document Models
data class DocumentResponse(
    val id: Int,
    val title: String,
    val file_path: String,
    val description: String,
    val uploaded_by: Int,
    val created_at: String,
    val updated_at: String
)

data class DocumentListResponse(
    val documents: List<DocumentResponse>,
    val message: String
)
