package com.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@Serializable
data class DeepSeekChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val temperature: Double? = null,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null
)

@Serializable
data class DeepSeekMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class DeepSeekChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<DeepSeekChoice>? = null,
    val usage: DeepSeekUsage? = null
)

@Serializable
data class DeepSeekChoice(
    val index: Int = 0,
    val message: DeepSeekResponseMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class DeepSeekResponseMessage(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

@Serializable
data class DeepSeekUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

interface DeepSeekApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekChatRequest
    ): DeepSeekChatResponse
}

object DeepSeekClient {
    const val BASE_URL = "https://api.deepseek.com/"
    const val DEFAULT_TOKEN = "sk-20ab9e0968ac4ae5878ca819caf82e26"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: DeepSeekApiService by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(DeepSeekApiService::class.java)
    }
}
