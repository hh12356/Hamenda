package io.github.hh12356.hamenda.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.hh12356.hamenda.data.CourseBlock
import io.github.hh12356.hamenda.data.DEFAULT_COLORS
import io.github.hh12356.hamenda.data.SCHOOL_PERIODS
import io.github.hh12356.hamenda.util.dayName
import io.github.hh12356.hamenda.util.periodsInRange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditScreen(initial: CourseBlock, onCancel: () -> Unit, onSave: (CourseBlock) -> Unit) {
    BackHandler(onBack = onCancel)
    var name by remember { mutableStateOf(initial.name) }
    var teacher by remember { mutableStateOf(initial.teacher) }
    var location by remember { mutableStateOf(initial.location) }
    var color by remember { mutableIntStateOf(initial.color) }
    var parity by remember { mutableIntStateOf(initial.parity) }
    var day by remember { mutableIntStateOf(initial.dayOfWeek) }
    var startH by remember { mutableStateOf((initial.startMin / 60).toString()) }
    var startM by remember { mutableStateOf((initial.startMin % 60).toString()) }
    var endH by remember { mutableStateOf((initial.endMin / 60).toString()) }
    var endM by remember { mutableStateOf((initial.endMin % 60).toString()) }
    var showNameError by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    val weeks = remember { mutableStateListOf<Int>().apply { addAll(initial.startWeek..initial.endWeek) } }
    val periods = remember { mutableStateListOf<Int>().apply { addAll(periodsInRange(initial.startMin, initial.endMin)) } }

    fun syncTimesFromPeriods() {
        val sel = periods.sorted()
        if (sel.isEmpty()) return
        val first = SCHOOL_PERIODS.first { it.index == sel.first() }
        val last = SCHOOL_PERIODS.first { it.index == sel.last() }
        startH = (first.startMin / 60).toString()
        startM = (first.startMin % 60).toString()
        endH = (last.endMin / 60).toString()
        endM = (last.endMin % 60).toString()
        showTimeError = false
    }

    fun timeOrNull(h: String, m: String): Int? {
        val hh = h.toIntOrNull() ?: return null
        val mm = m.toIntOrNull() ?: return null
        if (hh !in 0..23 || mm !in 0..59) return null
        return hh * 60 + mm
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                if (initial.name.isBlank()) "新建课程" else "编辑课程",
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showNameError = true
                        return@Button
                    }
                    val start = timeOrNull(startH, startM)
                    val end = timeOrNull(endH, endM)
                    // 格式或顺序不合法就不让存。
                    if (start == null || end == null || end <= start) {
                        showTimeError = true
                        return@Button
                    }
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            teacher = teacher.trim(),
                            location = location.trim(),
                            dayOfWeek = day,
                            startMin = start,
                            endMin = end,
                            startWeek = weeks.minOrNull() ?: initial.startWeek,
                            endWeek = weeks.maxOrNull() ?: initial.endWeek,
                            parity = parity,
                            color = color
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(36.dp)
            ) { Text("保存", style = MaterialTheme.typography.labelLarge) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("颜色") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DEFAULT_COLORS.forEach { c ->
                        val selected = c == color
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { color = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2B2B33))
                                )
                            }
                        }
                    }
                }
            }

            SectionCard("课程信息") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("课程名") },
                    singleLine = true,
                    isError = showNameError,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = if (showNameError) {
                        { Text("请输入课程名") }
                    } else null
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("老师") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("地点") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                TimeRow(
                    label = "开始",
                    hour = startH,
                    minute = startM,
                    isError = showTimeError,
                    onHour = { startH = it; showTimeError = false },
                    onMinute = { startM = it; showTimeError = false }
                )
                TimeRow(
                    label = "结束",
                    hour = endH,
                    minute = endM,
                    isError = showTimeError,
                    onHour = { endH = it; showTimeError = false },
                    onMinute = { endM = it; showTimeError = false }
                )
                Text(
                    if (showTimeError) "小时填 0–23，分钟填 0–59，且结束时间要晚于开始时间"
                    else "小时 0–23，分钟 0–59",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showTimeError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("上课周次") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..25).forEach { w ->
                        ToggleChip(
                            label = "$w",
                            selected = w in weeks,
                            onClick = { if (w in weeks) weeks.remove(w) else weeks.add(w) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("每周" to 0, "单周" to 1, "双周" to 2).forEach { (label, v) ->
                        ToggleChip(
                            label = label,
                            selected = parity == v,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                parity = v
                                // 选单/双周时按当前已选范围自动填好周次，不用手点。
                                val lo = weeks.minOrNull() ?: 1
                                val hi = weeks.maxOrNull() ?: 25
                                val picked = (lo..hi).filter {
                                    when (v) {
                                        1 -> it % 2 == 1
                                        2 -> it % 2 == 0
                                        else -> true
                                    }
                                }
                                weeks.clear()
                                weeks.addAll(picked)
                            }
                        )
                    }
                }
            }

            SectionCard("节数") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..13).forEach { p ->
                        ToggleChip(
                            label = "$p",
                            selected = p in periods,
                            onClick = {
                                if (p in periods) periods.remove(p) else periods.add(p)
                                syncTimesFromPeriods()
                            }
                        )
                    }
                }
                Text(
                    "勾选节数会自动换算上下课时间，也可在上方手动微调",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("星期") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { iso ->
                        ToggleChip(
                            label = dayName(iso),
                            selected = day == iso,
                            modifier = Modifier.weight(1f),
                            onClick = { day = iso }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    hour: String,
    minute: String,
    isError: Boolean,
    onHour: (String) -> Unit,
    onMinute: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(36.dp), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = hour,
            onValueChange = { v -> onHour(v.filter { it.isDigit() }.take(2)) },
            modifier = Modifier.weight(1f),
            label = { Text("时") },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = minute,
            onValueChange = { v -> onMinute(v.filter { it.isDigit() }.take(2)) },
            modifier = Modifier.weight(1f),
            label = { Text("分") },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}
