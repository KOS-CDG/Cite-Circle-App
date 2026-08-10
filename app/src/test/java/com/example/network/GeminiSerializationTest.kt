package com.example.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Gemini wire format. These DTOs are only ever validated at runtime against a live API, so
 * a renamed or dropped field would otherwise fail silently in production rather than at build time.
 */
class GeminiSerializationTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `request serializes to the field names the api expects`() {
    val request =
      GenerateContentRequest(
        contents = listOf(Content(role = "user", parts = listOf(Part(text = "hello"))))
      )

    val encoded = json.encodeToString(GenerateContentRequest.serializer(), request)

    assertEquals("""{"contents":[{"role":"user","parts":[{"text":"hello"}]}]}""", encoded)
  }

  @Test
  fun `null optional fields are omitted from the request`() {
    val request =
      GenerateContentRequest(contents = listOf(Content(parts = listOf(Part(text = "hi")))))

    val encoded = json.encodeToString(GenerateContentRequest.serializer(), request)

    assertTrue("role should be omitted when null", !encoded.contains("role"))
    assertTrue("tools should be omitted when null", !encoded.contains("tools"))
    assertTrue("generationConfig should be omitted", !encoded.contains("generationConfig"))
    assertTrue("systemInstruction should be omitted", !encoded.contains("systemInstruction"))
  }

  @Test
  fun `inline image data serializes with camelCase keys`() {
    val request =
      GenerateContentRequest(
        contents =
          listOf(
            Content(
              role = "user",
              parts = listOf(Part(inlineData = InlineData("image/jpeg", "QUJD"))),
            )
          )
      )

    val encoded = json.encodeToString(GenerateContentRequest.serializer(), request)

    assertTrue(encoded.contains(""""inlineData""""))
    assertTrue(encoded.contains(""""mimeType":"image/jpeg""""))
    assertTrue(encoded.contains(""""data":"QUJD""""))
  }

  @Test
  fun `google search tool serializes as an empty object`() {
    val request =
      GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = "hi")))),
        tools = listOf(Tool(googleSearch = JsonObject(emptyMap()))),
      )

    val encoded = json.encodeToString(GenerateContentRequest.serializer(), request)

    assertTrue(encoded.contains(""""tools":[{"googleSearch":{}}]"""))
  }

  @Test
  fun `realistic response deserializes`() {
    val payload =
      """
      {
        "candidates": [
          {
            "content": {
              "role": "model",
              "parts": [{"text": "A preprint is a manuscript shared before peer review."}]
            }
          }
        ]
      }
      """
        .trimIndent()

    val response = json.decodeFromString(GenerateContentResponse.serializer(), payload)

    assertEquals(
      "A preprint is a manuscript shared before peer review.",
      response.candidates?.single()?.content?.parts?.single()?.text,
    )
    assertEquals("model", response.candidates?.single()?.content?.role)
  }

  @Test
  fun `unknown response fields are ignored`() {
    val payload =
      """
      {
        "candidates": [
          {
            "content": {"role": "model", "parts": [{"text": "hi"}]},
            "finishReason": "STOP",
            "safetyRatings": [{"category": "HARM_CATEGORY_HATE_SPEECH", "probability": "NEGLIGIBLE"}]
          }
        ],
        "usageMetadata": {"promptTokenCount": 7}
      }
      """
        .trimIndent()

    val response = json.decodeFromString(GenerateContentResponse.serializer(), payload)

    assertEquals("hi", response.candidates?.single()?.content?.parts?.single()?.text)
  }

  @Test
  fun `response without candidates deserializes to null rather than throwing`() {
    val response = json.decodeFromString(GenerateContentResponse.serializer(), "{}")

    assertNull(response.candidates)
  }

  @Test
  fun `grounding metadata is retained when present`() {
    val payload =
      """
      {
        "candidates": [
          {
            "content": {"role": "model", "parts": [{"text": "hi"}]},
            "groundingMetadata": {"webSearchQueries": ["preprint"]}
          }
        ]
      }
      """
        .trimIndent()

    val response = json.decodeFromString(GenerateContentResponse.serializer(), payload)

    assertTrue(
      response.candidates?.single()?.groundingMetadata?.containsKey("webSearchQueries") == true
    )
  }
}
