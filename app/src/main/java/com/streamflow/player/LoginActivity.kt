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

class LoginActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var networkUtils: NetworkUtils
    private lateinit var etPanelUrl: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        configManager = ConfigManager(this)
        networkUtils = NetworkUtils()

        etPanelUrl = findViewById(R.id.etPanelUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        etPanelUrl.setText(configManager.panelUrl.ifBlank { "https://" })

        btnLogin.setOnClickListener {
            val panelUrl = etPanelUrl.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (panelUrl.isBlank() || username.isBlank() || password.isBlank()) {
                tvError.text = "Preencha todos os campos"
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = TextView.GONE
            progressBar.visibility = ProgressBar.VISIBLE
            btnLogin.isEnabled = false
            btnLogin.text = "Entrando..."

            CoroutineScope(Dispatchers.Main).launch {
                val result = networkUtils.authenticate(panelUrl, username, password)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    btnLogin.isEnabled = true
                    btnLogin.text = "Entrar"

                    result.onSuccess { userInfo ->
                        configManager.panelUrl = panelUrl
                        configManager.username = username
                        configManager.password = password
                        configManager.appName = userInfo.username

                        Toast.makeText(this@LoginActivity, "Bem-vindo, ${userInfo.username}!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@LoginActivity, PlayerActivity::class.java))
                        finish()
                    }.onFailure { error ->
                        tvError.text = error.message ?: "Erro de conexao"
                        tvError.visibility = TextView.VISIBLE
                    }
                }
            }
        }
    }
}
