package io.github.hh12356.hamenda.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import io.github.hh12356.hamenda.data.AppSettings
import io.github.hh12356.hamenda.data.CourseBlock
import io.github.hh12356.hamenda.data.DEFAULT_COLORS
import io.github.hh12356.hamenda.data.SCHOOL_PERIODS
import io.github.hh12356.hamenda.data.Store
import io.github.hh12356.hamenda.util.currentWeek
import io.github.hh12356.hamenda.util.dateFor
import io.github.hh12356.hamenda.util.dayName
import io.github.hh12356.hamenda.util.daysOf
import io.github.hh12356.hamenda.util.minuteLabel
import io.github.hh12356.hamenda.util.snap15
import io.github.hh12356.hamenda.util.weekRangeLabel
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import kotlin.math.roundToInt

private val LEFT_WIDTH = 44.dp
private val TIMELINE_PAD = 10.dp
private const val SWIPE_THRESHOLD_PX = 120f

private data class DragPreview(
    val blockId: Long,
    val dayIndex: Int,
    val startMin: Int,
    val endMin: Int
)

private data class EditTarget(val block: CourseBlock)

@Composable
fun WeekScreen(store: Store, onOpenSettings: () -> Unit) {
    val settings = store.data.settings
    val today = LocalDate.now()
    var week by remember(settings.semesterStart, settings.weekStartMonday) {
        mutableIntStateOf(currentWeek(settings, today))
    }
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var detailBlock by remember { mutableStateOf<CourseBlock?>(null) }
    var menuBlock by remember { mutableStateOf<CourseBlock?>(null) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopBar(
                settings = settings,
                week = week,
                onPrev = { if (week > 1) week-- },
                onNext = { week++ },
                onOpenSettings = onOpenSettings
            )
            DayHeaderRow(settings, week, today)
            WeekTimeline(
                store = store,
                settings = settings,
                week = week,
                modifier = Modifier.weight(1f),
                onChangeWeek = { delta -> if (week + delta >= 1) week += delta },
                onCreate = { day, minute ->
                    editTarget = EditTarget(
                        CourseBlock(
                            id = System.nanoTime(),
                            dayOfWeek = day,
                            startMin = minute,
                            endMin = (minute + 45).coerceAtMost(settings.dayEndMin),
                            color = DEFAULT_COLORS[store.data.blocks.size % DEFAULT_COLORS.size]
                        )
                    )
                },
                onTapBlock = { detailBlock = it },
                onLongPressBlock = { menuBlock = it }
            )
        }

        editTarget?.let { target ->
            EditScreen(
                initial = target.block,
                onCancel = { editTarget = null },
                onSave = { block ->
                    store.upsertBlock(block)
                    editTarget = null
                }
            )
        }
    }

    detailBlock?.let { block ->
        AlertDialog(
            onDismissRequest = { detailBlock = null },
            confirmButton = { TextButton(onClick = { detailBlock = null }) { Text("关闭") } },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(block.color)))
                    Spacer(Modifier.width(8.dp))
                    Text(block.name.ifBlank { "未命名课程" }, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (block.teacher.isNotBlank()) Text("老师 · ${block.teacher}", style = MaterialTheme.typography.bodyMedium)
                    if (block.location.isNotBlank()) Text("地点 · ${block.location}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "时间 · ${dayName(block.dayOfWeek)} ${minuteLabel(block.startMin)}–${minuteLabel(block.endMin)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("周次 · ${weekRangeLabel(block)}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    menuBlock?.let { block ->
        CourseMenuDialog(
            block = block,
            onDismiss = { menuBlock = null },
            onEdit = { menuBlock = null; editTarget = EditTarget(block) },
            onDeleteThis = { menuBlock = null; store.deleteBlock(block.id) },
            onDeleteCourse = { menuBlock = null; store.deleteByName(block.name) }
        )
    }
}

@Composable
private fun TopBar(
    settings: AppSettings,
    week: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("第 $week 周", style = MaterialTheme.typography.titleLarge)
            Text(
                weekDateRangeLabel(settings, week),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            if (settings.showWeekArrows) {
                IconButton(onClick = onPrev) { ChevronIcon(left = true) }
            }
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            if (settings.showWeekArrows) {
                IconButton(onClick = onNext) { ChevronIcon(left = false) }
            }
            IconButton(onClick = onOpenSettings) { SlidersIcon() }
        }
    }
}

@Composable
private fun DayHeaderRow(settings: AppSettings, week: Int, today: LocalDate) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)) {
        Spacer(Modifier.width(LEFT_WIDTH))
        daysOf(settings).forEach { iso ->
            val date = dateFor(settings, week, iso)
            val isToday = date == today
            Column(
                Modifier.weight(1f).padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    dayName(iso),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium
                )
                Spacer(Modifier.height(3.dp))
                if (isToday) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "${date.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Text(
                        "${date.monthValue}/${date.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun weekDateRangeLabel(settings: AppSettings, week: Int): String {
    val days = daysOf(settings)
    val start = dateFor(settings, week, days.first())
    val end = dateFor(settings, week, days.last())
    return "${start.monthValue}月${start.dayOfMonth}日 – ${end.monthValue}月${end.dayOfMonth}日"
}

@Composable
private fun WeekTimeline(
    store: Store,
    settings: AppSettings,
    week: Int,
    modifier: Modifier = Modifier,
    onChangeWeek: (Int) -> Unit,
    onCreate: (Int, Int) -> Unit,
    onTapBlock: (CourseBlock) -> Unit,
    onLongPressBlock: (CourseBlock) -> Unit
) {
    val days = remember(settings.fiveDayMode, settings.weekStartMonday) { daysOf(settings) }
    val blocks = store.data.blocks
    var drag by remember { mutableStateOf<DragPreview?>(null) }

    BoxWithConstraints(modifier.fillMaxWidth().clipToBounds()) {
        val totalMinutes = (settings.dayEndMin - settings.dayStartMin).coerceAtLeast(60)
        val dpPerMin = (maxHeight - TIMELINE_PAD * 2f) / totalMinutes
        val colWidth = (maxWidth - LEFT_WIDTH) / days.size
        val density = LocalDensity.current
        val colWidthPx = with(density) { colWidth.toPx() }
        val pxPerMin = with(density) { dpPerMin.toPx() }
        val leftPx = with(density) { LEFT_WIDTH.toPx() }
        val padPx = with(density) { TIMELINE_PAD.toPx() }
        val todayIdx = days.indexOf(LocalDate.now().dayOfWeek.value)

        val separator = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        val hourGrid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        val dragGrid = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        val todayTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.045f)

        fun previewFrom(block: CourseBlock, originIdx: Int, acc: Offset): DragPreview {
            val dayOffset = (acc.x / colWidthPx).roundToInt()
            val newIdx = (originIdx + dayOffset).coerceIn(0, days.size - 1)
            val raw = block.startMin + (acc.y / pxPerMin).toInt()
            val maxStart = (settings.dayEndMin - block.duration).coerceAtLeast(settings.dayStartMin)
            val start = snap15(raw).coerceIn(settings.dayStartMin, maxStart)
            return DragPreview(block.id, newIdx, start, start + block.duration)
        }

        fun hitBlock(pos: Offset): Boolean {
            if (pos.x < leftPx) return false
            val idx = ((pos.x - leftPx) / colWidthPx).toInt().coerceIn(0, days.size - 1)
            val minute = snap15(settings.dayStartMin + ((pos.y - padPx) / pxPerMin).toInt())
            return blocks.any { b ->
                b.dayOfWeek == days[idx] && b.activeInWeek(week) &&
                    minute in b.startMin until b.endMin
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(days, week, settings.dayStartMin, settings.dayEndMin, blocks) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 从课程块起手时交给块的拖动，否则会把长按拖动当横滑消费掉。
                        if (hitBlock(down.position)) return@awaitEachGesture
                        var total = 0f
                        drag(down.id) { change ->
                            total += change.positionChange().x
                            change.consume()
                        }
                        if (total < -SWIPE_THRESHOLD_PX) onChangeWeek(1)
                        else if (total > SWIPE_THRESHOLD_PX) onChangeWeek(-1)
                    }
                }
                .pointerInput(days, week, settings.dayStartMin, settings.dayEndMin, blocks) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 只有真正等满长按时长才创建；被子元素消费（拖动课程）时提前返回，不能误建。
                        val startAt = System.currentTimeMillis()
                        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation()
                        }
                        val elapsed = System.currentTimeMillis() - startAt
                        if (up == null && elapsed >= viewConfiguration.longPressTimeoutMillis - 32) {
                            val pos = down.position
                            if (!hitBlock(pos)) {
                                val idx = ((pos.x - leftPx) / colWidthPx).toInt().coerceIn(0, days.size - 1)
                                val minute = snap15(settings.dayStartMin + ((pos.y - padPx) / pxPerMin).toInt())
                                onCreate(days[idx], minute)
                            }
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (todayIdx >= 0) {
                    drawRect(
                        todayTint,
                        Offset(leftPx + todayIdx * colWidthPx, 0f),
                        Size(colWidthPx, size.height)
                    )
                }
                var m = ((settings.dayStartMin + 59) / 60) * 60
                while (m <= settings.dayEndMin) {
                    val y = padPx + (m - settings.dayStartMin) * pxPerMin
                    drawLine(hourGrid, Offset(leftPx, y), Offset(size.width, y), 1.dp.toPx())
                    m += 60
                }
                for (i in 0..days.size) {
                    val x = leftPx + i * colWidthPx
                    drawLine(separator, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                }
                drag?.let { d ->
                    val x0 = leftPx + d.dayIndex * colWidthPx
                    drawRect(highlight, Offset(x0, 0f), Size(colWidthPx, size.height))
                    var mm = settings.dayStartMin
                    while (mm <= settings.dayEndMin) {
                        val y = padPx + (mm - settings.dayStartMin) * pxPerMin
                        drawLine(dragGrid, Offset(leftPx, y), Offset(size.width, y), 1.dp.toPx())
                        mm += 15
                    }
                }
            }

            LeftLabels(
                settings = settings,
                dpPerMin = dpPerMin,
                onCycle = {
                    store.updateSettings { s -> s.copy(leftLabelMode = (s.leftLabelMode + 1) % 3) }
                }
            )

            days.forEachIndexed { idx, iso ->
                blocks.forEach { block ->
                    if (block.dayOfWeek == iso && block.activeInWeek(week)) {
                        val preview = drag?.takeIf { it.blockId == block.id }
                        val targetIdx = preview?.dayIndex ?: idx
                        val start = preview?.startMin ?: block.startMin
                        val end = preview?.endMin ?: block.endMin
                        if (end > settings.dayStartMin && start < settings.dayEndMin) {
                            val top = TIMELINE_PAD + (start - settings.dayStartMin).toFloat() * dpPerMin
                            val height = (end - start).toFloat() * dpPerMin
                            val x = LEFT_WIDTH + colWidth * targetIdx.toFloat() + 1.dp
                            val shape = RoundedCornerShape(8.dp)

                            val gesture = Modifier.pointerInput(
                                block, week, colWidthPx, pxPerMin, days.size
                            ) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val longPressed = awaitLongPressOrCancellation(down.id)
                                    if (longPressed == null) {
                                        // 快速抬起且未被上层消费 = 点击；被消费（如横向滑动）则忽略。
                                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                                        if (up != null && !up.pressed && !up.isConsumed) onTapBlock(block)
                                        return@awaitEachGesture
                                    }

                                    var acc = Offset.Zero
                                    var moved = false
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                change.consume()
                                                break
                                            }
                                            val delta = change.positionChange()
                                            if (delta != Offset.Zero) {
                                                acc += delta
                                                change.consume()
                                                moved = true
                                                drag = previewFrom(block, idx, acc)
                                            }
                                            if (event.changes.none { it.pressed }) break
                                        }
                                    } finally {
                                        if (moved) {
                                            drag?.takeIf { it.blockId == block.id }?.let { p ->
                                                store.upsertBlock(
                                                    block.copy(
                                                        dayOfWeek = days[p.dayIndex],
                                                        startMin = p.startMin,
                                                        endMin = p.endMin
                                                    )
                                                )
                                            }
                                        }
                                        drag = null
                                    }
                                    if (!moved) onLongPressBlock(block)
                                }
                            }

                            val bg = Color(block.color)
                            val fg = if (bg.luminance() > 0.5f) Color(0xFF2B2B33) else Color.White

                            Column(
                                Modifier
                                    .offset(x = x, y = top)
                                    .width(colWidth - 3.dp)
                                    .height(height.coerceAtLeast(2.dp))
                                    .zIndex(if (preview != null) 1f else 0f)
                                    .shadow(if (preview != null) 8.dp else 0.dp, shape)
                                    .clip(shape)
                                    .background(bg)
                                    .border(1.dp, fg.copy(alpha = 0.07f), shape)
                                    .then(gesture)
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    block.name,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = fg,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (block.teacher.isNotBlank()) {
                                    Text(
                                        block.teacher,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = fg.copy(alpha = 0.75f)
                                    )
                                }
                                if (block.location.isNotBlank()) {
                                    Text(
                                        block.location,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = fg.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftLabels(settings: AppSettings, dpPerMin: Dp, onCycle: () -> Unit) {
    Box(
        Modifier
            .fillMaxHeight()
            .width(LEFT_WIDTH)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCycle
            )
    ) {
        val style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFeatureSettings = "tnum"
        )
        AnimatedContent(
            targetState = settings.leftLabelMode,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 40 })
                    .togetherWith(fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 40 })
            },
            label = "leftLabels"
        ) { mode ->
            Box(Modifier.fillMaxSize()) {
                buildLeftLabels(settings, mode).forEach { (minute, text) ->
                    val y = TIMELINE_PAD + (minute - settings.dayStartMin).toFloat() * dpPerMin
                    Text(
                        text = text,
                        modifier = Modifier.offset(x = 6.dp, y = y - 5.dp),
                        style = style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun buildLeftLabels(settings: AppSettings, mode: Int): List<Pair<Int, String>> = when (mode) {
    1 -> SCHOOL_PERIODS
        .filter { it.startMin >= settings.dayStartMin && it.endMin <= settings.dayEndMin }
        .map { it.startMin to "${it.index}" }

    2 -> SCHOOL_PERIODS
        .filter { it.startMin >= settings.dayStartMin && it.endMin <= settings.dayEndMin }
        .map { it.startMin to "${minuteLabel(it.startMin)}\n${minuteLabel(it.endMin)}" }

    else -> {
        val result = mutableListOf<Pair<Int, String>>()
        var h = ((settings.dayStartMin + 59) / 60) * 60
        while (h <= settings.dayEndMin) {
            result += h to minuteLabel(h)
            h += 60
        }
        result
    }
}

@Composable
private fun CourseMenuDialog(
    block: CourseBlock,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteThis: () -> Unit,
    onDeleteCourse: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.width(240.dp).padding(vertical = 8.dp)) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(block.color)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        block.name.ifBlank { "未命名课程" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                MenuItemText("编辑", onEdit)
                MenuItemText("删除这节课", onDeleteThis, danger = true)
                MenuItemText("删除这门课", onDeleteCourse, danger = true)
            }
        }
    }
}

@Composable
private fun MenuItemText(text: String, onClick: () -> Unit, danger: Boolean = false) {
    Text(
        text,
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )
}
