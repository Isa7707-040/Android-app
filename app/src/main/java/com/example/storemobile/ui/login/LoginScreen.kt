package com.example.storemobile.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storemobile.data.model.Seller
import com.example.storemobile.ui.components.EmptyState
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.JeskoButtonStyle
import com.example.storemobile.ui.components.JeskoLogo
import com.example.storemobile.ui.components.JeskoTextField
import com.example.storemobile.ui.components.LoadingBox
import com.example.storemobile.ui.theme.Jesko
import androidx.compose.material.icons.filled.SentimentDissatisfied

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showServerDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Jesko.BgDark)) {

        // Decorative gradient header
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Jesko.BrandGradient)
        )

        Column(Modifier.fillMaxSize()) {

            // ── Top bar: logo + server settings ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 12.dp, top = 46.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JeskoLogo(size = 52, showWordmark = true)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Jesko.CardElevated, CircleShape)
                        .border(1.dp, Jesko.Border, CircleShape)
                        .clickable { showServerDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Settings, "Server sozlamasi", tint = Jesko.TextSecondary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            AnimatedContent(
                targetState = when {
                    ui.adminMode -> LoginPhase.Admin
                    ui.selectedSeller != null -> LoginPhase.Password
                    else -> LoginPhase.Picker
                },
                transitionSpec = {
                    (slideInVertically { it / 6 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 6 } + fadeOut())
                },
                label = "loginPhase"
            ) { phase ->
                when (phase) {
                    LoginPhase.Picker -> SellerPicker(
                        ui = ui,
                        onSelect = vm::selectSeller,
                        onRetry = vm::loadSellers,
                        onAdmin = vm::openAdmin
                    )
                    LoginPhase.Password -> PasswordPanel(
                        seller = ui.selectedSeller ?: Seller(0, "", ""),
                        ui = ui,
                        onBack = vm::clearSelection,
                        onPassword = vm::setPassword,
                        onRemember = vm::setRemember,
                        onLogin = { vm.login(onLoggedIn) }
                    )
                    LoginPhase.Admin -> AdminLoginPanel(
                        ui = ui,
                        onBack = vm::closeAdmin,
                        onUsername = vm::setAdminUsername,
                        onPassword = vm::setPassword,
                        onRemember = vm::setRemember,
                        onLogin = { vm.adminLogin(onLoggedIn) }
                    )
                }
            }
        }
    }

    if (showServerDialog) {
        ServerDialog(
            current = ui.serverUrl,
            discovering = ui.discovering,
            status = ui.discoveryStatus,
            detectedUrl = ui.detectedUrl,
            onDetect = { vm.detectServer() },
            onDismiss = { showServerDialog = false },
            onSave = {
                vm.setServerUrl(it)
                showServerDialog = false
            }
        )
    }
}

/* ───────────────────────  SELLER PICKER  ─────────────────────── */

private enum class LoginPhase { Picker, Password, Admin }

@Composable
private fun SellerPicker(
    ui: LoginUiState,
    onSelect: (Seller) -> Unit,
    onRetry: () -> Unit,
    onAdmin: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("Xush kelibsiz", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Davom etish uchun xodimni tanlang",
            color = Jesko.TextSecondary, fontSize = 14.sp
        )
        Spacer(Modifier.height(18.dp))

        // Ro'yxat maydoni (qolgan balandlikni egallaydi)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                ui.loadingSellers -> LoadingBox()
                ui.sellers.isEmpty() -> {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(
                            title = ui.error ?: "Sotuvchilar topilmadi",
                            subtitle = "Server manzilini tekshiring (yuqori o'ngdagi ⚙) yoki qayta urinib ko'ring.",
                            icon = Icons.Filled.SentimentDissatisfied
                        )
                        JeskoButton(
                            text = "Qayta urinish",
                            onClick = onRetry,
                            style = JeskoButtonStyle.Outline,
                            leadingIcon = Icons.Filled.Refresh,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ui.sellers, key = { it.id }) { seller ->
                            SellerRow(seller = seller, onClick = { onSelect(seller) })
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }

        // ── Pastki burchakda: Admin panelga kirish ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .background(Jesko.Card, RoundedCornerShape(16.dp))
                .border(1.dp, Jesko.Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable(onClick = onAdmin)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).background(Jesko.CardElevated, CircleShape)
                    .border(1.dp, Jesko.Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, null, tint = Jesko.GoldLight, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Admin panel", color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Boshliq sifatida parol bilan kirish", color = Jesko.TextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Jesko.GoldLight)
        }
    }
}

