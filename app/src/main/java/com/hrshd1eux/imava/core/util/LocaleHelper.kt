package com.hrshd1eux.imava.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    data class SupportedLanguage(
        val code: String,
        val displayName: String,
        val nativeName: String
    )

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("", "System Default", "System Default"),
        SupportedLanguage("en", "English", "English"),
        SupportedLanguage("es", "Spanish", "Español"),
        SupportedLanguage("pt-BR", "Portuguese (Brazil)", "Português (Brasil)"),
        SupportedLanguage("de", "German", "Deutsch"),
        SupportedLanguage("ru", "Russian", "Русский"),
        SupportedLanguage("fr", "French", "Français"),
        SupportedLanguage("hi", "Hindi", "हिन्दी"),
        SupportedLanguage("bn", "Bengali", "বাংলা"),
        SupportedLanguage("mr", "Marathi", "मराठी"),
        SupportedLanguage("te", "Telugu", "తెలుగు"),
        SupportedLanguage("ta", "Tamil", "தமிழ்"),
        SupportedLanguage("gu", "Gujarati", "ગુજરાતી")
    )

    fun getSavedLanguageTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val locales = localeManager?.applicationLocales
                if (locales != null && !locales.isEmpty) {
                    val tag = locales.get(0).toLanguageTag()
                    val matched = SUPPORTED_LANGUAGES.firstOrNull {
                        it.code.equals(tag, ignoreCase = true) ||
                        (it.code.isNotEmpty() && tag.startsWith(it.code, ignoreCase = true))
                    }
                    if (matched != null) return matched.code
                }
            } catch (_: Exception) {}
        }
        val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
        return prefs.getString("app_language", "") ?: ""
    }

    fun setLocale(context: Context, languageTag: String) {
        val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", languageTag).apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (languageTag.isBlank()) {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageTag)
            }
        } else {
            val locale = if (languageTag.isBlank()) {
                Locale.getDefault()
            } else {
                Locale.forLanguageTag(languageTag)
            }
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    fun wrapContext(context: Context): Context {
        val lang = getSavedLanguageTag(context)
        if (lang.isBlank()) return context

        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
