package io.github.twc528491.betterledger.data.backup

import android.content.Context
import androidx.annotation.StringRes
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.AppStrings
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

/** 备份文件分类、排序与显示文案（除按语言解析的文案外均为纯函数，可 JVM 测试）。 */
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


    /** 文件类别的显示名资源。 */
    @StringRes
    fun typeLabelRes(kind: BackupFileKind): Int = when (kind) {
        BackupFileKind.BACKUP -> R.string.data_backup_kind_backup
        BackupFileKind.PRE_RESTORE -> R.string.data_backup_kind_pre_restore
        BackupFileKind.OTHER -> R.string.data_backup_kind_other
    }

    /** 文件类别的显示名（当前应用语言）。Compose 中可用 stringResource([typeLabelRes])。 */
    fun typeLabel(kind: BackupFileKind): String = AppStrings.get(typeLabelRes(kind))

    /** 备份时间显示，如 “2026-10-02 14:30”；0 表示未知，返回 null。 */
    fun formatTimeOrNull(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String? =
        if (epochMs <= 0L) null else DISPLAY_TIME.format(java.time.Instant.ofEpochMilli(epochMs).atZone(zone))

    /** 备份时间显示；未知时显示“时间未知”（当前应用语言）。 */
    fun formatTime(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        formatTimeOrNull(epochMs, zone) ?: AppStrings.get(R.string.data_backup_time_unknown)

    private val DISPLAY_TIME = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 人类可读的文件大小，如 “856 B”“12.4 KB”“1.2 MB”；未知（负数）返回 null。 */
    fun formatSizeOrNull(bytes: Long): String? = when {
        bytes < 0 -> null
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /** 人类可读的文件大小；未知时显示“未知大小”（当前应用语言）。 */
    fun formatSize(bytes: Long): String =
        formatSizeOrNull(bytes) ?: AppStrings.get(R.string.data_backup_size_unknown)

    /** SAF 树位置：资源化的卷名（可带子路径），或无法翻译的原始路径。 */
    sealed interface TreeLocation {
        /** [res] 为不带路径的卷名，[pathRes] 为带 “%1$s” 子路径的格式。 */
        data class Named(@StringRes val res: Int, @StringRes val pathRes: Int, val path: String) : TreeLocation
        data class Raw(val path: String) : TreeLocation
    }

    /** 解析 SAF 树的 documentId（如 “primary:Documents/账本”）；无法识别时为“已选择的文件夹”。 */
    fun treeLocation(documentId: String?): TreeLocation {
        val selected = TreeLocation.Named(R.string.data_location_selected_folder, R.string.data_location_selected_folder, "")
        if (documentId.isNullOrBlank()) return selected
        val volume = documentId.substringBefore(':', missingDelimiterValue = "")
        val path = documentId.substringAfter(':', missingDelimiterValue = documentId).trim('/')
        return when {
            volume == "raw" -> documentId.substringAfter(':').takeIf { it.isNotEmpty() }
                ?.let { TreeLocation.Raw(it) } ?: selected
            volume == "primary" ->
                TreeLocation.Named(R.string.data_location_internal, R.string.data_location_internal_path, path)
            volume == "home" ->
                TreeLocation.Named(R.string.data_location_documents, R.string.data_location_documents_path, path)
            volume == "downloads" ->
                TreeLocation.Named(R.string.data_location_downloads, R.string.data_location_downloads, "")
            volume.isNotEmpty() && volume.matches(Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")) ->
                TreeLocation.Named(R.string.data_location_sd_card, R.string.data_location_sd_card_path, path)
            else -> selected
        }
    }

    /**
     * 把 SAF 树的 documentId 转换为易读的位置文字（如 “内部存储/Documents/账本”）。
     * [context] 为 null 时按当前应用语言解析。
     */
    fun describeTreeDocumentId(documentId: String?, context: Context? = null): String {
        val resolve: (Int, Array<out Any>) -> String = { id, args ->
            if (context != null) context.getString(id, *args) else AppStrings.get(id, *args)
        }
        return when (val location = treeLocation(documentId)) {
            is TreeLocation.Raw -> location.path
            is TreeLocation.Named ->
                if (location.path.isEmpty()) resolve(location.res, emptyArray())
                else resolve(location.pathRes, arrayOf(location.path))
        }
    }
}
