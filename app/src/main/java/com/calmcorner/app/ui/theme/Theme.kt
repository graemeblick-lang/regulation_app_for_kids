package com.calmcorner.app.ui.theme

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
import com.calmcorner.app.R

/**
 * Each tool has a soft colour (backgrounds, tracks, shapes) and a deep colour
 * (buttons, the moving dot). Deep colours pass WCAG AA (4.5:1) with white text.
 */
object CalmColors {
    val Teal = Color(0xFF3A7D7C)
    val TealDeep = Color(0xFF2F6B6A)
    val Sky = Color(0xFF6C9BD2)
    val SkyDeep = Color(0xFF2F67A3)
    val Peach = Color(0xFFE9A06B)
    val PeachDeep = Color(0xFFA8541C)
    val Rose = Color(0xFFE57B8D)
    val RoseDeep = Color(0xFFB03A55)
    val Leaf = Color(0xFF7DB88A)
    val LeafDeep = Color(0xFF3B7548)
    val Lavender = Color(0xFF9C84C9)
    val LavenderDeep = Color(0xFF684CA0)
    val Gold = Color(0xFFF2C14E)
    val GoldDeep = Color(0xFF8A6400)
    val Aqua = Color(0xFF5BC0BE)
    val AquaDeep = Color(0xFF1F6F6D)
    val Slate = Color(0xFF8FA3BF)
    val SlateDeep = Color(0xFF4A5D7E)
}

/** Nunito: a rounded, friendly, very readable font (SIL Open Font License). */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private val LightColors = lightColorScheme(
    primary = CalmColors.TealDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFEA),
    onPrimaryContainer = Color(0xFF173B3A),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF2B2D33),
    surface = Color.White,
    onSurface = Color(0xFF2B2D33),
    surfaceVariant = Color(0xFFEDE7DD),
    onSurfaceVariant = Color(0xFF545761),
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

private fun TextStyle.nunito() = copy(fontFamily = Nunito)

private val base = Typography()

private val CalmTypography = Typography(
    displayLarge = base.displayLarge.nunito(),
    displayMedium = base.displayMedium.nunito(),
    displaySmall = TextStyle(fontFamily = Nunito, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 42.sp),
    headlineLarge = base.headlineLarge.nunito(),
    headlineMedium = TextStyle(fontFamily = Nunito, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    headlineSmall = base.headlineSmall.nunito(),
    titleLarge = TextStyle(fontFamily = Nunito, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleSmall = base.titleSmall.nunito(),
    bodyLarge = TextStyle(fontFamily = Nunito, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp),
    bodySmall = base.bodySmall.nunito(),
    labelLarge = TextStyle(fontFamily = Nunito, fontSize = 18.sp, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.nunito(),
    labelSmall = base.labelSmall.nunito(),
)

@Composable
fun CalmTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CalmTypography,
        content = content,
    )
}
