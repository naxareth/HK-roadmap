package com.second_year.hkroadmap.Api.Models

data class StudentProfileResponse(
    val id: Int,
    val name: String,
    val email: String,
    val created_at: String,
    val updated_at: String
)