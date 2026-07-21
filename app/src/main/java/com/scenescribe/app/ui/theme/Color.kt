package com.scenescribe.app.ui.theme

import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0E0E1A)
val CardBackground = Color(0xFF1A1A2E)
val CardBorder = Color(0xFF252535)
val Accent = Color(0xFFE8FF47)
val TextPrimary = Color(0xFFF0F0F0)
val TextSecondary = Color(0xFF74748A)
val Success = Color(0xFF4ADE80)
val Warning = Color(0xFFF59E0B)
val Danger = Color(0xFFF87171)
val Purple = Color(0xFF7C6FEF)
val InputBackground = Color(0xFF12121E)
val DividerColor = Color(0xFF252535)

fun scoreColor(score: Int?): Color = when {
    score == null -> TextSecondary
    score >= 8    -> Success
    score >= 5    -> Warning
    else          -> Danger
}

fun difficultyColor(difficulty: String?): Color = when (difficulty?.lowercase()) {
    "beginner"     -> Success
    "intermediate" -> Warning
    "advanced"     -> Danger
    else           -> TextSecondary
}
