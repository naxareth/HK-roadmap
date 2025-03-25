package com.second_year.hkroadmap.Api.Models

import com.google.gson.annotations.SerializedName

data class Comment(
    val commentId: Int,
    val documentId: Int? = null,
    val requirementId: Int,
    val studentId: Int,
    val userType: String,      // enum('admin','student','staff')
    val userName: String,      // Not null in DB
    val body: String,          // Not null in DB
    val createdAt: String,     // timestamp
    val updatedAt: String,     // timestamp
    val isOwner: Boolean = false
) {
    companion object {
        const val USER_TYPE_STUDENT = "student"
        const val USER_TYPE_ADMIN = "admin"
        const val USER_TYPE_STAFF = "staff"
    }

    // If you need userId, derive it from studentId for students
    val userId: String
        get() = when (userType) {
            USER_TYPE_STUDENT -> studentId.toString()
            USER_TYPE_ADMIN -> "admin"
            USER_TYPE_STAFF -> "staff"
            else -> studentId.toString()
        }
}

data class CommentResponse(
    @SerializedName("comment_id")
    val commentId: Int,
    @SerializedName("document_id")
    val documentId: Int?,
    @SerializedName("requirement_id")
    val requirementId: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("user_type")
    val userType: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("is_owner")
    val isOwner: Boolean = false
)

data class CommentRequest(
    @SerializedName("requirement_id")
    val requirementId: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("body")
    val body: String
)

data class CommentUpdateRequest(
    @SerializedName("comment_id")
    val commentId: Int,
    @SerializedName("body")
    val body: String
)

data class CommentOperationResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: String
)

data class CommentDeleteRequest(
    @SerializedName("comment_id")
    val commentId: Int
)