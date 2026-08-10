package com.example.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Drives the real [RetrofitClient] configuration -- same converter factory, same annotations --
 * against a local mock server. This is what catches a broken `@Path`/`@Query` or a converter that
 * no longer handles the response content type; a hand-rolled fake service cannot.
 */
class GeminiApiServiceTest {

  private lateinit var server: MockWebServer
  private lateinit var service: GeminiApiService

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    service = RetrofitClient.create(server.url("/").toString())
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  private fun request(text: String = "hello") =
    GenerateContentRequest(
      contents = listOf(Content(role = "user", parts = listOf(Part(text = text))))
    )

  @Test
  fun `generateContent targets the documented path with the key as a query parameter`() =
    runBlocking {
      server.enqueue(
        MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("""{"candidates":[{"content":{"role":"model","parts":[{"text":"hi"}]}}]}""")
      )

      service.generateContent("gemini-3.5-flash", "secret-key", request())

      val recorded = server.takeRequest()
      assertEquals("POST", recorded.method)
      assertEquals(
        "/v1beta/models/gemini-3.5-flash:generateContent?key=secret-key",
        recorded.path,
      )
    }

  @Test
  fun `request body is sent as json`() = runBlocking {
    server.enqueue(
      MockResponse().setHeader("Content-Type", "application/json").setBody("""{"candidates":[]}""")
    )

    service.generateContent("gemini-3.5-flash", "k", request("what is a preprint?"))

    val recorded = server.takeRequest()
    assertTrue(recorded.getHeader("Content-Type").orEmpty().startsWith("application/json"))
    assertEquals(
      """{"contents":[{"role":"user","parts":[{"text":"what is a preprint?"}]}]}""",
      recorded.body.readUtf8(),
    )
  }

  @Test
  fun `response is decoded through the shipping converter`() = runBlocking {
    server.enqueue(
      MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(
          """
          {
            "candidates": [
              {"content": {"role": "model", "parts": [{"text": "A preprint is..."}]},
               "finishReason": "STOP"}
            ],
            "usageMetadata": {"promptTokenCount": 5}
          }
          """
            .trimIndent()
        )
    )

    val response = service.generateContent("gemini-3.5-flash", "k", request())

    assertEquals("A preprint is...", response.candidates?.single()?.content?.parts?.single()?.text)
  }

  @Test
  fun `model name is escaped into the path`() = runBlocking {
    server.enqueue(
      MockResponse().setHeader("Content-Type", "application/json").setBody("""{"candidates":[]}""")
    )

    service.generateContent("gemini-3.1-flash-lite-preview", "k", request())

    assertTrue(
      server.takeRequest().path.orEmpty().startsWith(
        "/v1beta/models/gemini-3.1-flash-lite-preview:generateContent"
      )
    )
  }

  @Test(expected = retrofit2.HttpException::class)
  fun `http error surfaces as an exception`(): Unit = runBlocking {
    server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"rate limited"}"""))

    service.generateContent("gemini-3.5-flash", "k", request())
    Unit
  }
}
