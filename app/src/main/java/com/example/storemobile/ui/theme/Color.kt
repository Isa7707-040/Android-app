package com.example.storemobile.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Themeable values that differ between the dark and light look.
 * The accent colours (gold / green / blue / red) stay the same in both
 * themes so the brand identity is preserved.
 */
data class JeskoPalette(
    val bgDark: Color,
    val bgPanel: Color,
    val card: Color,
    val cardElevated: Color,
    val input: Color,
    val inputBorder: Color,
    val border: Color,
    val overlay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val goldLight: Color,        // accent text/icon colour — needs contrast per theme
    val brandGradient: Brush,
    val sidebarGradient: Brush
)

/** Premium DARK look (original JESKO desktop palette). */
val JeskoDarkPalette = JeskoPalette(
    bgDark = Color(0xFF0B1120),
    bgPanel = Color(0xFF0F172A),
    card = Color(0xFF131D33),
    cardElevated = Color(0xFF18243F),
    input = Color(0xFF1E293B),
    inputBorder = Color(0xFF334155),
    border = Color(0xFF1F2A44),
    overlay = Color(0xCC05080F),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    goldLight = Color(0xFFFCD34D),
    brandGradient = Brush.linearGradient(
        listOf(Color(0xFF111827), Color(0xFF1E293B), Color(0xFF0B1120))
    ),
    sidebarGradient = Brush.verticalGradient(
        listOf(Color(0xFF111827), Color(0xFF0B1322))
    )
)

/** Clean LIGHT look — soft slate backgrounds with white cards. */
val JeskoLightPalette = JeskoPalette(
    bgDark = Color(0xFFEEF2F7),
    bgPanel = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF1F5F9),
    input = Color(0xFFF1F5F9),
    inputBorder = Color(0xFFCBD5E1),
    border = Color(0xFFE2E8F0),
    overlay = Color(0x66000000),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    goldLight = Color(0xFFB45309),     // deep amber — readable on white surfaces
    brandGradient = Brush.linearGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFEFF4FA), Color(0xFFE3EAF3))
    ),
    sidebarGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
    )
)

/**
 * JESKO brand palette — mirrors the WPF Desktop application so the
 * mobile app feels like the same product.
 *
 * The theme-dependent fields are backed by [mutableStateOf] so that calling
 * [applyDark] anywhere updates every screen automatically (Compose recomposes
 * each `Jesko.X` read). This avoids having to thread a CompositionLocal
 * through the hundreds of existing `Jesko.*` references.
 */
object Jesko {

    /* ── Theme-dependent (state-backed). Mutated only via apply()/applyDark(). ── */
    var isDark by mutableStateOf(true)
        private set

    var BgDark by mutableStateOf(JeskoDarkPalette.bgDark)
        private set
    var BgPanel by mutableStateOf(JeskoDarkPalette.bgPanel)
        private set
    var Card by mutableStateOf(JeskoDarkPalette.card)
        private set
    var CardElevated by mutableStateOf(JeskoDarkPalette.cardElevated)
        private set
    var Input by mutableStateOf(JeskoDarkPalette.input)
        private set
    var InputBorder by mutableStateOf(JeskoDarkPalette.inputBorder)
        private set
    var Border by mutableStateOf(JeskoDarkPalette.border)
        private set
    var Overlay by mutableStateOf(JeskoDarkPalette.overlay)
        private set

    var TextPrimary by mutableStateOf(JeskoDarkPalette.textPrimary)
        private set
    var TextSecondary by mutableStateOf(JeskoDarkPalette.textSecondary)
        private set
    var TextMuted by mutableStateOf(JeskoDarkPalette.textMuted)
        private set

    var GoldLight by mutableStateOf(JeskoDarkPalette.goldLight)
        private set

    var BrandGradient: Brush by mutableStateOf(JeskoDarkPalette.brandGradient)
        private set
    var SidebarGradient: Brush by mutableStateOf(JeskoDarkPalette.sidebarGradient)
        private set

    /* ── Constant accents (same in both themes) ── */
    val Gold = Color(0xFFF59E0B)
    val GoldDeep = Color(0xFFD97706)

    val Green = Color(0xFF22C55E)
    val GreenLight = Color(0xFF34D399)
    val GreenDeep = Color(0xFF16A34A)

    val Blue = Color(0xFF38BDF8)
    val BlueDeep = Color(0xFF1D4ED8)

    val Red = Color(0xFFF87171)
    val RedDeep = Color(0xFFDC2626)

    // Gradients (brand accent — unchanged across themes)
    val GoldGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))
    )
    val GoldGradientSoft = Brush.horizontalGradient(
        listOf(Color(0xFFFCD34D), Color(0xFFFBBF24))
    )
    val GreenGradient = Brush.horizontalGradient(
        listOf(Color(0xFF22C55E), Color(0xFF16A34A))
    )

    /** Apply a full palette to all theme-dependent fields. */
    fun apply(p: JeskoPalette) {
        BgDark = p.bgDark
        BgPanel = p.bgPanel
        Card = p.card
        CardElevated = p.cardElevated
        Input = p.input
        InputBorder = p.inputBorder
        Border = p.border
        Overlay = p.overlay
        TextPrimary = p.textPrimary
        TextSecondary = p.textSecondary
        TextMuted = p.textMuted
        GoldLight = p.goldLight
        BrandGradient = p.brandGradient
        SidebarGradient = p.sidebarGradient
    }

    /** Switch between the dark and light look. Safe to call repeatedly. */
    fun applyDark(dark: Boolean) {
        isDark = dark
        apply(if (dark) JeskoDarkPalette else JeskoLightPalette)
    }
}
