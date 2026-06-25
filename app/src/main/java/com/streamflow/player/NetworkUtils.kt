package com.streamflow.player

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AuthResponse(
    @SerializedName("user_info") val userInfo: UserInfoData? = null
)

data class UserInfoData(
    val username: String = "",
    val password: String = "",
    val auth: Int = 0,
    val status: String = "",
    @SerializedName("exp_date") val expDate: String = "",
    @SerializedName("is_trial") val isTrial: String = "0",
    @SerializedName("max_connections") val maxConnections: String = "1",
    @SerializedName("active_cons") val activeCons: String = "0",
    val message: String = ""
)

data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: String = "0"
)

data class XtreamStream(
    val num: Int = 0,
    val name: String = "",
    @SerializedName("stream_type") val streamType: String = "",
    @SerializedName("stream_id") val streamId: String = "",
    @SerializedName("stream_icon") val streamIcon: String = "",
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("stream_url") val streamUrl: String = "",
    @SerializedName("direct_source") val directSource: String = "",
    @SerializedName("container_extension") val containerExtension: String = "ts"
)

data class VodInfo(
    val num: Int = 0,
    val name: String = "",
    @SerializedName("stream_id") val streamId: String = "",
    @SerializedName("stream_icon") val streamIcon: String = "",
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("container_extension") val containerExtension: String = "mp4",
    val rating: String = "0",
    @SerializedName("direct_source") val directSource: String = ""
)

data class SeriesItemInfo(
    val num: Int = 0,
    val name: String = "",
    @SerializedName("series_id") val seriesId: String = "",
    val cover: String = "",
    val rating: String = "0",
    @SerializedName("category_id") val categoryId: String = ""
)

data class SeriesDetail(
    val episodes: Map<String, List<EpisodeInfo>> = emptyMap()
)

data class EpisodeInfo(
    val id: String = "",
    val title: String = "",
    @SerializedName("container_extension") val containerExtension: String = "mp4",
    val info: EpisodeInfoData = EpisodeInfoData()
)

data class EpisodeInfoData(
    @SerializedName("movie_image") val movieImage: String = ""
)

data class EpisodeItem(
    val episode: EpisodeInfo,
    val seasonNum: String,
    val seriesName: String
)

sealed class ContentItem {
    abstract val name: String
    abstract val icon: String
    abstract val categoryId: String

    data class Live(val stream: XtreamStream) : ContentItem() {
        override val name get() = stream.name
        override val icon get() = stream.streamIcon
        override val categoryId get() = stream.categoryId
        fun streamUrl(panel: String, user: String, pass: String): String {
            if (stream.directSource.isNotBlank()) return stream.directSource
            if (stream.streamUrl.isNotBlank()) return stream.streamUrl
            return "$panel/live/$user/$pass/${stream.streamId}.${stream.containerExtension}"
        }
    }

    data class Movie(val vod: VodInfo) : ContentItem() {
        override val name get() = vod.name
        override val icon get() = vod.streamIcon
        override val categoryId get() = vod.categoryId
        fun streamUrl(panel: String, user: String, pass: String): String {
            if (vod.directSource.isNotBlank()) return vod.directSource
            return "$panel/movie/$user/$pass/${vod.streamId}.${vod.containerExtension}"
        }
    }

    data class Series(val series: SeriesItemInfo) : ContentItem() {
        override val name get() = series.name
        override val icon get() = series.cover
        override val categoryId get() = series.categoryId
    }

    data class Episode(val ep: EpisodeItem) : ContentItem() {
        override val name get() = "${ep.seasonNum} - ${ep.episode.title}"
        override val icon get() = ep.episode.info.movieImage
        override val categoryId get() = ""
        fun streamUrl(panel: String, user: String, pass: String): String {
            return "$panel/series/$user/$pass/${ep.episode.id}.${ep.episode.containerExtension}"
        }
    }
}

enum class MenuType { LIVE, VOD, SERIES }

class NetworkUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun apiUrl(panelUrl: String, user: String, pass: String, action: String, extra: String = ""): String {
        val base = panelUrl.trimEnd('/')
        return "$base/player_api.php?username=$user&password=$pass&action=$action$extra"
    }

    suspend fun authenticate(panelUrl: String, username: String, password: String): Result<UserInfoData> =
        withContext(Dispatchers.IO) {
            try {
                val url = apiUrl(panelUrl, username, password, "")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")
                val authResponse = gson.fromJson(body, AuthResponse::class.java)
                val info = authResponse.userInfo
                if (info != null && info.auth == 1) {
                    Result.success(info)
                } else {
                    Result.failure(Exception(info?.message ?: "Credenciais invalidas"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getCategories(panelUrl: String, username: String, password: String, type: MenuType): Result<List<XtreamCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val action = when (type) {
                    MenuType.LIVE -> "get_live_categories"
                    MenuType.VOD -> "get_vod_categories"
                    MenuType.SERIES -> "get_series_categories"
                }
                val url = apiUrl(panelUrl, username, password, action)
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")
                val listType = object : TypeToken<List<XtreamCategory>>() {}.type
                Result.success(gson.fromJson(body, listType))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getContent(panelUrl: String, username: String, password: String, type: MenuType, categoryId: String? = null): Result<List<ContentItem>> =
        withContext(Dispatchers.IO) {
            try {
                when (type) {
                    MenuType.LIVE -> {
                        val action = "get_live_streams"
                        var url = apiUrl(panelUrl, username, password, action)
                        if (categoryId != null) url += "&category_id=$categoryId"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: throw Exception("Resposta vazia")
                        val listType = object : TypeToken<List<XtreamStream>>() {}.type
                        val streams: List<XtreamStream> = gson.fromJson(body, listType)
                        Result.success(streams.map { ContentItem.Live(it) })
                    }
                    MenuType.VOD -> {
                        val action = "get_vod_streams"
                        var url = apiUrl(panelUrl, username, password, action)
                        if (categoryId != null) url += "&category_id=$categoryId"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: throw Exception("Resposta vazia")
                        val listType = object : TypeToken<List<VodInfo>>() {}.type
                        val vods: List<VodInfo> = gson.fromJson(body, listType)
                        Result.success(vods.map { ContentItem.Movie(it) })
                    }
                    MenuType.SERIES -> {
                        val action = "get_series"
                        var url = apiUrl(panelUrl, username, password, action)
                        if (categoryId != null) url += "&category_id=$categoryId"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: throw Exception("Resposta vazia")
                        val listType = object : TypeToken<List<SeriesItemInfo>>() {}.type
                        val series: List<SeriesItemInfo> = gson.fromJson(body, listType)
                        Result.success(series.map { ContentItem.Series(it) })
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSeriesEpisodes(panelUrl: String, username: String, password: String, seriesId: String): Result<Map<String, List<EpisodeInfo>>> =
        withContext(Dispatchers.IO) {
            try {
                val url = apiUrl(panelUrl, username, password, "get_series_info", "&series_id=$seriesId")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")
                val detail = gson.fromJson(body, SeriesDetail::class.java)
                Result.success(detail.episodes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
