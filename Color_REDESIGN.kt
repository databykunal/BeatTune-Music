package com.sonify.music.presentation.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// BEATTUNE V2.0 PREMIUM COLOR SCHEME
// Exact colors from BeatTune design image
// ============================================================================

// Primary Colors (Music Note - Purple/Blue Gradient)
val BeatPurple = Color(0xFF8574E8)      // Primary music note color
val BeatViolet = Color(0xFF7567C8)      // Darker purple
val BeatPink = Color(0xFFE48BC1)        // Accent pink
val BeatBlue = Color(0xFF5E82C9)        // Secondary blue
val BeatCyan = Color(0xFF6CAFC1)        // Cyan accent
val BeatDeepBlue = Color(0xFF3D5A99)    // Deep blue for gradients

// Dark Theme - BeatTune V2.0
val md_theme_dark_primary = BeatPurple              // #8574E8
val md_theme_dark_onPrimary = Color.White
val md_theme_dark_primaryContainer = Color(0xFF2D1F4D)  // Dark purple container
val md_theme_dark_onPrimaryContainer = Color(0xFFE9D4FF)

val md_theme_dark_secondary = BeatBlue              // #5E82C9
val md_theme_dark_onSecondary = Color.White
val md_theme_dark_secondaryContainer = Color(0xFF1F3A5A)
val md_theme_dark_onSecondaryContainer = Color(0xFFD4E4FF)

val md_theme_dark_tertiary = BeatPink               // #E48BC1
val md_theme_dark_onTertiary = Color.White
val md_theme_dark_tertiaryContainer = Color(0xFF5A2A50)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD4EA)

// Backgrounds - Deep Navy from image
val md_theme_dark_background = Color(0xFF070B18)   // Very dark navy
val md_theme_dark_onBackground = Color(0xFFF4F3F8)

val md_theme_dark_surface = Color(0xFF0C1223)      // Dark surface
val md_theme_dark_onSurface = Color(0xFFF4F3F8)
val md_theme_dark_surfaceVariant = Color(0xFF131B30)   // Slightly lighter surface
val md_theme_dark_onSurfaceVariant = Color(0xFFADB5C9)

val md_theme_dark_outline = Color(0xFF57606A)
val md_theme_dark_outlineVariant = Color(0xFF474F57)
val md_theme_dark_scrim = Color.Black
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color.White
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

// Light Theme - BeatTune (minimal use)
val md_theme_light_primary = Color(0xFF6554C5)
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFEAE6FF)
val md_theme_light_onPrimaryContainer = Color(0xFF21174F)

val md_theme_light_secondary = Color(0xFF4C6EA9)
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFDFE3FF)
val md_theme_light_onSecondaryContainer = Color(0xFF04226D)

val md_theme_light_background = Color(0xFFF8F8FB)
val md_theme_light_onBackground = Color(0xFF171821)
val md_theme_light_surface = Color.White
val md_theme_light_onSurface = Color(0xFF171821)
val md_theme_light_surfaceVariant = Color(0xFFF0F0F5)
val md_theme_light_onSurfaceVariant = Color(0xFF626575)

// ============================================================================
// GRADIENT COLORS (For backgrounds)
// ============================================================================

val GradientPurpleBlue = listOf(BeatPurple, BeatBlue)
val GradientDarkNavy = listOf(Color(0xFF070B18), Color(0xFF0C1223))
val GradientBannerDark = listOf(Color(0xFF20284D), Color(0xFF31305E), Color(0xFF243A5A))
val GradientCardDark = listOf(Color(0xFF131B30), Color(0xFF0C1223))

// ============================================================================
// SEMANTIC COLORS
// ============================================================================

// Success/Playing
val PlayingGreen = Color(0xFF4CAF50)

// Warning/Pending
val WarningYellow = Color(0xFFFFC107)

// Error/Failed
val ErrorRed = Color(0xFFEF5350)

// Info/Loading
val InfoBlue = Color(0xFF2196F3)

// ============================================================================
// CUSTOM COMPONENT COLORS
// ============================================================================

// Music Player Controls
val PlayButtonColor = BeatPurple          // Play button
val PauseButtonColor = BeatBlue           // Pause button
val ShuffleOnColor = BeatPink             // Shuffle enabled
val RepeatOnColor = BeatCyan              // Repeat enabled

// Cards & Containers
val CategoryCardBackground = Color(0xFF1A2547)    // Category button background
val PlaylistCardBackground = Color(0xFF131B30)   // Playlist card background
val SearchInputBackground = Color(0xFF0F1629)    // Search field background

// Text Colors
val PrimaryTextColor = Color(0xFFF4F3F8)         // Main text
val SecondaryTextColor = Color(0xFFADB5C9)       // Secondary text
val HintTextColor = Color(0xFF7A838F)            // Hint/placeholder text

// Dividers & Borders
val DividerColor = Color(0xFF1F2A3F)
val BorderColor = Color(0xFF2A3A54)
