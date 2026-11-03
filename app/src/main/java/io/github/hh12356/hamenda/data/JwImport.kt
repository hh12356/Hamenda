package io.github.hh12356.hamenda.data

import org.json.JSONObject
import java.time.LocalDate

/** 武大统一身份认证入口，登录后回到教务首页。 */
const val JW_LOGIN_URL =
    "https://cas.whu.edu.cn/authserver/login" +
        "?service=https%3A%2F%2Fjwgl.whu.edu.cn%2Fsso%2Fjznewsixlogin"

/**
 * 按开学日期推算正方的 学年(xnm) 和 学期(xqm：3=第一学期，12=第二学期)。
 */
fun zfTerm(semesterStart: String): Pair<String, String> {
    val d = runCatching { LocalDate.parse(semesterStart) }.getOrNull() ?: LocalDate.now()
    // 8 月以后算本学年第一学期；1 月仍在上一学年的第一学期里。
    val firstTerm = d.monthValue >= 8 || d.monthValue == 1
    val year = if (d.monthValue >= 8) d.year else d.year - 1
    return year.toString() to if (firstTerm) "3" else "12"
}

/**
 * 解析 /kbcx/xskbcx_cxXsgrkb.html 返回的 JSON。
 * 结构不对（登录过期、系统改版）返回 null。
 */
fun parseZfTimetable(text: String): List<CourseBlock>? = try {
    val list = JSONObject(text).optJSONArray("kbList") ?: return null
    (0 until list.length()).mapNotNull { i ->
        val o = list.getJSONObject(i)
        // jcs="1-2" 节次（老版本叫 jc），zcd="1-16周(单)" 周次，xqj 星期 1-7。
        val bounds = Regex("\\d+").findAll(o.optString("jcs").ifEmpty { o.optString("jc") })
            .map { it.value.toInt() }.toList()
        val weeks = parseWeeks(o.optString("zcd"))
        val day = o.optInt("xqj")
        if (day !in 1..7 || bounds.isEmpty() || weeks.isEmpty()) return@mapNotNull null
        val from = bounds.first().coerceIn(1, SCHOOL_PERIODS.size)
        val to = bounds.last().coerceIn(from, SCHOOL_PERIODS.size)
        CourseBlock(
            id = System.nanoTime() + i,
            name = o.optString("kcmc"),
            teacher = o.optString("xm"),
            location = o.optString("cdmc"),
            dayOfWeek = day,
            startMin = SCHOOL_PERIODS[from - 1].startMin,
            endMin = SCHOOL_PERIODS[to - 1].endMin,
            color = DEFAULT_COLORS[i % DEFAULT_COLORS.size]
        ).withWeeks(weeks)
    }
} catch (e: Exception) {
    null
}

/** "1-16周(单)" / "1-4周,6-18周(双)" / "5周" → 具体周次。 */
private fun parseWeeks(zcd: String): Set<Int> =
    zcd.split(',', '，', '、').flatMap { part ->
        val m = Regex("(\\d+)(?:-(\\d+))?").find(part) ?: return@flatMap emptyList()
        val lo = m.groupValues[1].toInt()
        val hi = m.groupValues[2].ifBlank { m.groupValues[1] }.toInt()
        (lo..hi).filter {
            when {
                part.contains("单") -> it % 2 == 1
                part.contains("双") -> it % 2 == 0
                else -> true
            }
        }
    }.toSet()
