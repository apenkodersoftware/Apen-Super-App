package com.aistudio.fasmovapos.app.ui.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aistudio.fasmovapos.app.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Which look the app uses: follow the phone, or force light or dark. */
enum class ThemeMode(
  @StringRes val labelRes: Int,
) {
  SYSTEM(R.string.theme_system),
  LIGHT(R.string.theme_light),
  DARK(R.string.theme_dark),
}

private val Context.settingsDataStore by preferencesDataStore(name = "fasmovapos_settings")

/** Remembers the user's [ThemeMode] choice across launches. */
class ThemePreferences(
  private val context: Context,
) {
  private val modeKey = stringPreferencesKey("theme_mode")

  val themeMode: Flow<ThemeMode> =
    context.settingsDataStore.data
      .catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
      }.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[modeKey] } ?: ThemeMode.SYSTEM
      }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.settingsDataStore.edit { prefs -> prefs[modeKey] = mode.name }
  }
}
