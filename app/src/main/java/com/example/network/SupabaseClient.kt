package com.example.network

import android.util.Log
import com.example.data.Comment
import com.example.data.SavedPaper
import com.example.data.chat.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SupabaseAuthUser(
    val id: String,
    val email: String,
    val fullName: String,
    val username: String
)

data class SupabaseAuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: SupabaseAuthUser
)

object SupabaseClient {

    private const val TAG = "SupabaseClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fallbackFormat.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (ex: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "U"
            parts.size == 1 -> parts[0].take(2).uppercase(Locale.ROOT)
            else -> "${parts[0].take(1)}${parts.last().take(1)}".uppercase(Locale.ROOT)
        }
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        username: String
    ): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email.trim().lowercase(Locale.ROOT))
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", fullName.trim())
                    put("username", username.trim())
                })
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/auth/v1/signup")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(body).optString("msg", JSONObject(body).optString("error_description", "Sign up failed ($response)"))
                } catch (e: Exception) {
                    "Sign up failed (${response.code})"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            val obj = JSONObject(body)
            val accessToken = obj.optString("access_token", "")
            val refreshToken = obj.optString("refresh_token", "")
            val userObj = obj.optJSONObject("user") ?: obj

            val userId = userObj.optString("id", UUID.randomUUID().toString())
            val userEmail = userObj.optString("email", email)
            val metadata = userObj.optJSONObject("user_metadata")
            val name = metadata?.optString("full_name", fullName) ?: fullName
            val uname = metadata?.optString("username", username) ?: username

            Result.success(
                SupabaseAuthResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    user = SupabaseAuthUser(
                        id = userId,
                        email = userEmail,
                        fullName = name,
                        username = uname
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "signUp error", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(
        email: String,
        password: String
    ): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email.trim().lowercase(Locale.ROOT))
                put("password", password)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/auth/v1/token?grant_type=password")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(body).optString("error_description", JSONObject(body).optString("msg", "Invalid email or password"))
                } catch (e: Exception) {
                    "Invalid login credentials (${response.code})"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            val obj = JSONObject(body)
            val accessToken = obj.getString("access_token")
            val refreshToken = obj.optString("refresh_token", "")
            val userObj = obj.getJSONObject("user")

            val userId = userObj.getString("id")
            val userEmail = userObj.optString("email", email)
            val metadata = userObj.optJSONObject("user_metadata")
            val name = metadata?.optString("full_name", "") ?: ""
            val uname = metadata?.optString("username", userEmail.substringBefore("@")) ?: ""

            Result.success(
                SupabaseAuthResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    user = SupabaseAuthUser(
                        id = userId,
                        email = userEmail,
                        fullName = name,
                        username = uname
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "signIn error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // PUBLIC FEED & POSTS
    // ==========================================

    suspend fun getPosts(
        limit: Int = 50,
        accessToken: String? = null
    ): Result<List<SavedPaper>> = withContext(Dispatchers.IO) {
        try {
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseConfig.ANON_KEY
            val url = "${SupabaseConfig.URL}/rest/v1/posts?select=*,author:profiles!posts_author_id_fkey(*)&order=created_at.desc&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch posts: ${response.code}"))
            }

            val jsonArray = JSONArray(body)
            val papers = mutableListOf<SavedPaper>()

            for (i in 0 until jsonArray.length()) {
                val row = jsonArray.getJSONObject(i)
                val authorObj = row.optJSONObject("author")
                val authorName = authorObj?.optString("full_name", "")?.takeIf { it.isNotBlank() }
                    ?: authorObj?.optString("username", "Researcher") ?: "Researcher"
                val authorInitials = getInitials(authorName)
                val affiliation = authorObj?.optString("bio", "") ?: ""

                val paper = SavedPaper(
                    id = row.getString("id"),
                    authorInitials = authorInitials,
                    authorName = authorName,
                    affiliation = affiliation,
                    content = row.optString("content", ""),
                    title = row.optString("title", ""),
                    authors = row.optString("authors", ""),
                    year = row.optString("year", ""),
                    venue = row.optString("venue", ""),
                    doi = row.optString("doi", ""),
                    url = row.optString("url", ""),
                    publishedAt = parseIsoTimestamp(row.optString("created_at", "")),
                    citationOverride = "",
                    isEndorsed = false,
                    endorsementCount = row.optInt("likes_count", 0),
                    commentCount = row.optInt("comments_count", 0),
                    repostCount = 0,
                    isBookmarked = false,
                    imageUri = row.optString("image_url", ""),
                    quotedId = row.optString("quoted_post_id", ""),
                    quotedAuthorName = "",
                    quotedTitle = "",
                    quotedContent = "",
                    pdfUrl = row.optString("pdf_url", ""),
                    pdfLocalPath = "",
                    abstractText = row.optString("abstract_text", ""),
                    openAccess = row.optBoolean("open_access", false)
                )
                papers.add(paper)
            }

            Result.success(papers)
        } catch (e: Exception) {
            Log.e(TAG, "getPosts error", e)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        paper: SavedPaper,
        authorId: String,
        accessToken: String
    ): Result<SavedPaper> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                if (paper.id.isNotBlank() && paper.id.length >= 30) {
                    put("id", paper.id)
                }
                put("author_id", authorId)
                put("content", paper.content)
                put("title", paper.title)
                put("authors", paper.authors)
                put("year", paper.year)
                put("venue", paper.venue)
                put("doi", paper.doi)
                put("url", paper.url)
                put("pdf_url", paper.pdfUrl)
                put("abstract_text", paper.abstractText)
                put("open_access", paper.openAccess)
                if (paper.imageUri.isNotBlank()) {
                    put("image_url", paper.imageUri)
                }
                if (paper.quotedId.isNotBlank()) {
                    put("quoted_post_id", paper.quotedId)
                }
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/posts")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Prefer", "return=representation")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Post failed: ${response.code} $body"))
            }

            val returnedArray = JSONArray(body)
            val returnedRow = returnedArray.getJSONObject(0)
            val returnedPaper = paper.copy(
                id = returnedRow.getString("id"),
                publishedAt = parseIsoTimestamp(returnedRow.optString("created_at", ""))
            )

            Result.success(returnedPaper)
        } catch (e: Exception) {
            Log.e(TAG, "createPost error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // INTERACTIONS (LIKES & COMMENTS)
    // ==========================================

    suspend fun toggleLike(
        postId: String,
        userId: String,
        isCurrentlyEndorsed: Boolean,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isCurrentlyEndorsed) {
                // Add endorsement
                val json = JSONObject().apply {
                    put("post_id", postId)
                    put("user_id", userId)
                }
                val request = Request.Builder()
                    .url("${SupabaseConfig.URL}/rest/v1/post_likes")
                    .header("apikey", SupabaseConfig.ANON_KEY)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = httpClient.newCall(request).execute()
                Result.success(response.isSuccessful)
            } else {
                // Remove endorsement
                val request = Request.Builder()
                    .url("${SupabaseConfig.URL}/rest/v1/post_likes?post_id=eq.$postId&user_id=eq.$userId")
                    .header("apikey", SupabaseConfig.ANON_KEY)
                    .header("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()

                val response = httpClient.newCall(request).execute()
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Log.e(TAG, "toggleLike error", e)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        postId: String,
        authorId: String,
        content: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("post_id", postId)
                put("author_id", authorId)
                put("content", content.trim())
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/comments")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "addComment error", e)
            Result.failure(e)
        }
    }

    suspend fun getComments(
        postId: String,
        accessToken: String? = null
    ): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseConfig.ANON_KEY
            val url = "${SupabaseConfig.URL}/rest/v1/comments?select=*,author:profiles!comments_author_id_fkey(*)&post_id=eq.$postId&order=created_at.asc"

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to get comments: ${response.code}"))
            }

            val jsonArray = JSONArray(body)
            val list = mutableListOf<Comment>()

            for (i in 0 until jsonArray.length()) {
                val row = jsonArray.getJSONObject(i)
                val authorObj = row.optJSONObject("author")
                val authorName = authorObj?.optString("full_name", "")?.takeIf { it.isNotBlank() }
                    ?: authorObj?.optString("username", "Peer Reviewer") ?: "Peer Reviewer"

                list.add(
                    Comment(
                        id = row.getString("id"),
                        paperId = row.getString("post_id"),
                        authorInitials = getInitials(authorName),
                        authorName = authorName,
                        affiliation = authorObj?.optString("bio", "") ?: "",
                        body = row.optString("content", ""),
                        createdAt = parseIsoTimestamp(row.optString("created_at", ""))
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "getComments error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // ACADEMIC LOUNGE CHAT
    // ==========================================

    suspend fun getLoungeMessages(
        currentUserId: String,
        limit: Int = 50,
        accessToken: String? = null
    ): Result<List<ChatMessageEntity>> = withContext(Dispatchers.IO) {
        try {
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseConfig.ANON_KEY
            val url = "${SupabaseConfig.URL}/rest/v1/messages?select=*,sender:profiles(*)&conversation_id=eq.${SupabaseConfig.ACADEMIC_LOUNGE_CONVERSATION_ID}&order=created_at.asc&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to get messages: ${response.code}"))
            }

            val jsonArray = JSONArray(body)
            val messages = mutableListOf<ChatMessageEntity>()

            for (i in 0 until jsonArray.length()) {
                val row = jsonArray.getJSONObject(i)
                val senderId = row.optString("sender_id", "")
                val senderObj = row.optJSONObject("sender")
                val senderName = senderObj?.optString("full_name", "")?.takeIf { it.isNotBlank() }
                    ?: senderObj?.optString("username", "Colleague") ?: "Colleague"

                messages.add(
                    ChatMessageEntity(
                        id = row.getString("id"),
                        conversationId = SupabaseConfig.ACADEMIC_LOUNGE_CONVERSATION_ID,
                        senderName = senderName,
                        senderInitials = getInitials(senderName),
                        text = row.optString("content", ""),
                        timestamp = parseIsoTimestamp(row.optString("created_at", "")),
                        isOutgoing = senderId == currentUserId
                    )
                )
            }

            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "getLoungeMessages error", e)
            Result.failure(e)
        }
    }

    suspend fun sendLoungeMessage(
        senderId: String,
        content: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("conversation_id", SupabaseConfig.ACADEMIC_LOUNGE_CONVERSATION_ID)
                put("sender_id", senderId)
                put("content", content.trim())
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/messages")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "sendLoungeMessage error", e)
            Result.failure(e)
        }
    }
}
