package com.second_year.finalproject.Api.Models

data class AdminRegisterRequest(val name: String, val email: String, val password: String, val confirm_password: String)
data class AdminRegisterResponse(val message: String, val token: String?)

data class StudentRegisterRequest(val name: String, val email: String, val password: String, val confirm_password: String)
data class StudentRegisterResponse(val message: String, val token: String?)

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val message: String, val token: String?)

data class LogoutResponse(val message: String)

data class EmailRequest(val email: String)
data class OtpResponse(val message: String)

data class OtpVerificationRequest(val email: String, val otp: String)
data class OtpVerificationResponse(val message: String)

data class PasswordChangeRequest(val email: String, val new_password: String)
data class PasswordChangeResponse(val message: String)
