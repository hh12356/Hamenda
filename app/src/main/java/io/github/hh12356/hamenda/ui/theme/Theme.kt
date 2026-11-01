package io.github.hh12356.hamenda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F5F8A),
    background = Color(0xFFF6F6F8),
    surface = Color(0xFFF6F6F8),
    surfaceVariant = Color(0xFFEDEDF1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7F0),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF141416),
    surfaceVariant = Color(0xFF1C1C1F)
)

/** mode: 0=跟随系统, 1=浅色, 2=深色 */
@Composable
fun HamendaTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
