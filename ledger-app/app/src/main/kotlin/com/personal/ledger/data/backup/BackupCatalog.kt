package com.personal.ledger.data.backup

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/** 备份目录中的文件类别。 */
enum class BackupFileKind {
    /** 自动 / 立即备份（ledger-backup-*.json），参与轮换。 */
    BACKUP,

    /** 恢复前自动生成的安全备份（pre-restore-*.json），永不参与轮换。 */
    PRE_RESTORE,

    /** 其他文件：不在备份列表中显示，也不会被删除。 */
    OTHER,
}

/**
 * 备份存储中的一个文件。[location] 为 content:// 或 file:// URI 字符串，
 * 保持纯 Kotlin 类型以便 JVM 测试。
 */
data class BackupEntry(
    val name: String,
    val sizeBytes: Long,
    /** 文件修改时间（毫秒）；未知时为 0。 */
    val modifiedAtMs: Long,
    val location: String,
) {
    val kind: BackupFileKind get() = BackupCatalog.classify(name)
}

/** 备份文件分类、排序与显示文案（纯函数，可 JVM 测试）。 */
object BackupCatalog {

    const val PRE_RESTORE_PREFIX = "pre-restore-"

    fun classify(name: String): BackupFileKind = when {
        !name.endsWith(".json", ignoreCase = true) -> BackupFileKind.OTHER
        name.startsWith(PRE_RESTORE_PREFIX) -> BackupFileKind.PRE_RESTORE
        name.startsWith(BackupConstants.FILE_PREFIX) -> BackupFileKind.BACKUP
        else -> BackupFileKind.OTHER
    }

    /** 恢复前备份的文件名：pre-restore-ledger-backup-yyyyMMdd-HHmmss.json。 */
    fun preRestoreFileName(time: LocalDateTime): String =
        PRE_RESTORE_PREFIX + BackupRotation.fileName(time)

    /** 优先使用文件名中的时间（不受复制 / 同步改动影响），否则回退到文件修改时间。 */
    fun backupTimeMs(entry: BackupEntry, zone: ZoneId = ZoneId.systemDefault()): Long {
        val base = entry.name.removePrefix(PRE_RESTORE_PREFIX)
        val parsed = BackupRotation.parseTime(base)
            // MediaStore 重名时会改为 “xxx (1).json”，去掉后缀再解析一次
            ?: BackupRotation.parseTime(base.replace(Regex("""\s*\(\d+\)(?=\.json$)"""), ""))
        return parsed?.atZone(zone)?.toInstant()?.toEpochMilli() ?: entry.modifiedAtMs
    }

    /** 只保留可展示的备份文件（BACKUP / PRE_RESTORE），按备份时间从新到旧排序。 */
    fun visibleNewestFirst(
        entries: List<BackupEntry>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<BackupEntry> =
        entries.filter { it.kind != BackupFileKind.OTHER }
            .sortedWith(
                compareByDescending<BackupEntry> { backupTimeMs(it, zone) }
                    .thenByDescending { it.name },
            )

    /** 参与轮换的文件名：只有严格命名的 ledger-backup-*.json，恢复前备份与其他文件永远不会被选中。 */
    fun rotationCandidates(entries: List<BackupEntry>): List<String> =
        entries.map { it.name }
            .filter { classify(it) == BackupFileKind.BACKUP && BackupRotation.parseTime(it) != null }

    fun typeLabel(kind: BackupFileKind): String = when (kind) {
        BackupFileKind.BACKUP -> "备份"
        BackupFileKind.PRE_RESTORE -> "恢复前备份"
        BackupFileKind.OTHER -> "其他文件"
    }

    /** 备份时间显示，如 “2026-10-02 14:30”；0 表示未知。 */
    fun formatTime(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        if (epochMs <= 0L) {
            "时间未知"
        } else {
            DISPLAY_TIME.format(java.time.Instant.ofEpochMilli(epochMs).atZone(zone))
        }

    private val DISPLAY_TIME = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 人类可读的文件大小，如 “856 B”“12.4 KB”“1.2 MB”。 */
    fun formatSize(bytes: Long): String = when {
        bytes < 0 -> "未知大小"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /**
     * 把 SAF 树的 documentId（如 “primary:Documents/账本”）转换为易读的位置文字。
     * 无法识别时返回 “已选择的文件夹”。
     */
    fun describeTreeDocumentId(documentId: String?): String {
        if (documentId.isNullOrBlank()) return "已选择的文件夹"
        val volume = documentId.substringBefore(':', missingDelimiterValue = "")
        val path = documentId.substringAfter(':', missingDelimiterValue = documentId).trim('/')
        return when {
            volume == "raw" -> documentId.substringAfter(':').ifEmpty { "已选择的文件夹" }
            volume == "primary" -> if (path.isEmpty()) "内部存储" else "内部存储/$path"
            volume == "home" -> if (path.isEmpty()) "文档" else "文档/$path"
            volume == "downloads" -> "下载"
            volume.isNotEmpty() && volume.matches(Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")) ->
                if (path.isEmpty()) "存储卡" else "存储卡/$path"
            else -> "已选择的文件夹"
        }
    }
}
