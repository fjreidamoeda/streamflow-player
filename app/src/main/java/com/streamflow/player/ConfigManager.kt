package com.streamflow.player

import android.content.Context
import android.content.SharedPreferences

data class UserInfo(
    val username: String = "",
    val password: String = "",
    val auth: Int = 0,
    val status: String = "",
    val expDate: String = "",
    val isTrial: String = "0",
    val maxConnections: String = "1",
    val activeCons: String = "0",
    val message: String = ""
)

class ConfigManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("streamflow_config", Context.MODE_PRIVATE)

    private var storedPanelUrl: String
        get() = prefs.getString("panel_url", "https://streamflow.totalmente.online") ?: "https://streamflow.totalmente.online"
        set(value) = prefs.edit().putString("panel_url", value.trimEnd('/')).apply()

    var panelUrl: String
        get() = customPanelUrl.ifBlank { storedPanelUrl }
        set(value) = prefs.edit().putString("panel_url", value.trimEnd('/')).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(value) = prefs.edit().putString("password", value).apply()

    var appName: String
        get() = prefs.getString("app_name", "StreamFlow") ?: "StreamFlow"
        set(value) = prefs.edit().putString("app_name", value).apply()

    var playerType: String
        get() = prefs.getString("player_type", "internal") ?: "internal"
        set(value) = prefs.edit().putString("player_type", value).apply()

    var streamFormat: String
        get() = prefs.getString("stream_format", "") ?: ""
        set(value) = prefs.edit().putString("stream_format", value).apply()

    private val TOKEN_PREFIX = "047a416986f5309df000da276e674"

    var tokenSuffix: String
        get() = prefs.getString("token_suffix", "4a") ?: "4a"
        set(value) = prefs.edit().putString("token_suffix", value).apply()

    var token: String
        get() {
            val v = prefs.getString("setup_token", "") ?: ""
            if (v.isNotBlank()) return v
            val suffix = tokenSuffix
            return if (suffix.isNotBlank()) "$TOKEN_PREFIX$suffix" else ""
        }
        set(value) = prefs.edit().putString("setup_token", value).apply()

    var gamesUrl: String
        get() {
            val v = prefs.getString("games_url", "") ?: ""
            if (v.isNotBlank()) return v
            val panel = panelUrl
            return if (panel.isNotBlank()) "${panel.trimEnd('/')}/jogos.html" else "https://streamflow.totalmente.online/jogos.html"
        }
        set(value) = prefs.edit().putString("games_url", value).apply()

    var introVideoUrl: String
        get() {
            val v = prefs.getString("intro_video_url", "") ?: ""
            if (v.isNotBlank()) return v
            val panel = panelUrl
            return if (panel.isNotBlank()) "${panel.trimEnd('/')}/intro.mp4" else ""
        }
        set(value) = prefs.edit().putString("intro_video_url", value).apply()

    var customPanelUrl: String
        get() = prefs.getString("custom_panel_url", "") ?: ""
        set(value) = prefs.edit().putString("custom_panel_url", value.trimEnd('/')).apply()

    var logoUrl: String
        get() = prefs.getString("logo_url", "") ?: ""
        set(value) = prefs.edit().putString("logo_url", value).apply()

    var backgroundUrl: String
        get() = prefs.getString("background_url", "") ?: ""
        set(value) = prefs.edit().putString("background_url", value).apply()

    val isConfigured: Boolean
        get() = panelUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
