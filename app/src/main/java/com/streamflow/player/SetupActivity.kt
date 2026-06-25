package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var etPanelUrl: EditText
    private lateinit var btnConnect: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        configManager = ConfigManager(this)

        etPanelUrl = findViewById(R.id.etPanelUrl)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        if (configManager.panelUrl.isNotBlank()) {
            etPanelUrl.setText(configManager.panelUrl)
        }

        btnConnect.setOnClickListener {
            val panelUrl = etPanelUrl.text.toString().trim()

            if (panelUrl.isBlank() || panelUrl == "https://") {
                tvStatus.text = "Informe a URL do painel"
                tvStatus.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            tvStatus.visibility = TextView.GONE
            progressBar.visibility = ProgressBar.VISIBLE
            btnConnect.isEnabled = false

            configManager.panelUrl = panelUrl

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
