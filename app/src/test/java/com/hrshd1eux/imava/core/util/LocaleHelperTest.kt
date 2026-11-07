package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocaleHelperTest {

    @Test
    fun testSupportedLanguagesCount() {
        // 1 System default + 1 English + 11 targeted languages = 13
        assertEquals(13, LocaleHelper.SUPPORTED_LANGUAGES.size)
    }

    @Test
    fun testAllRequiredLanguagesArePresent() {
        val codes = LocaleHelper.SUPPORTED_LANGUAGES.map { it.code }

        val expectedCodes = listOf(
            "",      // System Default
            "en",    // English
            "es",    // Spanish
            "pt-BR", // Portuguese (Brazil)
            "de",    // German
            "ru",    // Russian
            "fr",    // French
            "hi",    // Hindi
            "bn",    // Bengali
            "mr",    // Marathi
            "te",    // Telugu
            "ta",    // Tamil
            "gu"     // Gujarati
        )

        for (expected in expectedCodes) {
            assertTrue("Expected language code '$expected' to be present", codes.contains(expected))
        }
    }

    @Test
    fun testSupportedLanguagesProperties() {
        for (lang in LocaleHelper.SUPPORTED_LANGUAGES) {
            assertTrue("Display name should not be blank for code: ${lang.code}", lang.displayName.isNotBlank())
            assertTrue("Native name should not be blank for code: ${lang.code}", lang.nativeName.isNotBlank())

            if (lang.code.isNotEmpty()) {
                val parsedLocale = Locale.forLanguageTag(lang.code)
                assertNotNull("Parsed locale should not be null for ${lang.code}", parsedLocale)
                assertTrue("Language should not be empty for ${lang.code}", parsedLocale.language.isNotEmpty())
            }
        }
    }

    @Test
    fun testNativeNamesCorrectness() {
        val langMap = LocaleHelper.SUPPORTED_LANGUAGES.associateBy { it.code }

        assertEquals("Español", langMap["es"]?.nativeName)
        assertEquals("Português (Brasil)", langMap["pt-BR"]?.nativeName)
        assertEquals("Deutsch", langMap["de"]?.nativeName)
        assertEquals("Русский", langMap["ru"]?.nativeName)
        assertEquals("Français", langMap["fr"]?.nativeName)
        assertEquals("हिन्दी", langMap["hi"]?.nativeName)
        assertEquals("বাংলা", langMap["bn"]?.nativeName)
        assertEquals("मराठी", langMap["mr"]?.nativeName)
        assertEquals("తెలుగు", langMap["te"]?.nativeName)
        assertEquals("தமிழ்", langMap["ta"]?.nativeName)
        assertEquals("ગુજરાતી", langMap["gu"]?.nativeName)
    }
}
