package com.scenescribe.app.data.api.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val email: String
)

data class VerifyRequest(
    val email: String,
    val otp: String,
    @SerializedName("user_name") val userName: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserDto(
    val id: String,
    @SerializedName("user_name") val userName: String,
    val email: String,
    @SerializedName("is_admin") val isAdmin: Boolean = false
)

data class AuthData(
    val token: String,
    val user: UserDto
)

data class AuthResponse(
    val success: Boolean,
    val data: AuthData?,
    val message: String?
)

data class RegisterResponse(
    val success: Boolean,
    val data: RegisterData?,
    val message: String?
)

data class RegisterData(
    val email: String
)
