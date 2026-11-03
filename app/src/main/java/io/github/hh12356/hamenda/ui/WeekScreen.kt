package io.github.hh12356.hamenda.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
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
    val originIdx: Int,
    val dxPx: Float,
    val startMin: Float,
    val duration: Int
) {
    /** 松手/高光才吸附到某一列，拖动过程中块是连续的。 */
    fun targetIdx(colWidthPx: Float, dayCount: Int): Int =
        (originIdx + (dxPx / colWidthPx).roundToInt()).coerceIn(0, dayCount - 1)
}

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
                onLongPressBlock = { menuBlock = it },
                onDragBlock = { menuBlock = null }
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
            onDeleteThis = { menuBlock = null; store.skipWeek(block, week) },
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
            if (settings.showWeekRange) {
                Text(
                    weekDateRangeLabel(settings, week),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (settings.showWeekArrows) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier.align(Alignment.Center).offset(x = (-62).dp)
            ) { ChevronIcon(left = true) }
            IconButton(
                onClick = onNext,
                modifier = Modifier.align(Alignment.Center).offset(x = 62.dp)
            ) { ChevronIcon(left = false) }
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) { SlidersIcon() }
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
    onLongPressBlock: (CourseBlock) -> Unit,
    onDragBlock: () -> Unit
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
        val textMeasurer = rememberTextMeasurer()

        val separator = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        val hourGrid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        val todayTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.045f)

        fun previewFrom(block: CourseBlock, originIdx: Int, acc: Offset): DragPreview {
            val raw = block.startMin + acc.y / pxPerMin
            val maxStart = (settings.dayEndMin - block.duration).coerceAtLeast(settings.dayStartMin)
            return DragPreview(
                blockId = block.id,
                originIdx = originIdx,
                dxPx = acc.x.coerceIn(
                    -(originIdx * colWidthPx),
                    (days.size - 1 - originIdx) * colWidthPx
                ),
                startMin = raw.coerceIn(settings.dayStartMin.toFloat(), maxStart.toFloat()),
                duration = block.duration
            )
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
                // 横线跟左缘同一种分格，且一直画到左缘里，两边对得上。
                val cells = labelCells(settings, settings.leftLabelMode)
                (cells.map { it.startMin } + cells.map { it.endMin }).distinct().forEach { m ->
                    val y = padPx + (m - settings.dayStartMin) * pxPerMin
                    drawLine(hourGrid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                for (i in 0..days.size) {
                    val x = leftPx + i * colWidthPx
                    drawLine(separator, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                }
                drag?.let { d ->
                    val x0 = leftPx + d.targetIdx(colWidthPx, days.size) * colWidthPx
                    drawRect(highlight, Offset(x0, 0f), Size(colWidthPx, size.height))
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
                        val start = preview?.startMin ?: block.startMin.toFloat()
                        val end = preview?.let { it.startMin + it.duration } ?: block.endMin.toFloat()
                        if (end > settings.dayStartMin && start < settings.dayEndMin) {
                            val top = TIMELINE_PAD + (start - settings.dayStartMin) * dpPerMin
                            val height = (end - start) * dpPerMin
                            val dx = preview?.let { with(density) { it.dxPx.toDp() } } ?: 0.dp
                            val x = LEFT_WIDTH + colWidth * idx.toFloat() + 1.dp + dx
                            val shape = RoundedCornerShape(8.dp)

                            // 两段手势：一动就拖（跟手），按住不动才弹菜单。两者互不阻塞。
                            val gesture = Modifier
                                .pointerInput(block, week, colWidthPx, pxPerMin, days.size) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var acc = Offset.Zero
                                        var dragging = false
                                        var tapped = false
                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change =
                                                    event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) {
                                                    tapped = !dragging && !change.isConsumed
                                                    break
                                                }
                                                val delta = change.positionChange()
                                                if (delta != Offset.Zero) {
                                                    acc += delta
                                                    if (!dragging && acc.getDistance() > viewConfiguration.touchSlop) {
                                                        dragging = true
                                                        // 从越过触摸阈值处起算，块的首帧不跳。
                                                        acc = Offset.Zero
                                                        onDragBlock()
                                                    }
                                                    if (dragging) {
                                                        change.consume()
                                                        drag = previewFrom(block, idx, acc)
                                                    }
                                                }
                                            }
                                            if (tapped) onTapBlock(block)
                                        } finally {
                                            if (dragging) {
                                                drag?.takeIf { it.blockId == block.id }?.let { p ->
                                                    // 拖动时不吸附，松手才吸附到 15 分钟和最近的一列。
                                                    val snapped = snap15(p.startMin.roundToInt())
                                                        .coerceIn(
                                                            settings.dayStartMin,
                                                            settings.dayEndMin - p.duration
                                                        )
                                                    store.upsertBlock(
                                                        block.copy(
                                                            dayOfWeek =
                                                                days[p.targetIdx(colWidthPx, days.size)],
                                                            startMin = snapped,
                                                            endMin = snapped + p.duration
                                                        )
                                                    )
                                                }
                                            }
                                            drag = null
                                        }
                                    }
                                }
                                .pointerInput(block, week) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        // 拖动会消费移动事件，长按自动作废；原地不动满时长才弹菜单。
                                        val longPressed = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                                        onLongPressBlock(block)
                                        // 吞掉抬起，避免同时被判成点击。
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                change.consume()
                                                break
                                            }
                                        }
                                    }
                                }

                            val bg = Color(block.color)
                            val fg = if (bg.luminance() > 0.5f) Color(0xFF2B2B33) else Color.White

                            // 只排能完整放下的行，避免半个字被切在块边缘。
                            val lineName = with(density) { 12.sp.toDp().value }
                            val lineSub = with(density) { 11.sp.toDp().value }
                            val innerH = (height - 6.dp).value.coerceAtLeast(0f)
                            val textWidthPx = with(density) { (colWidth - 11.dp).roundToPx() }
                            val nameStyle = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
                            val subStyle = TextStyle(fontSize = 9.sp, lineHeight = 11.sp)
                            fun linesOf(text: String, style: TextStyle, max: Int): Int =
                                if (text.isBlank()) 0 else textMeasurer.measure(
                                    text, style, maxLines = max,
                                    constraints = Constraints(maxWidth = textWidthPx)
                                ).lineCount

                            val nameLines = minOf(
                                linesOf(block.name, nameStyle, 3),
                                (innerH / lineName).toInt().coerceIn(0, 3)
                            )
                            var rest = innerH - nameLines * lineName
                            val teacherLines = minOf(
                                linesOf(block.teacher, subStyle, 2),
                                (rest / lineSub).toInt().coerceIn(0, 2)
                            )
                            rest -= teacherLines * lineSub
                            val locationLines = minOf(
                                linesOf(block.location, subStyle, 3),
                                (rest / lineSub).toInt().coerceIn(0, 3)
                            )

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
                                if (nameLines > 0) Text(
                                    block.name,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    maxLines = nameLines,
                                    overflow = TextOverflow.Ellipsis,
                                    color = fg,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (teacherLines > 0) {
                                    Text(
                                        block.teacher,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        maxLines = teacherLines,
                                        overflow = TextOverflow.Ellipsis,
                                        color = fg.copy(alpha = 0.75f)
                                    )
                                }
                                if (locationLines > 0) {
                                    Text(
                                        block.location,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        maxLines = locationLines,
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
                (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 3 })
                    .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(140)) { -it / 3 })
            },
            label = "leftLabels"
        ) { mode ->
            Box(Modifier.fillMaxSize()) {
                labelCells(settings, mode).forEach { cell ->
                    // 文字放在这一格的中间，横线才不会压到字上。
                    val mid = TIMELINE_PAD +
                        ((cell.startMin + cell.endMin) / 2f - settings.dayStartMin) * dpPerMin
                    val half = if ('\n' in cell.text) 12.dp else 5.dp
                    Text(
                        text = cell.text,
                        modifier = Modifier.offset(x = 6.dp, y = mid - half),
                        style = style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class LabelCell(val startMin: Int, val endMin: Int, val text: String)

/** 左缘的每一格，同时也是日程表横线的来源。 */
private fun labelCells(settings: AppSettings, mode: Int): List<LabelCell> = when (mode) {
    1, 2 -> {
        // 格子画到下一节上课为止，下课那条线归上一节，避免两条线挤在一起。
        val periods = SCHOOL_PERIODS
            .filter { it.startMin >= settings.dayStartMin && it.endMin <= settings.dayEndMin }
        periods.mapIndexed { i, p ->
            LabelCell(
                p.startMin,
                periods.getOrNull(i + 1)?.startMin ?: p.endMin,
                if (mode == 1) "${p.index}" else "${minuteLabel(p.startMin)}\n${minuteLabel(p.endMin)}"
            )
        }
    }

    else -> buildList {
        var h = ((settings.dayStartMin + 59) / 60) * 60
        while (h <= settings.dayEndMin) {
            add(LabelCell(h, (h + 60).coerceAtMost(settings.dayEndMin), minuteLabel(h)))
            h += 60
        }
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
                MenuItemText("删除本周这节课", onDeleteThis, danger = true)
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
