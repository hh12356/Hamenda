package io.github.hh12356.hamenda.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hh12356.hamenda.data.SCHOOL_PERIODS
import io.github.hh12356.hamenda.data.Store
import io.github.hh12356.hamenda.util.minuteLabel
import java.time.LocalDate

@Composable
fun SettingsScreen(store: Store, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = store.data.settings
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(store.exportJson().toByteArray())
                } ?: error("cannot open output")
            }.isSuccess
            Toast.makeText(context, if (ok) "已导出" else "导出失败", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text == null) {
                Toast.makeText(context, "读取失败", Toast.LENGTH_SHORT).show()
            } else {
                pendingImport = text
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹", fontSize = 24.sp) }
            Text(
                "设置",
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Spacer(Modifier.width(48.dp))
        }
        HorizontalDivider()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            SettingRow(
                title = "开学日期",
                value = settings.semesterStart.ifBlank { "未设置" }
            ) {
                val initial = runCatching { LocalDate.parse(settings.semesterStart) }
                    .getOrNull() ?: LocalDate.now()
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        store.updateSettings { it.copy(semesterStart = LocalDate.of(y, m + 1, d).toString()) }
                    },
                    initial.year,
                    initial.monthValue - 1,
                    initial.dayOfMonth
                ).show()
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("一周起始日")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip("周日", !settings.weekStartMonday) {
                        store.updateSettings { it.copy(weekStartMonday = false) }
                    }
                    ToggleChip("周一", settings.weekStartMonday) {
                        store.updateSettings { it.copy(weekStartMonday = true) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("显示天数")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip("7 天", !settings.fiveDayMode) {
                        store.updateSettings { it.copy(fiveDayMode = false) }
                    }
                    ToggleChip("5 天（周一至周五）", settings.fiveDayMode) {
                        store.updateSettings { it.copy(fiveDayMode = true) }
                    }
                }
            }

            SettingRow("每日范围开始", minuteLabel(settings.dayStartMin)) {
                TimePickerDialog(
                    context,
                    { _, h, m ->
                        store.updateSettings { s ->
                            s.copy(dayStartMin = (h * 60 + m).coerceAtMost(s.dayEndMin - 60))
                        }
                    },
                    settings.dayStartMin / 60,
                    settings.dayStartMin % 60,
                    true
                ).show()
            }

            SettingRow("每日范围结束", minuteLabel(settings.dayEndMin)) {
                TimePickerDialog(
                    context,
                    { _, h, m ->
                        store.updateSettings { s ->
                            s.copy(dayEndMin = (h * 60 + m).coerceAtLeast(s.dayStartMin + 60))
                        }
                    },
                    settings.dayEndMin / 60,
                    settings.dayEndMin % 60,
                    true
                ).show()
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("显示左右箭头", Modifier.weight(1f), fontSize = 15.sp)
                Switch(
                    checked = settings.showWeekArrows,
                    onCheckedChange = { v -> store.updateSettings { it.copy(showWeekArrows = v) } }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("主题")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip("跟随系统", settings.themeMode == 0) {
                        store.updateSettings { it.copy(themeMode = 0) }
                    }
                    ToggleChip("浅色", settings.themeMode == 1) {
                        store.updateSettings { it.copy(themeMode = 1) }
                    }
                    ToggleChip("深色", settings.themeMode == 2) {
                        store.updateSettings { it.copy(themeMode = 2) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("作息时间（只读）")
                SCHOOL_PERIODS.forEach { p ->
                    Text(
                        "第 ${p.index} 节　${minuteLabel(p.startMin)} – ${minuteLabel(p.endMin)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("备份")
                Button(onClick = { exportLauncher.launch("hamenda-backup.json") }, Modifier.fillMaxWidth()) {
                    Text("导出备份")
                }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) {
                    Text("导入备份")
                }
            }

            Text(
                "Hamenda 0.1.0 · 数据仅保存在本机",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入备份") },
            text = { Text("导入会覆盖当前所有课程和设置，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = store.importJson(text)
                    pendingImport = null
                    Toast.makeText(context, if (ok) "导入成功" else "文件格式无效", Toast.LENGTH_SHORT).show()
                }) { Text("覆盖导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), fontSize = 15.sp)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("  ›", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
