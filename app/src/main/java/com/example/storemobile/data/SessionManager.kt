package com.example.storemobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.storemobile.data.remote.ApiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jesko_session")

/** Persisted, logged-in seller. */
data class Session(
    val userId: Int,
    val userName: String,
    val userName2: String, // username (login)
    val role: String
)

/**
 * Stores the logged-in seller and the configured server URL so the app does
 * not show the login screen on every launch.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val USER_ID = intPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
        val REMEMBER = booleanPreferencesKey("remember")
        val SERVER_URL = stringPreferencesKey("server_url")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SERVER_URL] ?: ApiProvider.DEFAULT_BASE_URL
    }

    /** Theme preference: "system" | "light" | "dark". Defaults to system. */
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: THEME_SYSTEM
    }

    /** Returns the saved seller only when "remember me" was enabled — used for auto-login on cold start. */
    val autoLoginSession: Flow<Session?> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID] ?: -1
        val remember = prefs[Keys.REMEMBER] ?: false
        if (id != -1 && remember) buildSession(prefs, id) else null
    }

    /** Returns whatever seller is currently saved, regardless of remember — used right after an explicit login. */
    val savedSession: Flow<Session?> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID] ?: -1
        if (id != -1) buildSession(prefs, id) else null
    }

    private fun buildSession(
        prefs: androidx.datastore.preferences.core.Preferences,
        id: Int
    ): Session = Session(
        userId = id,
        userName = prefs[Keys.USER_NAME] ?: "Sotuvchi",
        userName2 = prefs[Keys.USERNAME] ?: "",
        role = prefs[Keys.ROLE] ?: "Seller"
    )

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_URL] = url }
        ApiProvider.setBaseUrl(url)
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun saveSession(session: Session, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USER_NAME] = session.userName
            prefs[Keys.USERNAME] = session.userName2
            prefs[Keys.ROLE] = session.role
            prefs[Keys.REMEMBER] = remember
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.ROLE)
            prefs[Keys.REMEMBER] = false
        }
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
}
