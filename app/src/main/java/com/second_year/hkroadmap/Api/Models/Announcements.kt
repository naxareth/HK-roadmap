package com.second_year.hkroadmap.Api.Models

import com.google.gson.annotations.SerializedName
import java.util.*


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
    val authorName: String
)

data class AnnouncementResponse(
    @SerializedName("notifications")  // Changed from "announcements" to "notifications"
    val announcements: List<AnnouncementItem>
)