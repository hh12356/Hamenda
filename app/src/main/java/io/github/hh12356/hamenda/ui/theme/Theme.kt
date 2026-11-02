package io.github.hh12356.hamenda.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B57C9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E2FF),
    onPrimaryContainer = Color(0xFF17134F),
    secondary = Color(0xFF5F5D71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E1F5),
    onSecondaryContainer = Color(0xFF1B192B),
    tertiary = Color(0xFF2F6B5B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFF7F7FB),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE7E5EF),
    onSurfaceVariant = Color(0xFF46444E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F0F6),
    surfaceContainerHigh = Color(0xFFEBEAF2),
    surfaceContainerHighest = Color(0xFFE5E4ED),
    outline = Color(0xFF76747E),
    outlineVariant = Color(0xFFD8D6E1),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC2C0FF),
    onPrimary = Color(0xFF2C2868),
    primaryContainer = Color(0xFF423E86),
    onPrimaryContainer = Color(0xFFE4E2FF),
    secondary = Color(0xFFC9C5DD),
    onSecondary = Color(0xFF302E42),
    secondaryContainer = Color(0xFF474459),
    onSecondaryContainer = Color(0xFFE5E1F5),
    tertiary = Color(0xFF9AD3C0),
    onTertiary = Color(0xFF093729),
    background = Color(0xFF101014),
    onBackground = Color(0xFFE6E4EC),
    surface = Color(0xFF101014),
    onSurface = Color(0xFFE6E4EC),
    surfaceVariant = Color(0xFF292830),
    onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainerLowest = Color(0xFF0B0B0F),
    surfaceContainerLow = Color(0xFF17171C),
    surfaceContainer = Color(0xFF1C1C22),
    surfaceContainerHigh = Color(0xFF26262D),
    surfaceContainerHighest = Color(0xFF313038),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF3B3A43),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
)

/** mode: 0=跟随系统, 1=浅色, 2=深色 */
@Composable
fun HamendaTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
