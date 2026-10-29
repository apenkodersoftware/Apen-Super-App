package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.ui.theme.LocalStatusColors

/** Readable defaults for the simple interface; the complete interface keeps its existing theme. */
@Composable
fun SimpleModeTheme(content: @Composable () -> Unit) {
  val base = MaterialTheme.typography
  val body = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp)
  val label = base.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp)
  val current = MaterialTheme.colorScheme
  val dark = current.background.luminance() < 0.5f
  val blue = if (dark) Color(0xFFA9C7FF) else Color(0xFF1455B6)
  val onBlue = if (dark) Color(0xFF003061) else Color.White
  val container = if (dark) Color(0xFF164477) else Color(0xFFD9E8FF)
  val onContainer = if (dark) Color(0xFFD9E8FF) else Color(0xFF082C59)
  val stockColors =
    LocalStatusColors.current.copy(
      warning = if (dark) Color(0xFFFDBA74) else Color(0xFF92400E),
      danger = if (dark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
    )
  CompositionLocalProvider(LocalStatusColors provides stockColors) {
    MaterialTheme(
      colorScheme =
        current.copy(
          primary = blue,
          onPrimary = onBlue,
          primaryContainer = container,
          onPrimaryContainer = onContainer,
          secondary = blue,
          onSecondary = onBlue,
          secondaryContainer = container,
          onSecondaryContainer = onContainer,
          tertiary = blue,
          onTertiary = onBlue,
          tertiaryContainer = container,
          onTertiaryContainer = onContainer,
        ),
      typography =
        base.copy(
          bodyLarge = body,
          bodyMedium = body,
          bodySmall = body,
          labelLarge = label,
          labelMedium = label,
          labelSmall = label,
          titleSmall = base.titleSmall.copy(fontSize = 18.sp, lineHeight = 26.sp),
        ),
      content = content,
    )
  }
}
