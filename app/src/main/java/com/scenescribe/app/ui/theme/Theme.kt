package com.scenescribe.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background       = Background,
    surface          = CardBackground,
    primary          = Accent,
    onPrimary        = Background,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    outline          = CardBorder,
    surfaceVariant   = InputBackground,
    onSurfaceVariant = TextSecondary
)

@Composable
fun SceneScribeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
