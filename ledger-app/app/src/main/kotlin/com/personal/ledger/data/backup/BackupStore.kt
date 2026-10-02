package com.personal.ledger.data.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.personal.ledger.R
import com.personal.ledger.util.LocaleContext
import com.personal.ledger.util.UserFacingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/** 备份存储位置的种类。 */
enum class BackupStoreKind {
    /** API 29+：公共 Documents/LedgerBackups（MediaStore，卸载后保留）。 */
    DEFAULT_SHARED,

    /** API 26–28：应用专属外部目录（卸载时随应用删除）。 */
    DEFAULT_APP_FILES,

    /** 用户通过 SAF 选择的文件夹。 */
    CUSTOM_FOLDER,

    /** 应用内部目录（恢复前备份的兜底位置，用户不可见）。 */
    INTERNAL,
}

/** 备份存储抽象：列出 / 写入 / 读取 / 删除 / 描述。所有 IO 在 [Dispatchers.IO] 执行。 */
interface BackupStore {
    val kind: BackupStoreKind

    /** 用于界面显示的位置说明。 */
    fun describe(): String

    /** 当前是否可写（SAF 授权丢失时为 false）。 */
    suspend fun isAvailable(): Boolean

    /** 列出目录中的所有文件（未过滤、未排序）。 */
    suspend fun list(): List<BackupEntry>

    /** 新建文件并写入内容；失败抛出异常。 */
    suspend fun write(name: String, bytes: ByteArray): BackupEntry

    /** 打开读取流；调用方负责关闭。 */
    suspend fun open(entry: BackupEntry): InputStream

    suspend fun delete(entry: BackupEntry): Boolean
}

/** 普通文件目录（应用专属外部目录、filesDir 下的恢复前备份目录）。 */
class FileBackupStore(
    private val dir: File,
    override val kind: BackupStoreKind,
    /** 位置说明（每次调用时解析，跟随当前应用语言）。 */
    private val label: () -> String,
) : BackupStore {

    override fun describe(): String = label()

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        (dir.isDirectory || dir.mkdirs()) && dir.canWrite()
    }

    override suspend fun list(): List<BackupEntry> = withContext(Dispatchers.IO) {
        dir.listFiles()?.filter { it.isFile }?.map { it.toEntry() }.orEmpty()
    }

    override suspend fun write(name: String, bytes: ByteArray): BackupEntry = withContext(Dispatchers.IO) {
        if (!dir.isDirectory && !dir.mkdirs()) throw UserFacingException(R.string.err_create_backup_dir)
        val target = File(dir, name)
        // 先写临时文件再改名，避免半截文件被当作备份
        val temp = File(dir, "$name.tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            temp.delete()
            throw UserFacingException(R.string.err_write_backup_file)
        }
        target.toEntry()
    }

    override suspend fun open(entry: BackupEntry): InputStream = withContext(Dispatchers.IO) {
        fileOf(entry).inputStream()
    }

    override suspend fun delete(entry: BackupEntry): Boolean = withContext(Dispatchers.IO) {
        fileOf(entry).delete()
    }

    private fun fileOf(entry: BackupEntry): File {
        val file = File(Uri.parse(entry.location).path ?: throw UserFacingException(R.string.err_invalid_file_location))
        // 只允许访问本目录内的文件
        if (file.parentFile?.canonicalPath != dir.canonicalPath) throw UserFacingException(R.string.err_file_outside_backup_dir)
        return file
    }

    private fun File.toEntry() = BackupEntry(
        name = name,
        sizeBytes = length(),
        modifiedAtMs = lastModified(),
        location = Uri.fromFile(this).toString(),
    )
}

/**
 * API 29+ 默认目录：通过 MediaStore 写入公共 Documents/LedgerBackups/。
 * - 不需要任何存储权限；应用自己创建的文件可直接列出与删除。
 * - 文件在卸载后保留，可在文件管理器中看到；但重装后应用不再“拥有”旧文件，
 *   MediaStore 不会再返回它们（仍可通过“从文件恢复”选择器手动恢复）。
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaStoreBackupStore(private val context: Context) : BackupStore {

    override val kind = BackupStoreKind.DEFAULT_SHARED

    override fun describe(): String =
        LocaleContext.getString(context, R.string.data_location_internal_path, RELATIVE_PATH.trimEnd('/'))

    private val collection: Uri get() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    override suspend fun isAvailable(): Boolean =
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    override suspend fun list(): List<BackupEntry> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        // 部分 ROM 的 RELATIVE_PATH 末尾斜杠不一致，用 LIKE 前缀匹配后再精确过滤
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(RELATIVE_PATH.trimEnd('/') + "%")
        val result = mutableListOf<BackupEntry>()
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            while (c.moveToNext()) {
                val path = c.getString(pathCol).orEmpty().trimEnd('/')
                if (!path.equals(RELATIVE_PATH.trimEnd('/'), ignoreCase = true)) continue
                val name = c.getString(nameCol) ?: continue
                val uri = android.content.ContentUris.withAppendedId(collection, c.getLong(idCol))
                val size = if (c.isNull(sizeCol)) -1L else c.getLong(sizeCol)
                result += BackupEntry(
                    name = name,
                    sizeBytes = if (size > 0) size else statSize(uri),
                    modifiedAtMs = c.getLong(dateCol) * 1000L,
                    location = uri.toString(),
                )
            }
        }
        result
    }

    override suspend fun write(name: String, bytes: ByteArray): BackupEntry = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, BackupConstants.MIME_JSON)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw UserFacingException(R.string.err_create_default_backup)
        try {
            resolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                ?: throw UserFacingException(R.string.err_write_backup_file)
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
        // MediaStore 可能因重名自动改名，读回实际文件名
        val actualName = resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: name
        BackupEntry(actualName, bytes.size.toLong(), System.currentTimeMillis(), uri.toString())
    }

    override suspend fun open(entry: BackupEntry): InputStream = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(Uri.parse(entry.location))
            ?: throw UserFacingException(R.string.err_backup_read)
    }

    override suspend fun delete(entry: BackupEntry): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.delete(Uri.parse(entry.location), null, null) > 0 }
            .getOrDefault(false)
    }

    private fun statSize(uri: Uri): Long = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
    }.getOrDefault(-1L)

    companion object {
        /** ASCII 路径，避免个别 ROM 对中文目录名的兼容问题。 */
        const val RELATIVE_PATH = "Documents/LedgerBackups/"
    }
}

