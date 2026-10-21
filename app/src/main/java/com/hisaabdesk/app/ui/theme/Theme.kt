
package com.hisaabdesk.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PrimaryGreen = Color(0xFF16834A)
val DarkGreen = Color(0xFF11643B)
val LightGreenBg = Color(0xFFEAF6EE)
val SurfaceWhite = Color(0xFFFFFFFF)
val MainText = Color(0xFF1B2A21)
val SecondaryText = Color(0xFF66736B)
val BorderColor = Color(0xFFE1E9E3)
val ErrorRed = Color(0xFFBA3030)
val WarningAmber = Color(0xFFA56713)

private val LightColors = lightColorScheme(
    primary = PrimaryGreen, onPrimary = Color.White, primaryContainer = LightGreenBg, onPrimaryContainer = DarkGreen,
    secondary = DarkGreen, background = Color(0xFFF7FAF8), onBackground = MainText,
    surface = SurfaceWhite, onSurface = MainText, surfaceVariant = LightGreenBg, onSurfaceVariant = SecondaryText,
    outline = BorderColor, error = ErrorRed
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MainText),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MainText),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, color = MainText),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, color = MainText),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun HisaabDeskTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = AppTypography, content = content)
}
