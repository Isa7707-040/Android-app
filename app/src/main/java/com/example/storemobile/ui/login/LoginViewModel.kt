package com.example.storemobile.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storemobile.data.ApiResult
import com.example.storemobile.data.Session
import com.example.storemobile.data.SessionManager
import com.example.storemobile.data.StoreRepository
import com.example.storemobile.data.model.Seller
import com.example.storemobile.data.remote.ApiProvider
import com.example.storemobile.data.remote.ServerDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginUiState(
    val loadingSellers: Boolean = true,
    val sellers: List<Seller> = emptyList(),
    val selectedSeller: Seller? = null,
    val adminMode: Boolean = false,          // "Admin panel" oynasi ochiqmi
    val adminUsername: String = "admin",     // admin login (odatda "admin")
    val password: String = "",
    val rememberMe: Boolean = true,
    val loggingIn: Boolean = false,
    val error: String? = null,
    val serverUrl: String = ApiProvider.DEFAULT_BASE_URL,
    // Serverni avtomatik aniqlash holati
    val discovering: Boolean = false,
    val discoveryStatus: String? = null,
    val detectedUrl: String? = null
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StoreRepository()
    private val session = SessionManager(app)

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val url = session.serverUrl.first()
            ApiProvider.setBaseUrl(url)
            _ui.value = _ui.value.copy(serverUrl = ApiProvider.baseUrl())
            loadSellers()
        }
    }

    fun loadSellers() {
        _ui.value = _ui.value.copy(loadingSellers = true, error = null)
        viewModelScope.launch {
            // Login ro'yxatida FAQAT sotuvchilar ko'rinadi. Boshliq/admin esa
            // pastdagi "Admin panel" tugmasi orqali alohida parol bilan kiradi.
            when (val result = repo.getSellers()) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    loadingSellers = false,
                    sellers = result.data,
                    error = if (result.data.isEmpty()) "Serverda sotuvchilar topilmadi" else null
                )
                is ApiResult.Error -> _ui.value = _ui.value.copy(
                    loadingSellers = false,
                    sellers = emptyList(),
                    error = result.message
                )
            }
        }
    }

    fun selectSeller(seller: Seller) {
        _ui.value = _ui.value.copy(selectedSeller = seller, password = "", error = null)
    }

    fun clearSelection() {
        _ui.value = _ui.value.copy(selectedSeller = null, password = "", error = null)
    }

    /* ───────── Admin panel (boshliq) ───────── */

    fun openAdmin() {
        _ui.value = _ui.value.copy(adminMode = true, selectedSeller = null, password = "", error = null)
    }

    fun closeAdmin() {
        _ui.value = _ui.value.copy(adminMode = false, password = "", error = null)
    }

    fun setAdminUsername(value: String) {
        _ui.value = _ui.value.copy(adminUsername = value, error = null)
    }

    fun adminLogin(onSuccess: () -> Unit) {
        val state = _ui.value
        if (state.adminUsername.isBlank()) {
            _ui.value = state.copy(error = "Login kiriting (odatda: admin)")
            return
        }
        if (state.password.isBlank()) {
            _ui.value = state.copy(error = "Parolni kiriting")
            return
        }
        _ui.value = state.copy(loggingIn = true, error = null)
        viewModelScope.launch {
            when (val result = repo.login(state.adminUsername.trim(), state.password)) {
                is ApiResult.Success -> {
                    val r = result.data
                    session.saveSession(
                        Session(
                            userId = r.id,
                            userName = r.fullName.ifBlank { "Boshliq" },
                            userName2 = r.username.ifBlank { state.adminUsername.trim() },
                            // Server qaytargan rol (Admin/Accountant). Bo'sh bo'lsa Admin deb olamiz.
                            role = r.role.ifBlank { "Admin" }
                        ),
                        remember = state.rememberMe
                    )
                    _ui.value = _ui.value.copy(loggingIn = false)
                    onSuccess()
                }
                is ApiResult.Error -> _ui.value = _ui.value.copy(
                    loggingIn = false,
                    error = "Login yoki parol noto'g'ri"
                )
            }
        }
    }

    fun setPassword(value: String) {
        _ui.value = _ui.value.copy(password = value, error = null)
    }

    fun setRemember(value: Boolean) {
        _ui.value = _ui.value.copy(rememberMe = value)
    }

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            session.saveServerUrl(url)
            _ui.value = _ui.value.copy(serverUrl = ApiProvider.baseUrl())
            loadSellers()
        }
    }

    /**
     * Auto-detects the store server on the local Wi-Fi network and fills the
     * address in for the user. Does not save by itself — the user reviews the
     * found address and taps "Saqlash" (which persists it across restarts).
     */
    fun detectServer() {
        if (_ui.value.discovering) return
        _ui.value = _ui.value.copy(
            discovering = true,
            discoveryStatus = "Tarmoq tekshirilmoqda...",
            detectedUrl = null,
            error = null
        )
        viewModelScope.launch {
            val result = ServerDiscovery.discover()
            _ui.value = if (result != null) {
                _ui.value.copy(
                    discovering = false,
                    detectedUrl = result.baseUrl,
                    discoveryStatus = "✓ Server topildi: ${result.ip} — saqlash uchun \"Saqlash\"ni bosing."
                )
            } else {
                _ui.value.copy(
                    discovering = false,
                    detectedUrl = null,
                    discoveryStatus = "Server topilmadi. Telefon do'kon Wi-Fi'siga ulanganini tekshiring."
                )
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        val state = _ui.value
        val seller = state.selectedSeller
        if (seller == null) {
            _ui.value = state.copy(error = "Sotuvchini tanlang")
            return
        }
        if (state.password.isBlank()) {
            _ui.value = state.copy(error = "Parolni kiriting")
            return
        }
        _ui.value = state.copy(loggingIn = true, error = null)
        viewModelScope.launch {
            when (val result = repo.login(seller.username, state.password)) {
                is ApiResult.Success -> {
                    val r = result.data
                    session.saveSession(
                        Session(
                            userId = if (r.id != 0) r.id else seller.id,
                            userName = r.fullName.ifBlank { seller.fullName },
                            userName2 = r.username.ifBlank { seller.username },
                            role = r.role.ifBlank { "Seller" }
                        ),
                        remember = state.rememberMe
                    )
                    _ui.value = _ui.value.copy(loggingIn = false)
                    onSuccess()
                }
                is ApiResult.Error -> _ui.value = _ui.value.copy(
                    loggingIn = false,
                    error = "Parol noto'g'ri yoki server xatosi"
                )
            }
        }
    }
}