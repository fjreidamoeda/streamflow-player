package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    private lateinit var playerView: PlayerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var spinnerCategories: Spinner
    private lateinit var tvNowPlaying: TextView
    private lateinit var tvAppName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogout: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var layoutPlayer: LinearLayout

    private var categories = listOf<XtreamCategory>()
    private var allStreams = listOf<XtreamStream>()
    private var filteredStreams = listOf<XtreamStream>()
    private var adapter: ChannelAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        configManager = ConfigManager(this)
        networkUtils = NetworkUtils()

        if (!configManager.isConfigured) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        playerView = findViewById(R.id.playerView)
        rvChannels = findViewById(R.id.rvChannels)
        spinnerCategories = findViewById(R.id.spinnerGroups)
        tvNowPlaying = findViewById(R.id.tvNowPlaying)
        tvAppName = findViewById(R.id.tvAppName)
        progressBar = findViewById(R.id.progressBar)
        btnLogout = findViewById(R.id.btnSetup)
        btnRefresh = findViewById(R.id.btnRefresh)
        layoutPlayer = findViewById(R.id.layoutPlayer)

        tvAppName.text = configManager.appName.ifBlank { "StreamFlow" }

        rvChannels.layoutManager = LinearLayoutManager(this)

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (pos == 0) filteredStreams = allStreams
                else {
                    val catId = categories.getOrNull(pos - 1)?.categoryId
                    filteredStreams = allStreams.filter { it.categoryId == catId }
                }
                adapter = ChannelAdapter { stream -> playStream(stream) }
                rvChannels.adapter = adapter
                adapter?.submitList(filteredStreams)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnLogout.setOnClickListener {
            exoPlayer?.release()
            configManager.clear()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRefresh.setOnClickListener { loadCategories() }

        loadCategories()
    }

    private fun loadCategories() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = networkUtils.getLiveCategories(
                configManager.panelUrl, configManager.username, configManager.password
            )
            withContext(Dispatchers.Main) {
                result.onSuccess { cats ->
                    categories = cats
                    val names = listOf("Todas") + cats.map { it.categoryName }
                    spinnerCategories.adapter = ArrayAdapter(
                        this@PlayerActivity,
                        android.R.layout.simple_spinner_item,
                        names
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    loadStreams()
                }.onFailure { error ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@PlayerActivity, "Erro: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadStreams() {
        CoroutineScope(Dispatchers.Main).launch {
            val result = networkUtils.getLiveStreams(
                configManager.panelUrl, configManager.username, configManager.password
            )
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { streams ->
                    allStreams = streams
                    filteredStreams = streams
                    adapter = ChannelAdapter { stream -> playStream(stream) }
                    rvChannels.adapter = adapter
                    adapter?.submitList(filteredStreams)
                }.onFailure { error ->
                    Toast.makeText(this@PlayerActivity, "Erro: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun playStream(stream: XtreamStream) {
        val streamUrl = stream.directSource.ifBlank { stream.streamUrl }
        if (streamUrl.isBlank()) {
            Toast.makeText(this, "URL do stream invalida", Toast.LENGTH_SHORT).show()
            return
        }

        tvNowPlaying.text = "▶ ${stream.name}"
        layoutPlayer.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        exoPlayer?.release()
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer

        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    progressBar.visibility = View.GONE
                }
            }
        })
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
