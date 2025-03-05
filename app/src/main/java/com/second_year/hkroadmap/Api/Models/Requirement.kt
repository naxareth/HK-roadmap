package com.second_year.hkroadmap.Api.Models

//data class RequirementRequest(
//    val title: String,
 //   val description: String,
//    val deadline: String,
//    val status: String
//)

data class RequirementItem(
    val requirement_id: Int,
    val event_id: Int,
    val requirement_name: String,
    val requirement_desc: String, // Add this field
    val due_date: String
)

data class RequirementResponse(
    val requirement_id: Int,
    val event_id: Int,
    val requirement_name: String,
    val requirement_desc: String, // Add this field
    val due_date: String,
)
// Optional wrapper if needed later
data class RequirementListResponse(
    val requirements: List<RequirementResponse>,
    val message: String
)