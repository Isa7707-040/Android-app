package com.example.storemobile.ui.boss

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.data.model.Client
import com.example.storemobile.data.model.DebtPaymentDto
import com.example.storemobile.data.model.PendingOperation
import com.example.storemobile.data.model.Supplier
import com.example.storemobile.data.model.SupplierPaymentDto
import com.example.storemobile.ui.components.EmptyState
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.JeskoButtonStyle
import com.example.storemobile.ui.components.JeskoTextField
import com.example.storemobile.ui.components.JeskoThemeSelector
import com.example.storemobile.ui.components.LoadingBox
import com.example.storemobile.ui.components.StatusPill
import com.example.storemobile.ui.components.clickableNoRipple
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

private enum class BossTab(val title: String, val icon: ImageVector) {
    Debtors("Qarzdorlar", Icons.Filled.Groups),
    Suppliers("Firmalar", Icons.Filled.LocalShipping),
    Sync("Sinxron", Icons.Filled.CloudSync)
}

/** Task #4: summa yashirilganda ko’rsatiladigan niqob (bank ilovalaridek). */
private fun maskedMoney(value: Double, masked: Boolean): String =
    if (masked) "••••••" else Format.money(value)

@Composable
fun BossScreen(
    vm: BossViewModel,
    onLoggedOut: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(BossTab.Debtors) }
    val snackbar = remember { SnackbarHostState() }

    // Toʼlov dialogi uchun tanlangan mijoz/firma
    var payClient by remember { mutableStateOf<Client?>(null) }
    var paySupplier by remember { mutableStateOf<Supplier?>(null) }

    LaunchedEffect(ui.toast) {
        ui.toast?.let {
            snackbar.showSnackbar(it)
            vm.consumeToast()
        }
    }

    Box(Modifier.fillMaxSize().background(Jesko.BgDark)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "bosstab"
                ) { current ->
                    when (current) {
                        BossTab.Debtors -> DebtorsTab(vm, ui, onPay = { payClient = it })
                        BossTab.Suppliers -> SuppliersTab(vm, ui, onPay = { paySupplier = it })
                        BossTab.Sync -> SyncTab(vm, ui, onLoggedOut = onLoggedOut)
                    }
                }
            }
            BossBottomBar(selected = tab, pendingCount = ui.pendingCount, onSelect = { tab = it })
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 96.dp, start = 12.dp, end = 12.dp)
        )
    }

    // ── Mijoz qarzini toʼlov dialogi ──
    payClient?.let { c ->
        PaymentDialog(
            title = "Qarz toʼovi",
            name = c.name,
            subtitle = c.phone ?: "—",
            currentDebt = c.debtBalance,
            accent = Jesko.Green,
            confirmLabel = "Qabul qilish",
            onDismiss = { payClient = null },
            onConfirm = { amount, note ->
                vm.payClient(c, amount, note) { ok -> if (ok) payClient = null }
            }
        )
    }

    // ── Firmaga toʼlov dialogi ──
    paySupplier?.let { s ->
        PaymentDialog(
            title = "Firmaga toʼlov",
            name = s.name,
            subtitle = s.phone ?: "—",
            currentDebt = s.debtBalance,
            accent = Jesko.Gold,
            confirmLabel = "Toʼlash",
            onDismiss = { paySupplier = null },
            onConfirm = { amount, note ->
                vm.paySupplier(s, amount, note) { ok -> if (ok) paySupplier = null }
            }
        )
    }

    // ── Qarz tarixi dialogi (mijoz yoki firma) ──
    if (ui.showHistory) {
        PaymentHistoryDialog(
            title = ui.historyName,
            loading = ui.historyLoading,
            error = ui.historyError,
            clientHistory = ui.clientHistory,
            supplierHistory = ui.supplierHistory,
            onDismiss = { vm.clearHistory() }
        )
    }
}

/* ────────────────────  QARZDORLAR  ───────────── */