@Composable
private fun SellerRow(seller: Seller, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(16.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(seller.fullName, size = 48)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(seller.fullName, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text("@${seller.username}", color = Jesko.TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Jesko.TextMuted)
    }
}

/* ───────────────────────  PASSWORD PANEL  ─────────────────────── */

@Composable
private fun PasswordPanel(
    seller: Seller,
    ui: LoginUiState,
    onBack: () -> Unit,
    onPassword: (String) -> Unit,
    onRemember: (Boolean) -> Unit,
    onLogin: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Jesko.CardElevated, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga", tint = Jesko.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Boshqa xodim", color = Jesko.TextSecondary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(22.dp))

        // Selected seller hero
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(seller.fullName, size = 86, ring = true)
            Spacer(Modifier.height(14.dp))
            Text(seller.fullName, color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("@${seller.username}", color = Jesko.TextSecondary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(28.dp))

        Text("PAROL", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        JeskoTextField(
            value = ui.password,
            onValueChange = onPassword,
            placeholder = "Parolingizni kiriting",
            leadingIcon = Icons.Filled.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Icon(
                    if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    "ko'rsatish",
                    tint = Jesko.TextMuted,
                    modifier = Modifier.size(20.dp).clickable { visible = !visible }
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth().clickable { onRemember(!ui.rememberMe) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBox(checked = ui.rememberMe)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Meni eslab qol", color = Jesko.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Keyingi safar to'g'ridan-to'g'ri kiramiz", color = Jesko.TextMuted, fontSize = 11.sp)
            }
        }

        if (ui.error != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.Red.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, Jesko.Red.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("⚠  ${ui.error}", color = Jesko.Red, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(22.dp))

        JeskoButton(
            text = "Kirish",
            onClick = onLogin,
            loading = ui.loggingIn,
            modifier = Modifier.fillMaxWidth(),
            height = 54
        )
    }
}

/* ───────────────────────  ADMIN LOGIN PANEL  ─────────────────────── */

@Composable
private fun AdminLoginPanel(
    ui: LoginUiState,
    onBack: () -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRemember: (Boolean) -> Unit,
    onLogin: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Jesko.CardElevated, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga", tint = Jesko.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Xodimlar ro'yxati", color = Jesko.TextSecondary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(22.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(86.dp).background(Jesko.CardElevated, CircleShape)
                    .border(2.5.dp, Jesko.Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, null, tint = Jesko.GoldLight, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Admin panel", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("Boshliq / administrator kirishi", color = Jesko.TextSecondary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(26.dp))

        Text("LOGIN", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        JeskoTextField(
            value = ui.adminUsername,
            onValueChange = onUsername,
            placeholder = "admin",
            leadingIcon = Icons.Filled.Lock,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("PAROL", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        JeskoTextField(
            value = ui.password,
            onValueChange = onPassword,
            placeholder = "Parolingizni kiriting",
            leadingIcon = Icons.Filled.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Icon(
                    if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    "ko'rsatish",
                    tint = Jesko.TextMuted,
                    modifier = Modifier.size(20.dp).clickable { visible = !visible }
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth().clickable { onRemember(!ui.rememberMe) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBox(checked = ui.rememberMe)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Meni eslab qol", color = Jesko.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Ilova qayta ochilsa to'g'ridan-to'g'ri admin panel ochiladi", color = Jesko.TextMuted, fontSize = 11.sp)
            }
        }

        if (ui.error != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.Red.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, Jesko.Red.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("⚠  ${ui.error}", color = Jesko.Red, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(22.dp))

        JeskoButton(
            text = "Admin panelga kirish",
            onClick = onLogin,
            loading = ui.loggingIn,
            style = JeskoButtonStyle.Gold,
            leadingIcon = Icons.Filled.Lock,
            modifier = Modifier.fillMaxWidth(),
            height = 54
        )
    }
}

/* ───────────────────────  HELPERS  ─────────────────────── */

@Composable
private fun Avatar(name: String, size: Int, ring: Boolean = false) {
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    Box(
        Modifier
            .size(size.dp)
            .background(Jesko.CardElevated, CircleShape)
            .then(if (ring) Modifier.border(2.5.dp, Jesko.Gold, CircleShape) else Modifier.border(1.dp, Jesko.Border, CircleShape)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = Jesko.GoldLight,
            fontWeight = FontWeight.Black,
            fontSize = (size / 2.6f).sp
        )
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    Box(
        Modifier
            .size(24.dp)
            .background(if (checked) Jesko.Gold else Jesko.Input, RoundedCornerShape(7.dp))
            .border(1.5.dp, if (checked) Jesko.Gold else Jesko.InputBorder, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text("✓", color = Jesko.BgDark, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ServerDialog(
    current: String,
    discovering: Boolean,
    status: String?,
    detectedUrl: String?,
    onDetect: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(current) }
    // Avtomatik aniqlash topgan manzilni maydonga yozamiz.
    LaunchedEffect(detectedUrl) {
        detectedUrl?.let { text = it }
    }
    val foundOk = status?.startsWith("✓") == true

    AlertDialog(
        containerColor = Jesko.Card,
        onDismissRequest = onDismiss,
        title = { Text("Server manzili", color = Jesko.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Server o'rnatilgan kompyuter manzili. Bilmasangiz \"Avtomatik aniqlash\"ni bosing — " +
                            "ilova do'kon Wi-Fi'sidagi serverni o'zi topadi.",
                    color = Jesko.TextSecondary, fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))

                // ── Avtomatik aniqlash tugmasi ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.Gold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.dp, Jesko.Gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable(enabled = !discovering, onClick = onDetect)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (discovering) {
                        CircularProgressIndicator(
                            color = Jesko.GoldLight,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.Filled.Wifi, null, tint = Jesko.GoldLight, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (discovering) "Qidirilmoqda..." else "Serverni avtomatik aniqlash",
                            color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                        Text(
                            "Tarmoqdan serverni o'zi topadi",
                            color = Jesko.TextSecondary, fontSize = 11.sp
                        )
                    }
                }

                // ── Aniqlash natijasi ──
                if (status != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        status,
                        color = if (foundOk) Jesko.GreenLight else Jesko.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (foundOk) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text("YOKI QO'LDA KIRITING", color = Jesko.TextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                JeskoTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "http://192.168.1.50:5050/",
                    keyboardType = KeyboardType.Uri,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Eslatma: http:// va 5050-port (server shu portda ishlaydi). " +
                            "Emulyator uchun http://10.0.2.2:5050/.",
                    color = Jesko.TextMuted, fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Saqlash", color = Jesko.Gold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor", color = Jesko.TextSecondary)
            }
        }
    )
}