package io.github.hh12356.hamenda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import io.github.hh12356.hamenda.data.CourseBlock
import io.github.hh12356.hamenda.data.DEFAULT_COLORS
import io.github.hh12356.hamenda.data.SCHOOL_PERIODS
import io.github.hh12356.hamenda.util.dayName
import io.github.hh12356.hamenda.util.minuteLabel
import io.github.hh12356.hamenda.util.parseTime
import io.github.hh12356.hamenda.util.periodsInRange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditScreen(initial: CourseBlock, onCancel: () -> Unit, onSave: (CourseBlock) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var teacher by remember { mutableStateOf(initial.teacher) }
    var location by remember { mutableStateOf(initial.location) }
    var color by remember { mutableIntStateOf(initial.color) }
    var parity by remember { mutableIntStateOf(initial.parity) }
    var day by remember { mutableIntStateOf(initial.dayOfWeek) }
    var startText by remember { mutableStateOf(minuteLabel(initial.startMin)) }
    var endText by remember { mutableStateOf(minuteLabel(initial.endMin)) }
    var showNameError by remember { mutableStateOf(false) }
    val weeks = remember { mutableStateListOf<Int>().apply { addAll(initial.startWeek..initial.endWeek) } }
    val periods = remember { mutableStateListOf<Int>().apply { addAll(periodsInRange(initial.startMin, initial.endMin)) } }

    fun syncTimesFromPeriods() {
        val sel = periods.sorted()
        if (sel.isEmpty()) return
        val first = SCHOOL_PERIODS.first { it.index == sel.first() }
        val last = SCHOOL_PERIODS.first { it.index == sel.last() }
        startText = minuteLabel(first.startMin)
        endText = minuteLabel(last.endMin)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) { Text("取消") }
            Text(
                if (initial.name.isBlank()) "新建课程" else "编辑课程",
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            TextButton(onClick = {
                if (name.isBlank()) {
                    showNameError = true
                    return@TextButton
                }
                val start = parseTime(startText) ?: initial.startMin
                val rawEnd = parseTime(endText) ?: initial.endMin
                val end = if (rawEnd <= start) start + 45 else rawEnd
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
            }) { Text("保存", fontWeight = FontWeight.SemiBold) }
        }
        HorizontalDivider()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Section("背景色") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DEFAULT_COLORS.forEach { c ->
                        val selected = c == color
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }

            Section("基本信息") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("课程名") },
                    singleLine = true,
                    isError = showNameError,
                    supportingText = if (showNameError) {
                        { Text("请输入课程名") }
                    } else null
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("老师") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("地点") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("开始") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("结束") },
                        singleLine = true
                    )
                }
                Text(
                    "时间格式 HH:mm，例：8:00",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Section("周数") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..25).forEach { w ->
                        BoxChip(
                            text = "$w",
                            selected = w in weeks,
                            onClick = { if (w in weeks) weeks.remove(w) else weeks.add(w) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("每周" to 0, "单周" to 1, "双周" to 2).forEach { (label, v) ->
                        BoxChip(text = label, selected = parity == v, wide = true) { parity = v }
                    }
                }
            }

            Section("节数") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..13).forEach { p ->
                        BoxChip(
                            text = "$p",
                            selected = p in periods,
                            onClick = {
                                if (p in periods) periods.remove(p) else periods.add(p)
                                syncTimesFromPeriods()
                            }
                        )
                    }
                }
            }

            Section("星期") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { iso ->
                        BoxChip(text = dayName(iso), selected = day == iso, wide = true) { day = iso }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun BoxChip(
    text: String,
    selected: Boolean,
    wide: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .then(if (wide) Modifier.height(34.dp) else Modifier.size(34.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (wide) 12.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
