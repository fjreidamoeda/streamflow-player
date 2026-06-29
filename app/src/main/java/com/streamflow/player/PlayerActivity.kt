package com.streamflow.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var networkUtils: NetworkUtils
    private var exoPlayer: ExoPlayer? = null
    private var currentMenu = MenuType.LIVE
    private var selectedCategoryId: String? = null
    private var selectedSeries: SeriesItemInfo? = null
    private var showingEpisodes = false
    private var isFullscreen = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var btnMenuLive: Button
    private lateinit var btnMenuMovies: Button
    private lateinit var btnMenuSeries: Button
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvContent: RecyclerView
    private lateinit var playerView: PlayerView
    private lateinit var tvNowPlaying: TextView
    private lateinit var topMenuBar: View
    private lateinit var tvAppName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnGames: ImageButton
    private lateinit var btnVolume: ImageButton
    private lateinit var layoutNoSignal: View
    private lateinit var mainLayout: View
    private lateinit var columnCategories: View
    private lateinit var columnContent: View
    private lateinit var columnPlayer: View
    private lateinit var playerFrame: View

    private var categories = listOf<XtreamCategory>()
    private var contentItems = listOf<ContentItem>()
    private var categoryAdapter: CategoryAdapter? = null
    private var contentAdapter: ContentAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_player)

        configManager = ConfigManager(this)
        networkUtils = NetworkUtils()

        if (!configManager.isConfigured) {
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        topMenuBar = findViewById(R.id.topMenuBar)
        etSearch = findViewById(R.id.etSearch)
        tvAppName = findViewById(R.id.tvAppName)
        btnMenuLive = findViewById(R.id.btnMenuLive)
        btnMenuMovies = findViewById(R.id.btnMenuMovies)
        btnMenuSeries = findViewById(R.id.btnMenuSeries)
        rvCategories = findViewById(R.id.rvCategories)
        rvContent = findViewById(R.id.rvContent)
        playerView = findViewById(R.id.playerView)
        tvNowPlaying = findViewById(R.id.tvNowPlaying)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnHome = findViewById(R.id.btnHome)
        btnGames = findViewById(R.id.btnGames)
        btnVolume = findViewById(R.id.btnVolume)
        layoutNoSignal = findViewById(R.id.layoutNoSignal)
        mainLayout = findViewById(R.id.mainLayout)
        columnCategories = findViewById(R.id.columnCategories)
        columnContent = findViewById(R.id.columnContent)
        columnPlayer = findViewById(R.id.columnPlayer)
        playerFrame = findViewById(R.id.playerFrame)

        tvAppName.text = configManager.appName.ifBlank { "StreamFlow" }
        if (configManager.logoUrl.isNotBlank()) {
            Picasso.get().load(configManager.logoUrl).into(findViewById<ImageView>(R.id.ivPlayerLogo))
        }

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvContent.layoutManager = LinearLayoutManager(this)

        btnMenuLive.setOnClickListener { switchMenu(MenuType.LIVE) }
        btnMenuMovies.setOnClickListener { switchMenu(MenuType.VOD) }
        btnMenuSeries.setOnClickListener { switchMenu(MenuType.SERIES) }

        playerView.setFullscreenButtonClickListener { toggleFullscreen() }
        columnPlayer.setOnClickListener { toggleFullscreen() }
        btnBack.setOnClickListener { toggleFullscreen() }

        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener {
            selectedCategoryId = null
            selectedSeries = null
            showingEpisodes = false
            loadCategories()
        }
        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao abrir config: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnHome.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        btnGames.setOnClickListener {
            val gamesUrl = configManager.gamesUrl
            if (gamesUrl.isNotBlank()) {
                try {
                    val intent = Intent(this, WebViewActivity::class.java).apply {
                        putExtra("url", gamesUrl)
                        putExtra("title", "Jogos")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao abrir jogos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Link dos jogos nao configurado", Toast.LENGTH_SHORT).show()
            }
        }
        btnVolume.setOnClickListener {
            exoPlayer?.let { player ->
                player.volume = if (player.volume > 0f) 0f else 1f
                btnVolume.setImageResource(if (player.volume > 0f) R.drawable.ic_volume else R.drawable.ic_volume_off)
            }
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString()?.lowercase() ?: ""
                contentAdapter?.submitList(
                    if (query.isEmpty()) contentItems
                    else contentItems.filter { it.name.lowercase().contains(query) }
                )
            }
        })

        val initialMenu = try {
            MenuType.valueOf(intent.getStringExtra("menu_type") ?: "LIVE")
        } catch (e: Exception) { MenuType.LIVE }
        switchMenu(initialMenu)
    }

    private fun switchMenu(type: MenuType) {
        currentMenu = type
        selectedCategoryId = null
        selectedSeries = null
        showingEpisodes = false

        // Clear content immediately when switching menus
        contentItems = listOf()
        contentAdapter = null
        rvContent.adapter = null

        val activated = 0xffe63e2e.toInt()
        val deactivated = 0xff333333.toInt()
        val activeText = 0xffffffff.toInt()
        val inactiveText = 0xffaaaaaa.toInt()

        btnMenuLive.setBackgroundColor(if (type == MenuType.LIVE) activated else deactivated)
        btnMenuLive.setTextColor(if (type == MenuType.LIVE) activeText else inactiveText)
        btnMenuMovies.setBackgroundColor(if (type == MenuType.VOD) activated else deactivated)
        btnMenuMovies.setTextColor(if (type == MenuType.VOD) activeText else inactiveText)
        btnMenuSeries.setBackgroundColor(if (type == MenuType.SERIES) activated else deactivated)
        btnMenuSeries.setTextColor(if (type == MenuType.SERIES) activeText else inactiveText)

        tvNowPlaying.text = when (type) {
            MenuType.LIVE -> "Canais ao vivo"
            MenuType.VOD -> "Filmes"
            MenuType.SERIES -> "Series"
        }

        loadCategories()
    }

    private fun loadCategories() {
        selectedCategoryId = null
        categoryAdapter = null
        rvCategories.adapter = null
        contentAdapter = null
        rvContent.adapter = null
        contentItems = listOf()

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = networkUtils.getCategories(
                configManager.panelUrl, configManager.username, configManager.password, currentMenu
            )
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { cats ->
                    categories = cats
                    selectedCategoryId = categories.firstOrNull()?.categoryId
                    categoryAdapter = CategoryAdapter(selectedCategoryId) { cat ->
                        selectedCategoryId = cat.categoryId
                        selectedSeries = null
                        showingEpisodes = false
                        categoryAdapter?.updateSelectedId(cat.categoryId)
                        loadContent(cat.categoryId)
                    }
                    rvCategories.adapter = categoryAdapter
                    categoryAdapter?.submitList(categories)
                    if (categories.isNotEmpty()) {
                        loadContent(categories.first().categoryId)
                    }
                }.onFailure { error ->
                    Toast.makeText(this@PlayerActivity, "Erro: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadContent(categoryId: String?) {
        contentAdapter = null
        rvContent.adapter = null
        contentItems = listOf()

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = networkUtils.getContent(
                configManager.panelUrl, configManager.username, configManager.password,
                currentMenu, categoryId
            )
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { items ->
                    contentItems = items
                    contentAdapter = ContentAdapter { item -> onContentClicked(item) }
                    rvContent.adapter = contentAdapter
                    contentAdapter?.submitList(contentItems)
                }.onFailure { error ->
                    Toast.makeText(this@PlayerActivity, "Erro: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun onContentClicked(item: ContentItem) {
        Toast.makeText(this, "Carregando: ${item.name}", Toast.LENGTH_SHORT).show()
        when (item) {
            is ContentItem.Live -> playStream(item.streamUrl(
                configManager.panelUrl, configManager.username, configManager.password,
                configManager.streamFormat
            ), item.name)
            is ContentItem.Movie -> playStream(item.streamUrl(
                configManager.panelUrl, configManager.username, configManager.password,
                configManager.streamFormat
            ), item.name)
            is ContentItem.Series -> loadEpisodes(item.series)
            is ContentItem.Episode -> playStream(item.streamUrl(
                configManager.panelUrl, configManager.username, configManager.password,
                configManager.streamFormat
            ), item.name)
        }
    }

    private fun loadEpisodes(series: SeriesItemInfo) {
        selectedSeries = series
        showingEpisodes = true

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = networkUtils.getSeriesEpisodes(
                configManager.panelUrl, configManager.username, configManager.password,
                series.seriesId
            )
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { episodesBySeason ->
                    val items = mutableListOf<ContentItem>()
                    for ((season, eps) in episodesBySeason) {
                        for (ep in eps) {
                            items.add(ContentItem.Episode(
                                EpisodeItem(ep, "S$season", series.name)
                            ))
                        }
                    }
                    contentItems = items
                    contentAdapter = ContentAdapter { item -> onContentClicked(item) }
                    rvContent.adapter = contentAdapter
                    contentAdapter?.submitList(contentItems)
                }.onFailure { error ->
                    Toast.makeText(this@PlayerActivity, "Erro: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun playStream(url: String, title: String) {
        if (url.isBlank()) {
            Toast.makeText(this, "URL invalida", Toast.LENGTH_SHORT).show()
            return
        }

        tvNowPlaying.text = "▶ $title"
        layoutNoSignal.visibility = View.GONE

        val playUrl = url
        CoroutineScope(Dispatchers.Main).launch {
            val resolvedUrl = try {
                withContext(Dispatchers.IO) {
                    networkUtils.resolveDirectUrl(playUrl)
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Erro ao resolver URL: ${e.message}", Toast.LENGTH_LONG).show()
                return@launch
            }

            if (resolvedUrl.isBlank()) {
                Toast.makeText(this@PlayerActivity, "URL do stream vazia", Toast.LENGTH_LONG).show()
                return@launch
            }

            if (configManager.playerType == "external") {
                exoPlayer?.release()
                playerView.player = null
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(resolvedUrl), "video/*")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@PlayerActivity, "Nenhum player externo encontrado", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            try {
                exoPlayer?.release()
                playerView.player = null

                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                val mediaSourceFactory = DefaultMediaSourceFactory(this@PlayerActivity).setDataSourceFactory(dataSourceFactory)
                exoPlayer = ExoPlayer.Builder(this@PlayerActivity)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()
                playerView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(resolvedUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Erro ao reproduzir: ${e.message}", Toast.LENGTH_LONG).show()
                return@launch
            }

            progressBar.visibility = View.VISIBLE

            var hideProgress: Runnable? = null

            exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (isFinishing || isDestroyed) return
                try {
                    when (state) {
                        Player.STATE_READY -> {
                            hideProgress?.let { handler.removeCallbacks(it) }
                            runOnUiThread {
                                if (isFinishing || isDestroyed) return@runOnUiThread
                                progressBar.visibility = View.GONE
                                layoutNoSignal.visibility = View.GONE
                            }
                        }
                        Player.STATE_ENDED, Player.STATE_IDLE -> {
                            runOnUiThread {
                                if (isFinishing || isDestroyed) return@runOnUiThread
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (isFinishing || isDestroyed) return
                hideProgress?.let { handler.removeCallbacks(it) }
                try {
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        progressBar.visibility = View.GONE
                        var detail = error.localizedMessage ?: error.cause?.message ?: ""
                        val cause = error.cause
                        if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                            if (cause is HttpDataSource.InvalidResponseCodeException) {
                                detail = "HTTP ${cause.responseCode}: ${cause.responseMessage}"
                            }
                        } else if (cause is HttpDataSource.InvalidResponseCodeException) {
                            detail = "HTTP ${cause.responseCode}: ${cause.responseMessage}"
                        }
                        val code = error.errorCode
                        val msg = when (code) {
                            androidx.media3.common.PlaybackException.ERROR_CODE_TIMEOUT -> "Tempo limite excedido ao conectar"
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "Erro de conexão com o servidor"
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Erro HTTP da fonte ($detail)"
                            androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "Sinal ao vivo perdido"
                            androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED -> "Falha ao decodificar video"
                            androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "Formato de video nao suportado"
                            androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "Video excede capacidade do aparelho (codec/resolucao)"
                            else -> "Erro (código $code): $detail"
                        }
                        if (code == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
                            code == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                            code == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED) {
                            layoutNoSignal.visibility = View.VISIBLE
                            Toast.makeText(this@PlayerActivity, "Sem sinal", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@PlayerActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (_: Exception) {}
            }
        })

        hideProgress = Runnable {
            if (isFinishing || isDestroyed) return@Runnable
            runOnUiThread {
                if (progressBar.visibility == View.VISIBLE) {
                    progressBar.visibility = View.GONE
                    try {
                        Toast.makeText(this@PlayerActivity, "O stream demorou muito para carregar", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {}
                }
            }
        }
        handler.postDelayed(hideProgress, 20000)
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        columnCategories.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        columnContent.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        topMenuBar.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        tvNowPlaying.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        btnBack.visibility = if (isFullscreen) View.VISIBLE else View.GONE
        playerView.useController = !isFullscreen
        if (isFullscreen) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen()
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.release()
    }
}
