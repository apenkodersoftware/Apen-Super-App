package com.aistudio.fasmovapos.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ProfessionalPrimaryDark,
    onPrimary = ProfessionalOnPrimaryDark,
    primaryContainer = ProfessionalPrimaryContainerDark,
    onPrimaryContainer = ProfessionalOnPrimaryContainerDark,
    secondary = ProfessionalSecondaryDark,
    onSecondary = ProfessionalOnSecondaryDark,
    secondaryContainer = ProfessionalSecondaryContainerDark,
    onSecondaryContainer = ProfessionalOnSecondaryContainerDark,
    background = ProfessionalBackgroundDark,
    onBackground = ProfessionalOnBackgroundDark,
    surface = ProfessionalSurfaceDark,
    onSurface = ProfessionalOnSurfaceDark,
    surfaceVariant = ProfessionalSurfaceVariantDark,
    onSurfaceVariant = ProfessionalOnSurfaceVariantDark,
    outline = ProfessionalOutline,
    outlineVariant = ProfessionalOutlineVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ProfessionalPrimary,
    onPrimary = ProfessionalOnPrimary,
    primaryContainer = ProfessionalPrimaryContainer,
    onPrimaryContainer = ProfessionalOnPrimaryContainer,
    secondary = ProfessionalSecondary,
    onSecondary = ProfessionalOnSecondary,
    secondaryContainer = ProfessionalSecondaryContainer,
    onSecondaryContainer = ProfessionalOnSecondaryContainer,
    background = ProfessionalBackground,
    onBackground = ProfessionalOnBackground,
    surface = ProfessionalSurface,
    onSurface = ProfessionalOnSurface,
    surfaceVariant = ProfessionalSurfaceVariant,
    onSurfaceVariant = ProfessionalOnSurfaceVariant,
    outline = ProfessionalOutline,
    outlineVariant = ProfessionalOutlineVariant,
  )

@Composable
fun FasMovaPOSTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use intentional FasMova POS brand palette
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
    )
  }
}
