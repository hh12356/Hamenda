package io.github.hh12356.hamenda.util

import io.github.hh12356.hamenda.data.AppSettings
import io.github.hh12356.hamenda.data.CourseBlock
import io.github.hh12356.hamenda.data.SCHOOL_PERIODS
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun dayName(iso: Int): String = DAY_NAMES[(iso - 1).coerceIn(0, 6)]

fun minuteLabel(min: Int): String = "%d:%02d".format(min / 60, min % 60)

fun snap15(min: Int): Int = ((min + 7) / 15) * 15

fun daysOf(settings: AppSettings): List<Int> =
    if (settings.fiveDayMode) listOf(1, 2, 3, 4, 5)
    else if (settings.weekStartMonday) listOf(1, 2, 3, 4, 5, 6, 7)
    else listOf(7, 1, 2, 3, 4, 5, 6)

fun startOfWeek(date: LocalDate, mondayStart: Boolean): LocalDate {
    val day = date.dayOfWeek.value
    return if (mondayStart) date.minusDays((day - 1).toLong())
    else date.minusDays((day % 7).toLong())
}

fun weekStartDate(settings: AppSettings, week: Int): LocalDate {
    val anchor = if (settings.semesterStart.isNotBlank()) {
        runCatching { LocalDate.parse(settings.semesterStart) }.getOrNull()
    } else null
    val base = anchor ?: startOfWeek(LocalDate.now(), settings.weekStartMonday)
    return base.plusDays(7L * (week - 1))
}

fun dateFor(settings: AppSettings, week: Int, iso: Int): LocalDate {
    val startIso = if (settings.weekStartMonday) 1 else 7
    val offset = (iso - startIso + 7) % 7
    return weekStartDate(settings, week).plusDays(offset.toLong())
}

fun currentWeek(settings: AppSettings, today: LocalDate = LocalDate.now()): Int {
    if (settings.semesterStart.isBlank()) return 1
    val start = runCatching { LocalDate.parse(settings.semesterStart) }.getOrNull() ?: return 1
    val days = ChronoUnit.DAYS.between(start, today)
    return maxOf(1, (days / 7 + 1).toInt())
}

fun weekRangeLabel(block: CourseBlock): String {
    val base = if (block.startWeek == block.endWeek) "第 ${block.startWeek} 周"
    else "${block.startWeek}–${block.endWeek} 周"
    return when (block.parity) {
        1 -> "$base（单周）"
        2 -> "$base（双周）"
        else -> base
    }
}

fun periodsInRange(startMin: Int, endMin: Int): List<Int> =
    SCHOOL_PERIODS.filter { it.startMin >= startMin && it.endMin <= endMin }.map { it.index }

fun parseTime(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
