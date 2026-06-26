package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configManager = ConfigManager(this)

        when {
            configManager.isConfigured ->
                startActivity(Intent(this, MainMenuActivity::class.java))
            configManager.panelUrl.isNotBlank() ->
                startActivity(Intent(this, LoginActivity::class.java))
            else ->
                startActivity(Intent(this, SetupActivity::class.java))
        }

        finish()
    }
}
