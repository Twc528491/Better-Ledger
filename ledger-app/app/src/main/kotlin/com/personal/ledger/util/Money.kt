package com.personal.ledger.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * 金额工具。金额一律以最小货币单位整数（Long）存储与计算，仅展示时转小数。
 */
object Money {

    /** 最小货币单位 → 带千分位的小数文本，如 2536 → "25.36"。 */
    fun formatMinor(minor: Long): String {
        val v = BigDecimal.valueOf(minor, 2)
        return String.format(Locale.CHINA, "%,.2f", v)
    }

    /** 带符号金额文本，如 -2536 → "-25.36"，正数可选显式 "+"。
     *  使用 BigDecimal 取绝对值，规避 Long.MIN_VALUE 的 abs 溢出。 */
    fun formatSignedMinor(minor: Long, explicitPlus: Boolean = false): String {
        val text = String.format(Locale.CHINA, "%,.2f", BigDecimal.valueOf(minor, 2).abs())
        return when {
            minor > 0 && explicitPlus -> "+$text"
            minor > 0 -> text
            else -> "-$text"
        }
    }

    /** 完整展示：币种符号 + 带符号金额，如 "¥-1,980.00"。 */
    fun formatWithSymbol(minor: Long, symbol: String = "¥"): String =
        "$symbol${formatSignedMinor(minor)}"

    /**
     * 用户输入的十进制金额文本 → 最小货币单位整数。
     * 最多两位小数，四舍五入到分；非法输入返回 null。
     *
     * @param allowNegative 允许负号（账户初始余额可为负，表示负债）；
     *                      交易金额始终为正，不传该参数。
     */
    fun parseToMinor(text: String, allowNegative: Boolean = false): Long? {
        val t = text.trim().replace(",", "")
        if (t.isEmpty()) return null
        val sign = if (allowNegative) "-?" else ""
        if (!t.matches(Regex("""$sign\d+(\.\d{1,2})?"""))) return null
        return try {
            BigDecimal(t).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }
}
