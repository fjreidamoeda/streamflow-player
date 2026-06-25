package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var etPanelUrl: EditText
    private lateinit var etToken: EditText
    private lateinit var btnConnect: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        configManager = ConfigManager(this)
        val networkUtils = NetworkUtils()

        etPanelUrl = findViewById(R.id.etPanelUrl)
        etToken = findViewById(R.id.etToken)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        etPanelUrl.setText(configManager.panelUrl.ifBlank { "https://" })

        btnConnect.setOnClickListener {
            val panelUrl = etPanelUrl.text.toString().trim()
            val token = etToken.text.toString().trim()

            if (panelUrl.isBlank() || token.isBlank()) {
                Toast.makeText(this, "Preencha a URL do painel e o token", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = android.view.View.VISIBLE
            tvStatus.text = "Conectando..."
            btnConnect.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                val result = networkUtils.fetchConfig(panelUrl, token)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    btnConnect.isEnabled = true

                    result.onSuccess { config ->
                        configManager.panelUrl = panelUrl
                        configManager.token = token
                        configManager.appName = config.appName
                        configManager.dnsUrl = config.dns
                        configManager.logoUrl = config.logoUrl ?: ""

                        Toast.makeText(this@SetupActivity, "Conectado a ${config.appName}", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SetupActivity, PlayerActivity::class.java))
                        finish()
                    }.onFailure { error ->
                        tvStatus.text = "Erro: ${error.message}"
                        Toast.makeText(this@SetupActivity, "Falha na conexão: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
