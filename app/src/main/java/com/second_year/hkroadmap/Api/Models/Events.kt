package com.second_year.hkroadmap.Api.Models

// Event Models
data class EventRequest(
    val title: String,
    val description: String,
    val date: String,
    val location: String
)

data class EventItem(
    val event_id: Int,
    val event_name: String,
    val date: String
)

data class EventResponse(
    val id: Int,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val created_at: String,
    val updated_at: String
)

// For responses that include metadata
data class EventListResponse(
    val events: List<EventResponse>,
    val message: String
)