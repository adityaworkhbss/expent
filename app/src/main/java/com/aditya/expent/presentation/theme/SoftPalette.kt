package com.aditya.expent.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Named colours for onboarding and the add-transaction sheet, taken from the app theme
// so those screens match Auth, Dashboard and Profile
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

private val PrimaryDeep = Color(0xFF004D40)
private val AiPurple = Color(0xFF6200EA)
private val AiPurpleEnd = Color(0xFF9C27B0)
private val InfoBlue = Color(0xFF0288D1)

// Light theme values; also the fallback when no palette is provided (previews)
val LightSoftColors = SoftColors(
    background = LightBackground,
    card = Color.White,
    cardBorder = Color(0xFFE0E0E0).copy(alpha = 0.5f),
    ink = Color(0xFF1C1B1F),
    muted = Color(0xFF49454F),
    primary = EmeraldPrimary,
    primaryEnd = PrimaryDeep,
    income = ColorIncome,
    expense = ColorExpense,
    ai = AiPurple,
    aiEnd = AiPurpleEnd,
    peach = GoldSecondary,
    sky = InfoBlue
)

val LocalSoftColors = staticCompositionLocalOf { LightSoftColors }

@Composable
fun softColors(): SoftColors {
    val scheme = MaterialTheme.colorScheme
    return LightSoftColors.copy(
        background = scheme.background,
        card = scheme.surface,
        cardBorder = scheme.outlineVariant.copy(alpha = 0.5f),
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant
    )
}