@Composable
private fun DebtorsTab(vm: BossViewModel, ui: BossUiState, onPay: (Client) -> Unit) {
    val debtors = ui.filteredClients.filter { it.hasDebt }.sortedByDescending { it.debtBalance }

    // Task #4: hideAmounts endi ViewModel darajasida saqlanadi (global).
    // Shu sabab Firmalar boʼligiga oʼtsangiz ham, qaytib kelganda holat saqlanadi.
    val hideAmounts = ui.hideAmounts
    val revealed = remember { mutableStateMapOf<Int, Boolean>() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TabHeader(
            title = "Qarzdorlar",
            subtitle = "Mijozlardan qarz yigʼish (internetsiz ishlaydi)",
            trailing = {
                EyeToggle(
                    hidden = hideAmounts,
                    boxSize = 42,
                    iconSize = 22,
                    onClick = {
                        vm.toggleHideAmounts()
                        revealed.clear()
                    }
                )
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Umumiy qarz", maskedMoney(ui.totalClientDebt, hideAmounts), "soʼm", Jesko.Red, Modifier.weight(1.5f))
            StatCard("Yigʼildi (navbatda)", maskedMoney(ui.pendingClientTotal, hideAmounts), "soʼm", Jesko.Green, Modifier.weight(1.5f))
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            JeskoTextField(
                value = ui.clientSearch,
                onValueChange = vm::setClientSearch,
                placeholder = "Mijozni qidirish...",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            ui.loading -> LoadingBox()
            !ui.hasSnapshot -> EmptyState(
                title = "Maʼlu mot yoʼq",
                subtitle = "Avval doʼkon WiFiʼida \"Sinxron\" boʼimidan maʼlu motlarni yuklab oling.",
                icon = Icons.Filled.CloudSync
            )
            debtors.isEmpty() -> EmptyState(
                title = "Qarzdor mijoz yoʼq",
                subtitle = "Hamma qarzlar toʼlangan yoki qidiruvga mos mijoz topilmadi.",
                icon = Icons.Filled.Groups
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debtors, key = { it.id }) { c ->
                    val masked = hideAmounts && revealed[c.id] != true
                    DebtRow(
                        name = c.name,
                        subtitle = c.phone ?: "—",
                        debt = c.debtBalance,
                        pending = ui.pendingByClient[c.id] ?: 0.0,
                        accent = Jesko.Red,
                        masked = masked,
                        showEye = hideAmounts,
                        onToggleReveal = { revealed[c.id] = !(revealed[c.id] ?: false) },
                        onHistory = { vm.loadClientHistory(c) },
                        onClick = { onPay(c) }
                    )
                }
            }
        }
    }
}

/* ────────────────────  FIRMALAR  ───────────── */

@Composable
private fun SuppliersTab(vm: BossViewModel, ui: BossUiState, onPay: (Supplier) -> Unit) {
    val owed = ui.filteredSuppliers.filter { it.hasDebt }.sortedByDescending { it.debtBalance }

    // Task #4: hideAmounts endi ViewModel darajasida saqlanadi (global).
    val hideAmounts = ui.hideAmounts
    val revealed = remember { mutableStateMapOf<Int, Boolean>() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TabHeader(
            title = "Firmalar",
            subtitle = "Firmalarga qarzni toʼlash (internetsiz ishlaydi)",
            trailing = {
                EyeToggle(
                    hidden = hideAmounts,
                    boxSize = 42,
                    iconSize = 22,
                    onClick = {
                        vm.toggleHideAmounts()
                        revealed.clear()
                    }
                )
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Jami qarzimiz", maskedMoney(ui.totalSupplierDebt, hideAmounts), "soʼm", Jesko.Gold, Modifier.weight(1.5f))
            StatCard("Toʼlandi (navbatda)", maskedMoney(ui.pendingSupplierTotal, hideAmounts), "soʼm", Jesko.Green, Modifier.weight(1.5f))
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            JeskoTextField(
                value = ui.supplierSearch,
                onValueChange = vm::setSupplierSearch,
                placeholder = "Firmani qidirish...",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            ui.loading -> LoadingBox()
            !ui.hasSnapshot -> EmptyState(
                title = "Maʼlu mot yoʼq",
                subtitle = "Avval doʼkon WiFiʼida \"Sinxron\" boʼimidan maʼlu motlarni yuklab oling.",
                icon = Icons.Filled.CloudSync
            )
            owed.isEmpty() -> EmptyState(
                title = "Qarzimiz yoʼq firma",
                subtitle = "Barcha firmalarga toʼlov qilingan yoki qidiruvga mos firma topilmadi.",
                icon = Icons.Filled.LocalShipping
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(owed, key = { it.id }) { s ->
                    val masked = hideAmounts && revealed[s.id] != true
                    DebtRow(
                        name = s.name,
                        subtitle = s.phone ?: "—",
                        debt = s.debtBalance,
                        pending = ui.pendingBySupplier[s.id] ?: 0.0,
                        accent = Jesko.Gold,
                        masked = masked,
                        showEye = hideAmounts,
                        onToggleReveal = { revealed[s.id] = !(revealed[s.id] ?: false) },
                        onHistory = { vm.loadSupplierHistory(s) },
                        onClick = { onPay(s) }
                    )
                }
            }
        }
    }
}

/* ────────────────────  SINXRON  ───────────── */

@Composable
private fun SyncTab(vm: BossViewModel, ui: BossUiState, onLoggedOut: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TabHeader(title = "Sinxron", subtitle = "Doʼkon serveri bilan maʼlu motlarni tenglashtirish")

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.BrandGradient, RoundedCornerShape(20.dp))
                        .border(1.dp, Jesko.Gold.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(64.dp).background(Jesko.CardElevated, CircleShape)
                            .border(2.dp, Jesko.Gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Sync, null, tint = Jesko.GoldLight, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (ui.pendingCount > 0) "${ui.pendingCount} ta amal kutilmoqda" else "Hammasi sinxron",
                        color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Oxirgi sinxron: " + (if (ui.lastSyncIso.isBlank()) "hali boʼlmagan" else Format.dateTime(ui.lastSyncIso)),
                        color = Jesko.TextSecondary, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    JeskoButton(
                        text = if (ui.pendingCount > 0) "Sinxronlash (${ui.pendingCount})" else "Maʼlu motni yangilash",
                        onClick = { vm.syncNow() },
                        style = JeskoButtonStyle.Gold,
                        loading = ui.syncing,
                        leadingIcon = Icons.Filled.CloudSync,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Doʼkon WiFiʼiga ulangan holda bosing. Koʼchada (internetsiz) amallar telefonda saqlanadi.",
                        color = Jesko.TextMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (ui.syncSummary != null) {
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Jesko.Green.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, Jesko.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(ui.syncSummary!!, color = Jesko.GreenLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (ui.pending.isNotEmpty()) {
                item {
                    Text("KUTILAYOTGAN AMALLAR", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                items(ui.pending, key = { it.operationId }) { op ->
                    PendingRow(op = op, onCancel = { vm.cancelPending(op) })
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(Jesko.Card, RoundedCornerShape(12.dp))
                        .border(1.dp, Jesko.Border, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).background(Jesko.CardElevated, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            vm.userName.take(1).uppercase(),
                            color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(vm.userName, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Boshliq", color = Jesko.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            item {
                val themeMode by vm.themeMode.collectAsStateWithLifecycle()
                Text("KOʼRINISH", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                JeskoThemeSelector(current = themeMode, onSelect = { vm.setThemeMode(it) })
            }
            item {
                JeskoButton(
                    text = "Hisobdan chiqish",
                    onClick = { vm.logout(onLoggedOut) },
                    style = JeskoButtonStyle.Danger,
                    leadingIcon = Icons.AutoMirrored.Filled.Logout,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/* ────────────────────  TOʼLOV TARIXI DIALOGI  ───────────── */

/**
 * Mijoz yoki firma toʼlov tarixi dialogi.
 * Internet boʼlmasa [error] xato xabarini koʼrsatadi.
 * Yuklash jarayonida spinner koʼrinadi.
 * Har bir yozuv: sana, summa, qoldiq.
 */
@Composable
private fun PaymentHistoryDialog(
    title: String,
    loading: Boolean,
    error: String?,
    clientHistory: List<DebtPaymentDto>?,
    supplierHistory: List<SupplierPaymentDto>?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Jesko.Card, RoundedCornerShape(22.dp))
                .border(1.dp, Jesko.Border, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            // Sarlavha
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.History, null,
                    tint = Jesko.GoldLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Toʼlov tarixi", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    if (title.isNotBlank())
                        Text(title, color = Jesko.GoldLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier.size(32.dp).background(Jesko.Input, CircleShape)
                        .clickableNoRipple(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, "yopish", tint = Jesko.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Jesko.Gold, modifier = Modifier.size(36.dp))
                    }
                }
                error != null -> {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Jesko.Red.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, Jesko.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            "⚠️ $error",
                            color = Jesko.Red, fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Server bilan aloqa boʼling va qaytadan urinib koʼring.",
                        color = Jesko.TextMuted, fontSize = 11.sp
                    )
                }
                clientHistory != null -> HistoryList(
                    items = clientHistory.map { p ->
                        HistoryEntry(
                            date = Format.dateTime(p.paidAt),
                            amount = "+ ${Format.money(p.amount)} soʼm",
                            remaining = "Qoldiq: ${Format.money(p.remainingAfter)} soʼm",
                            amountColor = Jesko.GreenLight,
                            note = p.note
                        )
                    },
                    emptyText = "Bu mijozdan hali toʼlov olinmagan."
                )
                supplierHistory != null -> HistoryList(
                    items = supplierHistory.map { p ->
                        HistoryEntry(
                            date = Format.dateTime(p.paidAt),
                            amount = "− ${Format.money(p.amount)} soʼm",
                            remaining = "Qoldiq: ${Format.money(p.remainingAfter)} soʼm",
                            amountColor = Jesko.Gold,
                            note = p.note
                        )
                    },
                    emptyText = "Bu firmaga hali toʼlov qilinmagan."
                )
            }

            Spacer(Modifier.height(14.dp))
            JeskoButton(
                text = "Yopish",
                onClick = onDismiss,
                style = JeskoButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class HistoryEntry(
    val date: String,
    val amount: String,
    val remaining: String,
    val amountColor: Color,
    val note: String?
)

@Composable
private fun HistoryList(items: List<HistoryEntry>, emptyText: String) {
    if (items.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(emptyText, color = Jesko.TextMuted, fontSize = 13.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        // Dialog ichida uchun chegaralangan balandlik
        userScrollEnabled = true
    ) {
        items(items) { entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(10.dp))
                    .border(1.dp, Jesko.Border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.date, color = Jesko.TextSecondary, fontSize = 11.sp)
                    if (!entry.note.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text("📝 ${entry.note}", color = Jesko.TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(entry.amount, color = entry.amountColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(entry.remaining, color = Jesko.TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

/* ────────────────────  TOʼLOV DIALOGI  ───────────── */

@Composable
private fun PaymentDialog(
    title: String,
    name: String,
    subtitle: String,
    currentDebt: Double,
    accent: Color,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val amount = amountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val valid = amount > 0
    val remaining = (currentDebt - amount).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Jesko.Card, RoundedCornerShape(22.dp))
                .border(1.dp, Jesko.Border, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Text(title, color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Spacer(Modifier.height(2.dp))
            Text(name, color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Jesko.TextSecondary, fontSize = 12.sp)

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Joriy qarz", color = Jesko.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("${Format.money(currentDebt)} soʼm", color = Jesko.Red, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }

            Spacer(Modifier.height(12.dp))

            JeskoTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                placeholder = "Toʼlov summasi (soʼm)",
                leadingIcon = Icons.Filled.AccountBalanceWallet,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JeskoButton(
                    text = "Toʼiq qarz",
                    onClick = { amountText = Math.round(currentDebt).toString() },
                    style = JeskoButtonStyle.Outline,
                    height = 44,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            JeskoTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Izoh (ixtiyoriy)",
                modifier = Modifier.fillMaxWidth()
            )

            if (valid) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Toʼlovdan keyin qoladi", color = Jesko.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${Format.money(remaining)} soʼm", color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row {
                JeskoButton("Bekor", onDismiss, style = JeskoButtonStyle.Outline, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                JeskoButton(
                    text = confirmLabel,
                    onClick = { if (valid) onConfirm(amount, note.ifBlank { null }) },
                    style = JeskoButtonStyle.Green,
                    enabled = valid,
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

/* ────────────────────  UMUMIY KOMPONENTLAR  ───────────── */

@Composable
private fun TabHeader(title: String, subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(subtitle, color = Jesko.TextSecondary, fontSize = 13.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/** Task #4: koz tugmasi — ochiq koz = koʼrinadi, yopiq koz = yashirilgan. */
@Composable
private fun EyeToggle(hidden: Boolean, boxSize: Int, iconSize: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(boxSize.dp)
            .background(Jesko.CardElevated, CircleShape)
            .border(1.dp, Jesko.Border, CircleShape)
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = if (hidden) "Koʼrsatish" else "Yashirish",
            tint = if (hidden) Jesko.GoldLight else Jesko.TextSecondary,
            modifier = Modifier.size(iconSize.dp)
        )
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
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(3.dp))
            Text(unit, color = Jesko.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

/**
 * Qarz qatori — har bir mijoz/firma uchun.
 * [onHistory] null boʼlmasa, oʼng tomonida “Tarix” tugmasi koʼrinadi.
 */
@Composable
private fun DebtRow(
    name: String,
    subtitle: String,
    debt: Double,
    pending: Double,
    accent: Color,
    masked: Boolean = false,
    showEye: Boolean = false,
    onToggleReveal: (() -> Unit)? = null,
    onHistory: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .clickableNoRipple(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).background(Jesko.CardElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Jesko.TextSecondary, fontSize = 11.sp)
            if (pending > 0.5) {
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    if (masked) "Navbatda: •••• soʼm" else "Navbatda: ${Format.money(pending)} soʼm",
                    Jesko.Green
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(maskedMoney(debt, masked), color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("soʼm", color = Jesko.TextMuted, fontSize = 10.sp)
        }
        // Vaqtincha koʼrsatish tugmasi (yashirish rejimida)
        if (showEye && onToggleReveal != null) {
            Spacer(Modifier.width(6.dp))
            EyeToggle(hidden = masked, boxSize = 32, iconSize = 17, onClick = onToggleReveal)
        }
        // Tarix tugmasi
        if (onHistory != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(32.dp)
                    .background(Jesko.CardElevated, CircleShape)
                    .border(1.dp, Jesko.Border, CircleShape)
                    .clickableNoRipple(onClick = onHistory),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.History, "Tarix",
                    tint = Jesko.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingRow(op: PendingOperation, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(op.entityName, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                (if (op.isSupplier) "Firmaga toʼlov" else "Qarz toʼovi") +
                        " · " + Format.dateTime(op.createdAt),
                color = Jesko.TextSecondary, fontSize = 11.sp
            )
        }
        Text("${Format.money(op.amount)} soʼm", color = Jesko.GreenLight, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(30.dp).background(Jesko.Input, CircleShape).clickableNoRipple(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, "bekor qilish", tint = Jesko.Red, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BossBottomBar(selected: BossTab, pendingCount: Int, onSelect: (BossTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.BgPanel)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BossTab.entries.forEach { item ->
            BossBottomItem(
                item = item,
                active = item == selected,
                badge = if (item == BossTab.Sync && pendingCount > 0) pendingCount else null,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(item) }
            )
        }
    }
}

@Composable
private fun BossBottomItem(
    item: BossTab,
    active: Boolean,
    badge: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clickableNoRipple(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.height(34.dp).width(64.dp).background(
                if (active) Jesko.Gold.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(18.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon, item.title,
                tint = if (active) Jesko.GoldLight else Jesko.TextMuted,
                modifier = Modifier.size(23.dp)
            )
            if (badge != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(end = 8.dp).size(18.dp)
                        .background(Jesko.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badge > 9) "9+" else badge.toString(),
                        color = Jesko.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.title,
            color = if (active) Jesko.GoldLight else Jesko.TextMuted,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}
