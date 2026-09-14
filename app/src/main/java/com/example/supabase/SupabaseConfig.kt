package com.example.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SupabaseConfig {
    private const val PREFS_CONFIG = "supabase_config_prefs"
    private const val KEY_CUSTOM_URL = "custom_supabase_url"
    private const val KEY_CUSTOM_ANON_KEY = "custom_supabase_anon_key"

    const val DEFAULT_BASE_URL = "https://micbhfqvlqugylqsevhd.supabase.co"
    const val DEFAULT_ANON_KEY = "sb_publishable_Q_LvPoJfYflKRR_P2QhFhg_a_M9jDw0"

    private var prefs: SharedPreferences? = null

    private val _currentBaseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val currentBaseUrl: StateFlow<String> = _currentBaseUrl.asStateFlow()

    private val _currentAnonKey = MutableStateFlow(DEFAULT_ANON_KEY)
    val currentAnonKey: StateFlow<String> = _currentAnonKey.asStateFlow()

    /**
     * Limpia y normaliza cualquier formato de URL de Supabase para evitar el error
     * "Invalid path specified in request URL" (PGRST125).
     * Elimina /rest/v1, /auth/v1, /rest, /auth y barras diagonales finales.
     */
    fun cleanUrl(raw: String): String {
        var u = raw.trim()
        if (u.isEmpty() || u == "MY_SUPABASE_URL") {
            return DEFAULT_BASE_URL
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://$u"
        }
        u = u.trimEnd('/')
        while (u.endsWith("/rest/v1") || u.endsWith("/auth/v1") || u.endsWith("/rest") || u.endsWith("/auth")) {
            u = when {
                u.endsWith("/rest/v1") -> u.removeSuffix("/rest/v1")
                u.endsWith("/auth/v1") -> u.removeSuffix("/auth/v1")
                u.endsWith("/rest") -> u.removeSuffix("/rest")
                u.endsWith("/auth") -> u.removeSuffix("/auth")
                else -> u
            }.trimEnd('/')
        }
        return u
    }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
            loadConfig()
        }
    }

    private fun loadConfig() {
        val p = prefs
        val savedUrl = p?.getString(KEY_CUSTOM_URL, null)
        val savedKey = p?.getString(KEY_CUSTOM_ANON_KEY, null)

        val buildUrl = try {
            val u = BuildConfig.SUPABASE_URL
            if (u.isNotBlank() && u != "MY_SUPABASE_URL") u else null
        } catch (_: Exception) { null }

        val buildKey = try {
            val k = BuildConfig.SUPABASE_ANON_KEY
            if (k.isNotBlank() && k != "MY_SUPABASE_ANON_KEY") k else null
        } catch (_: Exception) { null }

        val rawUrl = savedUrl ?: buildUrl ?: DEFAULT_BASE_URL
        val finalUrl = cleanUrl(rawUrl)
        val finalKey = (savedKey ?: buildKey ?: DEFAULT_ANON_KEY).trim()

        _currentBaseUrl.value = finalUrl
        _currentAnonKey.value = finalKey
    }

    fun updateCredentials(url: String, key: String) {
        val cleaned = cleanUrl(url)
        val trimmedKey = key.trim().ifEmpty { DEFAULT_ANON_KEY }
        prefs?.edit()
            ?.putString(KEY_CUSTOM_URL, cleaned)
            ?.putString(KEY_CUSTOM_ANON_KEY, trimmedKey)
            ?.apply()
        _currentBaseUrl.value = cleaned
        _currentAnonKey.value = trimmedKey
    }

    fun resetToDefaults() {
        prefs?.edit()?.clear()?.apply()
        loadConfig()
    }

    val baseUrl: String get() = _currentBaseUrl.value
    val anonKey: String get() = _currentAnonKey.value

    val restUrl: String get() = "$baseUrl/rest/v1"
    val authUrl: String get() = "$baseUrl/auth/v1"
}
