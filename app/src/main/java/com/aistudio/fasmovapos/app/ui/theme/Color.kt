package com.aistudio.fasmovapos.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Professional Polish Theme Colors
val ProfessionalPrimary = Color(0xFF0061A4)
val ProfessionalOnPrimary = Color(0xFFFFFFFF)
val ProfessionalPrimaryContainer = Color(0xFFD1E4FF)
val ProfessionalOnPrimaryContainer = Color(0xFF001D35)

val ProfessionalSecondary = Color(0xFF535F70)
val ProfessionalOnSecondary = Color(0xFFFFFFFF)
val ProfessionalSecondaryContainer = Color(0xFFF0F4F8)
val ProfessionalOnSecondaryContainer = Color(0xFF101C2B)

val ProfessionalTertiary = Color(0xFF6B5778)
val ProfessionalTertiaryContainer = Color(0xFFF2DAFF)
val ProfessionalOnTertiaryContainer = Color(0xFF251431)

val ProfessionalBackground = Color(0xFFFDFBFF)
val ProfessionalOnBackground = Color(0xFF1A1C1E)

val ProfessionalSurface = Color(0xFFFDFBFF)
val ProfessionalOnSurface = Color(0xFF1A1C1E)

val ProfessionalSurfaceVariant = Color(0xFFE1E2EC)
val ProfessionalOnSurfaceVariant = Color(0xFF43474E)

val ProfessionalOutline = Color(0xFF74777F)
val ProfessionalOutlineVariant = Color(0xFFC4C6CF)

// Dark theme variants
val ProfessionalPrimaryDark = Color(0xFF9ECAFF)
val ProfessionalOnPrimaryDark = Color(0xFF003258)
val ProfessionalPrimaryContainerDark = Color(0xFF00497D)
val ProfessionalOnPrimaryContainerDark = Color(0xFFD1E4FF)

val ProfessionalSecondaryDark = Color(0xFFBBC7DB)
val ProfessionalOnSecondaryDark = Color(0xFF253140)
val ProfessionalSecondaryContainerDark = Color(0xFF3B4857)
val ProfessionalOnSecondaryContainerDark = Color(0xFFD7E3F7)

val ProfessionalBackgroundDark = Color(0xFF1A1C1E)
val ProfessionalOnBackgroundDark = Color(0xFFE2E2E6)

val ProfessionalSurfaceDark = Color(0xFF1A1C1E)
val ProfessionalOnSurfaceDark = Color(0xFFE2E2E6)

val ProfessionalSurfaceVariantDark = Color(0xFF43474E)
val ProfessionalOnSurfaceVariantDark = Color(0xFFC4C6CF)

/**
 * Status colours (success / warning / danger / info) that stay readable on both themes.
 * Containers are soft backgrounds, "on" colours are text on them, and the plain colours are
 * icons, bars and accents drawn straight on the surface.
 */
@Immutable
data class StatusColors(
  val successContainer: Color,
  val onSuccessContainer: Color,
  val success: Color,
  val warningContainer: Color,
  val onWarningContainer: Color,
  val warning: Color,
  val dangerContainer: Color,
  val onDangerContainer: Color,
  val danger: Color,
  val info: Color,
)

val LightStatusColors =
  StatusColors(
    successContainer = Color(0xFFD1FAE5),
    onSuccessContainer = Color(0xFF065F46),
    success = Color(0xFF047857),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF92400E),
    warning = Color(0xFFD97706),
    dangerContainer = Color(0xFFFEE2E2),
    onDangerContainer = Color(0xFF991B1B),
    danger = Color(0xFFDC2626),
    info = Color(0xFF2563EB),
  )

val DarkStatusColors =
  StatusColors(
    successContainer = Color(0xFF0F3D2E),
    onSuccessContainer = Color(0xFFA7F3D0),
    success = Color(0xFF34D399),
    warningContainer = Color(0xFF42310A),
    onWarningContainer = Color(0xFFFDE68A),
    warning = Color(0xFFFBBF24),
    dangerContainer = Color(0xFF4A1818),
    onDangerContainer = Color(0xFFFECACA),
    danger = Color(0xFFF87171),
    info = Color(0xFF60A5FA),
  )

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** Read status colours with `FasMovaStatus.success` etc. inside any composable. */
object FasMovaStatus {
  val successContainer: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.successContainer
  val onSuccessContainer: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.onSuccessContainer
  val success: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.success
  val warningContainer: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.warningContainer
  val onWarningContainer: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.onWarningContainer
  val warning: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.warning
  val dangerContainer: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.dangerContainer
  val onDangerContainer: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.onDangerContainer
  val danger: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.danger
  val info: Color
    @Composable @ReadOnlyComposable
    get() = LocalStatusColors.current.info
}
