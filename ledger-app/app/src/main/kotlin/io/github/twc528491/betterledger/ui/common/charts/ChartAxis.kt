package io.github.twc528491.betterledger.ui.common.charts

import io.github.twc528491.betterledger.util.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * 图表坐标轴的纯计算（不依赖 Compose，可在 JVM 单测中验证）。
 * 金额一律为最小货币单位（分）。
 */
object ChartAxis {

    private val TEN_THOUSAND = BigDecimal(10_000)
    private val HUNDRED_MILLION = BigDecimal(100_000_000)
    private val THOUSAND = BigDecimal(1_000)
    private val MILLION = BigDecimal(1_000_000)
    private val BILLION = BigDecimal(1_000_000_000)

    /** 超过该金额（100 万元）时读数改用紧凑写法，避免在窄屏上被截断。 */
    private const val READOUT_COMPACT_FROM = 100_000_000L

    /** 让刻度落在 1 / 2 / 5 × 10^k 上的步长，使最多 [targetSteps] 格即可覆盖 [maxValue]。 */
    fun niceStep(maxValue: Long, targetSteps: Int = 3): Long {
        if (maxValue <= 0L) return 1L
        val steps = targetSteps.coerceAtLeast(1)
        val raw = (maxValue + steps - 1) / steps
        var magnitude = 1L
        while (magnitude <= raw / 10) magnitude *= 10
        for (factor in longArrayOf(1, 2, 5, 10)) {
            if (factor * magnitude >= raw) return factor * magnitude
        }
        return 10 * magnitude
    }

    /** 从 0 开始的整齐刻度，最后一格不小于 [maxValue]；无数据时只有 0。 */
    fun ticks(maxValue: Long, targetSteps: Int = 3): List<Long> {
        if (maxValue <= 0L) return listOf(0L)
        val step = niceStep(maxValue, targetSteps)
        val count = (maxValue + step - 1) / step
        return (0..count).map { it * step }
    }

    /**
     * 紧凑轴标签，不带币种符号。
     * 中文：8000 / 2.5 / 1.2万 / 3亿；英文：800 / 2.5 / 8K / 12.3K / 1.2M / 3B。
     */
    fun compactLabel(minor: Long, locale: Locale = Locale.getDefault()): String {
        val sign = if (minor < 0) "-" else ""
        val yuan = BigDecimal.valueOf(minor, 2).abs()
        val body = if (locale.language == Locale.CHINESE.language) chineseCompact(yuan) else englishCompact(yuan)
        return sign + body
    }

    private fun chineseCompact(yuan: BigDecimal): String = when {
        yuan >= HUNDRED_MILLION -> trimmed(yuan.divide(HUNDRED_MILLION), 1) + "亿"
        yuan >= TEN_THOUSAND -> {
            val wan = yuan.divide(TEN_THOUSAND).setScale(1, RoundingMode.HALF_UP)
            // 9999.95 万这类边界四舍五入后已到 1 亿，改用亿表示。
            if (wan >= TEN_THOUSAND) trimmed(yuan.divide(HUNDRED_MILLION), 1) + "亿"
            else trimmed(wan, 1) + "万"
        }
        else -> trimmed(yuan, 2)
    }

    /** 英文按千进位：K / M / B；四舍五入后进位到下一级（999.95K → 1M）。 */
    private fun englishCompact(yuan: BigDecimal): String {
        if (yuan < THOUSAND) return trimmed(yuan, 2)
        val units = listOf(THOUSAND to "K", MILLION to "M", BILLION to "B")
        for ((index, unit) in units.withIndex()) {
            val scaled = yuan.divide(unit.first).setScale(1, RoundingMode.HALF_UP)
            if (scaled < THOUSAND || index == units.lastIndex) return trimmed(scaled, 1) + unit.second
        }
        return trimmed(yuan, 2)
    }

    /**
     * 图表读数金额：符号在前（-¥25.00，与全局 signedMoney 一致），
     * 一般显示完整金额，超过 100 万时改用「¥123.4万」/「¥1.2M」避免在窄屏上截断。
     */
    fun readoutMoney(
        minor: Long,
        currency: String,
        explicitPlus: Boolean = false,
        locale: Locale = Locale.getDefault(),
    ): String {
        val sign = when {
            minor < 0 -> "-"
            minor > 0 && explicitPlus -> "+"
            else -> ""
        }
        val compact = minor >= READOUT_COMPACT_FROM || minor <= -READOUT_COMPACT_FROM
        val body = if (compact) compactLabel(minor, locale) else Money.formatSignedMinor(minor)
        return sign + Money.symbolFor(currency) + body.removePrefix("-")
    }

    private fun trimmed(value: BigDecimal, scale: Int): String =
        value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
