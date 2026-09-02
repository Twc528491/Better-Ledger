package com.personal.ledger.data.backup

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

/**
 * 自动备份文件命名与轮换策略（纯函数，可 JVM 测试）。
 *
 * 保留策略（MVP.md 第 10 节）：7 个每日 + 4 个每周 + 12 个每月。
 * 文件名：ledger-backup-yyyyMMdd-HHmmss.json
 */
object BackupRotation {

    private val namePattern = Regex("""^ledger-backup-(\d{8})-(\d{6})\.json$""")
    private val timePattern = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    /** 生成备份文件名（按当前时间）。 */
    fun fileName(time: LocalDateTime): String =
        BackupConstants.FILE_PREFIX + timePattern.format(time) + ".json"

    /** 解析文件名中的时间；无法解析返回 null。 */
    fun parseTime(name: String): LocalDateTime? =
        namePattern.matchEntire(name)?.let { m ->
            runCatching {
                LocalDateTime.parse(m.groupValues[1] + "-" + m.groupValues[2], timePattern)
            }.getOrNull()
        }

    private data class Entry(val name: String, val time: LocalDateTime, val date: LocalDate)

    private fun isoWeek(date: LocalDate): Pair<Int, Int> {
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return year to week
    }

    /**
     * 从现有备份文件名中选出应删除的旧备份。
     *
     * 规则：
     * - 每日：最近 keepDaily 个不同日期，各保留该日期最新一个；
     * - 每周：更早的日期中，最近 keepWeekly 个自然周（ISO 周），各保留该周最新一个；
     * - 每月：再更早的日期中，最近 keepMonthly 个月，各保留该月最新一个；
     * - 其余全部删除；无法解析的文件名不参与（保留）。
     */
    fun selectFilesToDelete(
        names: List<String>,
        today: LocalDate,
        keepDaily: Int = 7,
        keepWeekly: Int = 4,
        keepMonthly: Int = 12,
    ): List<String> {
        val entries = names.mapNotNull { n ->
            parseTime(n)?.let { Entry(n, it, it.toLocalDate()) }
        }
            .filter { it.date <= today } // 忽略未来时间（时钟偏差）
            .sortedByDescending { it.time }
        if (entries.isEmpty()) return emptyList()

        val kept = HashSet<String>()

        fun keepNewest(group: List<Entry>) {
            group.maxByOrNull { it.time }?.let { kept.add(it.name) }
        }

        // 每日
        val dailyGroups = entries.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
            .values.take(keepDaily)
        dailyGroups.forEach { keepNewest(it) }

        // 每周
        val afterDaily = entries.filter { it.name !in kept }
        val weekComparator = compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second }
        val weeklyGroups = afterDaily.groupBy { isoWeek(it.date) }
            .toSortedMap(weekComparator)
            .values.take(keepWeekly)
        weeklyGroups.forEach { keepNewest(it) }

        // 每月
        val afterWeekly = entries.filter { it.name !in kept }
        val monthlyGroups = afterWeekly.groupBy { YearMonth.from(it.date) }
            .toSortedMap(compareByDescending { it })
            .values.take(keepMonthly)
        monthlyGroups.forEach { keepNewest(it) }

        return entries.map { it.name }.filterNot { it in kept }
    }
}
