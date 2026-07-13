package com.example.storemobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storemobile.ui.theme.Jesko

/* ───────────────────────────  BRAND LOGO  ─────────────────────────── */

@Composable
fun JeskoLogo(
    size: Int = 56,
    showWordmark: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape((size / 3.4f).dp))
                .background(Jesko.CardElevated)
                .border(
                    BorderStroke(1.dp, Jesko.Gold.copy(alpha = 0.35f)),
                    RoundedCornerShape((size / 3.4f).dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "J",
                fontSize = (size / 1.7f).sp,
                fontWeight = FontWeight.Black,
                color = Jesko.Gold
            )
        }
        if (showWordmark) {
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "JESKO",
                    color = Jesko.GoldLight,
                    fontWeight = FontWeight.Black,
                    fontSize = (size / 2.6f).sp
                )
                Text(
                    "SAVDO TIZIMI",
                    color = Jesko.TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

/* ───────────────────────────  BUTTONS  ─────────────────────────── */

enum class JeskoButtonStyle { Gold, Green, Outline, Danger, Ghost }

@Composable
fun JeskoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: JeskoButtonStyle = JeskoButtonStyle.Gold,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    height: Int = 52
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btnScale")

    val (background, content, border) = when (style) {
        JeskoButtonStyle.Gold -> Triple(Jesko.GoldGradient, Jesko.BgDark, null)
        JeskoButtonStyle.Green -> Triple(Jesko.GreenGradient, Color(0xFF06210F), null)
        JeskoButtonStyle.Outline -> Triple(
            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            Jesko.TextPrimary, Jesko.InputBorder
        )
        JeskoButtonStyle.Danger -> Triple(
            Brush.horizontalGradient(listOf(Jesko.RedDeep, Color(0xFFB91C1C))),
            Color.White, null
        )
        JeskoButtonStyle.Ghost -> Triple(
            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            Jesko.TextSecondary, null
        )
    }

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(height.dp)
            .scale(scale)
            .clip(shape)
            .then(if (border != null) Modifier.border(1.5.dp, border, shape) else Modifier)
            .background(if (enabled) background else Brush.linearGradient(listOf(Jesko.Input, Jesko.Input)))
            .alpha(if (enabled) 1f else 0.55f)
            .clickableNoRipple(enabled = enabled && !loading, interactionSource = interaction, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = if (style == JeskoButtonStyle.Gold) Jesko.BgDark else Jesko.Gold,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, tint = content, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    color = if (enabled) content else Jesko.TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/* ───────────────────────────  CARD  ─────────────────────────── */

@Composable
fun JeskoCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Jesko.Card,
        border = BorderStroke(1.dp, Jesko.Border)
    ) {
        Box(Modifier.padding(padding)) { content() }
    }
}

/* ───────────────────────────  EMPTY STATE  ─────────────────────────── */

@Composable
fun EmptyState(
    title: String,
    subtitle: String = "",
    icon: ImageVector = Icons.Filled.Inbox,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Jesko.Card),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Jesko.TextMuted, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                color = Jesko.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ───────────────────────────  LOADING  ─────────────────────────── */

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Jesko.Gold, strokeWidth = 3.dp)
    }
}

/* ───────────────────────────  PILL / BADGE  ─────────────────────────── */

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/* ───────────────────────────  THEME SELECTOR  ─────────────────────────── */

/**
 * Segmented control for the app appearance: Tizim (system) / Yorug' (light) /
 * Tungi (dark). Values match [com.example.storemobile.data.SessionManager]
 * constants ("system" | "light" | "dark").
 */
@Composable
fun JeskoThemeSelector(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple("system", "Tizim", Icons.Filled.BrightnessAuto),
        Triple("light", "Yorug'", Icons.Filled.LightMode),
        Triple("dark", "Tungi", Icons.Filled.DarkMode)
    )
    Row(
        modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        options.forEach { (value, label, icon) ->
            val active = value == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Jesko.Gold.copy(alpha = 0.16f) else Color.Transparent)
                    .then(
                        if (active) Modifier.border(1.dp, Jesko.Gold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clickableNoRipple(onClick = { onSelect(value) })
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon, label,
                    tint = if (active) Jesko.GoldLight else Jesko.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    color = if (active) Jesko.GoldLight else Jesko.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
