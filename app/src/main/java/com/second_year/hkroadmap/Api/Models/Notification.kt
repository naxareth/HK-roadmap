package com.second_year.hkroadmap.Api.Models

data class NotificationResponse(
    val notification_id: Int,
    val notification_body: String,
    val read_notif: Int, // Changed from Boolean to Int
    val created_at: String,
    val recipient_id: Int,
    val staff_recipient_id: Int,
    val related_user_id: Int?,
    val notification_type: String
) {
    // Add a helper property to convert the Int to Boolean
    val isRead: Boolean
        get() = read_notif == 1
}

data class UnreadCountResponse(
    val unread_count: Int
)

data class NotificationToggleRequest(
    val notification_id: Int,
    val read: Boolean
)

data class MarkReadResponse(
    val success: Boolean,
    val message: String
)