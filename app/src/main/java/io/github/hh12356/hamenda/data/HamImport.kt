package io.github.hh12356.hamenda.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import java.io.File

/** Ham 的数据库路径；别人的私有目录普通应用读不到，只有 root 能拷。 */
private const val HAM_DB = "/data/data/com.nowcent.ham/databases/ham.db"

/**
 * 直接去手机里找已安装 Ham 的课表，找不到（没 root）返回 null，由界面退回手动选文件。
 */
fun parseInstalledHam(context: Context): List<CourseBlock>? {
    val cache = File(context.cacheDir, "ham-import.db")
    fun cleanup() = listOf("", "-wal", "-shm").forEach { File(cache.path + it).delete() }
    cleanup()
    // Magisk 之类会把 su 放进 PATH；没进 PATH 的设备再试几个绝对路径。
    val su = listOf("su", "/system/bin/su", "/sbin/su", "/system/xbin/su").firstOrNull { exe ->
        runCatching {
            ProcessBuilder(exe, "-c", "cp $HAM_DB ${cache.absolutePath}").start().waitFor() == 0 &&
                cache.length() > 0
        }.getOrDefault(false)
    }
    val copied = su != null
    if (copied) {
        // 带上 WAL，Ham 还在跑时最新数据可能还没写进主库。
        runCatching {
            ProcessBuilder(su, "-c", "cp $HAM_DB-wal ${cache.absolutePath}-wal").start().waitFor()
        }
    }
    val blocks = if (copied) parseHamDb(cache.absolutePath) else null
    cleanup()
    return blocks
}

/**
 * 读 Ham（com.nowcent.ham）的 ham.db，转成课程块。
 * 不是 SQLite 或者没有对应的表就返回 null。
 */
// shortcut: 只读主库文件，拉取时 Ham 还在跑的话，WAL 里尚未检查点的改动可能缺失。
fun parseHamDb(path: String): List<CourseBlock>? = try {
    SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        // course.grid 按周展开，正好能还原单双周和中间缺的周。
        val grid = mutableMapOf<Int, MutableSet<Int>>()
        db.rawQuery("SELECT course_table_id, week FROM CourseGrid", null).use { c ->
            while (c.moveToNext()) grid.getOrPut(c.getInt(0)) { mutableSetOf() }.add(c.getInt(1))
        }
        val blocks = mutableListOf<CourseBlock>()
        db.rawQuery(
            "SELECT id, name, instructor, location, weekday, week_from, week_to, class_from, class_to, color FROM course",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val from = c.getInt(7).coerceIn(1, SCHOOL_PERIODS.size)
                val to = c.getInt(8).coerceIn(from, SCHOOL_PERIODS.size)
                val weeks = (grid[c.getInt(0)] ?: emptySet<Int>()).toSortedSet()
                if (weeks.isEmpty()) weeks += c.getInt(5)..c.getInt(6)
                if (weeks.isEmpty()) continue
                val lo = weeks.first()
                val hi = weeks.last()
                val missing = (lo..hi).filterNot { it in weeks }
                val parity = when {
                    missing.isEmpty() -> 0
                    weeks.all { it % 2 == 1 } -> 1
                    weeks.all { it % 2 == 0 } -> 2
                    else -> 0
                }
                blocks += CourseBlock(
                    id = System.nanoTime() + blocks.size,
                    name = c.getString(1),
                    teacher = c.getString(2),
                    location = c.getString(3),
                    dayOfWeek = c.getInt(4).let { if (it == 0) 7 else it }.coerceIn(1, 7),
                    startMin = SCHOOL_PERIODS[from - 1].startMin,
                    endMin = SCHOOL_PERIODS[to - 1].endMin,
                    startWeek = lo,
                    endWeek = hi,
                    parity = parity,
                    skippedWeeks = if (parity == 0) missing else emptyList(),
                    color = runCatching { Color.parseColor(c.getString(9)) }
                        .getOrDefault(DEFAULT_COLORS[blocks.size % DEFAULT_COLORS.size])
                )
            }
        }
        blocks
    }
} catch (e: Exception) {
    null
}
