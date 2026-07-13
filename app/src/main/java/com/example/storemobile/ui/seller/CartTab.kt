package com.example.storemobile.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.data.model.CartLine
import com.example.storemobile.data.model.SaleMode
import com.example.storemobile.ui.components.EmptyState
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.JeskoButtonStyle
import com.example.storemobile.ui.components.QuantityStepper
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

@Composable
fun CartTab(
    vm: SellerViewModel,
    onBrowse: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showCheckout by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Savat", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("${ui.cartCount} ta pozitsiya · ${ui.cartPieces} dona", color = Jesko.TextSecondary, fontSize = 13.sp)
            }
            if (ui.cart.isNotEmpty()) {
                Text(
                    "Tozalash",
                    color = Jesko.Red,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(Jesko.Red.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                        .clickable { vm.clearCart() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        if (ui.cart.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    title = "Savat bo'sh",
                    subtitle = "Mahsulotlarni tanlab savatga qo'shing",
                    icon = Icons.Filled.RemoveShoppingCart
                )
                JeskoButton("Mahsulotlarga o'tish", onBrowse, style = JeskoButtonStyle.Outline, modifier = Modifier.padding(horizontal = 40.dp).fillMaxWidth())
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ui.cart, key = { it.key }) { line ->
                    CartRow(
                        line = line,
                        onPlus = { vm.changeCartCount(line, +1) },
                        onMinus = { vm.changeCartCount(line, -1) },
                        onDelete = { vm.removeCartLine(line) }
                    )
                }
            }

            // Summary + send
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Umumiy summa", color = Jesko.TextSecondary, fontSize = 12.sp)
                        Text("${Format.money(ui.cartTotal)} so'm", color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                JeskoButton(
                    text = "Kassirga yuborish",
                    onClick = { showCheckout = true },
                    style = JeskoButtonStyle.Green,
                    leadingIcon = Icons.Filled.Send,
                    loading = ui.sending,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56
                )
            }
        }
    }

    if (showCheckout) {
        CheckoutDialog(
            vm = vm,
            onDismiss = { showCheckout = false },
            onSent = { showCheckout = false }
        )
    }
}

@Composable
private fun CartRow(
    line: CartLine,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.product.name, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(
                            (if (line.mode == SaleMode.BLOCK) Jesko.Blue else Jesko.Green).copy(alpha = 0.16f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        line.unitLabel,
                        color = if (line.mode == SaleMode.BLOCK) Jesko.Blue else Jesko.Green,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${Format.money(line.lineTotal)} so'm", color = Jesko.GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        QuantityStepper(
            value = line.count,
            onMinus = onMinus,
            onPlus = onPlus,
            buttonSize = 34
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(34.dp).background(Jesko.Red.copy(alpha = 0.12f), CircleShape).clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.DeleteOutline, "o'chirish", tint = Jesko.Red, modifier = Modifier.size(18.dp))
        }
    }
}
