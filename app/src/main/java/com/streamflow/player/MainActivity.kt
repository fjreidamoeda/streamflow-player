package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            configManager = ConfigManager(this)

            if (configManager.isConfigured) {
                startActivity(Intent(this, MainMenuActivity::class.java))
            } else {
                startActivity(Intent(this, SetupActivity::class.java))
            }

            finish()
        } catch (e: Exception) {
            android.widget.TextView(this).apply {
                text = "ERRO: ${e.message}\n\n${e.stackTraceToString()}"
                textSize = 14f
                setTextColor(-0x1)
                setBackgroundColor(-0x1000000)
                setHorizontallyScrolling(false)
            }.let { setContentView(it) }
            e.printStackTrace()
        }
    }
}
