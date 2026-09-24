package com.personal.ledger.ui.transaction

import com.personal.ledger.util.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountEntryTest {
    @Test fun `accepts trailing dot and at most two decimals`() {
        assertEquals("0.", AmountEntry.press("", "."))
        assertEquals("12.", AmountEntry.press("12", "."))
        assertEquals("12.34", AmountEntry.press("12.34", "5"))
        assertEquals("12.3", AmountEntry.press("12.34", AmountEntry.BACKSPACE))
        assertEquals("12", AmountEntry.forSaving("12."))
        assertEquals("", AmountEntry.press("12.34", AmountEntry.CLEAR))
    }

    @Test fun `old grouped amount remains completely editable`() {
        val old = Money.formatMinor(Long.MAX_VALUE)
        assertEquals("92233720368547758.07", AmountEntry.fromStored(old))
        assertEquals("92233720368547758.0", AmountEntry.press(old, AmountEntry.BACKSPACE))
        assertEquals(Long.MAX_VALUE, Money.parseToMinor(AmountEntry.forSaving(old)))
    }

    @Test fun `rejects minor unit overflow`() {
        assertEquals("92233720368547758.0", AmountEntry.press("92233720368547758.0", "8"))
        assertEquals("92233720368547758.07", AmountEntry.press("92233720368547758.0", "7"))
    }
}
