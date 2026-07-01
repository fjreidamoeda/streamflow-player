package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
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
    private lateinit var btnRetry: Button
    private lateinit var btnSettings: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var formCard: MaterialCardView

    private var connecting = false
    private var showingSettings = false

    companion object {
        const val EXTRA_SHOW_SETTINGS = "show_settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        configManager = ConfigManager(this)

        etToken = findViewById(R.id.etToken)
        etPanelUrl = findViewById(R.id.etPanelUrl)
        btnConnect = findViewById(R.id.btnConnect)
        btnRetry = findViewById(R.id.btnRetry)
        btnSettings = findViewById(R.id.btnSettings)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        formCard = findViewById(R.id.formCard)

        val forceSettings = intent.getBooleanExtra(EXTRA_SHOW_SETTINGS, false)

        if (forceSettings) {
            showSettingsForm()
            return
        }

        if (configManager.token.isNotBlank() && configManager.panelUrl.isNotBlank()) {
            startAutoConnect()
        } else {
            showSettingsForm()
        }

        btnConnect.setOnClickListener {
            if (connecting) return@setOnClickListener
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
            connecting = true
            connectToPanel(token, panelUrl)
        }

        btnRetry.setOnClickListener {
            startAutoConnect()
        }

        btnSettings.setOnClickListener {
            showSettingsForm()
        }
    }

    private fun startAutoConnect() {
        showingSettings = false
        tvSubtitle.text = "Conectando..."
        progressBar.visibility = ProgressBar.VISIBLE
        tvStatus.visibility = TextView.GONE
        btnRetry.visibility = TextView.GONE
        btnSettings.visibility = TextView.GONE
        formCard.visibility = TextView.GONE

        connectToPanel(configManager.token, configManager.panelUrl)
    }

    private fun showSettingsForm() {
        showingSettings = true
        tvSubtitle.text = "Configuracoes"
        progressBar.visibility = ProgressBar.GONE
        tvStatus.visibility = TextView.GONE
        btnRetry.visibility = TextView.GONE
        btnSettings.visibility = TextView.GONE
        formCard.visibility = TextView.VISIBLE

        etToken.setText(configManager.token)
        etPanelUrl.setText(configManager.panelUrl)
    }

    private fun showConnectionError(message: String) {
        tvSubtitle.text = "Falha na conexao"
        tvStatus.text = message
        tvStatus.visibility = TextView.VISIBLE
        progressBar.visibility = ProgressBar.GONE
        btnRetry.visibility = TextView.VISIBLE
        btnSettings.visibility = TextView.VISIBLE
    }

    private fun connectToPanel(token: String, panelUrl: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                fetchConfig(panelUrl, token)
            }

            result?.let { config ->
                configManager.token = token
                configManager.panelUrl = panelUrl
                configManager.gamesUrl = config.gamesUrl
                configManager.introVideoUrl = config.introVideoUrl
                configManager.customPanelUrl = config.customPanelUrl
                configManager.appName = config.appName
                configManager.logoUrl = config.logoUrl
                configManager.backgroundUrl = config.backgroundUrl

                startActivity(Intent(this@SetupActivity, LoginActivity::class.java))
                finish()
            } ?: run {
                if (showingSettings) {
                    progressBar.visibility = ProgressBar.GONE
                    btnConnect.isEnabled = true
                    connecting = false
                    tvStatus.text = "Token invalido ou sem conexao com o painel"
                    tvStatus.visibility = TextView.VISIBLE
                } else {
                    showConnectionError("Token invalido ou sem conexao com o painel")
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

