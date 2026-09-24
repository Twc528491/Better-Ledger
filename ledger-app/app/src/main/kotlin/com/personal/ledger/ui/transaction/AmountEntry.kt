package com.personal.ledger.ui.transaction

import com.personal.ledger.util.Money

/** Decimal draft shared by the custom keypad and old transactions. */
internal object AmountEntry {
    const val BACKSPACE = "⌫"
    const val CLEAR = "清空"

    fun fromStored(formatted: String): String = formatted.replace(",", "")

    fun press(current: String, key: String): String {
        val draft = fromStored(current)
        if (key == CLEAR) return ""
        if (key == BACKSPACE) return draft.dropLast(1)
        if (key != "." && (key.length != 1 || key[0] !in '0'..'9')) return draft
        if (key == ".") {
            if ('.' in draft) return draft
            return if (draft.isEmpty()) "0." else draft + "."
        }
        val candidate = when (draft) {
            "", "0" -> key
            else -> draft + key
        }
        val fraction = candidate.substringAfter('.', "")
        if ('.' in candidate && fraction.length > 2) return draft
        // A trailing dot is a valid editing state; Money validates complete values.
        val complete = if (candidate.endsWith('.')) candidate.dropLast(1) else candidate
        return if (Money.parseToMinor(complete) != null) candidate else draft
    }

    fun forSaving(draft: String): String = fromStored(draft).removeSuffix(".")
}
