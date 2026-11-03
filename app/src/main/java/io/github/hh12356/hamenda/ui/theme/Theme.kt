package io.github.hh12356.hamenda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 主题色只换 primary 这一组，中性色固定，省得每套色都手写一整张表。 */
data class Accent(
    val name: String,
    val lightPrimary: Color,
    val lightContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color
)

val ACCENTS = listOf(
    Accent("蓝", Color(0xFF2E63C8), Color(0xFFDCE6FF), Color(0xFFB3C8FF), Color(0xFF2A4374)),
    Accent("青", Color(0xFF0F7A6E), Color(0xFFC9F2EA), Color(0xFF7FD8CB), Color(0xFF0F544B)),
    Accent("绿", Color(0xFF2E7D32), Color(0xFFD7F0D2), Color(0xFFA5D6A0), Color(0xFF1E5222)),
    Accent("橙", Color(0xFF8F5A00), Color(0xFFFFDDB8), Color(0xFFF7BE72), Color(0xFF6E4200)),
    Accent("玫红", Color(0xFFB3315B), Color(0xFFFFD9E2), Color(0xFFFFB1C6), Color(0xFF7D2B47)),
    Accent("紫罗兰", Color(0xFF5B57C9), Color(0xFFE4E2FF), Color(0xFFC2C0FF), Color(0xFF423E86))
)

/** primary/container 上的前景色由亮度决定，不用每个色号各写一遍。 */
private fun onColor(c: Color) = if (c.luminance() > 0.5f) Color(0xFF1B1B1E) else Color.White

private val NeutralLight = lightColorScheme(
    secondary = Color(0xFF5F5F66),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E4E8),
    onSecondaryContainer = Color(0xFF1B1B21),
    tertiary = Color(0xFF5F5F66),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF1B1B1E),
    surface = Color(0xFFF7F7F8),
    onSurface = Color(0xFF1B1B1E),
    surfaceVariant = Color(0xFFE6E6E9),
    onSurfaceVariant = Color(0xFF46464C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F1F3),
    surfaceContainerHigh = Color(0xFFEBEBEE),
    surfaceContainerHighest = Color(0xFFE5E5E9),
    outline = Color(0xFF76767C),
    outlineVariant = Color(0xFFD8D8DD),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val NeutralDark = darkColorScheme(
    secondary = Color(0xFFC8C8CF),
    onSecondary = Color(0xFF303036),
    secondaryContainer = Color(0xFF47474D),
    onSecondaryContainer = Color(0xFFE4E4E8),
    tertiary = Color(0xFFC8C8CF),
    onTertiary = Color(0xFF303036),
    background = Color(0xFF101012),
    onBackground = Color(0xFFE6E6EA),
    surface = Color(0xFF101012),
    onSurface = Color(0xFFE6E6EA),
    surfaceVariant = Color(0xFF29292E),
    onSurfaceVariant = Color(0xFFC8C8CF),
    surfaceContainerLowest = Color(0xFF0B0B0D),
    surfaceContainerLow = Color(0xFF17171A),
    surfaceContainer = Color(0xFF1C1C20),
    surfaceContainerHigh = Color(0xFF26262A),
    surfaceContainerHighest = Color(0xFF313136),
    outline = Color(0xFF91919A),
    outlineVariant = Color(0xFF3B3B41),
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

/** mode: 0=跟随系统, 1=浅色, 2=深色；accent 是 [ACCENTS] 的下标。 */
@Composable
fun HamendaTheme(mode: Int, accent: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val a = ACCENTS.getOrElse(accent) { ACCENTS[0] }
    val primary = if (dark) a.darkPrimary else a.lightPrimary
    val container = if (dark) a.darkContainer else a.lightContainer
    MaterialTheme(
        colorScheme = (if (dark) NeutralDark else NeutralLight).copy(
            primary = primary,
            onPrimary = onColor(primary),
            primaryContainer = container,
            onPrimaryContainer = onColor(container)
        ),
        typography = AppTypography,
        content = content
    )
}
