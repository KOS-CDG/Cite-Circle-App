package com.example.network

import android.util.Log
import com.example.data.CollectionEntity
import com.example.data.Comment
import com.example.data.PaperCollectionEntry
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
import java.io.File
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

    data class AppUpdateInfo(
        val updateAvailable: Boolean,
        val latestVersionName: String,
        val latestVersionCode: Int,
        val mandatory: Boolean,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileSizeMb: Double
    )

    suspend fun checkForAppUpdate(currentVersionCode: Int = 1): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/functions/v1/cite-server?action=check_update&code=$currentVersionCode"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Server error ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(null)
            val json = JSONObject(body)

            if (!json.optBoolean("success", false)) {
                return@withContext Result.success(null)
            }

            val info = AppUpdateInfo(
                updateAvailable = json.optBoolean("update_available", false),
                latestVersionName = json.optString("latest_version_name", "1.0"),
                latestVersionCode = json.optInt("latest_version_code", 1),
                mandatory = json.optBoolean("mandatory", false),
                releaseNotes = json.optString("release_notes", "Performance and stability improvements."),
                downloadUrl = json.optString("download_url", ""),
                fileSizeMb = json.optDouble("file_size_mb", 0.0)
            )

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "checkForAppUpdate error", e)
            Result.failure(e)
        }
    }

    data class NotificationItemDto(
        val id: String,
        val recipientId: String,
        val actorId: String,
        val actorName: String,
        val actorUsername: String,
        val actorAvatarUrl: String?,
        val type: String,
        val postId: String?,
        val postTitle: String?,
        val isRead: Boolean,
        val createdAt: Long
    )

    suspend fun getNotifications(
        recipientId: String,
        accessToken: String
    ): Result<List<NotificationItemDto>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/notifications?select=*,actor:profiles!notifications_actor_id_fkey(*),post:posts!notifications_post_id_fkey(id,content,metadata)&recipient_id=eq.$recipientId&order=created_at.desc&limit=50"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to get notifications: ${response.code} $body"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<NotificationItemDto>()

            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                val actorObj = row.optJSONObject("actor")
                val postObj = row.optJSONObject("post")
                val postMeta = postObj?.optJSONObject("metadata")

                val actorName = actorObj?.optString("full_name", "")?.takeIf { it.isNotBlank() }
                    ?: actorObj?.optString("username", "Researcher") ?: "Researcher"
                val postTitle = postMeta?.optString("title", "")?.takeIf { it.isNotBlank() }
                    ?: postObj?.optString("content", "")?.take(60) ?: "Research Paper"

                list.add(
                    NotificationItemDto(
                        id = row.getString("id"),
                        recipientId = row.getString("recipient_id"),
                        actorId = row.getString("actor_id"),
                        actorName = actorName,
                        actorUsername = actorObj?.optString("username", "") ?: "",
                        actorAvatarUrl = actorObj?.optString("avatar_url")?.takeIf { it.isNotBlank() },
                        type = row.optString("type", "like"),
                        postId = row.optString("post_id").takeIf { it.isNotBlank() },
                        postTitle = postTitle,
                        isRead = row.optBoolean("is_read", false),
                        createdAt = parseIsoTimestamp(row.optString("created_at", ""))
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "getNotifications error", e)
            Result.failure(e)
        }
    }

    suspend fun markNotificationsAsRead(
        recipientId: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/notifications?recipient_id=eq.$recipientId&is_read=eq.false"
            val updateBody = JSONObject().apply {
                put("is_read", true)
            }
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .patch(updateBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "markNotificationsAsRead error", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads user avatar to Supabase Storage and synchronizes avatar_url in public.profiles.
     */
    suspend fun uploadAvatar(
        userId: String,
        imageFile: File,
        accessToken: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() <= 0) {
            return@withContext Result.failure(IOException("Avatar file does not exist or is empty"))
        }

        try {
            val fileName = "avatar_${userId}.jpg"
            val bucket = "manuscripts" // Public bucket configured in Supabase
            val storageUrl = "${SupabaseConfig.URL}/storage/v1/object/$bucket/avatars/$fileName"

            val requestBody = imageFile.readBytes().toRequestBody("image/jpeg".toMediaType())
            val uploadRequest = Request.Builder()
                .url(storageUrl)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("x-upsert", "true")
                .header("Content-Type", "image/jpeg")
                .post(requestBody)
                .build()

            val uploadResponse = httpClient.newCall(uploadRequest).execute()
            if (!uploadResponse.isSuccessful && uploadResponse.code != 200 && uploadResponse.code != 201) {
                val errorBody = uploadResponse.body?.string() ?: ""
                Log.w(TAG, "Avatar storage upload returned ${uploadResponse.code}: $errorBody")
            }

            val publicUrl = "${SupabaseConfig.URL}/storage/v1/object/public/$bucket/avatars/$fileName"

            // Update avatar_url in public.profiles
            val profileUrl = "${SupabaseConfig.URL}/rest/v1/profiles?id=eq.$userId"
            val profileBody = JSONObject().apply {
                put("avatar_url", publicUrl)
            }
            val profileRequest = Request.Builder()
                .url(profileUrl)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .patch(profileBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val profileResponse = httpClient.newCall(profileRequest).execute()
            if (!profileResponse.isSuccessful) {
                Log.w(TAG, "Profile avatar_url update returned ${profileResponse.code}")
            }

            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "uploadAvatar error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // REPOSITORY & LIBRARY CLOUD SYNC
    // ==========================================

    private fun formatIsoTimestamp(millis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return format.format(java.util.Date(millis))
    }

    suspend fun pushUserCollections(
        userId: String,
        collections: List<CollectionEntity>,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (collections.isEmpty()) return@withContext Result.success(true)
        try {
            val array = JSONArray()
            for (c in collections) {
                val obj = JSONObject().apply {
                    put("user_id", userId)
                    put("id", c.id)
                    put("name", c.name)
                    put("description", c.description)
                    put("color_hex", c.colorHex)
                    put("icon_name", c.iconName)
                    put("created_at", formatIsoTimestamp(c.createdAt))
                    put("updated_at", formatIsoTimestamp(c.updatedAt))
                }
                array.put(obj)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/user_collections?on_conflict=user_id,id")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Prefer", "resolution=merge-duplicates")
                .header("Content-Type", "application/json")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to push collections: ${response.code} ${response.body?.string()}"))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "pushUserCollections error", e)
            Result.failure(e)
        }
    }

    suspend fun pullUserCollections(
        userId: String,
        accessToken: String
    ): Result<List<CollectionEntity>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_collections?user_id=eq.$userId&order=updated_at.desc"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to pull collections: ${response.code} $body"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<CollectionEntity>()
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                list.add(
                    CollectionEntity(
                        id = row.getString("id"),
                        name = row.getString("name"),
                        description = row.optString("description", ""),
                        colorHex = row.optString("color_hex", "#1A73E8"),
                        iconName = row.optString("icon_name", "folder"),
                        createdAt = parseIsoTimestamp(row.optString("created_at", "")),
                        updatedAt = parseIsoTimestamp(row.optString("updated_at", ""))
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "pullUserCollections error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserCollection(
        userId: String,
        collectionId: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_collections?user_id=eq.$userId&id=eq.$collectionId"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "deleteUserCollection error", e)
            Result.failure(e)
        }
    }

    suspend fun pushUserCollectionEntries(
        userId: String,
        entries: List<PaperCollectionEntry>,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext Result.success(true)
        try {
            val array = JSONArray()
            for (e in entries) {
                val obj = JSONObject().apply {
                    put("user_id", userId)
                    put("collection_id", e.collectionId)
                    put("paper_id", e.paperId)
                    put("added_at", formatIsoTimestamp(e.addedAt))
                }
                array.put(obj)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/user_collection_papers?on_conflict=user_id,collection_id,paper_id")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Prefer", "resolution=merge-duplicates")
                .header("Content-Type", "application/json")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to push entries: ${response.code} ${response.body?.string()}"))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "pushUserCollectionEntries error", e)
            Result.failure(e)
        }
    }

    suspend fun pullUserCollectionEntries(
        userId: String,
        accessToken: String
    ): Result<List<PaperCollectionEntry>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_collection_papers?user_id=eq.$userId"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to pull entries: ${response.code} $body"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<PaperCollectionEntry>()
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                list.add(
                    PaperCollectionEntry(
                        paperId = row.getString("paper_id"),
                        collectionId = row.getString("collection_id"),
                        addedAt = parseIsoTimestamp(row.optString("added_at", ""))
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "pullUserCollectionEntries error", e)
            Result.failure(e)
        }
    }

    suspend fun removeUserCollectionEntry(
        userId: String,
        collectionId: String,
        paperId: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_collection_papers?user_id=eq.$userId&collection_id=eq.$collectionId&paper_id=eq.$paperId"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "removeUserCollectionEntry error", e)
            Result.failure(e)
        }
    }

    suspend fun pushUserLibraryPapers(
        userId: String,
        papers: List<SavedPaper>,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (papers.isEmpty()) return@withContext Result.success(true)
        try {
            val array = JSONArray()
            for (p in papers) {
                val obj = JSONObject().apply {
                    put("user_id", userId)
                    put("id", p.id)
                    put("title", p.title)
                    put("authors", p.authors)
                    put("year", p.year)
                    put("venue", p.venue)
                    put("doi", p.doi)
                    put("url", p.url)
                    put("pdf_url", p.pdfUrl)
                    put("abstract_text", p.abstractText)
                    put("open_access", p.openAccess)
                    put("content", p.content)
                    put("author_name", p.authorName)
                    put("author_initials", p.authorInitials)
                    put("affiliation", p.affiliation)
                    put("citation_override", p.citationOverride)
                    put("reading_status", p.readingStatus)
                    put("research_notes", p.researchNotes)
                    put("is_bookmarked", p.isBookmarked)
                    put("last_read_page", p.lastReadPage)
                    put("total_page_count", p.totalPageCount)
                    put("page_bookmarks", p.pageBookmarks)
                    put("published_at", p.publishedAt)
                    put("updated_at", formatIsoTimestamp(System.currentTimeMillis()))
                }
                array.put(obj)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/user_library_papers?on_conflict=user_id,id")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Prefer", "resolution=merge-duplicates")
                .header("Content-Type", "application/json")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to push papers: ${response.code} ${response.body?.string()}"))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "pushUserLibraryPapers error", e)
            Result.failure(e)
        }
    }

    suspend fun pullUserLibraryPapers(
        userId: String,
        accessToken: String
    ): Result<List<SavedPaper>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_library_papers?user_id=eq.$userId&order=updated_at.desc"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to pull papers: ${response.code} $body"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<SavedPaper>()
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                val authorName = row.optString("author_name", "")
                val authorInitials = row.optString("author_initials", "").ifBlank { getInitials(authorName) }
                list.add(
                    SavedPaper(
                        id = row.getString("id"),
                        authorInitials = authorInitials,
                        authorName = authorName,
                        affiliation = row.optString("affiliation", ""),
                        content = row.optString("content", ""),
                        title = row.optString("title", ""),
                        authors = row.optString("authors", ""),
                        year = row.optString("year", ""),
                        venue = row.optString("venue", ""),
                        doi = row.optString("doi", ""),
                        url = row.optString("url", ""),
                        publishedAt = row.optLong("published_at", parseIsoTimestamp(row.optString("created_at", ""))),
                        citationOverride = row.optString("citation_override", ""),
                        isEndorsed = false,
                        endorsementCount = 0,
                        commentCount = 0,
                        repostCount = 0,
                        isBookmarked = row.optBoolean("is_bookmarked", false),
                        imageUri = "",
                        quotedId = "",
                        quotedAuthorName = "",
                        quotedTitle = "",
                        quotedContent = "",
                        pdfUrl = row.optString("pdf_url", ""),
                        pdfLocalPath = "",
                        abstractText = row.optString("abstract_text", ""),
                        openAccess = row.optBoolean("open_access", false),
                        readingStatus = row.optString("reading_status", "TO_READ"),
                        researchNotes = row.optString("research_notes", ""),
                        lastReadPage = row.optInt("last_read_page", 1),
                        totalPageCount = row.optInt("total_page_count", 0),
                        pageBookmarks = row.optString("page_bookmarks", "")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "pullUserLibraryPapers error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserLibraryPaper(
        userId: String,
        paperId: String,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_library_papers?user_id=eq.$userId&id=eq.$paperId"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "deleteUserLibraryPaper error", e)
            Result.failure(e)
        }
    }

    suspend fun syncUserPaperMeta(
        userId: String,
        paperId: String,
        readingStatus: String,
        notes: String,
        isBookmarked: Boolean,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_library_papers?user_id=eq.$userId&id=eq.$paperId"
            val body = JSONObject().apply {
                put("reading_status", readingStatus)
                put("research_notes", notes)
                put("is_bookmarked", isBookmarked)
                put("updated_at", formatIsoTimestamp(System.currentTimeMillis()))
            }
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "syncUserPaperMeta error", e)
            Result.failure(e)
        }
    }

    suspend fun syncUserPaperReadingProgress(
        userId: String,
        paperId: String,
        lastReadPage: Int,
        totalPageCount: Int,
        pageBookmarks: String,
        readingStatus: String? = null,
        accessToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.URL}/rest/v1/user_library_papers?user_id=eq.$userId&id=eq.$paperId"
            val body = JSONObject().apply {
                put("last_read_page", lastReadPage)
                put("total_page_count", totalPageCount)
                put("page_bookmarks", pageBookmarks)
                if (readingStatus != null) {
                    put("reading_status", readingStatus)
                }
                put("updated_at", formatIsoTimestamp(System.currentTimeMillis()))
            }
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "syncUserPaperReadingProgress error", e)
            Result.failure(e)
        }
    }
}

