package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SetupActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var etToken: EditText
    private lateinit var etPanelUrl: EditText
    private lateinit var btnConnect: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        configManager = ConfigManager(this)

        etToken = findViewById(R.id.etToken)
        etPanelUrl = findViewById(R.id.etPanelUrl)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        if (configManager.token.isNotBlank()) {
            etToken.setText(configManager.token)
        }
        if (configManager.panelUrl.isNotBlank()) {
            etPanelUrl.setText(configManager.panelUrl)
        }

        // Auto-connect if both token and panel URL are pre-configured
        if (configManager.token.isNotBlank() && configManager.panelUrl.isNotBlank()) {
            btnConnect.post { btnConnect.performClick() }
        }

        btnConnect.setOnClickListener {
            val token = etToken.text.toString().trim()
            var panelUrl = etPanelUrl.text.toString().trim()

            if (token.isBlank()) {
                tvStatus.text = "Informe o token de configuracao"
                tvStatus.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            if (panelUrl.isBlank()) {
                panelUrl = configManager.panelUrl
            }
            if (panelUrl.isBlank()) {
                tvStatus.text = "Informe a URL do painel"
                tvStatus.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            tvStatus.visibility = TextView.GONE
            progressBar.visibility = ProgressBar.VISIBLE
            btnConnect.isEnabled = false

            val finalPanelUrl = panelUrl
            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    fetchConfig(finalPanelUrl, token)
                }

                progressBar.visibility = ProgressBar.GONE
                btnConnect.isEnabled = true

                result?.let { config ->
                    configManager.token = token
                    configManager.panelUrl = finalPanelUrl
                    configManager.gamesUrl = config.gamesUrl
                    configManager.introVideoUrl = config.introVideoUrl
                    configManager.customPanelUrl = config.customPanelUrl
                    configManager.appName = config.appName
                    configManager.logoUrl = config.logoUrl
                    configManager.backgroundUrl = config.backgroundUrl

                    startActivity(Intent(this@SetupActivity, LoginActivity::class.java))
                    finish()
                } ?: run {
                    tvStatus.text = "Token invalido ou sem conexao com o painel"
                    tvStatus.visibility = TextView.VISIBLE
                }
            }
        }
    }

    private fun fetchConfig(panelUrl: String, token: String): ConfigResult? {
        return try {
            val base = panelUrl.trimEnd('/')
            val urlStr = "$base/api/app_config.php?token=$token"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                if (json.optBoolean("success", false)) {
                    return ConfigResult(
                        dns = json.optString("dns", ""),
                        appName = json.optString("app_name", ""),
                        logoUrl = json.optString("logo_url", ""),
                        backgroundUrl = json.optString("background_url", ""),
                        gamesUrl = json.optString("games_url", ""),
                        introVideoUrl = json.optString("intro_video_url", ""),
                        customPanelUrl = json.optString("custom_panel_url", "")
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private data class ConfigResult(
        val dns: String,
        val appName: String,
        val logoUrl: String = "",
        val backgroundUrl: String = "",
        val gamesUrl: String = "",
        val introVideoUrl: String = "",
        val customPanelUrl: String = ""
    )
}