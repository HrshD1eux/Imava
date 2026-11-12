package com.hrshd1eux.imava.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
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

    private fun resolveSystemLocale(): Locale {
        val sysConfig = Resources.getSystem()?.configuration ?: return Locale.getDefault()
        return ConfigurationCompat.getLocales(sysConfig).get(0) ?: Locale.getDefault()
    }

    fun resolveTargetLocale(languageTag: String): Locale {
        return if (languageTag.isBlank()) resolveSystemLocale() else Locale.forLanguageTag(languageTag)
    }

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

        val targetLocale = resolveTargetLocale(languageTag)
        Locale.setDefault(targetLocale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (languageTag.isBlank()) {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageTag)
            }
        } else {
            val config = context.resources.configuration
            config.setLocale(targetLocale)
            config.setLayoutDirection(targetLocale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    fun wrapContext(context: Context): Context {
        val lang = getSavedLanguageTag(context)
        val locale = resolveTargetLocale(lang)
        Locale.setDefault(locale)
        if (lang.isBlank()) return context

        val baseConfig = context.resources?.configuration ?: return context
        val config = Configuration(baseConfig)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config) ?: context
    }

    /** Returns a Context bound to the active app locale for resolving localized strings inside long-lived ViewModels. */
    fun localizedContext(context: Context): Context {
        val lang = getSavedLanguageTag(context)
        val locale = resolveTargetLocale(lang)
        Locale.setDefault(locale)
        val baseConfig = context.resources?.configuration ?: return context
        val config = Configuration(baseConfig)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config) ?: context
    }

    /**
     * Resolves a localized string under the active app locale, falling back to [fallback] if the
     * supplied Context has incomplete resources (e.g. JVM unit-test mocks).
     */
    fun getString(context: Context?, resId: Int, fallback: String = "", vararg formatArgs: Any): String {
        if (context == null) return fallback
        return try {
            val locCtx = try {
                localizedContext(context)
            } catch (_: Exception) {
                context
            }
            val resolved = if (formatArgs.isEmpty()) {
                locCtx.getString(resId)
            } else {
                locCtx.getString(resId, *formatArgs)
            }
            resolved.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
