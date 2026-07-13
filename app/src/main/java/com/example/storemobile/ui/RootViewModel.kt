package com.example.storemobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storemobile.data.Session
import com.example.storemobile.data.SessionManager
import com.example.storemobile.data.remote.ApiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RootState {
    data object Loading : RootState
    data object LoggedOut : RootState
    data class LoggedIn(val session: Session) : RootState
}

class RootViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionManager(app)

    private val _state = MutableStateFlow<RootState>(RootState.Loading)
    val state: StateFlow<RootState> = _state.asStateFlow()

    /** App-wide theme preference ("system" | "light" | "dark"). */
    val themeMode: StateFlow<String> = session.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionManager.THEME_SYSTEM
    )

    init {
        viewModelScope.launch {
            // Apply the saved server URL before any network call happens.
            ApiProvider.setBaseUrl(session.serverUrl.first())
            val auto = session.autoLoginSession.first()
            _state.value = if (auto != null) RootState.LoggedIn(auto) else RootState.LoggedOut
        }
    }

    /** Called after a successful explicit login on the LoginScreen. */
    fun onLoggedIn() {
        viewModelScope.launch {
            val saved = session.savedSession.first()
            if (saved != null) _state.value = RootState.LoggedIn(saved)
        }
    }

    fun onLoggedOut() {
        _state.value = RootState.LoggedOut
    }
}
