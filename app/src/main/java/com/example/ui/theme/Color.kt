package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Sunlit Studio - Light (Warm Cream Paper, Electric Cobalt, Bright Coral, Emerald Mint)
val LightPrimary = Color(0xFF2357E8)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDFE8FF)
val LightOnPrimaryContainer = Color(0xFF0C246B)

val LightSecondary = Color(0xFFFF5D47)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFE3DD)
val LightOnSecondaryContainer = Color(0xFF6B1408)

val LightTertiary = Color(0xFF10A37F)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFD5F7EC)
val LightOnTertiaryContainer = Color(0xFF064233)

val LightBackground = Color(0xFFFFFBF4)
val LightOnBackground = Color(0xFF141B29)
val LightSurface = Color(0xFFFFFBF4)
val LightOnSurface = Color(0xFF141B29)
val LightSurfaceVariant = Color(0xFFF3ECE0)
val LightOnSurfaceVariant = Color(0xFF535E73)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF7F1E7)
val LightSurfaceContainer = Color(0xFFF0E8DA)
val LightSurfaceContainerHigh = Color(0xFFE7DDD0)
val LightOutline = Color(0xFFC8BEAE)
val LightOutlineVariant = Color(0xFFE6DDD0)

val LightError = Color(0xFFE03E36)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFE2E0)
val LightOnErrorContainer = Color(0xFF66110D)

// Studio Night - Dark (Midnight Slate, Luminous Sky-Cobalt, Warm Coral, Mint Emerald)
val DarkPrimary = Color(0xFF6E9BFF)
val DarkOnPrimary = Color(0xFF071D54)
val DarkPrimaryContainer = Color(0xFF19357A)
val DarkOnPrimaryContainer = Color(0xFFDDE7FF)

val DarkSecondary = Color(0xFFFF7B68)
val DarkOnSecondary = Color(0xFF561006)
val DarkSecondaryContainer = Color(0xFF6E2419)
val DarkOnSecondaryContainer = Color(0xFFFFE1DC)

val DarkTertiary = Color(0xFF38D39F)
val DarkOnTertiary = Color(0xFF043827)
val DarkTertiaryContainer = Color(0xFF124D3A)
val DarkOnTertiaryContainer = Color(0xFFCFFAEA)

val DarkBackground = Color(0xFF0E131F)
val DarkOnBackground = Color(0xFFF7F3EB)
val DarkSurface = Color(0xFF0E131F)
val DarkOnSurface = Color(0xFFF7F3EB)
val DarkSurfaceVariant = Color(0xFF1E2639)
val DarkOnSurfaceVariant = Color(0xFFA3AEC4)
val DarkSurfaceContainerLowest = Color(0xFF0A0E17)
val DarkSurfaceContainerLow = Color(0xFF141B2B)
val DarkSurfaceContainer = Color(0xFF1B2336)
val DarkSurfaceContainerHigh = Color(0xFF242E45)
val DarkOutline = Color(0xFF3A4663)
val DarkOutlineVariant = Color(0xFF28324B)

val DarkError = Color(0xFFFF837A)
val DarkOnError = Color(0xFF560D09)
val DarkErrorContainer = Color(0xFF6B1E19)
val DarkOnErrorContainer = Color(0xFFFFDFDC)

data class CadenceSemanticColors(
  val urgent: Color,
  val urgentContainer: Color,
  val high: Color,
  val highContainer: Color,
  val medium: Color,
  val mediumContainer: Color,
  val low: Color,
  val lowContainer: Color,
  val success: Color,
  val successContainer: Color,
  val overdue: Color,
  val overdueContainer: Color,
  val marigold: Color,
  val marigoldContainer: Color,
  val heroStart: Color,
  val heroEnd: Color,
  val cardElevated: Color,
  val subtleDivider: Color,
)

val LightCadenceColors =
  CadenceSemanticColors(
    urgent = Color(0xFFE03E36),
    urgentContainer = Color(0xFFFFE3E0),
    high = Color(0xFFFF5D47),
    highContainer = Color(0xFFFFE8E2),
    medium = Color(0xFF2357E8),
    mediumContainer = Color(0xFFE1E9FF),
    low = Color(0xFF535E73),
    lowContainer = Color(0xFFECE7DE),
    success = Color(0xFF10A37F),
    successContainer = Color(0xFFD6F6EB),
    overdue = Color(0xFFE03E36),
    overdueContainer = Color(0xFFFFE3E0),
    marigold = Color(0xFFD97706),
    marigoldContainer = Color(0xFFFEF1D6),
    heroStart = Color(0xFFEAF0FF),
    heroEnd = Color(0xFFFFEFE7),
    cardElevated = Color(0xFFFFFFFF),
    subtleDivider = Color(0xFFE6DDD0),
  )

val DarkCadenceColors =
  CadenceSemanticColors(
    urgent = Color(0xFFFF837A),
    urgentContainer = Color(0xFF591D1A),
    high = Color(0xFFFF8A78),
    highContainer = Color(0xFF54221B),
    medium = Color(0xFF6E9BFF),
    mediumContainer = Color(0xFF193066),
    low = Color(0xFFA3AEC4),
    lowContainer = Color(0xFF222B3E),
    success = Color(0xFF38D39F),
    successContainer = Color(0xFF104635),
    overdue = Color(0xFFFF837A),
    overdueContainer = Color(0xFF591D1A),
    marigold = Color(0xFFFBBF24),
    marigoldContainer = Color(0xFF47330E),
    heroStart = Color(0xFF172447),
    heroEnd = Color(0xFF2B1C24),
    cardElevated = Color(0xFF171E2E),
    subtleDivider = Color(0xFF28324B),
  )

data class CategoryColorOption(val name: String, val hex: Long)

val CategoryColorOptions =
  listOf(
    CategoryColorOption("Cobalt", 0xFF2357E8),
    CategoryColorOption("Coral", 0xFFFF5D47),
    CategoryColorOption("Emerald", 0xFF10A37F),
    CategoryColorOption("Marigold", 0xFFE5890A),
    CategoryColorOption("Cerulean", 0xFF0284C7),
    CategoryColorOption("Berry", 0xFFD93870),
    CategoryColorOption("Violet Ink", 0xFF7C3AED),
    CategoryColorOption("Moss", 0xFF3D8B40),
    CategoryColorOption("Tangerine", 0xFFEA580C),
    CategoryColorOption("Ink Slate", 0xFF475569),
  )
