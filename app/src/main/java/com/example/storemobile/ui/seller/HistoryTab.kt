package com.example.storemobile.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.data.model.Order
import com.example.storemobile.ui.components.EmptyState
import com.example.storemobile.ui.components.LoadingBox
import com.example.storemobile.ui.components.StatusPill
import com.example.storemobile.ui.components.clickableNoRipple
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

@Composable
fun HistoryTab(vm: SellerViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    val pending = ui.history.filter { it.status == "Pending" }
    // Sotilgan (tushum): naqd (Paid) + qarzga (Debt) sotilganlar, to'liq qiymati bo'yicha
    val sold = ui.history.filter { it.status == "Paid" || it.status == "Debt" }
    val soldSum = sold.sumOf { it.totalSum }

    // Bosilgan sotuv (batafsil oyna uchun). null bo'lsa oyna yopiq.
    var selected by remember { mutableStateOf<Order?>(null) }

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
                Text("Mening sotuvlarim", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Yuborilgan va sotilgan buyurtmalar", color = Jesko.TextSecondary, fontSize = 13.sp)
            }
            Box(
                Modifier
                    .background(Jesko.CardElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, Jesko.Border, RoundedCornerShape(12.dp))
                    .clickable { vm.loadHistory() }
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.Refresh, "yangilash", tint = Jesko.GoldLight, modifier = Modifier.padding(0.dp))
            }
        }

        // Stat strip
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Sotilgan", "${Format.money(soldSum)}", "so'm", Jesko.Green, Modifier.weight(1.4f))
            StatCard("Sotuvlar", sold.size.toString(), "ta", Jesko.Gold, Modifier.weight(1f))
            StatCard("Kutilmoqda", pending.size.toString(), "ta", Jesko.Blue, Modifier.weight(1f))
        }

        when {
            ui.historyLoading && ui.history.isEmpty() -> LoadingBox()
            ui.history.isEmpty() -> EmptyState(
                title = "Hozircha sotuvlar yo'q",
                subtitle = "Siz yuborgan buyurtmalar shu yerda ko'rinadi",
                icon = Icons.AutoMirrored.Filled.ReceiptLong
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ui.history, key = { it.id }) { order -> OrderCard(order) { selected = order } }
            }
        }
    }

    // Batafsil oyna: sotuvchi bosgan buyurtmaning to'liq ma'lumoti.
    selected?.let { order ->
        OrderDetailDialog(order = order, onDismiss = { selected = null })
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(label, color = Jesko.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(3.dp))
            Text(unit, color = Jesko.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    val (label, color) = when (order.status) {
        "Paid" -> "To'langan" to Jesko.Green
        "Debt" -> "Qarz" to Jesko.Gold
        "Pending" -> "Kutilmoqda" to Jesko.Blue
        "Cancelled" -> "Bekor qilingan" to Jesko.Red
        else -> order.status to Jesko.TextSecondary
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("#${order.id} \u00B7 ${order.clientName}", color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(Format.dateTime(order.createdAt), color = Jesko.TextMuted, fontSize = 11.sp)
            }
            StatusPill(label, color)
        }
        Spacer(Modifier.height(10.dp))
        // items preview
        order.items.take(3).forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text("\u2022 ${item.displayName}", color = Jesko.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("\u00D7${item.quantity}", color = Jesko.TextMuted, fontSize = 12.sp)
            }
        }
        if (order.items.size > 3) {
            Text("+${order.items.size - 3} ta boshqa", color = Jesko.TextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${order.itemCount} dona", color = Jesko.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("${Format.money(order.totalSum)} so'm", color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text("Batafsil ko'rish uchun bosing", color = Jesko.TextMuted, fontSize = 10.sp)
    }
}

// ──────────────────────────── BATAFSIL OYNA ────────────────────────────
// Sotuvchi bosgan buyurtmaning to'liq ma'lumoti: mijoz, sana, holat, to'lov turi,
// har bir mahsulot (nomi, soni, narxi, jami), umumiy summa, to'langan va qarz.
@Composable
private fun OrderDetailDialog(order: Order, onDismiss: () -> Unit) {
    val (statusLabel, statusColor) = when (order.status) {
        "Paid" -> "To'langan" to Jesko.Green
        "Debt" -> "Qarz" to Jesko.Gold
        "Pending" -> "Kutilmoqda" to Jesko.Blue
        "Cancelled" -> "Bekor qilingan" to Jesko.Red
        else -> order.status to Jesko.TextSecondary
    }
    val payLabel = when (order.paymentType) {
        "Cash" -> "Naqd"
        "Card" -> "Karta"
        "Mixed" -> "Aralash (naqd + karta)"
        "Debt" -> "Qarzga"
        else -> if (order.paymentType.isBlank()) "\u2014" else order.paymentType
    }
    val debt = (order.totalSum - order.paidSum).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .background(Jesko.Card, RoundedCornerShape(22.dp))
                .border(1.dp, Jesko.Border, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Buyurtma #${order.id}", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(Format.dateTime(order.createdAt), color = Jesko.TextMuted, fontSize = 12.sp)
                }
                Box(
                    Modifier.size(34.dp).background(Jesko.Input, CircleShape).clickableNoRipple(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, "yopish", tint = Jesko.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Meta: holat, mijoz, sotuvchi, to'lov turi
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                InfoRow("Holat") { StatusPill(statusLabel, statusColor) }
                Spacer(Modifier.height(8.dp))
                InfoText("Mijoz", order.clientName)
                order.client?.phone?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp)); InfoText("Telefon", it)
                }
                Spacer(Modifier.height(8.dp))
                InfoText("Sotuvchi", order.sellerName)
                Spacer(Modifier.height(8.dp))
                InfoText("To'lov turi", payLabel)
            }

            Spacer(Modifier.height(14.dp))

            Text("MAHSULOTLAR (${order.items.size} tur)", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            // Mahsulotlar ro'yxati (ko'p bo'lsa scroll bo'ladi)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                order.items.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.displayName, color = Jesko.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${item.quantity} \u00D7 ${Format.money(item.price)} so'm", color = Jesko.TextMuted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("${Format.money(item.total)} so'm", color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Summalar
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                SummaryRow("Jami summa", "${Format.money(order.totalSum)} so'm", Jesko.GoldLight, big = true)
                Spacer(Modifier.height(8.dp))
                SummaryRow("To'langan", "${Format.money(order.paidSum)} so'm", Jesko.Green)
                if (debt > 0.5) {
                    Spacer(Modifier.height(8.dp))
                    SummaryRow("Qarz", "${Format.money(debt)} so'm", Jesko.Red)
                }
            }
        }
    }
}

@Composable
private fun InfoText(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Jesko.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Jesko.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoRow(label: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Jesko.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: Color, big: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Jesko.TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = if (big) 14.sp else 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = if (big) 19.sp else 15.sp)
    }
}
