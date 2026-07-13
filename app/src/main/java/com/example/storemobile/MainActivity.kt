package com.example.storemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storemobile.data.SessionManager
import com.example.storemobile.ui.RootState
import com.example.storemobile.ui.RootViewModel
import com.example.storemobile.ui.boss.BossScreen
import com.example.storemobile.ui.boss.BossViewModel
import com.example.storemobile.ui.login.LoginScreen
import com.example.storemobile.ui.seller.SellerScreen
import com.example.storemobile.ui.seller.SellerViewModel
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.ui.theme.JeskoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        setContent {
            val rootVm: RootViewModel = viewModel()
            val themeMode by rootVm.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                SessionManager.THEME_LIGHT -> false
                SessionManager.THEME_DARK -> true
                else -> systemDark
            }

            JeskoTheme(darkTheme = darkTheme) {
                val state by rootVm.state.collectAsStateWithLifecycle()

                if (state !is RootState.Loading) keepSplash = false

                Box(Modifier.fillMaxSize().background(Jesko.BgDark)) {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "root"
                    ) { s ->
                        when (s) {
                            is RootState.Loading -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(color = Jesko.Gold) }

                            is RootState.LoggedOut -> LoginScreen(
                                onLoggedIn = { rootVm.onLoggedIn() }
                            )

                            is RootState.LoggedIn -> {
                                // Boshliq/Admin/Buxgalter -> offline qarz yig'ish (BossScreen).
                                // Sotuvchi -> odatdagi savdo ekrani (SellerScreen).
                                val role = s.session.role.trim().lowercase()
                                val isBoss = role == "admin" || role == "accountant" ||
                                        role == "boshliq" || role == "buxgalter"
                                if (isBoss) {
                                    val bossVm: BossViewModel = viewModel()
                                    bossVm.start(s.session.userName)
                                    BossScreen(
                                        vm = bossVm,
                                        onLoggedOut = { rootVm.onLoggedOut() }
                                    )
                                } else {
                                    val sellerVm: SellerViewModel = viewModel()
                                    sellerVm.start(s.session.userId, s.session.userName)
                                    SellerScreen(
                                        vm = sellerVm,
                                        onLoggedOut = { rootVm.onLoggedOut() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}