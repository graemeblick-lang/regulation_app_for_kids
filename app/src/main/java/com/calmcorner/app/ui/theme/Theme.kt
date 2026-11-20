package com.calmcorner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CalmColors {
    val Teal = Color(0xFF3A7D7C)
    val Sky = Color(0xFF6C9BD2)
    val Peach = Color(0xFFE9A06B)
    val Rose = Color(0xFFE57B8D)
    val Leaf = Color(0xFF7DB88A)
    val Lavender = Color(0xFF9C84C9)
    val Sun = Color(0xFFF2C14E)
}

private val LightColors = lightColorScheme(
    primary = CalmColors.Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFEA),
    onPrimaryContainer = Color(0xFF173B3A),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF2B2D33),
    surface = Color.White,
    onSurface = Color(0xFF2B2D33),
    surfaceVariant = Color(0xFFF1ECE4),
    onSurfaceVariant = Color(0xFF5E616B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DC4C1),
    onPrimary = Color(0xFF0E2E2D),
    primaryContainer = Color(0xFF244E4D),
    onPrimaryContainer = Color(0xFFDCEFEA),
    background = Color(0xFF1D1F24),
    onBackground = Color(0xFFE8E6E1),
    surface = Color(0xFF272A30),
    onSurface = Color(0xFFE8E6E1),
    surfaceVariant = Color(0xFF30333A),
    onSurfaceVariant = Color(0xFFB9BBC2),
)

private val CalmTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CalmTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CalmTypography,
        content = content,
    )
}
