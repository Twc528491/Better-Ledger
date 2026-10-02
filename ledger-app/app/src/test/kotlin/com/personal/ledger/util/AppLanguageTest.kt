package com.personal.ledger.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun fromTag_mapsRegionalVariantsToSupportedOptions() {
        assertEquals(AppLanguage.Option.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.Option.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.Option.CHINESE, AppLanguage.fromTag("zh-CN"))
        assertEquals(AppLanguage.Option.CHINESE, AppLanguage.fromTag("zh-Hans-CN"))
        assertEquals(AppLanguage.Option.ENGLISH, AppLanguage.fromTag("en-GB"))
        assertEquals(AppLanguage.Option.SYSTEM, AppLanguage.fromTag("fr-FR"))
    }
}
