package com.example.storemobile.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.data.remote.ApiProvider
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.JeskoButtonStyle
import com.example.storemobile.ui.components.JeskoThemeSelector
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

@Composable
fun ProfileTab(
    vm: SellerViewModel,
    onLoggedOut: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    // Tushum: naqd (Paid) va qarzga (Debt) sotilganlarning hammasi hisoblanadi.
    // Qarzga sotilgan bo'lsa ham — sotuvchi o'sha qiymatdagi tovarni sotgan.
    val sold = ui.history.filter { it.status == "Paid" || it.status == "Debt" }
    val initials = vm.userName.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text("Profil", color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        // Hero card
        Column(
            Modifier
                .fillMaxWidth()
                .background(Jesko.BrandGradient, RoundedCornerShape(20.dp))
                .border(1.dp, Jesko.Gold.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(84.dp).background(Jesko.CardElevated, CircleShape).border(2.5.dp, Jesko.Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 32.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(vm.userName, color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.background(Jesko.Gold.copy(alpha = 0.16f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Sotuvchi", color = Jesko.GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("Jami sotuvlar", sold.size.toString(), Jesko.Gold, Modifier.weight(1f))
            MiniStat("Tushum", Format.money(sold.sumOf { it.totalSum }), Jesko.Green, Modifier.weight(1.5f))
        }

        Spacer(Modifier.height(16.dp))

        Text("MA'LUMOT", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        InfoRow(Icons.Filled.Person, "Foydalanuvchi", vm.userName)
        Spacer(Modifier.height(8.dp))
        InfoRow(Icons.Filled.Badge, "ID raqami", "#${vm.userId}")
        Spacer(Modifier.height(8.dp))
        InfoRow(Icons.Filled.Dns, "Server", ApiProvider.baseUrl())
        Spacer(Modifier.height(8.dp))
        InfoRow(Icons.Filled.Info, "Ilova versiyasi", "JESKO Savdo v1.0")

        Spacer(Modifier.height(20.dp))

        Text("KO'RINISH", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        JeskoThemeSelector(current = themeMode, onSelect = { vm.setThemeMode(it) })

        Spacer(Modifier.height(24.dp))

        JeskoButton(
            text = "Hisobdan chiqish",
            onClick = { vm.logout(onLoggedOut) },
            style = JeskoButtonStyle.Danger,
            leadingIcon = Icons.AutoMirrored.Filled.Logout,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(label, color = Jesko.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 19.sp, maxLines = 1)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).background(Jesko.CardElevated, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Jesko.GoldLight, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Jesko.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Jesko.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
    }
}