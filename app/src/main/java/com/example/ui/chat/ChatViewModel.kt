package com.example.ui.chat

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val imageUrl: Bitmap? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val conversationHistory = mutableListOf<Content>()

    var currentModel by mutableStateOf("gemini-2.5-flash")
    var useSearchGrounding by mutableStateOf(false)

    fun clearChat() {
        conversationHistory.clear()
        _messages.value = emptyList()
    }

    fun retryLastMessage() {
        val lastUserMsgIndex = _messages.value.indexOfLast { it.isUser }
        if (lastUserMsgIndex == -1) return
        val lastUserMsg = _messages.value[lastUserMsgIndex]

        // Drop trailing error message if present
        if (_messages.value.isNotEmpty() && _messages.value.last().isError) {
            _messages.update { it.dropLast(1) }
        }

        // Remove the failed user bubble from list so re-sending replaces it cleanly
        _messages.update { list ->
            list.filterIndexed { index, _ -> index != lastUserMsgIndex }
        }

        sendMessage(lastUserMsg.text, lastUserMsg.imageUrl)
    }

    fun sendMessage(text: String, image: Bitmap? = null) {
        if (text.isBlank() && image == null) return

        val userMessage = ChatMessage(text = text, isUser = true, imageUrl = image)
        _messages.update { it + userMessage }

        val parts = mutableListOf<Part>()
        if (text.isNotBlank()) {
            parts.add(Part(text = text))
        } else if (image != null) {
            parts.add(Part(text = "Please analyze and describe this academic diagram, chart, or document in detail."))
        }

        image?.let {
            val base64Image = it.toSafeBase64()
            parts.add(Part(inlineData = InlineData("image/jpeg", base64Image)))
        }

        conversationHistory.add(Content(role = "user", parts = parts))

        val loadingMessage = ChatMessage(text = "", isUser = false, isLoading = true)
        _messages.update { it + loadingMessage }

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "your_actual_key_here") {
                    if (conversationHistory.isNotEmpty() && conversationHistory.last().role == "user") {
                        conversationHistory.removeAt(conversationHistory.lastIndex)
                    }
                    _messages.update { list ->
                        list.dropLast(1) + ChatMessage(
                            text = "⚠️ Gemini API key not configured.\n\nPlease add GEMINI_API_KEY=your_key to your .env file and rebuild the app. You can get a free key at https://aistudio.google.com/",
                            isUser = false,
                            isError = true
                        )
                    }
                    return@launch
                }

                val tools = if (useSearchGrounding) {
                    listOf(Tool(googleSearch = JsonObject(emptyMap())))
                } else null

                val request = GenerateContentRequest(
                    contents = conversationHistory.toList(),
                    systemInstruction = Content(parts = listOf(Part(text = "You are a helpful, professional AI assistant for the Cite Circle academic network."))),
                    tools = tools
                )

                // Automatic retry loop with exponential backoff on transient 503 demand spikes and 429 rate limits
                var attempts = 0
                var lastException: Exception? = null
                var response: GenerateContentResponse? = null

                while (attempts < 3) {
                    try {
                        response = RetrofitClient.service.generateContent(currentModel, apiKey, request)
                        break
                    } catch (e: HttpException) {
                        lastException = e
                        if (e.code() == 503 || e.code() == 429) {
                            attempts++
                            if (attempts < 3) {
                                delay(attempts * 1500L)
                                continue
                            }
                        }
                        throw e
                    } catch (e: IOException) {
                        lastException = e
                        attempts++
                        if (attempts < 3) {
                            delay(attempts * 1000L)
                            continue
                        }
                        throw e
                    }
                }

                val finalResponse = response ?: throw (lastException ?: IllegalStateException("No response received"))
                val candidate = finalResponse.candidates?.firstOrNull()
                val candidateText = candidate?.content?.parts
                    ?.mapNotNull { it.text }
                    ?.filter { it.isNotBlank() }
                    ?.joinToString("\n")

                val responseText = when {
                    !candidateText.isNullOrBlank() -> candidateText
                    candidate?.finishReason == "SAFETY" -> "⚠️ Response was blocked by Google AI content safety policies."
                    candidate?.finishReason == "RECITATION" -> "⚠️ Response was blocked by recitation check."
                    else -> "I couldn't generate a response. Please try rephrasing your question."
                }

                conversationHistory.add(Content(role = "model", parts = listOf(Part(text = responseText))))

                _messages.update { list ->
                    list.dropLast(1) + ChatMessage(text = responseText, isUser = false)
                }
            } catch (e: Exception) {
                // Remove trailing user turn from history so multi-turn alternation remains valid
                if (conversationHistory.isNotEmpty() && conversationHistory.last().role == "user") {
                    conversationHistory.removeAt(conversationHistory.lastIndex)
                }

                val errorMessage = if (e is HttpException) {
                    try {
                        val errorJson = e.response()?.errorBody()?.string() ?: ""
                        val json = Json { ignoreUnknownKeys = true }
                        val parsedMessage = json.parseToJsonElement(errorJson)
                            .jsonObject["error"]
                            ?.jsonObject?.get("message")
                            ?.jsonPrimitive?.content
                        parsedMessage ?: "HTTP ${e.code()}: ${e.message()}"
                    } catch (_: Exception) {
                        "HTTP ${e.code()}: ${e.message()}"
                    }
                } else {
                    e.localizedMessage ?: e.message ?: "Unknown error"
                }

                _messages.update { list ->
                    list.dropLast(1) + ChatMessage(text = "Error: $errorMessage", isUser = false, isError = true)
                }
            }
        }
    }

    private fun Bitmap.toSafeBase64(): String {
        val safeBitmap = if (config == Bitmap.Config.HARDWARE) {
            copy(Bitmap.Config.ARGB_8888, false) ?: this
        } else {
            this
        }
        val maxDim = 1536
        val scaledBitmap = if (safeBitmap.width > maxDim || safeBitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / safeBitmap.width, maxDim.toFloat() / safeBitmap.height)
            val newWidth = (safeBitmap.width * ratio).toInt()
            val newHeight = (safeBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(safeBitmap, newWidth, newHeight, true)
        } else {
            safeBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
