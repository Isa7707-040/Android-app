package com.example.storemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storemobile.ui.theme.Jesko

@Composable
fun JeskoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    height: Int = 52
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier
            .height(height.dp)
            .background(Jesko.Input, shape)
            .border(
                width = if (focused) 2.dp else 1.5.dp,
                color = if (focused) Jesko.Gold else Jesko.InputBorder,
                shape = shape
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon, null,
                    tint = if (focused) Jesko.Gold else Jesko.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Jesko.TextMuted, fontSize = 15.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = LocalTextStyle.current.copy(
                        color = Jesko.TextPrimary, fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(Jesko.Gold),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    interactionSource = interaction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/** A round +/- stepper used in the add-to-cart dialog and the cart. */
@Composable
fun QuantityStepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
    buttonSize: Int = 40
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        StepButton(Icons.Filled.Remove, minusEnabled, buttonSize, onMinus)
        Box(Modifier.width(54.dp), contentAlignment = Alignment.Center) {
            Text(
                value.toString(),
                color = Jesko.TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
        }
        StepButton(Icons.Filled.Add, plusEnabled, buttonSize, onPlus, gold = true)
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    enabled: Boolean,
    size: Int,
    onClick: () -> Unit,
    gold: Boolean = false
) {
    val bg = when {
        !enabled -> Jesko.Input
        gold -> Jesko.Gold
        else -> Jesko.CardElevated
    }
    val tint = when {
        !enabled -> Jesko.TextMuted
        gold -> Jesko.BgDark
        else -> Jesko.TextPrimary
    }
    Box(
        Modifier
            .size(size.dp)
            .background(bg, CircleShape)
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.5).dp))
    }
}
