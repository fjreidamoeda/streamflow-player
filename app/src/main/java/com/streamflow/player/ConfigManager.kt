package com.streamflow.player

import android.content.Context
import android.content.SharedPreferences

data class AppConfig(
    val appName: String = "StreamFlow",
    val packageName: String = "",
    val dns: String = "",
    val baseUrl: String = "",
    val token: String = "",
    val logoUrl: String? = null,
    val backgroundUrl: String? = null,
    val appConfig: Map<String, Any>? = null
)

class ConfigManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("streamflow_config", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) = prefs.edit().putString("token", value).apply()

    var panelUrl: String
        get() = prefs.getString("panel_url", "") ?: ""
        set(value) = prefs.edit().putString("panel_url", value).apply()

    var appName: String
        get() = prefs.getString("app_name", "StreamFlow") ?: "StreamFlow"
        set(value) = prefs.edit().putString("app_name", value).apply()

    var dnsUrl: String
        get() = prefs.getString("dns_url", "") ?: ""
        set(value) = prefs.edit().putString("dns_url", value).apply()

    var logoUrl: String
        get() = prefs.getString("logo_url", "") ?: ""
        set(value) = prefs.edit().putString("logo_url", value).apply()

    var backgroundUrl: String
        get() = prefs.getString("background_url", "") ?: ""
        set(value) = prefs.edit().putString("background_url", value).apply()

    val isConfigured: Boolean
        get() = token.isNotBlank() && panelUrl.isNotBlank()

    fun saveConfig(config: AppConfig) {
        token = config.token
        panelUrl = config.dns
        appName = config.appName
        dnsUrl = config.dns
        logoUrl = config.logoUrl ?: ""
        backgroundUrl = config.backgroundUrl ?: ""
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
