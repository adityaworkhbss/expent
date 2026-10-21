package com.aditya.expent.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Soft, friendly palette shared by the add-transaction sheet and onboarding
data class SoftColors(
    val background: Color,
    val card: Color,
    val cardBorder: Color,
    val ink: Color,
    val muted: Color,
    val primary: Color,
    val primaryEnd: Color,
    val income: Color,
    val expense: Color,
    val ai: Color,
    val aiEnd: Color,
    val peach: Color,
    val sky: Color
)

val LightSoftColors = SoftColors(
    background = Color(0xFFFFFBF7),
    card = Color.White,
    cardBorder = Color(0xFFF1E9E3),
    ink = Color(0xFF2D2A32),
    muted = Color(0xFF8C8696),
    primary = Color(0xFF12A386),
    primaryEnd = Color(0xFF4FD1A5),
    income = Color(0xFF2EB67D),
    expense = Color(0xFFFF6B6B),
    ai = Color(0xFF7C5CFF),
    aiEnd = Color(0xFFC06CFF),
    peach = Color(0xFFFFB38A),
    sky = Color(0xFF5B8DEF)
)

val DarkSoftColors = SoftColors(
    background = Color(0xFF1B1A21),
    card = Color(0xFF26252D),
    cardBorder = Color(0xFF35343E),
    ink = Color(0xFFF2EFF7),
    muted = Color(0xFFA19DAD),
    primary = Color(0xFF3DD9B0),
    primaryEnd = Color(0xFF7BE7C6),
    income = Color(0xFF4CD39B),
    expense = Color(0xFFFF8A80),
    ai = Color(0xFF9D85FF),
    aiEnd = Color(0xFFD08BFF),
    peach = Color(0xFFFFB38A),
    sky = Color(0xFF7FA7F5)
)

val LocalSoftColors = staticCompositionLocalOf { LightSoftColors }

// Picks the light or dark palette from the current Material theme
@Composable
fun softColors(): SoftColors =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) DarkSoftColors else LightSoftColors
