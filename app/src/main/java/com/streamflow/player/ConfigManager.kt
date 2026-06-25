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

    var panelUrl: String
        get() = prefs.getString("panel_url", "") ?: ""
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

    val isConfigured: Boolean
        get() = panelUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
