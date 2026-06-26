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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var tvAppName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
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
        mainLayout = findViewById(R.id.mainLayout)
        columnCategories = findViewById(R.id.columnCategories)
        columnContent = findViewById(R.id.columnContent)
        columnPlayer = findViewById(R.id.columnPlayer)
        playerFrame = findViewById(R.id.playerFrame)

        tvAppName.text = configManager.appName.ifBlank { "StreamFlow" }

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvContent.layoutManager = LinearLayoutManager(this)

        btnMenuLive.setOnClickListener { switchMenu(MenuType.LIVE) }
        btnMenuMovies.setOnClickListener { switchMenu(MenuType.VOD) }
        btnMenuSeries.setOnClickListener { switchMenu(MenuType.SERIES) }

        playerView.setFullscreenButtonClickListener(android.view.View.OnClickListener { toggleFullscreen() })
        btnBack.setOnClickListener { toggleFullscreen() }

        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener {
            selectedCategoryId = null
            selectedSeries = null
            showingEpisodes = false
            loadCategories()
        }
        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

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
                    categoryAdapter = CategoryAdapter(selectedCategoryId) { cat ->
                        selectedCategoryId = cat.categoryId
                        selectedSeries = null
                        showingEpisodes = false
                        categoryAdapter = CategoryAdapter(selectedCategoryId) { loadContent(cat.categoryId) }
                        rvCategories.adapter = categoryAdapter
                        categoryAdapter?.submitList(categories)
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

        if (configManager.playerType == "external") {
            exoPlayer?.release()
            playerView.player = null
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(url), "video/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Nenhum player externo encontrado", Toast.LENGTH_LONG).show()
            }
            return
        }

        exoPlayer?.release()

        val dataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent("XCIPTV")
        val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
        exoPlayer = ExoPlayer.Builder(this).setMediaSourceFactory(mediaSourceFactory).build()
        playerView.player = exoPlayer

        val mediaItem = MediaItem.fromUri(url)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()

        progressBar.visibility = View.VISIBLE

        var hideProgress: Runnable? = null

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        hideProgress?.let { handler.removeCallbacks(it) }
                        runOnUiThread { progressBar.visibility = View.GONE }
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        runOnUiThread { progressBar.visibility = View.GONE }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hideProgress?.let { handler.removeCallbacks(it) }
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    val detail = error.localizedMessage ?: error.cause?.message ?: ""
                    val msg = when (error.errorCode) {
                        androidx.media3.common.PlaybackException.ERROR_CODE_TIMEOUT -> "Tempo limite excedido ao conectar"
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "Erro de conexão com o servidor"
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Erro HTTP da fonte ($detail)"
                        androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "Sinal ao vivo perdido"
                        else -> "Erro ao reproduzir: $detail"
                    }
                    Toast.makeText(this@PlayerActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        })

        hideProgress = Runnable {
            runOnUiThread {
                if (progressBar.visibility == View.VISIBLE) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@PlayerActivity, "O stream demorou muito para carregar", Toast.LENGTH_LONG).show()
                }
            }
        }
        handler.postDelayed(hideProgress, 20000)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        columnCategories.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        columnContent.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        btnBack.visibility = if (isFullscreen) View.VISIBLE else View.GONE
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
        exoPlayer?.release()
    }
}
