package com.second_year.hkroadmap.Api.Repository

import com.second_year.hkroadmap.Api.Interfaces.ApiService
import com.second_year.hkroadmap.Api.Models.RequirementItem

class RequirementRepository(private val apiService: ApiService) {

    suspend fun getRequirements(token: String): List<RequirementItem> {
        return try {
            apiService.getRequirements(token)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getRequirementsByEventId(token: String, eventId: Int): List<RequirementItem> {
        return try {
            apiService.getRequirementsByEventId(token, eventId)
        } catch (e: Exception) {
            throw e
        }
    }
}