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
    @SerializedName("category_name") val categoryName: String
)

data class XtreamStream(
    val num: Int = 0,
    val name: String = "",
    @SerializedName("stream_type") val streamType: String = "",
    @SerializedName("stream_id") val streamId: String = "",
    @SerializedName("stream_icon") val streamIcon: String = "",
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("stream_url") val streamUrl: String = "",
    @SerializedName("direct_source") val directSource: String = ""
)

class NetworkUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun authenticate(panelUrl: String, username: String, password: String): Result<UserInfoData> =
        withContext(Dispatchers.IO) {
            try {
                val base = panelUrl.trimEnd('/')
                val url = "$base/player_api.php?username=$username&password=$password"
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

    suspend fun getLiveCategories(panelUrl: String, username: String, password: String): Result<List<XtreamCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val base = panelUrl.trimEnd('/')
                val url = "$base/player_api.php?username=$username&password=$password&action=get_live_categories"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")
                val listType = object : TypeToken<List<XtreamCategory>>() {}.type
                val categories: List<XtreamCategory> = gson.fromJson(body, listType)
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getLiveStreams(panelUrl: String, username: String, password: String, categoryId: String? = null): Result<List<XtreamStream>> =
        withContext(Dispatchers.IO) {
            try {
                val base = panelUrl.trimEnd('/')
                var url = "$base/player_api.php?username=$username&password=$password&action=get_live_streams"
                if (categoryId != null) url += "&category_id=$categoryId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")
                val listType = object : TypeToken<List<XtreamStream>>() {}.type
                val streams: List<XtreamStream> = gson.fromJson(body, listType)
                Result.success(streams)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
