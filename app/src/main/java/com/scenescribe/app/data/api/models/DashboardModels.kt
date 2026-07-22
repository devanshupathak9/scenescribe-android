package com.scenescribe.app.data.api.models

import com.google.gson.annotations.SerializedName

data class VocabularyDto(
    val word: String,
    val definition: String,
    val example: String?,
    @SerializedName("part_of_speech") val partOfSpeech: String?
)

data class GrammarDto(
    val pattern: String,
    val explanation: String,
    val example: String?
)

// Matches the `video` object returned by GET /dashboard/today
data class VideoDto(
    @SerializedName("video_id") val videoId: String,
    @SerializedName("video_url") val videoUrl: String,
    val title: String?,
    val description: String?,
    @SerializedName("additional_notes") val additionalNotes: String?,
    val difficulty: String?,
    val vocabularies: List<VocabularyDto> = emptyList(),
    val grammars: List<GrammarDto> = emptyList()
)

data class FeedbackDto(
    val issues: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
)

data class BreakdownDto(
    val grammar: Int?,
    val vocabulary: Int?,
    val clarity: Int?
)

data class SubmissionDto(
    @SerializedName("submission_id") val submissionId: String,
    @SerializedName("response_text") val responseText: String?,
    @SerializedName("input_type") val inputType: String?,
    val score: Int?,
    val breakdown: BreakdownDto?,
    @SerializedName("improved_ai_response") val improvedAiResponse: String?,
    @SerializedName("ideal_sentence") val idealSentence: String?,
    val feedback: FeedbackDto?,
    @SerializedName("new_streak") val newStreak: Int?
)

// One entry in the `scenes` array from GET /dashboard/today
data class SceneItem(
    val status: String,
    val video: VideoDto,
    val submission: SubmissionDto?
)

// GET /dashboard/today → { success, data: { scenes: [...] } }
data class TodayResponseData(
    val scenes: List<SceneItem>
)

data class TodayResponse(
    val success: Boolean,
    val data: TodayResponseData?
)

data class SubmitRequest(
    @SerializedName("video_id") val videoId: String,
    @SerializedName("response_text") val responseText: String,
    @SerializedName("input_type") val inputType: String
)

data class SubmitResponse(
    val success: Boolean,
    val data: SubmissionDto?
)
