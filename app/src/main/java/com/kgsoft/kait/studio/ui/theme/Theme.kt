package com.kgsoft.kait.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kgsoft.kait.studio.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFF7B1FA2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFCE93D8),
)

val CairoFont: FontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontFamily = CairoFont, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = CairoFont, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = CairoFont, fontSize = 12.sp),
    titleLarge = TextStyle(fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = CairoFont, fontSize = 14.sp),
)

/**
 * KAIT Studio theme: Cairo everywhere, dark mode follows the system.
 * There is no manual dark toggle by design (KG's spec: dark mode follows
 * the system).
 */
@Composable
fun KAITStudioTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
