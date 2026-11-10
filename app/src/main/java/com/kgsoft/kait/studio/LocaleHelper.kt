package com.kgsoft.kait.studio

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * In-app language switching (Arabic / English / Simplified Chinese / System).
 * The choice is stored in SharedPreferences and applied with
 * AppCompatDelegate, so it survives restarts and switches instantly.
 */
object LocaleHelper {

    const val LANG_SYSTEM = "system"
    const val LANG_ARABIC = "ar"
    const val LANG_ENGLISH = "en"
    const val LANG_CHINESE = "zh"

    private const val PREFS = "kait_prefs"
    private const val KEY_LANGUAGE = "language"

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setLanguage(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, code).apply()
        applyLanguage(code)
    }

    fun applySavedLanguage(context: Context) {
        applyLanguage(getLanguage(context))
    }

    fun applyLanguage(code: String) {
        val locales = when (code) {
            LANG_ARABIC -> LocaleListCompat.create(Locale("ar"))
            LANG_ENGLISH -> LocaleListCompat.create(Locale("en"))
            LANG_CHINESE -> LocaleListCompat.create(Locale.forLanguageTag("zh-Hans"))
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun getDisplayName(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("display_name", "KG") ?: "KG"
    }

    fun setDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("display_name", name).apply()
    }
}
