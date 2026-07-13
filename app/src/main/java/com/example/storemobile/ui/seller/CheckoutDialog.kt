package com.example.storemobile.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.data.model.Client
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.JeskoButtonStyle
import com.example.storemobile.ui.components.JeskoTextField
import com.example.storemobile.ui.components.clickableNoRipple
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

@Composable
fun CheckoutDialog(
    vm: SellerViewModel,
    onDismiss: () -> Unit,
    onSent: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    // null = "Naqd xaridor"
    var selected by remember { mutableStateOf<Client?>(null) }
    var addingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var clientSearch by remember { mutableStateOf("") }
    // "Yuborish" bosilgach qarzdor mijoz uchun chiqadigan ogohlantirish
    var showDebtWarning by remember { mutableStateOf(false) }

    // Qidiruv bo'yicha mijozlarni filtrlash (ism yoki telefon)
    val filteredClients = remember(ui.clients, clientSearch) {
        val q = clientSearch.trim()
        if (q.isBlank()) ui.clients
        else ui.clients.filter {
            it.name.contains(q, ignoreCase = true) || (it.phone ?: "").contains(q)
        }
    }

    Dialog(onDismissRequest = { if (!ui.sending) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Jesko.Card, RoundedCornerShape(22.dp))
                .border(1.dp, Jesko.Border, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Text("Mijozni tanlang", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Spacer(Modifier.height(2.dp))
            Text("Buyurtma kassirga shu nom bilan boradi", color = Jesko.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            if (addingNew) {
                JeskoTextField(newName, { newName = it }, "Mijoz ismi", leadingIcon = Icons.Filled.Person, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                JeskoTextField(newPhone, { newPhone = it }, "Telefon (ixtiyoriy)", keyboardType = KeyboardType.Phone, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row {
                    JeskoButton("Bekor", { addingNew = false }, style = JeskoButtonStyle.Outline, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    JeskoButton(
                        "Saqlash",
                        {
                            if (newName.isNotBlank()) {
                                vm.addClient(newName.trim(), newPhone.trim().ifBlank { null }) { created ->
                                    if (created != null) {
                                        selected = created
                                        addingNew = false
                                        newName = ""; newPhone = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Mijozlar ko'p bo'lsa — tez topish uchun qidiruv
                JeskoTextField(
                    value = clientSearch,
                    onValueChange = { clientSearch = it },
                    placeholder = "Mijozni qidirish (ism yoki telefon)...",
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Naqd xaridor" va "Yangi mijoz" faqat qidiruv bo'sh bo'lsa ko'rinadi
                    if (clientSearch.isBlank()) {
                        item {
                            ClientOption(
                                title = "Naqd xaridor",
                                subtitle = "Mijozsiz sotuv",
                                icon = true,
                                active = selected == null,
                                debt = 0.0,
                                onClick = { selected = null }
                            )
                        }
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Jesko.Gold.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Jesko.Gold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable { addingNew = true }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PersonAdd, null, tint = Jesko.GoldLight, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Yangi mijoz qo'shish", color = Jesko.GoldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    if (clientSearch.isNotBlank() && filteredClients.isEmpty()) {
                        item {
                            Text(
                                "“$clientSearch” bo'yicha mijoz topilmadi",
                                color = Jesko.TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    items(filteredClients, key = { it.id }) { client ->
                        ClientOption(
                            title = client.name,
                            subtitle = client.phone ?: "—",
                            icon = false,
                            active = selected?.id == client.id,
                            debt = client.debtBalance,
                            onClick = { selected = client }
                        )
                    }
                }

                // Tanlangan mijozda eski qarz bo'lsa — sotuvchiga aniq ogohlantirish
                selected?.let { c ->
                    if (c.hasDebt) {
                        Spacer(Modifier.height(12.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Jesko.Red.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, Jesko.Red.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = Jesko.Red, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Diqqat — eski qarz bor!", color = Jesko.Red, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Spacer(Modifier.weight(1f))
                                Text("${Format.money(c.debtBalance)} so'm", color = Jesko.Red, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Mijozga eslating: “Avvalgi qarzingiz ham bor — kassaga qo'shib to'lashingiz mumkin.” " +
                                        "Qarz tafsilotlarini (qachon, nima olgani) kassir aytib beradi.",
                                color = Jesko.TextSecondary, fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jami", color = Jesko.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${Format.money(ui.cartTotal)} so'm", color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }

                Spacer(Modifier.height(14.dp))

                Row {
                    JeskoButton("Bekor", onDismiss, style = JeskoButtonStyle.Outline, modifier = Modifier.weight(1f), enabled = !ui.sending)
                    Spacer(Modifier.width(10.dp))
                    JeskoButton(
                        text = "Yuborish",
                        onClick = {
                            val c = selected
                            // Qarzdor mijoz tanlangan bo'lsa — avval ogohlantirish ko'rsatamiz,
                            // sotuvchi "Tushunarli"ni bosgach buyurtma kassirga yuboriladi.
                            if (c != null && c.hasDebt) {
                                showDebtWarning = true
                            } else {
                                vm.sendOrder(selected) { ok -> if (ok) onSent() }
                            }
                        },
                        style = JeskoButtonStyle.Green,
                        loading = ui.sending,
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }

    // ── Qarzdor mijoz uchun ogohlantirish oynasi ──
    if (showDebtWarning) {
        val c = selected
        Dialog(onDismissRequest = { if (!ui.sending) showDebtWarning = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.Card, RoundedCornerShape(22.dp))
                    .border(1.dp, Jesko.Red.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(58.dp).background(Jesko.Red.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Jesko.Red, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Diqqat! Mijozda qarz bor", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    c?.name ?: "",
                    color = Jesko.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.Red.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Umumiy qarzi", color = Jesko.TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("${Format.money(c?.debtBalance ?: 0.0)} so'm", color = Jesko.Red, fontWeight = FontWeight.Black, fontSize = 26.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Mijozga eslating: avvalgi qarzini ham kassaga qo'shib to'lashi mumkin. " +
                            "Qarz tafsilotlarini (qachon, nima olgani) kassir aytib beradi.",
                    color = Jesko.TextSecondary, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(18.dp))
                JeskoButton(
                    text = "Tushunarli, kassirga yuborish",
                    onClick = {
                        vm.sendOrder(c) { ok ->
                            if (ok) {
                                showDebtWarning = false
                                onSent()
                            }
                        }
                    },
                    style = JeskoButtonStyle.Green,
                    loading = ui.sending,
                    modifier = Modifier.fillMaxWidth(),
                    height = 52
                )
                Spacer(Modifier.height(8.dp))
                JeskoButton(
                    text = "Ortga",
                    onClick = { if (!ui.sending) showDebtWarning = false },
                    style = JeskoButtonStyle.Outline,
                    enabled = !ui.sending,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ClientOption(
    title: String,
    subtitle: String,
    icon: Boolean,
    active: Boolean,
    debt: Double = 0.0,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (active) Jesko.Gold.copy(alpha = 0.14f) else Jesko.Input, RoundedCornerShape(12.dp))
            .border(1.dp, if (active) Jesko.Gold else Jesko.Border, RoundedCornerShape(12.dp))
            .clickableNoRipple(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).background(Jesko.CardElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon) {
                Icon(Icons.Filled.Money, null, tint = Jesko.Green, modifier = Modifier.size(20.dp))
            } else {
                Text(title.take(1).uppercase(), color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Jesko.TextSecondary, fontSize = 11.sp)
            // Qarzi bor mijozni darhol qizil belgi bilan ko'rsatamiz
            if (debt > 0.5) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .background(Jesko.Red.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        "⚠️ Qarz: ${Format.money(debt)} so'm",
                        color = Jesko.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (active) {
            Box(
                Modifier.size(24.dp).background(Jesko.Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, null, tint = Jesko.BgDark, modifier = Modifier.size(15.dp))
            }
        }
    }
}