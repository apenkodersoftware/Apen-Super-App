package com.aistudio.fasmovapos.app.ui.language

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import com.aistudio.fasmovapos.app.R
import java.util.Locale

/** The language the app shows: follow the phone, or force English or Kiswahili. */
enum class AppLanguage(
  val tag: String?,
) {
  SYSTEM(null),
  ENGLISH("en"),
  SWAHILI("sw"),
}

/**
 * Stores the chosen [AppLanguage] and applies it to a [Context].
 *
 * Kept in plain SharedPreferences (not DataStore) because it has to be read synchronously
 * in `Activity.attachBaseContext`, before any UI exists.
 */
object LanguageSettings {
  private const val PREFS = "fasmovapos_language"
  private const val KEY = "language"

  fun get(context: Context): AppLanguage {
    val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
    return AppLanguage.entries.firstOrNull { it.name == saved } ?: AppLanguage.SYSTEM
  }

  fun set(
    context: Context,
    language: AppLanguage,
  ) {
    context
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY, language.name)
      .apply()
  }

  /** [base] with the saved language applied, or [base] itself when following the system. */
  fun localize(base: Context): Context {
    val tag = get(base).tag ?: return base
    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    return base.createConfigurationContext(config)
  }
}

/** Name of the language in the string resources (each language is shown in its own name). */
@StringRes
fun AppLanguage.labelRes(): Int =
  when (this) {
    AppLanguage.SYSTEM -> R.string.theme_system
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.SWAHILI -> R.string.language_swahili
  }
