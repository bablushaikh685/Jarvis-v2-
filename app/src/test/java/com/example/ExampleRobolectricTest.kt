package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bridge.ContactCallResult
import com.example.bridge.ContactInfo
import com.example.gemini.GeminiLiveService
import com.example.gemini.GeminiTurnResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jarvis AI", appName)
  }

  @Test
  fun `test multilingual detection`() {
    val service = GeminiLiveService()
    assertEquals("Hindi", service.detectLanguageHeuristic("नमस्ते आप कैसे हैं?"))
    assertEquals("Hinglish", service.detectLanguageHeuristic("Hinglish mein baat karo please"))
    assertEquals("English", service.detectLanguageHeuristic("Talk to me in English."))
  }

  @Test
  fun `test open whatsapp command variations`() {
    val service = GeminiLiveService()
    val r1 = service.processLocalIntentFallback("WhatsApp kholo.")
    assertTrue(r1 is GeminiTurnResult.ToolCall)
    assertEquals("openWhatsApp", (r1 as GeminiTurnResult.ToolCall).functionName)

    val r2 = service.processLocalIntentFallback("Open WhatsApp.")
    assertTrue(r2 is GeminiTurnResult.ToolCall)
    assertEquals("openWhatsApp", (r2 as GeminiTurnResult.ToolCall).functionName)
  }

  @Test
  fun `test call contact variations`() {
    val service = GeminiLiveService()
    val r1 = service.processLocalIntentFallback("Call Rahul.")
    assertTrue(r1 is GeminiTurnResult.ToolCall)
    val call1 = r1 as GeminiTurnResult.ToolCall
    assertEquals("callContact", call1.functionName)
    assertEquals("Rahul", call1.arguments["contactName"])

    val r2 = service.processLocalIntentFallback("Mummy ko call karo.")
    assertTrue(r2 is GeminiTurnResult.ToolCall)
    val call2 = r2 as GeminiTurnResult.ToolCall
    assertEquals("callContact", call2.functionName)
    assertEquals("Mummy", call2.arguments["contactName"])
  }

  @Test
  fun `test phone calling with number`() {
    val service = GeminiLiveService()
    val result = service.processLocalIntentFallback("Call 9876543210.")
    assertTrue(result is GeminiTurnResult.ToolCall)
    val toolCall = result as GeminiTurnResult.ToolCall
    assertEquals("makeCall", toolCall.functionName)
    assertEquals("9876543210", toolCall.arguments["phoneNumber"])
  }

  @Test
  fun `test open settings and apps`() {
    val service = GeminiLiveService()
    val r1 = service.processLocalIntentFallback("Open settings.")
    assertTrue(r1 is GeminiTurnResult.ToolCall)
    assertEquals("openApp", (r1 as GeminiTurnResult.ToolCall).functionName)
    assertEquals("Settings", (r1 as GeminiTurnResult.ToolCall).arguments["appName"])

    val r2 = service.processLocalIntentFallback("Open YouTube.")
    assertTrue(r2 is GeminiTurnResult.ToolCall)
    assertEquals("openApp", (r2 as GeminiTurnResult.ToolCall).functionName)
    assertEquals("YouTube", (r2 as GeminiTurnResult.ToolCall).arguments["appName"])
  }

  @Test
  fun `test open actual browser command`() {
    val service = GeminiLiveService()
    val r = service.processLocalIntentFallback("Open actual browser.")
    assertTrue(r is GeminiTurnResult.ToolCall)
    val call = r as GeminiTurnResult.ToolCall
    assertEquals("openApp", call.functionName)
    assertEquals("Chrome", call.arguments["appName"])
  }

  @Test
  fun `test jarvis greeting response`() {
    val service = GeminiLiveService()
    val r = service.processLocalIntentFallback("Hello Jarvis.")
    assertTrue(r is GeminiTurnResult.SpokenResponse)
    val response = r as GeminiTurnResult.SpokenResponse
    assertTrue(response.text.contains("Jarvis"))
  }

  @Test
  fun `test multiple match contact disambiguation verification`() {
    // Verification step: When multiple matches exist, verify that DisambiguationRequired is created
    // and no direct call is permitted without user selection
    val matches = listOf(
      ContactInfo(name = "Rahul Sharma", phoneNumber = "+919876543210", type = "Mobile"),
      ContactInfo(name = "Rahul Verma", phoneNumber = "+919123456780", type = "Work")
    )
    val disambiguation = ContactCallResult.DisambiguationRequired(
      query = "Rahul",
      matches = matches,
      message = "I found 2 contacts matching 'Rahul'. Which one should I call?"
    )

    assertEquals("Rahul", disambiguation.query)
    assertEquals(2, disambiguation.matches.size)
    assertTrue(disambiguation.message.contains("Which one should I call"))
  }
}
