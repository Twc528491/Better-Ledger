package com.personal.ledger.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BackupRotationTest {

    private fun nameOf(date: LocalDate, hour: Int): String =
        BackupRotation.fileName(LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, 0))

    @Test
    fun `文件名解析与生成往返`() {
        val t = LocalDateTime.of(2026, 9, 2, 13, 45, 0)
        assertEquals(t, BackupRotation.parseTime(BackupRotation.fileName(t)))
        assertNull(BackupRotation.parseTime("random.json"))
        assertNull(BackupRotation.parseTime("ledger-backup-20260902-1345.json"))
    }

    @Test
    fun `最近 7 天各保留最新一个`() {
        val today = LocalDate.of(2026, 9, 10)
        val names = (0L..9L).flatMap { d ->
            listOf(nameOf(today.minusDays(d), 8), nameOf(today.minusDays(d), 20))
        }
        val deleted = BackupRotation.selectFilesToDelete(names, today)
        val kept = names.filterNot { it in deleted }

        // 每日窗口：最近 7 天各保留最新（20 点）一个
        (0L..6L).forEach { d ->
            assertTrue("${today.minusDays(d)} 的 20 点备份应保留", nameOf(today.minusDays(d), 20) in kept)
        }
        // 周/月窗口回退：总量 = 7 每日 + 2 每周 + 1 每月（10 天数据全部落在同一月、跨两个 ISO 周）
        assertEquals(10, kept.size)
        assertEquals(10, deleted.size)
        // 最旧一天的 8 点备份应删除
        assertTrue(nameOf(today.minusDays(9), 8) in deleted)
    }

    @Test
    fun `60 天历史按 7日+4周+1月 轮换`() {
        val today = LocalDate.of(2026, 9, 30)
        val names = (0L..59L).map { d -> nameOf(today.minusDays(d), 9) }
        val deleted = BackupRotation.selectFilesToDelete(names, today)
        val kept = names.filterNot { it in deleted }
        // 60 个文件：7 每日 + 4 每周 + 覆盖剩余月份的最多月保留（此处 60 天跨 2-3 个月 → 每月窗口 1 个）
        assertTrue("应删除相当数量的旧备份，实际删除 ${deleted.size}", deleted.size >= 40)
        assertTrue("保留数量应远小于总数，实际保留 ${kept.size}", kept.size <= 15)
        // 最新一天必然保留
        assertTrue(names.first() in kept)
        // 最旧一天必然删除
        assertTrue(names.last() in deleted)
    }

    @Test
    fun `无法解析的文件名不删除`() {
        val today = LocalDate.of(2026, 9, 10)
        val names = listOf("backup-99999999-000000.json", "readme.txt")
        assertEquals(emptyList<String>(), BackupRotation.selectFilesToDelete(names, today))
    }

    @Test
    fun `未来时间的文件不删除`() {
        val today = LocalDate.of(2026, 9, 10)
        val names = listOf(nameOf(today.plusDays(1), 9))
        assertEquals(emptyList<String>(), BackupRotation.selectFilesToDelete(names, today))
    }
}
