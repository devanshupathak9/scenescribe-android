package com.scenescribe.app.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scenescribe.app.data.api.models.FeedbackDetailData
import com.scenescribe.app.ui.components.*
import com.scenescribe.app.ui.home.FeedbackItem
import com.scenescribe.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    submissionId: String,
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(submissionId) {
        viewModel.load(submissionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is FeedbackState.Loading -> LoadingScreen()
                is FeedbackState.Error   -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            ErrorText(s.message)
                            ScButton(text = "← Back", onClick = onBack, modifier = Modifier.width(120.dp))
                        }
                    }
                }
                is FeedbackState.Success -> FeedbackContent(data = s.data, onBack = onBack)
            }
        }
    }
}

@Composable
private fun FeedbackContent(data: FeedbackDetailData, onBack: () -> Unit) {
    val formattedDate = data.date?.let {
        try {
            val input  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val output = SimpleDateFormat("MMMM d, yyyy", Locale.US)
            output.format(input.parse(it) ?: return@let it)
        } catch (_: Exception) { it }
    } ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Date header
        Text(
            text = "$formattedDate — ${data.video?.title ?: "Scene"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )

        // Video
        YouTubePlayer(videoUrl = data.video?.videoUrl)

        // Score card
        ScCard {
            val inputType = data.inputType
            if (inputType != null) {
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

            if (!data.responseText.isNullOrBlank()) {
                Text(
                    text = data.responseText,
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

            val score = data.score ?: 0
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
                            " / 10",
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
                            else       -> "Keep practicing!"
                        },
                        color = scoreColor(score),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }

            data.breakdown?.let { bd ->
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
                text  = data.improvedAiResponse,
                accentColor = Purple
            )
            SentenceBlock(
                label = "Ideal sentence",
                text  = data.idealSentence,
                accentColor = Accent
            )

            val issues = data.feedback?.issues ?: emptyList()
            if (issues.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Issues", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                issues.forEach { FeedbackItem(text = it, isIssue = true) }
            }

            val suggestions = data.feedback?.suggestions ?: emptyList()
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Suggestions", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                suggestions.forEach { FeedbackItem(text = it, isIssue = false) }
            }
        }

        // Reference card
        val hasReference = !data.video?.description.isNullOrBlank() ||
                !data.video?.additionalNotes.isNullOrBlank()
        if (hasReference) {
            ScCard {
                SectionLabel("Reference")
                Spacer(Modifier.height(12.dp))
                SentenceBlock(
                    label = "Admin sentence",
                    text  = data.video?.description,
                    accentColor = Color(0xFF888896)
                )
                SentenceBlock(
                    label = "Notes",
                    text  = data.video?.additionalNotes,
                    accentColor = Color(0xFF555566)
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}
