package com.example.storemobile.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.storemobile.data.model.Product
import com.example.storemobile.data.model.SaleMode
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.QuantityStepper
import com.example.storemobile.ui.components.clickableNoRipple
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

@Composable
fun AddToCartDialog(
    product: Product,
    alreadyInCart: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (SaleMode, Int) -> Unit
) {
    // ── OMBOR NAZORATI ──
    // Omborda mavjud dona (savatga allaqachon qo'shilganini ayirib). Bundan
    // ortiq qo'shib bo'lmaydi. Tugagan (yoki hammasi savatda) mahsulot sotilmaydi.
    val outOfStock = product.totalPieces <= 0
    val available = (product.totalPieces - alreadyInCart).coerceAtLeast(0)
    val blockAffordable = product.sellableByBlock && product.quantityInBlock > 0 && available >= product.quantityInBlock

    // Boshlang'ich rejim: blok yetsa blok, aks holda dona.
    var mode by remember { mutableStateOf(if (blockAffordable) SaleMode.BLOCK else SaleMode.PIECE) }
    var count by remember { mutableIntStateOf(1) }

    // Tanlangan rejimda maksimal nechta qo'shsa bo'ladi.
    val maxCount = if (mode == SaleMode.BLOCK) {
        if (product.quantityInBlock > 0) available / product.quantityInBlock else 0
    } else available

    val canAdd = !outOfStock && maxCount >= 1 && count in 1..maxCount

    // Hisoblangan (effective) narx — dona/blok narxidan biri 0 bo'lsa ham summa 0 bo'lmaydi.
    val unitPrice = if (mode == SaleMode.BLOCK) product.effectiveSellPriceBlock else product.effectiveSellPricePiece
    val total = unitPrice * count

    // Bu sotuvdan keyin omborda qancha qoladi (endi hech qachon minusga tushmaydi).
    val piecesConsumed = if (mode == SaleMode.BLOCK) count * product.quantityInBlock else count
    val projectedPieces = (product.totalPieces - alreadyInCart - piecesConsumed).coerceAtLeast(0)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Jesko.Card, RoundedCornerShape(22.dp))
                .border(1.dp, Jesko.Border, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(Jesko.CardElevated, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.name.take(1).uppercase(), color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2)
                    Text(
                        if (outOfStock) "Omborda tugagan" else product.stockLong,
                        color = if (outOfStock) Jesko.Red else Jesko.TextSecondary,
                        fontWeight = if (outOfStock) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
                Box(
                    Modifier.size(34.dp).background(Jesko.Input, CircleShape).clickableNoRipple(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, "yopish", tint = Jesko.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            if (outOfStock) {
                // ── Tugagan mahsulot: sotib bo'lmaydi ──
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.Red.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "Bu mahsulot omborda yo'q.",
                        color = Jesko.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sotish uchun avval admin yoki buxgalter kirim (prixod) qilishi kerak.",
                        color = Jesko.TextSecondary, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(18.dp))
                JeskoButton(
                    text = "Omborda yo'q",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    height = 52
                )
                return@Column
            }

            // Mode selector — faqat blok-tipdagi mahsulotda.
            if (product.sellableByBlock) {
                Text("O'LCHOV BIRLIGI", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    ModeChip(
                        label = "Blok",
                        sub = if (blockAffordable) "${Format.money(product.effectiveSellPriceBlock)} so'm" else "yetarli emas",
                        active = mode == SaleMode.BLOCK,
                        enabled = blockAffordable,
                        modifier = Modifier.weight(1f)
                    ) { if (blockAffordable) { mode = SaleMode.BLOCK; count = 1 } }
                    Spacer(Modifier.width(4.dp))
                    ModeChip(
                        label = "Dona",
                        sub = "${Format.money(product.effectiveSellPricePiece)} so'm",
                        active = mode == SaleMode.PIECE,
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    ) { mode = SaleMode.PIECE; count = 1 }
                }
                Spacer(Modifier.height(18.dp))
            }

            // Quantity — '+' ombordagi mavjud sonidan oshirmaydi.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Soni", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(
                        if (mode == SaleMode.BLOCK) "blok hisobida (max $maxCount)" else "dona hisobida (max $maxCount)",
                        color = Jesko.TextMuted, fontSize = 11.sp
                    )
                }
                QuantityStepper(
                    value = count,
                    onMinus = { if (count > 1) count-- },
                    onPlus = { if (count < maxCount) count++ },
                    minusEnabled = count > 1,
                    plusEnabled = count < maxCount,
                    buttonSize = 44
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Sotuvdan keyin omborda qancha qoladi ──
            val projectedLabel = if (product.isKg) "$projectedPieces kg" else "$projectedPieces dona"
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sotilgach omborda",
                    color = Jesko.TextSecondary,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    projectedLabel,
                    color = Jesko.TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Total
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Jami", color = Jesko.TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("${Format.money(total)} so'm", color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }

            Spacer(Modifier.height(18.dp))

            JeskoButton(
                text = if (canAdd) "Savatga qo'shish" else "Yetarli emas",
                onClick = { if (canAdd) onConfirm(mode, count) },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth(),
                height = 52
            )
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    sub: String,
    active: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .background(if (active) Jesko.Gold else Color.Transparent, RoundedCornerShape(9.dp))
            .clickableNoRipple(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = when {
                active -> Jesko.BgDark
                !enabled -> Jesko.TextMuted
                else -> Jesko.TextPrimary
            },
            fontWeight = FontWeight.Bold, fontSize = 14.sp
        )
        Text(
            sub,
            color = if (active) Jesko.BgDark.copy(alpha = 0.7f) else Jesko.TextMuted,
            fontSize = 11.sp
        )
    }
}
