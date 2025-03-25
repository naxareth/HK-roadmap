package com.second_year.hkroadmap.Api.Models

import com.google.gson.annotations.SerializedName

data class AnnouncementItem(
    @SerializedName("announcement_id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("author_name")
    val authorName: String,
    @SerializedName("is_read")
    val isRead: Int = 0,
    @SerializedName("read_at")
    val readAt: String? = null
)

data class AnnouncementResponse(
    @SerializedName("announcements")
    val announcements: List<AnnouncementItem>,
    @SerializedName("unread_count")
    val unread_count: Int  // Changed to match the API response
)