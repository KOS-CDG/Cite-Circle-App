package com.example.ui.chat

import com.example.network.Candidate
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerateContentResponse
import com.example.network.GeminiApiService
import com.example.network.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers [ChatViewModel]'s message-list bookkeeping and request construction against a recording
 * fake service. Nothing here touches the network or [com.example.BuildConfig].
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

  /** Captures each request and replies with whatever the test queued. */
  private class FakeGeminiApiService : GeminiApiService {
    val requests = mutableListOf<Triple<String, String, GenerateContentRequest>>()
    var response: GenerateContentResponse = textResponse("Hello from the model")
    var error: Exception? = null

    override suspend fun generateContent(
      model: String,
      apiKey: String,
      request: GenerateContentRequest,
    ): GenerateContentResponse {
      requests += Triple(model, apiKey, request)
      error?.let { throw it }
      return response
    }

    override suspend fun generateContentStream(
      model: String,
      apiKey: String,
      request: GenerateContentRequest,
    ): ResponseBody = throw UnsupportedOperationException("not used")
  }

  private lateinit var service: FakeGeminiApiService

  @Before
  fun setUp() {
    // Unconfined so viewModelScope work runs eagerly and the list is settled when send returns.
    Dispatchers.setMain(Dispatchers.Unconfined)
    service = FakeGeminiApiService()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() = ChatViewModel(service, apiKey = "test-key")

  @Test
  fun `user message is recorded and answered`() {
    val viewModel = createViewModel()

    viewModel.sendMessage("What is a preprint?")

    val messages = viewModel.messages.value
    assertEquals(2, messages.size)
    assertEquals("What is a preprint?", messages[0].text)
    assertTrue(messages[0].isUser)
    assertEquals("Hello from the model", messages[1].text)
    assertFalse(messages[1].isUser)
  }

  @Test
  fun `loading placeholder does not survive a successful response`() {
    val viewModel = createViewModel()

    viewModel.sendMessage("hi")

    assertTrue(viewModel.messages.value.none { it.isLoading })
  }

  @Test
  fun `api key and model are forwarded to the service`() {
    val viewModel = createViewModel()
    viewModel.currentModel = "gemini-3.1-pro-preview"

    viewModel.sendMessage("hi")

    val (model, apiKey, _) = service.requests.single()
    assertEquals("gemini-3.1-pro-preview", model)
    assertEquals("test-key", apiKey)
  }

  @Test
  fun `search grounding is off by default`() {
    val viewModel = createViewModel()

    viewModel.sendMessage("hi")

    assertNull(service.requests.single().third.tools)
  }

  @Test
  fun `enabling search grounding attaches the google search tool`() {
    val viewModel = createViewModel()
    viewModel.useSearchGrounding = true

    viewModel.sendMessage("hi")

    val tools = service.requests.single().third.tools
    assertEquals(1, tools?.size)
    assertNotNull(tools?.single()?.googleSearch)
  }

  @Test
  fun `system instruction is always sent`() {
    val viewModel = createViewModel()

    viewModel.sendMessage("hi")

    val instruction = service.requests.single().third.systemInstruction
    assertTrue(
      instruction?.parts?.single()?.text.orEmpty().contains("Cite Circle")
    )
  }

  @Test
  fun `conversation history accumulates across turns`() {
    val viewModel = createViewModel()

    viewModel.sendMessage("first")
    service.response = textResponse("second answer")
    viewModel.sendMessage("second")

    // Turn two carries: user "first", model reply, user "second".
    val secondRequest = service.requests[1].third
    assertEquals(
      listOf("first", "Hello from the model", "second"),
      secondRequest.contents.map { it.parts.single().text },
    )
    assertEquals(listOf("user", "model", "user"), secondRequest.contents.map { it.role })
  }

  @Test
  fun `service failure surfaces exactly one error message`() {
    val viewModel = createViewModel()
    service.error = IllegalStateException("network down")

    viewModel.sendMessage("hi")

    val messages = viewModel.messages.value
    assertEquals(2, messages.size)
    assertTrue(messages.none { it.isLoading })
    val error = messages.single { it.isError }
    assertTrue(error.text.contains("network down"))
    assertFalse(error.isUser)
  }

  @Test
  fun `empty candidate list falls back to placeholder text`() {
    val viewModel = createViewModel()
    service.response = GenerateContentResponse(candidates = null)

    viewModel.sendMessage("hi")

    assertEquals("No response text", viewModel.messages.value.last().text)
  }

  @Test
  fun `candidate without parts falls back to placeholder text`() {
    val viewModel = createViewModel()
    service.response = GenerateContentResponse(candidates = listOf(Candidate(content = null)))

    viewModel.sendMessage("hi")

    assertEquals("No response text", viewModel.messages.value.last().text)
  }

  @Test
  fun `blank text is not added as a request part`() {
    val viewModel = createViewModel()

    viewModel.sendMessage("")

    assertTrue(service.requests.single().third.contents.single().parts.isEmpty())
  }

}

private fun textResponse(text: String) =
  GenerateContentResponse(
    candidates =
      listOf(Candidate(content = Content(role = "model", parts = listOf(Part(text = text)))))
  )
