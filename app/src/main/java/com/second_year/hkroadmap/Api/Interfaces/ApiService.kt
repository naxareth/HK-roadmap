package com.second_year.hkroadmap.Api.Interfaces

import com.second_year.hkroadmap.Api.Models.*
import com.second_year.hkroadmap.data.models.DepartmentsResponse
import com.second_year.hkroadmap.data.models.Profile
import com.second_year.hkroadmap.data.models.ProfileUpdateRequest
import com.second_year.hkroadmap.data.models.ProfileUpdateResponse
import com.second_year.hkroadmap.data.models.ProgramsResponse
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

    @Multipart
    @POST("documents/upload")
    suspend fun uploadLinkDocument(
        @Header("Authorization") token: String,
        @Part("event_id") eventId: RequestBody,
        @Part("requirement_id") requirementId: RequestBody,
        @Part("link_url") linkUrl: RequestBody
    ): Response<DocumentStatusResponse>

    @POST("documents/submit-multiple")
    suspend fun submitMultipleDocuments(
        @Header("Authorization") token: String,
        @Body body: DocumentIdsRequest // Use a specific data class instead
    ): Response<DocumentStatusResponse>

    @POST("documents/unsubmit-multiple")
    suspend fun unsubmitMultipleDocuments(
        @Header("Authorization") token: String,
        @Body body: DocumentIdsRequest // Use a specific data class instead
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

    // Comments Management
    @POST("comments/add")
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Body comment: CommentRequest
    ): Response<CommentOperationResponse>

    @GET("comments/get")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Query("requirement_id") requirementId: Int,
        @Query("student_id") studentId: Int
    ): Response<List<CommentResponse>>

    @PUT("comments/update")
    suspend fun updateComment(
        @Header("Authorization") token: String,
        @Body comment: CommentUpdateRequest
    ): Response<CommentOperationResponse>

    @HTTP(method = "DELETE", path = "comments/delete", hasBody = true)
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Body body: Map<String, Int>  // {"comment_id": id}
    ): Response<Unit>

    // Student Notifications
    @GET("notification/student")
    suspend fun getStudentNotifications(
        @Header("Authorization") token: String
    ): Response<List<NotificationResponse>>

    @GET("notification/count-student")
    suspend fun getStudentUnreadCount(
        @Header("Authorization") token: String
    ): Response<UnreadCountResponse>

    @PUT("notification/edit-student")
    suspend fun toggleStudentNotification(
        @Header("Authorization") token: String,
        @Body request: NotificationToggleRequest
    ): Response<NotificationResponse>

    @PUT("notification/mark-student")
    suspend fun markAllStudentRead(
        @Header("Authorization") token: String
    ): Response<MarkReadResponse>


    // Announcement Management
    @GET("announcements/student")
    suspend fun getStudentAnnouncements(
        @Header("Authorization") token: String
    ): Response<AnnouncementResponse>

    // Event Management
    @GET("event/get")
    suspend fun getEvents(
        @Header("Authorization") token: String
    ): List<EventItem>

    // Profile Management
    @GET("profile/get")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<Profile>

    @POST("profile/update")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body profileUpdate: ProfileUpdateRequest
    ): Response<ProfileUpdateResponse>

    @Multipart
    @POST("profile/update")
    suspend fun updateProfileWithPicture(
        @Header("Authorization") token: String,
        @Part profilePicture: MultipartBody.Part,
        @Part("profile") profile: RequestBody
    ): Response<ProfileUpdateResponse>

    @GET("profile/departments")
    suspend fun getDepartments(
        @Header("Authorization") token: String
    ): Response<DepartmentsResponse>

    @GET("profile/programs")
    suspend fun getPrograms(
        @Header("Authorization") token: String
    ): Response<ProgramsResponse>



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