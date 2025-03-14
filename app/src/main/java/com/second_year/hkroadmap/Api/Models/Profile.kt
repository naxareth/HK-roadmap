package com.second_year.hkroadmap.data.models

import com.google.gson.annotations.SerializedName

data class Profile(
    @SerializedName("profile_id")
    val profileId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_type")
    val userType: String,
    val name: String?,
    val email: String?,
    val department: String?,
    @SerializedName("department_others")
    val departmentOthers: String?,
    @SerializedName("student_number")
    val studentNumber: String?,
    @SerializedName("college_program")
    val collegeProgram: String?,
    @SerializedName("year_level")
    val yearLevel: Int?,
    @SerializedName("scholarship_type")
    val scholarshipType: String?,
    val position: String?,
    @SerializedName("contact_number")
    val contactNumber: String?,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class ProfileUpdateRequest(
    val name: String?,
    val email: String?,
    val department: String?,
    @SerializedName("department_others")
    val departmentOthers: String?,
    @SerializedName("student_number")
    val studentNumber: String?,
    @SerializedName("college_program")
    val collegeProgram: String?,
    @SerializedName("year_level")
    val yearLevel: String?,
    @SerializedName("scholarship_type")
    val scholarshipType: String?,
    @SerializedName("contact_number")
    val contactNumber: String?
)

data class ProfileUpdateResponse(
    val message: String,
    val profile: Profile
)

// Response wrapper classes
data class DepartmentsResponse(
    val departments: Map<String, String>
)

data class ProgramsResponse(
    val programs: List<String>
)