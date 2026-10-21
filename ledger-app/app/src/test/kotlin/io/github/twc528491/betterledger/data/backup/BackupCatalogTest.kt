package io.github.twc528491.betterledger.data.backup

import io.github.twc528491.betterledger.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class BackupCatalogTest {

    private val utc: ZoneId = ZoneOffset.UTC

    private fun entry(name: String, modified: Long = 0L, size: Long = 100L) =
        BackupEntry(name = name, sizeBytes = size, modifiedAtMs = modified, location = "file:///backups/$name")

    @Test
    fun `按文件名区分备份与恢复前备份`() {
        assertEquals(BackupFileKind.BACKUP, BackupCatalog.classify("ledger-backup-20261002-143000.json"))
        assertEquals(BackupFileKind.PRE_RESTORE, BackupCatalog.classify("pre-restore-ledger-backup-20261002-143000.json"))
        // 手动导出的 JSON 同样以 ledger-backup- 开头，列表中显示为备份
        assertEquals(BackupFileKind.BACKUP, BackupCatalog.classify("ledger-backup-2026-10.json"))
        assertEquals(BackupFileKind.OTHER, BackupCatalog.classify("ledger-export-2026-10.csv"))
        assertEquals(BackupFileKind.OTHER, BackupCatalog.classify("notes.json"))
        assertEquals(BackupFileKind.OTHER, BackupCatalog.classify("ledger-backup-20261002-143000.json.tmp"))
    }

    @Test
    fun `恢复前备份文件名带前缀且时间可解析`() {
        val t = LocalDateTime.of(2026, 10, 2, 14, 30, 5)
        val name = BackupCatalog.preRestoreFileName(t)
        assertEquals("pre-restore-ledger-backup-20261002-143005.json", name)
        assertEquals(t.atZone(utc).toInstant().toEpochMilli(), BackupCatalog.backupTimeMs(entry(name), utc))
    }

    @Test
    fun `备份时间优先取文件名并兼容重名后缀`() {
        val t = LocalDateTime.of(2026, 9, 1, 8, 0, 0).atZone(utc).toInstant().toEpochMilli()
        assertEquals(t, BackupCatalog.backupTimeMs(entry("ledger-backup-20260901-080000.json", modified = 5L), utc))
        assertEquals(t, BackupCatalog.backupTimeMs(entry("ledger-backup-20260901-080000 (1).json", modified = 5L), utc))
        // 无法解析时回退到修改时间
        assertEquals(42L, BackupCatalog.backupTimeMs(entry("ledger-backup-2026-09.json", modified = 42L), utc))
    }

    @Test
    fun `列表过滤无关文件并从新到旧排序`() {
        val entries = listOf(
            entry("ledger-backup-20260901-080000.json"),
            entry("random.txt"),
            entry("pre-restore-ledger-backup-20261001-090000.json"),
            entry("ledger-backup-20260915-200000.json"),
        )
        val sorted = BackupCatalog.visibleNewestFirst(entries, utc).map { it.name }
        assertEquals(
            listOf(
                "pre-restore-ledger-backup-20261001-090000.json",
                "ledger-backup-20260915-200000.json",
                "ledger-backup-20260901-080000.json",
            ),
            sorted,
        )
    }

    @Test
    fun `轮换只考虑严格命名的自动或立即备份`() {
        val today = LocalDate.of(2026, 10, 2)
        val backups = (0L..40L).map { d ->
            BackupRotation.fileName(today.minusDays(d).atTime(9, 0))
        }
        val protected = listOf(
            "pre-restore-" + BackupRotation.fileName(today.minusDays(30).atTime(9, 0)),
            "ledger-backup-2026-08.json",
            "ledger-backup-20260801-090000 (1).json",
            "my-notes.json",
        )
        val candidates = BackupCatalog.rotationCandidates((backups + protected).map { entry(it) })
        assertEquals(backups.toSet(), candidates.toSet())

        val deleted = BackupRotation.selectFilesToDelete(candidates, today)
        assertTrue(deleted.isNotEmpty())
        protected.forEach { assertFalse("$it 不应被轮换删除", it in deleted) }
    }

    @Test
    fun `文件大小与时间显示`() {
        assertEquals("856 B", BackupCatalog.formatSizeOrNull(856))
        assertEquals("12.0 KB", BackupCatalog.formatSizeOrNull(12 * 1024))
        assertEquals("1.5 MB", BackupCatalog.formatSizeOrNull(1536L * 1024))
        assertNull(BackupCatalog.formatSizeOrNull(-1)) // 显示为“未知大小”（资源）
        val t = LocalDateTime.of(2026, 10, 2, 14, 30).atZone(utc).toInstant().toEpochMilli()
        assertEquals("2026-10-02 14:30", BackupCatalog.formatTimeOrNull(t, utc))
        assertNull(BackupCatalog.formatTimeOrNull(0L, utc)) // 显示为“时间未知”（资源）
    }

    @Test
    fun `类型标签`() {
        assertEquals(R.string.data_backup_kind_backup, BackupCatalog.typeLabelRes(BackupFileKind.BACKUP))
        assertEquals(R.string.data_backup_kind_pre_restore, BackupCatalog.typeLabelRes(BackupFileKind.PRE_RESTORE))
    }

    @Test
    fun `自定义文件夹位置描述`() {
        val internal = { path: String ->
            BackupCatalog.TreeLocation.Named(R.string.data_location_internal, R.string.data_location_internal_path, path)
        }
        val selected = BackupCatalog.TreeLocation.Named(
            R.string.data_location_selected_folder, R.string.data_location_selected_folder, "",
        )
        assertEquals(internal("Documents/账本"), BackupCatalog.treeLocation("primary:Documents/账本"))
        assertEquals(internal(""), BackupCatalog.treeLocation("primary:"))
        assertEquals(
            BackupCatalog.TreeLocation.Named(R.string.data_location_sd_card, R.string.data_location_sd_card_path, "Backup"),
            BackupCatalog.treeLocation("1A2B-3C4D:Backup"),
        )
        assertEquals(selected, BackupCatalog.treeLocation(null))
        assertEquals(selected, BackupCatalog.treeLocation("msf:1234"))
        assertEquals(
            BackupCatalog.TreeLocation.Raw("/storage/emulated/0/Download/x"),
            BackupCatalog.treeLocation("raw:/storage/emulated/0/Download/x"),
        )
    }
}
