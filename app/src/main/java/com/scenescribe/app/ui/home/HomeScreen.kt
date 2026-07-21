package com.scenescribe.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scenescribe.app.data.api.models.SubmissionDto
import com.scenescribe.app.data.api.models.TodayData
import com.scenescribe.app.ui.components.*
import com.scenescribe.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToFeedback: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechRecognition(context, viewModel)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (val screen = state.screenState) {
            is HomeState.Loading -> LoadingScreen()

            is HomeState.Error   -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🎬", fontSize = 40.sp)
                        Text(
                            "No scene scheduled for today.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            "Check back tomorrow — new content drops daily.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        TextButton(onClick = { viewModel.loadToday() }) {
                            Text("Retry", color = Accent)
                        }
                    }
                }
            }

            is HomeState.Success -> {
                val data = screen.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val today = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
                        Text(
                            text = "Today's Scene — $today",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        DifficultyBadge(difficulty = data.video.difficulty)
                    }

                    if (data.status == "pending") {
                        PendingContent(
                            data = data,
                            state = state,
                            onTextChange = { viewModel.updateDescription(it) },
                            onMicClick = {
                                if (state.isRecording) {
                                    viewModel.setRecording(false)
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        startSpeechRecognition(context, viewModel)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            onSubmit = { viewModel.submit() }
                        )
                    } else {
                        SubmittedContent(
                            data = data,
                            submission = data.submission
                        )
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun PendingContent(
    data: TodayData,
    state: HomeUiState,
    onTextChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSubmit: () -> Unit
) {
    // YouTube player
    YouTubePlayer(videoUrl = data.video.videoUrl)

    // Scene title
    if (!data.video.title.isNullOrBlank()) {
        Text(
            text = data.video.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }

    // Scene description
    if (!data.video.sceneDescription.isNullOrBlank()) {
        ScCard {
            SectionLabel("Scene Description")
            Spacer(Modifier.height(8.dp))
            Text(data.video.sceneDescription, style = MaterialTheme.typography.bodyLarge)
        }
    }

    // Input card
    ScCard {
        SectionLabel("Your Description")
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = state.descriptionText,
            onValueChange = onTextChange,
            placeholder = { Text("Describe what's happening in the video…", color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                cursorColor = Accent
            )
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recording indicator
            if (state.isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Danger)
                    )
                    Text("Listening…", color = Danger, fontSize = 13.sp)
                }
            } else {
                Spacer(Modifier.size(0.dp))
            }

            // Mic button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (state.isRecording) Danger.copy(0.15f) else CardBorder)
                    .border(1.dp, if (state.isRecording) Danger else CardBorder, CircleShape)
            ) {
                Text(if (state.isRecording) "⏹" else "🎤", fontSize = 18.sp)
            }
        }

        if (state.submitError.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            ErrorText(state.submitError)
        }

        Spacer(Modifier.height(12.dp))

        ScButton(
            text = if (state.isSubmitting) "Analysing with AI…" else "Submit description",
            onClick = onSubmit,
            enabled = !state.isSubmitting && state.descriptionText.trim().length >= 10
        )
    }
}

@Composable
private fun SubmittedContent(data: TodayData, submission: SubmissionDto?) {
    if (submission == null) return

    // Compact video
    YouTubePlayer(videoUrl = data.video.videoUrl)

    // Score card
    ScCard {
        submission.inputType?.let { inputType ->
            Text(
                text = if (inputType == "microphone") "via microphone" else "via keyboard",
                fontSize = 11.sp,
                color = if (inputType == "microphone") Purple else TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (inputType == "microphone") Purple.copy(0.1f)
                        else CardBorder
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        // User response
        if (!submission.responseText.isNullOrBlank()) {
            Text(
                text = submission.responseText,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        // Score row
        val score = submission.score ?: 0
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScoreRing(score = score)
            Column {
                Text("Overall score", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = score.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = " / 10",
                        fontSize = 16.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = when {
                        score >= 9 -> "Excellent!"
                        score >= 7 -> "Great work!"
                        score >= 5 -> "Good effort!"
                        else -> "Keep practicing!"
                    },
                    color = scoreColor(score),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }

        // Breakdown
        submission.breakdown?.let { bd ->
            Spacer(Modifier.height(14.dp))
            BreakdownGrid(
                grammar    = bd.grammar,
                vocabulary = bd.vocabulary,
                clarity    = bd.clarity
            )
        }
    }

    // AI Feedback card
    ScCard {
        SectionLabel("AI Feedback")
        Spacer(Modifier.height(12.dp))

        SentenceBlock(
            label = "Improved sentence",
            text  = submission.improvedAiResponse,
            accentColor = Purple
        )
        SentenceBlock(
            label = "Ideal sentence",
            text  = submission.idealSentence,
            accentColor = Accent
        )

        val issues = submission.feedback?.issues ?: emptyList()
        if (issues.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Issues", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            issues.forEach { issue ->
                FeedbackItem(text = issue, isIssue = true)
            }
        }

        val suggestions = submission.feedback?.suggestions ?: emptyList()
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Suggestions", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            suggestions.forEach { suggestion ->
                FeedbackItem(text = suggestion, isIssue = false)
            }
        }
    }

    // Admin reference card
    val hasAdminContent = !data.video.description.isNullOrBlank() ||
            !data.video.additionalNotes.isNullOrBlank()
    if (hasAdminContent) {
        ScCard {
            SectionLabel("Reference")
            Spacer(Modifier.height(12.dp))
            SentenceBlock(
                label = "Admin sentence",
                text  = data.video.description,
                accentColor = Color(0xFF888896)
            )
            SentenceBlock(
                label = "Notes",
                text  = data.video.additionalNotes,
                accentColor = Color(0xFF555566)
            )
        }
    }
}

@Composable
fun FeedbackItem(text: String, isIssue: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isIssue) "✕" else "→",
            color = if (isIssue) Danger else Success,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun startSpeechRecognition(context: android.content.Context, viewModel: HomeViewModel) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return

    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    var baseText = viewModel.uiState.value.descriptionText

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            viewModel.setRecording(true)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            viewModel.appendSpeechResult("$baseText $partial".trim())
        }
        override fun onResults(results: Bundle?) {
            val result = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: ""
            val finalText = "$baseText $result".trim()
            viewModel.appendSpeechResult(finalText)
            baseText = finalText
            viewModel.setRecording(false)
            recognizer.destroy()
        }
        override fun onError(error: Int) {
            viewModel.setRecording(false)
            recognizer.destroy()
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    recognizer.startListening(intent)
}
