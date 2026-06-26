package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var rbInternal: RadioButton
    private lateinit var rbExternal: RadioButton
    private lateinit var rbTs: RadioButton
    private lateinit var rbHls: RadioButton
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPanelUrl: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)

        rbInternal = findViewById(R.id.rbPlayerInternal)
        rbExternal = findViewById(R.id.rbPlayerExternal)
        rbTs = findViewById(R.id.rbFormatTs)
        rbHls = findViewById(R.id.rbFormatHls)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etPanelUrl = findViewById(R.id.etPanelUrl)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)
        tvStatus = findViewById(R.id.tvStatus)

        if (configManager.playerType == "external") rbExternal.isChecked = true
        else rbInternal.isChecked = true

        if (configManager.streamFormat == "m3u8") rbHls.isChecked = true
        else rbTs.isChecked = true

        etUsername.setText(configManager.username)
        etPassword.setText(configManager.password)
        etPanelUrl.setText(configManager.panelUrl)

        btnSave.setOnClickListener {
            val playerType = if (rbExternal.isChecked) "external" else "internal"
            val streamFormat = if (rbHls.isChecked) "m3u8" else "ts"
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val panelUrl = etPanelUrl.text.toString().trim()

            if (username.isBlank() || password.isBlank() || panelUrl.isBlank()) {
                tvStatus.text = "Preencha todos os campos obrigatorios"
                tvStatus.setTextColor(0xffff6b6b.toInt())
                tvStatus.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            configManager.playerType = playerType
            configManager.streamFormat = streamFormat
            configManager.username = username
            configManager.password = password
            configManager.panelUrl = panelUrl

            tvStatus.text = "Configuracoes salvas!"
            tvStatus.setTextColor(0xff4caf50.toInt())
            tvStatus.visibility = TextView.VISIBLE
        }

        btnLogout.setOnClickListener {
            configManager.clear()
            startActivity(Intent(this, SetupActivity::class.java))
            finishAffinity()
        }
    }
}
