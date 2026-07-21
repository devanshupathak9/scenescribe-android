package com.scenescribe.app.data.api.models

import com.google.gson.annotations.SerializedName

data class StatsDto(
    @SerializedName("avg_score") val avgScore: String?,
    @SerializedName("highest_score") val highestScore: Int?,
    @SerializedName("current_streak") val currentStreak: Int?,
    @SerializedName("longest_streak") val longestStreak: Int?,
    @SerializedName("total_completed") val totalCompleted: Int?
)

data class ProfileUserDto(
    @SerializedName("user_name") val userName: String?,
    val email: String?,
    @SerializedName("is_admin") val isAdmin: Boolean = false
)

data class ProfileData(
    val user: ProfileUserDto?,
    val stats: StatsDto?
)

data class ProfileResponse(
    val success: Boolean,
    val data: ProfileData?
)

data class HistoryItemDto(
    @SerializedName("submission_id") val submissionId: String,
    val date: String?,
    val score: Int?,
    @SerializedName("scene_title") val sceneTitle: String?,
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("publish_date") val publishDate: String?
)

data class HistoryMeta(
    val total: Int,
    val page: Int,
    val pages: Int
)

data class HistoryResponse(
    val success: Boolean,
    val data: List<HistoryItemDto>?,
    val meta: HistoryMeta?
)

data class FeedbackVideoDto(
    @SerializedName("video_url") val videoUrl: String?,
    val title: String?,
    @SerializedName("scene_description") val sceneDescription: String?,
    @SerializedName("reference_description") val referenceDescription: String?,
    @SerializedName("additional_notes") val additionalNotes: String?,
    val description: String?
)

data class FeedbackDetailData(
    @SerializedName("submission_id") val submissionId: String,
    val video: FeedbackVideoDto?,
    @SerializedName("response_text") val responseText: String?,
    @SerializedName("input_type") val inputType: String?,
    val score: Int?,
    val breakdown: com.scenescribe.app.data.api.models.BreakdownDto?,
    val feedback: com.scenescribe.app.data.api.models.FeedbackDto?,
    @SerializedName("improved_ai_response") val improvedAiResponse: String?,
    @SerializedName("ideal_sentence") val idealSentence: String?,
    val date: String?
)

data class FeedbackDetailResponse(
    val success: Boolean,
    val data: FeedbackDetailData?
)
