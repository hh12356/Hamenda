package io.github.hh12356.hamenda.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.hh12356.hamenda.BuildConfig
import io.github.hh12356.hamenda.data.CourseBlock
import io.github.hh12356.hamenda.data.Store
import io.github.hh12356.hamenda.util.minuteLabel
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(store: Store, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val settings = store.data.settings
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var pendingCourses by remember { mutableStateOf<List<CourseBlock>?>(null) }
    var clearStep by remember { mutableIntStateOf(0) }
    var showJw by remember { mutableStateOf(false) }

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
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { ChevronIcon(left = true) }
            Text(
                "设置",
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard("学期") {
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
            }

            SettingsCard("显示") {
                LabeledChips("一周起始日") {
                    ToggleChip("周日", !settings.weekStartMonday) {
                        store.updateSettings { it.copy(weekStartMonday = false) }
                    }
                    ToggleChip("周一", settings.weekStartMonday) {
                        store.updateSettings { it.copy(weekStartMonday = true) }
                    }
                }
                CardDivider()
                LabeledChips("显示天数") {
                    ToggleChip("7 天", !settings.fiveDayMode) {
                        store.updateSettings { it.copy(fiveDayMode = false) }
                    }
                    ToggleChip("5 天（周一至周五）", settings.fiveDayMode) {
                        store.updateSettings { it.copy(fiveDayMode = true) }
                    }
                }
                CardDivider()
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
                CardDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("显示左右箭头", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.showWeekArrows,
                        onCheckedChange = { v -> store.updateSettings { it.copy(showWeekArrows = v) } }
                    )
                }
                CardDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("显示周日期范围", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.showWeekRange,
                        onCheckedChange = { v -> store.updateSettings { it.copy(showWeekRange = v) } }
                    )
                }
            }

            SettingsCard("外观") {
                LabeledChips("主题") {
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

            SettingsCard("从教务系统导入") {
                OutlinedButton(
                    onClick = { showJw = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("登录教务系统") }
                Text(
                    "登录后自动取本学期课表并追加到现有课表；密码只在教务页面里输入，应用不保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard("备份") {
                Button(
                    onClick = { exportLauncher.launch("hamenda-backup.json") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("导出备份") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("导入备份") }
                Text(
                    "导入会覆盖当前所有课程和设置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { clearStep = 1 },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("清空所有课程") }
            }

            Text(
                "Hamenda ${BuildConfig.VERSION_NAME} · 数据保存在本机，登录教务时才联网",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showJw) {
        JwImportDialog(
            semesterStart = settings.semesterStart,
            onDismiss = { showJw = false },
            onResult = { blocks ->
                showJw = false
                pendingCourses = blocks
            }
        )
    }

    // 清空课程要连点两次确认，避免误触。
    if (clearStep > 0) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(if (clearStep == 1) "清空所有课程" else "再确认一次") },
            text = {
                Text(
                    if (clearStep == 1) "当前共 ${store.data.blocks.size} 门课程，清空后无法恢复，建议先导出备份。"
                    else "确定删除全部 ${store.data.blocks.size} 门课程吗？此操作不可撤销。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (clearStep == 1) {
                        clearStep = 2
                    } else {
                        store.clearBlocks()
                        clearStep = 0
                        Toast.makeText(context, "已清空课程", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(
                        if (clearStep == 1) "继续" else "清空",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { clearStep = 0 }) { Text("取消") }
            }
        )
    }

    pendingCourses?.let { blocks ->
        AlertDialog(
            onDismissRequest = { pendingCourses = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("导入课程") },
            text = { Text("解析到 ${blocks.size} 门课程，会追加到现有课表。") },
            confirmButton = {
                TextButton(onClick = {
                    store.addBlocks(blocks)
                    pendingCourses = null
                    Toast.makeText(context, "已导入 ${blocks.size} 门课程", Toast.LENGTH_SHORT).show()
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCourses = null }) { Text("取消") }
            }
        )
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("导入备份") },
            text = { Text("导入会覆盖当前所有课程和设置，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = store.importJson(text)
                    pendingImport = null
                    Toast.makeText(context, if (ok) "导入成功" else "文件格式无效", Toast.LENGTH_SHORT).show()
                }) { Text("覆盖导入", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabeledChips(label: String, content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        ChevronIcon(left = false, tint = MaterialTheme.colorScheme.outline)
    }
}
