package com.streamflow.player

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainMenuActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var networkUtils: NetworkUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        configManager = ConfigManager(this)
        networkUtils = NetworkUtils()

        if (!configManager.isConfigured) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        findViewById<TextView>(R.id.tvAppTitle).text = configManager.appName.ifBlank { "StreamFlow" }
        findViewById<TextView>(R.id.tvWelcome).text = "Bem-vindo, ${configManager.appName}!"

        findViewById<CardView>(R.id.cardLive).setOnClickListener {
            startPlayer(MenuType.LIVE)
        }
        findViewById<CardView>(R.id.cardMovies).setOnClickListener {
            startPlayer(MenuType.VOD)
        }
        findViewById<CardView>(R.id.cardSeries).setOnClickListener {
            startPlayer(MenuType.SERIES)
        }
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardGames).setOnClickListener {
            val gamesUrl = configManager.gamesUrl
            if (gamesUrl.isNotBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gamesUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao abrir jogos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Link dos jogos nao configurado", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            configManager.clear()
            startActivity(Intent(this, SetupActivity::class.java))
            finishAffinity()
        }

        fetchApkMessages()
    }

    private fun fetchApkMessages() {
        CoroutineScope(Dispatchers.Main).launch {
            val result = networkUtils.getApkMessages(
                configManager.panelUrl, configManager.username
            )
            withContext(Dispatchers.Main) {
                result.onSuccess { messages ->
                    if (messages.isNotEmpty()) {
                        showMessagesDialog(messages)
                    }
                }
            }
        }
    }

    private fun showMessagesDialog(messages: List<ApkMessage>) {
        val messageText = messages.joinToString("\n\n") { it.message }
        AlertDialog.Builder(this)
            .setTitle("Mensagens")
            .setMessage(messageText)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show()
    }

    private fun startPlayer(type: MenuType) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("menu_type", type.name)
        }
        startActivity(intent)
    }
}
