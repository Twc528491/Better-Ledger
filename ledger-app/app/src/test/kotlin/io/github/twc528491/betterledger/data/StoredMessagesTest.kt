package io.github.twc528491.betterledger.data

import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.util.UserFacingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredMessagesTest {

    @Test
    fun `登记过的无参数资源化异常存为稳定代码`() {
        val stored = StoredMessages.encode(
            UserFacingException(R.string.err_recurring_account_unavailable),
            StoredMessage.RECURRING_GENERATE_FAILED,
        )
        assertEquals("@msg:RECURRING_ACCOUNT_UNAVAILABLE", stored)
        assertTrue(StoredMessages.isEncoded(stored))
    }

    @Test
    fun `未登记或带参数的资源化异常回退为兜底代码`() {
        assertEquals(
            "@msg:RECURRING_GENERATE_FAILED",
            StoredMessages.encode(UserFacingException(R.string.err_db_version, 9), StoredMessage.RECURRING_GENERATE_FAILED),
        )
        assertEquals(
            "@msg:RECURRING_GENERATE_FAILED",
            StoredMessages.encode(IllegalStateException(), StoredMessage.RECURRING_GENERATE_FAILED),
        )
    }

    @Test
    fun `系统异常保留原文`() {
        val stored = StoredMessages.encode(IllegalStateException("disk I/O error"), StoredMessage.BACKUP_UNKNOWN_ERROR)
        assertEquals("disk I/O error", stored)
        assertFalse(StoredMessages.isEncoded(stored))
    }

    @Test
    fun `带详情的编码`() {
        val detail = StoredMessages.encode(StoredMessage.WRITE_BACKUP_FILE)
        assertEquals(
            "@msg:BACKUP_FAILED\u001F@msg:WRITE_BACKUP_FILE",
            StoredMessages.encode(StoredMessage.BACKUP_FAILED, detail),
        )
    }

    @Test
    fun `旧版中文原文不是编码消息`() {
        assertFalse(StoredMessages.isEncoded("上次备份失败：未知错误"))
    }

    @Test
    fun `代码与资源一一对应`() {
        val codes = StoredMessage.entries
        assertEquals(codes.size, codes.map { it.res }.distinct().size)
        codes.forEach { assertEquals(it, StoredMessage.fromCode(it.name)) }
    }
}
