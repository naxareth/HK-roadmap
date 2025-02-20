package com.second_year.hkroadmap.Api.Interfaces


import com.second_year.hkroadmap.Api.Models.AdminRegisterRequest
import com.second_year.hkroadmap.Api.Models.AdminRegisterResponse
import com.second_year.hkroadmap.Api.Models.EmailRequest
import com.second_year.hkroadmap.Api.Models.LoginRequest
import com.second_year.hkroadmap.Api.Models.LoginResponse
import com.second_year.hkroadmap.Api.Models.LogoutResponse
import com.second_year.hkroadmap.Api.Models.OtpResponse
import com.second_year.hkroadmap.Api.Models.OtpVerificationRequest
import com.second_year.hkroadmap.Api.Models.OtpVerificationResponse
import com.second_year.hkroadmap.Api.Models.PasswordChangeRequest
import com.second_year.hkroadmap.Api.Models.PasswordChangeResponse
import com.second_year.hkroadmap.Api.Models.StudentRegisterRequest
import com.second_year.hkroadmap.Api.Models.StudentRegisterResponse
import retrofit2.http.*

interface ApiService {
    // Admin endpoints
    @POST("admin/register")
    suspend fun adminRegister(@Body admin: AdminRegisterRequest): AdminRegisterResponse

    @POST("admin/login")
    suspend fun adminLogin(@Body credentials: LoginRequest): LoginResponse

    @POST("admin/logout")
    suspend fun adminLogout(@Header("Authorization") token: String): LogoutResponse

    @POST("admin/request-otp")
    suspend fun adminRequestOtp(@Body email: EmailRequest): OtpResponse

    @POST("admin/verify-otp")
    suspend fun adminVerifyOtp(@Body otpVerification: OtpVerificationRequest): OtpVerificationResponse

    @POST("admin/change-password")
    suspend fun adminChangePassword(@Body passwordChange: PasswordChangeRequest): PasswordChangeResponse

    // Student endpoints
    @POST("student/register")
    suspend fun studentRegister(@Body student: StudentRegisterRequest): StudentRegisterResponse

    @POST("student/login")
    suspend fun studentLogin(@Body loginRequest: LoginRequest): LoginResponse

    @POST("student/logout")
    suspend fun studentLogout(@Header("Authorization") token: String): LogoutResponse

    @POST("student/request-otp")
    suspend fun studentRequestOtp(@Body email: EmailRequest): OtpResponse

    @POST("student/verify-otp")
    suspend fun studentVerifyOtp(@Body otpVerification: OtpVerificationRequest): OtpVerificationResponse

    @POST("student/change-password")
    suspend fun studentChangePassword(@Body passwordChange: PasswordChangeRequest): PasswordChangeResponse
}