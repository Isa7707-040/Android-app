package com.example.storemobile.ui.seller

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.ui.components.clickableNoRipple
import com.example.storemobile.ui.theme.Jesko

private enum class SellerTab(val title: String, val icon: ImageVector) {
    Products("Mahsulotlar", Icons.Filled.Storefront),
    Cart("Savat", Icons.Filled.ShoppingCart),
    History("Sotuvlarim", Icons.AutoMirrored.Filled.ReceiptLong),
    Profile("Profil", Icons.Filled.Person)
}

@Composable
fun SellerScreen(
    vm: SellerViewModel,
    onLoggedOut: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(SellerTab.Products) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.toast) {
        ui.toast?.let {
            snackbar.showSnackbar(it)
            vm.consumeToast()
        }
    }
    LaunchedEffect(tab) {
        if (tab == SellerTab.History) vm.loadHistory()
    }

    Box(Modifier.fillMaxSize().background(Jesko.BgDark)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab"
                ) { current ->
                    when (current) {
                        SellerTab.Products -> ProductsTab(
                            vm = vm,
                            sellerName = vm.userName,
                            onOpenCart = { tab = SellerTab.Cart }
                        )
                        SellerTab.Cart -> CartTab(
                            vm = vm,
                            onBrowse = { tab = SellerTab.Products }
                        )
                        SellerTab.History -> HistoryTab(vm = vm)
                        SellerTab.Profile -> ProfileTab(vm = vm, onLoggedOut = onLoggedOut)
                    }
                }
            }
            BottomBar(
                selected = tab,
                cartCount = ui.cartCount,
                onSelect = { tab = it }
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 96.dp, start = 12.dp, end = 12.dp)
        )
    }
}

@Composable
private fun BottomBar(
    selected: SellerTab,
    cartCount: Int,
    onSelect: (SellerTab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.BgPanel)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SellerTab.entries.forEach { item ->
            BottomItem(
                item = item,
                active = item == selected,
                badge = if (item == SellerTab.Cart && cartCount > 0) cartCount else null,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(item) }
            )
        }
    }
}

@Composable
private fun BottomItem(
    item: SellerTab,
    active: Boolean,
    badge: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clickableNoRipple(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .height(34.dp)
                .width(64.dp)
                .background(
                    if (active) Jesko.Gold.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                item.title,
                tint = if (active) Jesko.GoldLight else Jesko.TextMuted,
                modifier = Modifier.size(23.dp)
            )
            if (badge != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                        .size(18.dp)
                        .background(Jesko.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badge > 9) "9+" else badge.toString(),
                        color = Jesko.TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
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
