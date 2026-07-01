package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
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
    private lateinit var btnSettings: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private var bgTarget: com.squareup.picasso.Target? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        configManager = ConfigManager(this)
        networkUtils = NetworkUtils()

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSettings = findViewById(R.id.btnSettings)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        btnSettings.setOnClickListener {
            val intent = Intent(this, SetupActivity::class.java)
            intent.putExtra("show_settings", true)
            startActivity(intent)
        }

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val scrollView = findViewById<android.widget.ScrollView>(R.id.loginScrollView)
        scrollView.setBackgroundColor(0xff1a1a2e.toInt())

        if (configManager.logoUrl.isNotBlank()) {
            Picasso.get().load(configManager.logoUrl).into(ivLogo)
        }
        if (configManager.backgroundUrl.isNotBlank()) {
            val target = object : com.squareup.picasso.Target {
                override fun onBitmapLoaded(bitmap: android.graphics.Bitmap, from: Picasso.LoadedFrom) {
                    scrollView.background = android.graphics.drawable.BitmapDrawable(this@LoginActivity.resources, bitmap)
                }
                override fun onBitmapFailed(e: Exception?, errorDrawable: android.graphics.drawable.Drawable?) {}
                override fun onPrepareLoad(placeHolderDrawable: android.graphics.drawable.Drawable?) {}
            }
            bgTarget = target
            Picasso.get().load(configManager.backgroundUrl).into(target)
        }
        if (configManager.appName.isNotBlank()) {
            tvTitle.text = configManager.appName
        }

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
