package io.github.twc528491.betterledger.data.backup

/** CSV 字段转义（RFC 4180 简化）：含逗号/引号/换行时加引号，内部引号翻倍。 */
object Csv {
    fun escape(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuote) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }

    fun row(values: List<String>): String = values.joinToString(",") { escape(it) }
}
