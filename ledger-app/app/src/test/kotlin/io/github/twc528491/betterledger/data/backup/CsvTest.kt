package io.github.twc528491.betterledger.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun `普通字段不转义`() {
        assertEquals("a,b,c", Csv.row(listOf("a", "b", "c")))
    }

    @Test
    fun `含逗号的字段加引号`() {
        assertEquals("\"a,b\",c", Csv.row(listOf("a,b", "c")))
    }

    @Test
    fun `含引号的字段翻倍并加引号`() {
        assertEquals("\"a\"\"b\"", Csv.row(listOf("a\"b")))
    }

    @Test
    fun `含换行与中文正常处理`() {
        assertEquals("\"多\n行\",备注", Csv.row(listOf("多\n行", "备注")))
    }
}
