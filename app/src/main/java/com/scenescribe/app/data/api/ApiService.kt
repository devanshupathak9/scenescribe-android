package com.scenescribe.app.data.api

import com.scenescribe.app.data.api.models.*
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("auth/verify")
    suspend fun verify(@Body body: VerifyRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    // Dashboard
    @GET("dashboard/today")
    suspend fun getToday(): TodayResponse

    @POST("dashboard/submit")
    suspend fun submit(@Body body: SubmitRequest): SubmitResponse

    // Profile
    @GET("profile/me")
    suspend fun getProfile(): ProfileResponse

    @GET("profile/history")
    suspend fun getHistory(@Query("page") page: Int = 1): HistoryResponse

    @GET("profile/history/{id}")
    suspend fun getFeedbackDetail(@Path("id") id: String): FeedbackDetailResponse
}
