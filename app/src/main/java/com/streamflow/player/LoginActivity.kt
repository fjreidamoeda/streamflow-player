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

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        if (configManager.token.isBlank()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        if (configManager.isConfigured) {
            startActivity(Intent(this, PlayerActivity::class.java))
            finish()
            return
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isBlank() || password.isBlank()) {
                tvError.text = "Preencha usuario e senha"
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = TextView.GONE
            progressBar.visibility = ProgressBar.VISIBLE
            btnLogin.isEnabled = false
            btnLogin.text = "ENTRANDO..."

            CoroutineScope(Dispatchers.Main).launch {
                val result = networkUtils.authenticate(
                    configManager.panelUrl, username, password
                )
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    btnLogin.isEnabled = true
                    btnLogin.text = "ENTRAR"

                    result.onSuccess { userInfo ->
                        configManager.username = username
                        configManager.password = password
                        configManager.appName = userInfo.username

                        Toast.makeText(
                            this@LoginActivity,
                            "Bem-vindo, ${userInfo.username}!",
                            Toast.LENGTH_LONG
                        ).show()
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
