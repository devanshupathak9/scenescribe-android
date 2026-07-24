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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.scenescribe.app.data.api.models.SceneItem
import com.scenescribe.app.data.api.models.SubmissionDto
import com.scenescribe.app.data.api.models.VideoDto
import com.scenescribe.app.ui.components.*
import com.scenescribe.app.ui.theme.*
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToFeedback: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (val screen = state.screenState) {
            is HomeState.Loading -> LoadingScreen()
            is HomeState.Error   -> NoSceneState(onRetry = viewModel::loadToday)
            is HomeState.Success -> SceneCarousel(
                scenes      = screen.scenes,
                sceneInputs = state.sceneInputs,
                viewModel   = viewModel
            )
        }
    }
}

// ── Error / empty state ───────────────────────────────────────────────────────

@Composable
private fun NoSceneState(onRetry: () -> Unit) {
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
            TextButton(onClick = onRetry) {
                Text("Retry", color = Accent)
            }
        }
    }
}

// ── Carousel — extracted so rememberPagerState is called unconditionally ──────
// (Compose Rules of Composables: never call remember-based APIs inside
//  a conditional branch of the parent composable.)

@Composable
private fun SceneCarousel(
    scenes: List<SceneItem>,
    sceneInputs: Map<String, SceneInputState>,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { scenes.size })
    val scope = rememberCoroutineScope()

    // Holds the active SpeechRecognizer so we can stop it on page swipe
    val activeRecognizer = remember { mutableStateOf<SpeechRecognizer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* user must tap mic again after granting */ }

    // Stop speech only on actual page changes — drop(1) skips the initial emission
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect {
                activeRecognizer.value?.stopListening()
                activeRecognizer.value = null
                viewModel.stopAllRecording()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val today = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
            Text(
                text = "Today — $today",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            if (scenes.size > 1) {
                Text(
                    text = "${scenes.count { it.status == "submitted" }}/${scenes.size} done",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // ── Dot nav — only when there are multiple scenes ─────────────
        if (scenes.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        "‹", fontSize = 24.sp,
                        color = if (pagerState.currentPage > 0) TextPrimary else TextSecondary
                    )
                }

                Spacer(Modifier.width(8.dp))

                scenes.forEachIndexed { idx, scene ->
                    val isActive = idx == pagerState.currentPage
                    val isDone   = scene.status == "submitted"
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> Accent
                                    isDone   -> Success
                                    else     -> CardBorder
                                }
                            )
                    )
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = pagerState.currentPage < scenes.size - 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        "›", fontSize = 24.sp,
                        color = if (pagerState.currentPage < scenes.size - 1) TextPrimary else TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        // ── Pager — key stabilises content across recompositions ──────
        HorizontalPager(
            state    = pagerState,
            key      = { scenes[it].video.videoId },
            modifier = Modifier.weight(1f)
        ) { page ->
            val scene = scenes[page]
            val input = sceneInputs[scene.video.videoId] ?: SceneInputState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DifficultyBadge(difficulty = scene.video.difficulty)

                if (scene.status == "pending") {
                    PendingContent(
                        video        = scene.video,
                        input        = input,
                        onTextChange = { viewModel.updateDescription(scene.video.videoId, it) },
                        onMicClick   = {
                            if (input.isRecording) {
                                activeRecognizer.value?.stopListening()
                                activeRecognizer.value = null
                                viewModel.setRecording(scene.video.videoId, false)
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    activeRecognizer.value = startSpeechRecognition(
                                        context, scene.video.videoId, input.text, viewModel
                                    )
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onSubmit = { viewModel.submit(scene.video.videoId) }
                    )
                } else {
                    SubmittedContent(video = scene.video, submission = scene.submission)
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Pending scene ─────────────────────────────────────────────────────────────

@Composable
private fun PendingContent(
    video: VideoDto,
    input: SceneInputState,
    onTextChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSubmit: () -> Unit
) {
    YouTubePlayer(videoUrl = video.videoUrl)

    if (!video.title.isNullOrBlank()) {
        Text(text = video.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }

    if (!video.description.isNullOrBlank()) {
        ScCard {
            SectionLabel("Scene")
            Spacer(Modifier.height(8.dp))
            Text(video.description, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
    }

    ScCard {
        SectionLabel("Your Description")
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value       = input.text,
            onValueChange = onTextChange,
            placeholder = { Text("Describe what's happening in the video…", color = TextSecondary) },
            modifier    = Modifier.fillMaxWidth().height(120.dp),
            shape       = RoundedCornerShape(10.dp),
            colors      = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = Accent,
                unfocusedBorderColor  = CardBorder,
                focusedTextColor      = TextPrimary,
                unfocusedTextColor    = TextPrimary,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                cursorColor           = Accent
            )
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (input.isRecording) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Danger))
                    Text("Listening…", color = Danger, fontSize = 13.sp)
                }
            } else {
                Spacer(Modifier.size(0.dp))
            }

            IconButton(
                onClick  = onMicClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (input.isRecording) Danger.copy(0.15f) else CardBorder)
                    .border(1.dp, if (input.isRecording) Danger else CardBorder, CircleShape)
            ) {
                Text(if (input.isRecording) "⏹" else "🎤", fontSize = 18.sp)
            }
        }

        if (input.submitError.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            ErrorText(input.submitError)
        }

        Spacer(Modifier.height(12.dp))

        ScButton(
            text    = if (input.isSubmitting) "Analysing with AI…" else "Submit description",
            onClick = onSubmit,
            enabled = !input.isSubmitting && input.text.trim().length >= 10
        )
    }
}

// ── Submitted scene ───────────────────────────────────────────────────────────

@Composable
private fun SubmittedContent(video: VideoDto, submission: SubmissionDto?) {
    if (submission == null) return

    YouTubePlayer(videoUrl = video.videoUrl)

    ScCard {
        submission.inputType?.let { inputType ->
            Text(
                text     = if (inputType == "microphone") "via microphone" else "via keyboard",
                fontSize = 11.sp,
                color    = if (inputType == "microphone") Purple else TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (inputType == "microphone") Purple.copy(0.1f) else CardBorder)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        if (!submission.responseText.isNullOrBlank()) {
            Text(
                text     = submission.responseText,
                style    = MaterialTheme.typography.bodyLarge,
                color    = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        val score = submission.score ?: 0
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScoreRing(score = score)
            Column {
                Text("Overall score", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text       = score.toString(),
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        text     = " / 10",
                        fontSize = 16.sp,
                        color    = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text       = scorePraise(score),
                    color      = scoreColor(score),
                    fontWeight = FontWeight.Medium,
                    fontSize   = 13.sp
                )
            }
        }

        submission.breakdown?.let { bd ->
            Spacer(Modifier.height(14.dp))
            BreakdownGrid(grammar = bd.grammar, vocabulary = bd.vocabulary, clarity = bd.clarity)
        }
    }

    ScCard {
        SectionLabel("AI Feedback")
        Spacer(Modifier.height(12.dp))

        SentenceBlock(label = "Improved sentence", text = submission.improvedAiResponse, accentColor = Purple)
        SentenceBlock(label = "Ideal sentence",    text = submission.idealSentence,       accentColor = Accent)

        val issues = submission.feedback?.issues ?: emptyList()
        if (issues.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Issues", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            issues.forEach { FeedbackItem(text = it, isIssue = true) }
        }

        val suggestions = submission.feedback?.suggestions ?: emptyList()
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Suggestions", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            suggestions.forEach { FeedbackItem(text = it, isIssue = false) }
        }
    }

    if (!video.description.isNullOrBlank() || !video.additionalNotes.isNullOrBlank()) {
        ScCard {
            SectionLabel("Reference")
            Spacer(Modifier.height(12.dp))
            SentenceBlock(label = "Admin sentence", text = video.description,     accentColor = Color(0xFF888896))
            SentenceBlock(label = "Notes",          text = video.additionalNotes, accentColor = Color(0xFF555566))
        }
    }
}

// ── Shared feedback row ───────────────────────────────────────────────────────

@Composable
fun FeedbackItem(text: String, isIssue: Boolean) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text     = if (isIssue) "✕" else "→",
            color    = if (isIssue) Danger else Success,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyMedium,
            color    = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Speech recognition helper ─────────────────────────────────────────────────

private fun startSpeechRecognition(
    context: android.content.Context,
    videoId: String,
    currentText: String,
    viewModel: HomeViewModel
): SpeechRecognizer? {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return null

    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    var baseText = currentText

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { viewModel.setRecording(videoId, true) }
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            viewModel.appendSpeechResult(videoId, "$baseText $partial".trim())
        }
        override fun onResults(results: Bundle?) {
            val result = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: ""
            val finalText = "$baseText $result".trim()
            viewModel.appendSpeechResult(videoId, finalText)
            baseText = finalText
            viewModel.setRecording(videoId, false)
            recognizer.destroy()
        }
        override fun onError(error: Int) {
            viewModel.setRecording(videoId, false)
            recognizer.destroy()
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    recognizer.startListening(intent)
    return recognizer
}
