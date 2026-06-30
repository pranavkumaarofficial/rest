package com.screentracker.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * REST Design System
 * Following Linear + Notion design principles:
 * - Deep dark canvas (Linear #010102)
 * - Lavender accent (Linear #5e6ad2)
 * - Clean spacing, no clutter
 * - Inter font family with negative tracking
 * - 8px button corners (Linear standard)
 * - Subtle surface elevation
 */
object RestDesignSystem {

    // ── Colors (Linear-inspired dark theme) ─────────────────────────────

    object Colors {
        // Brand & Accent
        val Primary = Color(0xFF6750A4)              // Material purple (original)
        val PrimaryHover = Color(0xFF7F67BE)
        val PrimaryPressed = Color(0xFF5E4192)
        val OnPrimary = Color(0xFFFFFFFF)

        // Surface (4-step ladder)
        val Canvas = Color(0xFF010102)               // Deepest dark
        val Surface1 = Color(0xFF0F1011)             // Cards
        val Surface2 = Color(0xFF141516)             // Lifted cards
        val Surface3 = Color(0xFF18191A)
        val Surface4 = Color(0xFF191A1B)

        // Borders
        val Hairline = Color(0xFF23252A)
        val HairlineStrong = Color(0xFF34343A)

        // Text
        val Ink = Color(0xFFF7F8F8)                  // Primary text
        val InkMuted = Color(0xFFD0D6E0)             // Secondary
        val InkSubtle = Color(0xFF8A8F98)            // Tertiary
        val InkTertiary = Color(0xFF62666D)          // Disabled

        // Semantic
        val Success = Color(0xFF27A644)
        val Warning = Color(0xFFDD5B00)
        val Error = Color(0xFFE03131)

        // Night mode (red-shifted)
        val NightCanvas = Color(0xFF1A0A0A)
        val NightText = Color(0xFFCC6666)
    }

    // ── Typography (Linear-inspired with Inter fallback) ────────────────

    object Typography {
        // Display (used for "rest" wordmark)
        val DisplayXL = TypographySpec(
            fontSize = 80.sp,
            fontWeight = FontWeight.SemiBold,      // 600
            lineHeight = 1.05f,
            letterSpacing = (-3.0).sp              // Aggressive negative tracking
        )

        val DisplayLG = TypographySpec(
            fontSize = 56.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 1.10f,
            letterSpacing = (-1.8).sp
        )

        val Headline = TypographySpec(
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 1.20f,
            letterSpacing = (-0.6).sp
        )

        val Subhead = TypographySpec(
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,       // 400
            lineHeight = 1.40f,
            letterSpacing = (-0.2).sp
        )

        val BodyLG = TypographySpec(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 1.50f,
            letterSpacing = (-0.1).sp
        )

        val Body = TypographySpec(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 1.50f,
            letterSpacing = (-0.05).sp
        )

        val BodySM = TypographySpec(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 1.50f,
            letterSpacing = 0.sp
        )

        val Caption = TypographySpec(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 1.40f,
            letterSpacing = 0.sp
        )

        val Button = TypographySpec(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,       // 500
            lineHeight = 1.20f,
            letterSpacing = 0.sp
        )

        val Eyebrow = TypographySpec(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 1.30f,
            letterSpacing = 0.4.sp                // Positive tracking
        )
    }

    data class TypographySpec(
        val fontSize: androidx.compose.ui.unit.TextUnit,
        val fontWeight: FontWeight,
        val lineHeight: Float,
        val letterSpacing: androidx.compose.ui.unit.TextUnit
    )

    // ── Spacing (Linear system, 4px base) ───────────────────────────────

    object Spacing {
        val XXS = 4.dp
        val XS = 8.dp
        val SM = 12.dp
        val MD = 16.dp
        val LG = 24.dp
        val XL = 32.dp
        val XXL = 48.dp
        val Section = 96.dp
    }

    // ── Border Radius (Linear scale) ─────────────────────────────────────

    object Rounded {
        val XS = 4.dp
        val SM = 6.dp
        val MD = 8.dp            // Standard button radius
        val LG = 12.dp           // Cards
        val XL = 16.dp           // Large panels
        val XXL = 24.dp
        val Pill = 9999.dp
    }

    // ── Elevation (via surface ladder, no shadows) ──────────────────────

    object Elevation {
        // Linear uses surface color steps instead of shadows
        // Level 0: Canvas
        // Level 1: Surface1 + hairline border
        // Level 2: Surface2 + hairline-strong border
        // Level 3: Surface3
    }
}
