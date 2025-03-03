package com.second_year.hkroadmap.Api.Interfaces

import com.second_year.hkroadmap.Api.Models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service interface defining all network endpoints for the application
 */
interface ApiService {
    // Student Authentication & Profile Management
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

    // Document Management
    @GET("documents/student")
    suspend fun getStudentDocuments(
        @Header("Authorization") token: String
    ): Response<DocumentListResponse>

    @GET("documents/admin")
    suspend fun getAdminDocuments(
        @Header("Authorization") token: String
    ): Response<DocumentListResponse>

    @Multipart
    @POST("documents/upload")
    suspend fun uploadDocument(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("event_id") eventId: RequestBody,
        @Part("requirement_id") requirementId: RequestBody
    ): Response<DocumentStatusResponse>

    @POST("documents/submit")
    suspend fun submitDocument(
        @Header("Authorization") token: String,
        @Body body: Map<String, Int>  // {"document_id": id}
    ): Response<DocumentStatusResponse>

    @POST("documents/unsubmit")
    suspend fun unsubmitDocument(
        @Header("Authorization") token: String,
        @Body body: Map<String, Int>
    ): Response<DocumentStatusResponse>

    @HTTP(method = "DELETE", path = "documents/delete", hasBody = true)
    suspend fun deleteDocument(
        @Header("Authorization") token: String,
        @Body body: Map<String, Int>  // {"document_id": id}
    ): Response<DocumentStatusResponse>

    @GET("documents/status/{id}")
    suspend fun getDocumentStatus(
        @Header("Authorization") token: String,
        @Path("id") documentId: Int
    ): Response<DocumentStatusResponse>

    // Event Management
    @GET("event/get")
    suspend fun getEvents(
        @Header("Authorization") token: String
    ): List<EventItem>



    // Requirement Management
    @GET("requirements/get")
    suspend fun getRequirements(
        @Header("Authorization") token: String
    ): List<RequirementItem>

    @GET("requirements/get")
    suspend fun getRequirementsByEventId(
        @Header("Authorization") token: String,
        @Query("event_id") eventId: Int
    ): List<RequirementItem>


    // Submission Management
    @PATCH("submission/update")
    suspend fun updateSubmissionStatus(
        @Header("Authorization") token: String,
        @Body submission: SubmissionUpdateRequest
    ): Response<SubmissionResponse>

    @GET("submission/update")
    suspend fun getAllSubmissions(
        @Header("Authorization") token: String
    ): Response<SubmissionListResponse>
}