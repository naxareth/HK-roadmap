package com.second_year.hkroadmap.Api.Interfaces

import com.second_year.hkroadmap.Api.Models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    // Student endpoints
    @POST("student/register")
    suspend fun studentRegister(@Body student: StudentRegisterRequest): StudentRegisterResponse

    @POST("student/login")
    suspend fun studentLogin(@Body loginRequest: LoginRequest): LoginResponse

    @POST("student/logout")
    suspend fun studentLogout(@Header("Authorization") token: String): LogoutResponse

    @GET("student/profile")
    suspend fun getStudentProfile(@Header("Authorization") token: String): StudentProfileResponse

    @POST("student/send-otp")
    suspend fun studentRequestOtp(@Body email: EmailRequest): OtpResponse

    @POST("student/verify-otp")
    suspend fun studentVerifyOtp(@Body request: OtpVerificationRequest): OtpVerificationResponse

    @POST("student/change-password")
    suspend fun changePassword(@Body passwordChange: PasswordChangeRequest): PasswordChangeResponse

    // Student document endpoints
    @GET("documents/student")
    suspend fun getStudentDocuments(
        @Header("Authorization") token: String
    ): DocumentListResponse

    @Multipart
    @POST("documents/upload")
    suspend fun uploadDocument(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("event_id") eventId: RequestBody,
        @Part("requirement_id") requirementId: RequestBody
    ): DocumentUploadResponse

    @HTTP(method = "DELETE", path = "documents/delete", hasBody = true)
    suspend fun deleteDocument(
        @Header("Authorization") token: String,
        @Body request: DocumentDeleteRequest
    ): DocumentDeleteResponse
    // Requirement endpoints
    @GET("requirements/get")
    suspend fun getRequirements(
        @Header("Authorization") token: String
    ): List<RequirementItem>

    @GET("requirements/get")
    suspend fun getRequirementsByEventId(
        @Header("Authorization") token: String,
        @Query("event_id") eventId: Int
    ): List<RequirementItem>

    // Event endpoints
    @POST("event/add")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Body event: EventRequest
    ): EventResponse

    @GET("event/get")
    suspend fun getEvents(@Header("Authorization") token: String): List<EventItem>

    // Submission endpoints
    @PATCH("submission/update")
    suspend fun updateSubmissionStatus(
        @Header("Authorization") token: String,
        @Body submission: SubmissionUpdateRequest
    ): SubmissionResponse

    @GET("submission/update")
    suspend fun getAllSubmissions(@Header("Authorization") token: String): SubmissionListResponse
}