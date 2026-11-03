package io.github.hh12356.hamenda.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun ToggleChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .height(36.dp)
            .background(
                if (selected) colors.primary else colors.surfaceContainerHigh,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 4.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** 用矢量绘制，避免 emoji/字体图标在不同设备上不一致。 */
@Composable
fun ChevronIcon(left: Boolean, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val nearX = size.width * if (left) 0.64f else 0.36f
        val farX = size.width * if (left) 0.38f else 0.62f
        drawLine(tint, Offset(nearX, size.height * 0.25f), Offset(farX, size.height * 0.5f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(farX, size.height * 0.5f), Offset(nearX, size.height * 0.75f), stroke, StrokeCap.Round)
    }
}

@Composable
fun SlidersIcon(tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val radius = 2.5.dp.toPx()
        drawLine(tint, Offset(size.width * 0.08f, size.height * 0.32f), Offset(size.width * 0.92f, size.height * 0.32f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.08f, size.height * 0.68f), Offset(size.width * 0.92f, size.height * 0.68f), stroke, StrokeCap.Round)
        drawCircle(tint, radius, Offset(size.width * 0.34f, size.height * 0.32f))
        drawCircle(tint, radius, Offset(size.width * 0.66f, size.height * 0.68f))
    }
}
