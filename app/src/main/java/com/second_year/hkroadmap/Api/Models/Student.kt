package com.second_year.hkroadmap.Api.Models

import com.google.gson.annotations.SerializedName

data class StudentProfileResponse(
    @SerializedName("student_id") val id: Int,  // Maps student_id from JSON to id in Kotlin
    val name: String,
    val email: String
)