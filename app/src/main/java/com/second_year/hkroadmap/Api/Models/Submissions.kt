package com.second_year.hkroadmap.Api.Models

// Submission Models
data class SubmissionUpdateRequest(
    val submission_id: Int,
    val status: String,
    val feedback: String?
)

data class SubmissionResponse(
    val id: Int,
    val student_id: Int,
    val requirement_id: Int,
    val document_id: Int,
    val status: String,
    val feedback: String?,
    val submitted_at: String,
    val updated_at: String
)

data class SubmissionListResponse(
    val submissions: List<SubmissionResponse>,
    val message: String
)