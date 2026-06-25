package com.streamflow.player

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    private lateinit var playerView: com.google.android.material.card.MaterialCardView
    private lateinit var surfaceView: android.view.SurfaceView
    private lateinit var rvChannels: RecyclerView
    private lateinit var spinnerGroups: Spinner
    private lateinit var tvNowPlaying: TextView
    private lateinit var tvAppName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSetup: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var layoutPlayer: LinearLayout

    private var channels = listOf<M3UChannel>()
    private var filteredChannels = listOf<M3UChannel>()
    private var currentUrl: String? = null
    private var adapter: ChannelAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        configManager = ConfigManager(this)
        networkUtils = NetworkUtils()

        if (!configManager.isConfigured) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        playerView = findViewById(R.id.playerView)
        surfaceView = findViewById(R.id.surfaceView)
        rvChannels = findViewById(R.id.rvChannels)
        spinnerGroups = findViewById(R.id.spinnerGroups)
        tvNowPlaying = findViewById(R.id.tvNowPlaying)
        tvAppName = findViewById(R.id.tvAppName)
        progressBar = findViewById(R.id.progressBar)
        btnSetup = findViewById(R.id.btnSetup)
        btnRefresh = findViewById(R.id.btnRefresh)
        layoutPlayer = findViewById(R.id.layoutPlayer)

        tvAppName.text = configManager.appName

        rvChannels.layoutManager = LinearLayoutManager(this)
        adapter = ChannelAdapter { channel -> playChannel(channel) }
        rvChannels.adapter = adapter

        spinnerGroups.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                filterChannels(if (pos == 0) null else parent.getItemAtPosition(pos).toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSetup.setOnClickListener {
            exoPlayer?.release()
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }

        btnRefresh.setOnClickListener { loadChannels() }

        loadChannels()
    }

    private fun loadChannels() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = networkUtils.fetchM3U(configManager.panelUrl, configManager.token)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { m3u ->
                    channels = m3u.channels
                    val groups = listOf("Todas") + m3u.groups
                    spinnerGroups.adapter = ArrayAdapter(
                        this@PlayerActivity,
                        android.R.layout.simple_spinner_item,
                        groups
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    filterChannels(null)
                }.onFailure { error ->
                    Toast.makeText(this@PlayerActivity, "Erro ao carregar canais: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun filterChannels(group: String?) {
        filteredChannels = if (group == null) channels
        else channels.filter { it.group == group }
        adapter?.submitList(filteredChannels)
    }

    private fun playChannel(channel: M3UChannel) {
        currentUrl = channel.url
        tvNowPlaying.text = "▶ ${channel.name}"

        layoutPlayer.visibility = View.VISIBLE

        exoPlayer?.release()
        exoPlayer = ExoPlayer.Builder(this).build()

        val mediaItem = MediaItem.fromUri(channel.url)
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
