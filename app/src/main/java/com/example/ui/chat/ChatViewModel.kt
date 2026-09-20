package com.example.ui.chat

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream

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

    var currentModel = "gemini-2.0-flash"
    var useSearchGrounding = false

    fun sendMessage(text: String, image: Bitmap? = null) {
        val userMessage = ChatMessage(text = text, isUser = true, imageUrl = image)
        _messages.update { it + userMessage }

        val parts = mutableListOf<Part>()
        if (text.isNotBlank()) parts.add(Part(text = text))
        
        image?.let {
            val base64Image = it.toBase64()
            parts.add(Part(inlineData = InlineData("image/jpeg", base64Image)))
        }

        conversationHistory.add(Content(role = "user", parts = parts))

        val loadingMessage = ChatMessage(text = "", isUser = false, isLoading = true)
        _messages.update { it + loadingMessage }

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "your_actual_key_here") {
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

                val response = RetrofitClient.service.generateContent(currentModel, apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response text"
                
                conversationHistory.add(Content(role = "model", parts = listOf(Part(text = responseText))))

                _messages.update { list ->
                    list.dropLast(1) + ChatMessage(text = responseText, isUser = false)
                }
            } catch (e: Exception) {
                _messages.update { list ->
                    list.dropLast(1) + ChatMessage(text = "Error: ${e.message}", isUser = false, isError = true)
                }
            }
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