/** 用户选择的 SAF 文件夹（沿用旧版的持久化授权）。 */
class SafBackupStore(
    private val context: Context,
    val treeUri: Uri,
) : BackupStore {

    override val kind = BackupStoreKind.CUSTOM_FOLDER

    override fun describe(): String = BackupCatalog.describeTreeDocumentId(
        runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull(),
        LocaleContext.of(context),
    )

    /** 持久化授权仍在且目录存在、可写。 */
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val granted = context.contentResolver.persistedUriPermissions.any {
                it.uri == treeUri && it.isWritePermission && it.isReadPermission
            }
            val dir = DocumentFile.fromTreeUri(context, treeUri)
            granted && dir != null && dir.exists() && dir.canWrite()
        }.getOrDefault(false)
    }

    private fun dir(): DocumentFile =
        DocumentFile.fromTreeUri(context, treeUri) ?: throw UserFacingException(R.string.err_access_backup_folder)

    override suspend fun list(): List<BackupEntry> = withContext(Dispatchers.IO) {
        dir().listFiles().filter { it.isFile }.mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null
            BackupEntry(name, file.length(), file.lastModified(), file.uri.toString())
        }
    }

    override suspend fun write(name: String, bytes: ByteArray): BackupEntry = withContext(Dispatchers.IO) {
        val file = dir().createFile(BackupConstants.MIME_JSON, name)
            ?: throw UserFacingException(R.string.err_create_in_backup_folder)
        try {
            context.contentResolver.openOutputStream(file.uri, "w")?.use { it.write(bytes) }
                ?: throw UserFacingException(R.string.err_write_backup_file)
        } catch (e: Exception) {
            runCatching { file.delete() }
            throw e
        }
        BackupEntry(file.name ?: name, bytes.size.toLong(), System.currentTimeMillis(), file.uri.toString())
    }

    override suspend fun open(entry: BackupEntry): InputStream = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(Uri.parse(entry.location))
            ?: throw UserFacingException(R.string.err_backup_read)
    }

    override suspend fun delete(entry: BackupEntry): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            DocumentFile.fromSingleUri(context, Uri.parse(entry.location))?.delete() == true
        }.getOrDefault(false)
    }

    /** 释放持久化授权（恢复为默认目录时调用）。 */
    fun releasePermission() {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }
}

/** 当前生效的备份位置及其状态。 */
data class BackupLocationState(
    val store: BackupStore,
    /** 是否为用户自定义文件夹。 */
    val isCustom: Boolean,
    /** 自定义文件夹的授权丢失或目录不可用。 */
    val customUnavailable: Boolean,
)

/** 根据设置解析备份位置。 */
class BackupStores(private val context: Context) {

    /** 开箱即用的默认目录。 */
    fun defaultStore(): BackupStore =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStoreBackupStore(context)
        } else {
            val external = context.getExternalFilesDir("backups")
            if (external != null) {
                FileBackupStore(external, BackupStoreKind.DEFAULT_APP_FILES) { label(R.string.data_store_app_external) }
            } else {
                FileBackupStore(File(context.filesDir, "backups"), BackupStoreKind.DEFAULT_APP_FILES) {
                    label(R.string.data_store_app_internal)
                }
            }
        }

    /** 恢复前备份的兜底目录（filesDir/pre-restore-backups）。 */
    fun internalPreRestoreStore(): BackupStore =
        FileBackupStore(File(context.filesDir, "pre-restore-backups"), BackupStoreKind.INTERNAL) {
            label(R.string.data_store_pre_restore)
        }

    private fun label(id: Int): String = LocaleContext.getString(context, id)

    fun customStore(treeUri: String): SafBackupStore = SafBackupStore(context, Uri.parse(treeUri))

    /** 解析当前位置：设置了自定义文件夹则使用它（即使不可用也如实返回，由调用方决定回退）。 */
    suspend fun resolve(customTreeUri: String?): BackupLocationState {
        if (customTreeUri == null) return BackupLocationState(defaultStore(), isCustom = false, customUnavailable = false)
        val custom = customStore(customTreeUri)
        return BackupLocationState(custom, isCustom = true, customUnavailable = !custom.isAvailable())
    }
}
