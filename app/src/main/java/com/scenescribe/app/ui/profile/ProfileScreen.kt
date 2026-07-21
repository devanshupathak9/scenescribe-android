package com.scenescribe.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scenescribe.app.data.api.models.HistoryItemDto
import com.scenescribe.app.data.api.models.ProfileData
import com.scenescribe.app.data.api.models.StatsDto
import com.scenescribe.app.ui.components.*
import com.scenescribe.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    onNavigateToFeedback: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val profileState by viewModel.profileState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (val ps = profileState) {
                is ProfileState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                    }
                }
                is ProfileState.Error   -> {
                    ErrorText(ps.message)
                }
                is ProfileState.Success -> {
                    ProfileHeader(data = ps.data)
                    StatsGrid(stats = ps.data.stats)
                }
            }

            // History section
            SectionLabel("Submission History")

            if (historyState.isLoading) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
                }
            } else if (historyState.items.isEmpty()) {
                EmptyHistory()
            } else {
                HistoryList(
                    items   = historyState.items,
                    onClick = onNavigateToFeedback
                )

                historyState.meta?.let { meta ->
                    if (meta.pages > 1) {
                        PaginationRow(
                            page     = historyState.currentPage,
                            total    = meta.pages,
                            onPrev   = { viewModel.prevPage() },
                            onNext   = { viewModel.nextPage() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProfileHeader(data: ProfileData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val name = data.user?.userName ?: data.user?.email ?: "U"
        val initials = name.take(2).uppercase()

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f))
                .border(2.dp, Accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Column {
            Text(
                text = data.user?.userName ?: "User",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = data.user?.email ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatsGrid(stats: StatsDto?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCell(
                value = stats?.avgScore ?: "–",
                label = "Average score",
                accentColor = Accent,
                modifier = Modifier.weight(1f)
            )
            StatCell(
                value = stats?.highestScore?.toString() ?: "–",
                label = "Highest score",
                accentColor = Purple,
                modifier = Modifier.weight(1f)
            )
        }

        // Streak card
        ScCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SectionLabel("Current Streak")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🔥 ${stats?.currentStreak ?: 0} days",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Warning
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    SectionLabel("Longest Streak")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${stats?.longestStreak ?: 0} days",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        ScCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("Scenes Completed")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${stats?.totalCompleted ?: 0}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = TextPrimary
                    )
                }
                // Mini bar chart decoration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(32.dp)
                ) {
                    listOf(0.3f, 0.5f, 0.65f, 0.8f, 1f).forEach { heightFraction ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(Accent.copy(alpha = 0.3f + heightFraction * 0.7f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, accentColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    ScCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(accentColor)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎬", fontSize = 36.sp)
            Text("No submissions yet", style = MaterialTheme.typography.titleMedium)
            Text("Head to the home tab to describe your first scene!", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HistoryList(items: List<HistoryItemDto>, onClick: (String) -> Unit) {
    ScCard {
        items.forEachIndexed { index, item ->
            HistoryRow(item = item, onClick = onClick)
            if (index < items.lastIndex) {
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItemDto, onClick: (String) -> Unit) {
    val score = item.score
    val scoreColor = scoreColor(score)
    val formattedDate = item.date?.let {
        try {
            val input  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val output = SimpleDateFormat("MMM d, yyyy", Locale.US)
            output.format(input.parse(it) ?: return@let it)
        } catch (_: Exception) { it }
    } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item.submissionId) }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(scoreColor.copy(alpha = 0.1f))
                .border(1.dp, scoreColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score?.toString() ?: "–",
                color = scoreColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.sceneTitle ?: "Scene",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text("→", color = TextSecondary, fontSize = 16.sp)
    }
}

@Composable
private fun PaginationRow(page: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrev, enabled = page > 1) {
            Text("← Prev", color = if (page > 1) Accent else TextSecondary)
        }
        Text(
            text = "$page / $total",
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = onNext, enabled = page < total) {
            Text("Next →", color = if (page < total) Accent else TextSecondary)
        }
    }
}
