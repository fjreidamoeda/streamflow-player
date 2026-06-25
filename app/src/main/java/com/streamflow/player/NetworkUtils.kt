package com.streamflow.player

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppConfigResponse(
    val success: Boolean,
    @SerializedName("app_name") val appName: String = "",
    @SerializedName("package_name") val packageName: String = "",
    val dns: String = "",
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("background_url") val backgroundUrl: String? = null,
    @SerializedName("app_config") val appConfig: Map<String, Any>? = null
)

data class M3UChannel(
    val name: String,
    val logo: String = "",
    val url: String,
    val group: String = "Geral",
    val epgId: String = ""
)

data class M3UData(
    val channels: List<M3UChannel>,
    val groups: List<String>
)

class NetworkUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchConfig(panelUrl: String, token: String): Result<AppConfigResponse> =
        withContext(Dispatchers.IO) {
            try {
                val baseUrl = panelUrl.trimEnd('/')
                val url = "$baseUrl/api/app_config.php?token=${token}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")
                val config = gson.fromJson(body, AppConfigResponse::class.java)
                if (config.success) Result.success(config)
                else Result.failure(Exception(config.appName))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchM3U(panelUrl: String, token: String): Result<M3UData> =
        withContext(Dispatchers.IO) {
            try {
                val baseUrl = panelUrl.trimEnd('/')
                val url = "$baseUrl/api/fetch_m3u.php?token=$token&type=m3u"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")

                val channels = mutableListOf<M3UChannel>()
                val groups = mutableSetOf<String>()

                if (body.startsWith("#EXTM3U")) {
                    var currentGroup = "Geral"
                    val lines = body.lines()
                    var i = 0
                    while (i < lines.size) {
                        val line = lines[i].trim()
                        if (line.startsWith("#EXTINF:")) {
                            val groupMatch = Regex("""group-title="([^"]*)"""").find(line)
                            if (groupMatch != null) currentGroup = groupMatch.groupValues[1]

                            val name = line.substringAfterLast(",", "").trim()
                            val logoMatch = Regex("""tvg-logo="([^"]*)"""").find(line)
                            val logo = logoMatch?.groupValues?.get(1) ?: ""
                            val epgMatch = Regex("""tvg-id="([^"]*)"""").find(line)
                            val epgId = epgMatch?.groupValues?.get(1) ?: ""

                            if (i + 1 < lines.size) {
                                val url = lines[i + 1].trim()
                                if (url.isNotBlank() && !url.startsWith("#")) {
                                    channels.add(M3UChannel(name, logo, url, currentGroup, epgId))
                                    groups.add(currentGroup)
                                }
                                i++
                            }
                        }
                        i++
                    }
                }

                Result.success(M3UData(channels, groups.toList().sorted()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
