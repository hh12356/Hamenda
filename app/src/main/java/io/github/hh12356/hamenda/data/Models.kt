package io.github.hh12356.hamenda.data

import kotlinx.serialization.Serializable

@Serializable
data class CourseBlock(
    val id: Long = 0L,
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int = 1,        // 1=Mon .. 7=Sun
    val startMin: Int = 480,
    val endMin: Int = 525,
    val startWeek: Int = 1,
    val endWeek: Int = 25,
    val parity: Int = 0,           // 0=every, 1=odd, 2=even
    val color: Int = DEFAULT_COLORS[0],
    val skippedWeeks: List<Int> = emptyList()  // 只删掉某几周的那一节
) {
    val duration: Int get() = endMin - startMin

    /** 按具体周次集合重算 起始周/结束周/单双周/跳过的周。 */
    fun withWeeks(weeks: Collection<Int>): CourseBlock {
        val sorted = weeks.sorted()
        if (sorted.isEmpty()) return this
        val lo = sorted.first()
        val hi = sorted.last()
        val missing = (lo..hi).filterNot { it in weeks }
        val parity = when {
            missing.isEmpty() -> 0
            sorted.all { it % 2 == 1 } -> 1
            sorted.all { it % 2 == 0 } -> 2
            else -> 0
        }
        return copy(
            startWeek = lo,
            endWeek = hi,
            parity = parity,
            skippedWeeks = if (parity == 0) missing else emptyList()
        )
    }

    fun activeInWeek(week: Int): Boolean =
        week in startWeek..endWeek && week !in skippedWeeks && when (parity) {
            1 -> week % 2 == 1
            2 -> week % 2 == 0
            else -> true
        }
}

@Serializable
data class AppSettings(
    val semesterStart: String = "",   // ISO date, first day of week 1
    val weekStartMonday: Boolean = false,
    val fiveDayMode: Boolean = false,
    val dayStartMin: Int = 480,
    val dayEndMin: Int = 1320,
    val showWeekArrows: Boolean = true,
    val showWeekRange: Boolean = false,
    val themeMode: Int = 0,           // 0=system, 1=light, 2=dark
    val leftLabelMode: Int = 0        // 0=hour, 1=period index, 2=period times
)

@Serializable
data class AppData(
    val settings: AppSettings = AppSettings(),
    val blocks: List<CourseBlock> = emptyList()
)

data class Period(val index: Int, val startMin: Int, val endMin: Int)

/** 学校作息表，v1 不可编辑。 */
val SCHOOL_PERIODS: List<Period> = listOf(
    Period(1, 480, 525),    // 8:00-8:45
    Period(2, 530, 575),    // 8:50-9:35
    Period(3, 590, 635),    // 9:50-10:35
    Period(4, 640, 685),    // 10:40-11:25
    Period(5, 690, 735),    // 11:30-12:15
    Period(6, 845, 890),    // 14:05-14:50
    Period(7, 895, 940),    // 14:55-15:40
    Period(8, 945, 990),    // 15:45-16:30
    Period(9, 1000, 1045),  // 16:40-17:25
    Period(10, 1050, 1095), // 17:30-18:15
    Period(11, 1110, 1155), // 18:30-19:15
    Period(12, 1160, 1205), // 19:20-20:05
    Period(13, 1210, 1255)  // 20:10-20:55
)

/** Ham 风格的 19 色浅色卡片色板。 */
val DEFAULT_COLORS: List<Int> = listOf(
    0xFFF6C9C9.toInt(), 0xFFD9CCF2.toInt(), 0xFFE3D4F5.toInt(),
    0xFFD5D9F2.toInt(), 0xFFC9DCF5.toInt(), 0xFFCCE4F7.toInt(),
    0xFFD2E9F7.toInt(), 0xFFCFF0EE.toInt(), 0xFFD3EFD9.toInt(),
    0xFFE8F3CE.toInt(), 0xFFF0F5C8.toInt(), 0xFFF7F2C4.toInt(),
    0xFFFAE9C9.toInt(), 0xFFF9E3BE.toInt(), 0xFFF7D9B8.toInt(),
    0xFFF6CFC4.toInt(), 0xFFE8DCD8.toInt(), 0xFFDDE3EA.toInt(),
    0xFFE6E1EE.toInt()
)
