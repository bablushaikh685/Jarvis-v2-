package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GeminiAudioPlayer
import com.example.audio.VoiceInputManager
import com.example.bridge.ActionResult
import com.example.bridge.AndroidAppActionBridge
import com.example.bridge.ContactCallResult
import com.example.bridge.ContactInfo
import com.example.gemini.GeminiLiveService
import com.example.gemini.GeminiTurnResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

enum class AssistantState {
  READY,
  LISTENING,
  THINKING,
  SPEAKING,
  EXECUTING_ACTION
}

sealed class InteractionLog {
  data class UserQuery(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timeMillis: Long = System.currentTimeMillis()
  ) : InteractionLog()

  data class AssistantResponse(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val language: String,
    val audioBase64: String? = null,
    val timeMillis: Long = System.currentTimeMillis()
  ) : InteractionLog()

  data class ActionExecuted(
    val id: String = UUID.randomUUID().toString(),
    val actionName: String,
    val target: String,
    val success: Boolean,
    val message: String,
    val timeMillis: Long = System.currentTimeMillis()
  ) : InteractionLog()

  data class DisambiguateContacts(
    val id: String = UUID.randomUUID().toString(),
    val query: String,
    val matches: List<ContactInfo>,
    val timeMillis: Long = System.currentTimeMillis()
  ) : InteractionLog()
}

data class DisambiguationSession(
  val query: String,
  val matches: List<ContactInfo>
)

data class AssistantUiState(
  val assistantState: AssistantState = AssistantState.READY,
  val statusText: String = "Ready. Ask Jarvis anything or give a command.",
  val detectedLanguage: String = "English",
  val partialSpeech: String = "",
  val logs: List<InteractionLog> = emptyList(),
  val isListening: Boolean = false,
  val isAudioPlaying: Boolean = false,
  val orbAmplitude: Float = 0f,
  val isWhatsAppInstalled: Boolean = false,
  val hasCallPermission: Boolean = false,
  val hasContactsPermission: Boolean = false,
  val activeCallTarget: String? = null,
  val pendingDisambiguation: DisambiguationSession? = null
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

  private val actionBridge = AndroidAppActionBridge(application)
  private val geminiService = GeminiLiveService()
  val audioPlayer = GeminiAudioPlayer(application)

  private val conversationHistory = mutableListOf<JSONObject>()
  private var activeDisambiguation: DisambiguationSession? = null

  private val _uiState = MutableStateFlow(AssistantUiState())
  val uiState = _uiState.asStateFlow()

  private val voiceInputManager = VoiceInputManager(
    context = application,
    onSpeechResult = { recognizedText ->
      processUserPrompt(recognizedText)
    },
    onPartialSpeech = { partial ->
      _uiState.update { it.copy(partialSpeech = partial) }
    },
    onSpeechStarted = {
      // Immediate interruption as soon as user opens mouth to speak
      interruptSpeaking()
    },
    onErrorOccurred = { errorMsg ->
      _uiState.update {
        it.copy(
          assistantState = AssistantState.READY,
          isListening = false,
          statusText = errorMsg,
          partialSpeech = ""
        )
      }
    }
  )

  init {
    updateBridgeStatus()

    viewModelScope.launch {
      voiceInputManager.isListening.collect { listening ->
        _uiState.update {
          it.copy(
            isListening = listening,
            assistantState = if (listening) AssistantState.LISTENING else if (it.isAudioPlaying) AssistantState.SPEAKING else AssistantState.READY
          )
        }
      }
    }

    viewModelScope.launch {
      voiceInputManager.micRmsDb.collect { rms ->
        if (_uiState.value.isListening) {
          _uiState.update { it.copy(orbAmplitude = rms) }
        }
      }
    }

    viewModelScope.launch {
      audioPlayer.isPlaying.collect { playing ->
        _uiState.update {
          it.copy(
            isAudioPlaying = playing,
            assistantState = if (playing) AssistantState.SPEAKING else if (it.isListening) AssistantState.LISTENING else AssistantState.READY
          )
        }
      }
    }

    viewModelScope.launch {
      audioPlayer.audioAmplitude.collect { amp ->
        if (_uiState.value.isAudioPlaying) {
          _uiState.update { it.copy(orbAmplitude = amp) }
        }
      }
    }

    // Initial greeting
    addLog(
      InteractionLog.AssistantResponse(
        text = "Greetings! I am Jarvis. Cybernetic voice and device control systems are online. Ask me to open WhatsApp, make a call, open any app, or speak in Hindi, English, Hinglish, Marathi, etc.",
        language = "English"
      )
    )
  }

  fun updateBridgeStatus() {
    _uiState.update {
      it.copy(
        isWhatsAppInstalled = actionBridge.isWhatsAppInstalled(),
        hasCallPermission = actionBridge.hasCallPermission(),
        hasContactsPermission = actionBridge.hasContactsPermission()
      )
    }
  }

  fun toggleListening() {
    // Interruption constraint: If Jarvis is currently speaking, stop audio immediately
    if (_uiState.value.isAudioPlaying) {
      interruptSpeaking()
      return
    }

    if (_uiState.value.isListening) {
      voiceInputManager.stopListening()
      _uiState.update { it.copy(isListening = false, assistantState = AssistantState.READY) }
    } else {
      _uiState.update {
        it.copy(
          isListening = true,
          assistantState = AssistantState.LISTENING,
          statusText = "Listening to your voice...",
          partialSpeech = ""
        )
      }
      voiceInputManager.startListening()
    }
  }

  fun interruptSpeaking() {
    audioPlayer.clearAudioBuffersAndHalt()
    voiceInputManager.stopListening()
    _uiState.update {
      it.copy(
        isAudioPlaying = false,
        isListening = false,
        orbAmplitude = 0f,
        assistantState = AssistantState.READY,
        statusText = "Ready."
      )
    }
  }

  fun cancelActiveCall() {
    audioPlayer.clearAudioBuffersAndHalt()
    voiceInputManager.stopListening()
    activeDisambiguation = null
    _uiState.update {
      it.copy(
        activeCallTarget = null,
        pendingDisambiguation = null,
        isAudioPlaying = false,
        orbAmplitude = 0f,
        assistantState = AssistantState.READY,
        statusText = "Call canceled."
      )
    }
    addLog(
      InteractionLog.ActionExecuted(
        actionName = "cancelCall",
        target = "Call Interrupted",
        success = true,
        message = "Call action was interrupted and stopped."
      )
    )
  }

  fun processUserPrompt(prompt: String) {
    if (prompt.isBlank()) return

    // Stop active audio immediately upon new user utterance (interruption)
    audioPlayer.stopPlayback()
    voiceInputManager.stopListening()

    val lower = prompt.trim().lowercase()

    // 1. Explicit Call / Action Interruption keywords
    val isInterruption = lower == "cancel" || lower == "stop" || lower == "cancel call" ||
      lower == "don't call" || lower == "dont call" || lower == "ruko" || lower == "ruk jao" ||
      lower == "nahi" || lower == "disconnect" || lower == "cut call" || lower == "cut the call"

    if (isInterruption) {
      val hadActiveCallOrDisambiguation = _uiState.value.activeCallTarget != null || activeDisambiguation != null
      activeDisambiguation = null
      _uiState.update {
        it.copy(
          activeCallTarget = null,
          pendingDisambiguation = null,
          assistantState = AssistantState.READY,
          statusText = if (hadActiveCallOrDisambiguation) "Call canceled." else "Command canceled.",
          partialSpeech = ""
        )
      }
      addLog(InteractionLog.UserQuery(text = prompt))
      addLog(
        InteractionLog.AssistantResponse(
          text = if (hadActiveCallOrDisambiguation) "Call canceled. Systems standing by." else "Action canceled.",
          language = "English"
        )
      )
      return
    }

    // 2. Pending Disambiguation Verification:
    // If Jarvis previously asked which contact to call among multiple matches
    val currentDisambiguation = activeDisambiguation
    if (currentDisambiguation != null) {
      val matchedContact = resolveDisambiguationChoice(lower, currentDisambiguation.matches)
      if (matchedContact != null) {
        activeDisambiguation = null
        _uiState.update { it.copy(pendingDisambiguation = null) }
        addLog(InteractionLog.UserQuery(text = prompt))
        callSpecificContact(matchedContact)
        return
      }
    }

    addLog(InteractionLog.UserQuery(text = prompt))
    _uiState.update {
      it.copy(
        assistantState = AssistantState.THINKING,
        statusText = "Jarvis is processing...",
        partialSpeech = ""
      )
    }

    viewModelScope.launch {
      val turnResult = geminiService.processUserTurn(conversationHistory, prompt)

      when (turnResult) {
        is GeminiTurnResult.ToolCall -> {
          executeToolCall(turnResult, prompt)
        }
        is GeminiTurnResult.SpokenResponse -> {
          handleSpokenResponse(turnResult)
        }
        is GeminiTurnResult.Error -> {
          _uiState.update {
            it.copy(
              assistantState = AssistantState.READY,
              statusText = turnResult.message
            )
          }
        }
      }
    }
  }

  private fun resolveDisambiguationChoice(input: String, matches: List<ContactInfo>): ContactInfo? {
    val clean = input.replace(Regex("""[^a-zA-Z0-9\s]"""), "").trim()

    // 1. Ordinal check (first, 1st, one, pahla)
    if (clean.contains("first") || clean.contains("1st") || clean == "1" || clean.contains("one") || clean.contains("pahla")) {
      return matches.getOrNull(0)
    }
    // 2. Second check (second, 2nd, two, doosra)
    if (clean.contains("second") || clean.contains("2nd") || clean == "2" || clean.contains("two") || clean.contains("doosra")) {
      return matches.getOrNull(1)
    }
    // 3. Third check
    if (clean.contains("third") || clean.contains("3rd") || clean == "3" || clean.contains("three") || clean.contains("teesra")) {
      return matches.getOrNull(2)
    }

    // 4. Contact name or phone type match
    return matches.firstOrNull { contact ->
      val nameLower = contact.name.lowercase()
      val typeLower = contact.type.lowercase()
      clean.contains(nameLower) || nameLower.contains(clean) ||
        (typeLower.isNotBlank() && clean.contains(typeLower))
    }
  }

  private suspend fun executeToolCall(toolCall: GeminiTurnResult.ToolCall, originalPrompt: String) {
    // Robust State Synchronization: Clear and flush any buffered audio before command execution begins
    audioPlayer.clearAudioBuffersAndHalt()
    voiceInputManager.stopListening()

    _uiState.update {
      it.copy(
        assistantState = AssistantState.EXECUTING_ACTION,
        isAudioPlaying = false,
        isListening = false,
        orbAmplitude = 0f,
        statusText = "Executing: ${toolCall.functionName}..."
      )
    }

    var resultSummary = ""
    var actionExecuted = false
    var disambiguationRequired = false
    var disambiguationQuery = ""
    var disambiguationMatchesCount = 0

    when (toolCall.functionName) {
      "openWhatsApp" -> {
        val result = actionBridge.openWhatsApp()
        resultSummary = result.message
        actionExecuted = result.success
        addLog(
          InteractionLog.ActionExecuted(
            actionName = "openWhatsApp",
            target = "WhatsApp",
            success = result.success,
            message = result.message
          )
        )
      }

      "openApp" -> {
        val appName = toolCall.arguments["appName"] ?: "Settings"
        val result = actionBridge.openApp(appName)
        resultSummary = result.message
        actionExecuted = result.success
        addLog(
          InteractionLog.ActionExecuted(
            actionName = "openApp",
            target = appName,
            success = result.success,
            message = result.message
          )
        )
      }

      "makeCall" -> {
        val phone = toolCall.arguments["phoneNumber"] ?: ""
        // Stop any audio before call intent
        audioPlayer.stopPlayback()
        val result = actionBridge.makeCall(phone)
        resultSummary = result.message
        actionExecuted = result.success
        if (result.success) {
          _uiState.update { it.copy(activeCallTarget = phone) }
        }
        addLog(
          InteractionLog.ActionExecuted(
            actionName = "makeCall",
            target = phone,
            success = result.success,
            message = result.message
          )
        )
      }

      "callContact" -> {
        val contactName = toolCall.arguments["contactName"] ?: ""
        val result = actionBridge.callContact(contactName)
        when (result) {
          is ContactCallResult.Initiated -> {
            resultSummary = result.message
            actionExecuted = true
            activeDisambiguation = null
            _uiState.update {
              it.copy(
                activeCallTarget = "${result.contact.name} (${result.contact.phoneNumber})",
                pendingDisambiguation = null
              )
            }
            addLog(
              InteractionLog.ActionExecuted(
                actionName = "callContact",
                target = "${result.contact.name} (${result.contact.phoneNumber})",
                success = true,
                message = result.message
              )
            )
          }
          is ContactCallResult.DisambiguationRequired -> {
            // VERIFICATION STEP: Multiple matches found. Do NOT place an incorrect call!
            resultSummary = result.message
            actionExecuted = false
            disambiguationRequired = true
            disambiguationQuery = result.query
            disambiguationMatchesCount = result.matches.size

            val session = DisambiguationSession(
              query = result.query,
              matches = result.matches
            )
            activeDisambiguation = session
            _uiState.update {
              it.copy(
                pendingDisambiguation = session,
                activeCallTarget = null
              )
            }
            addLog(
              InteractionLog.DisambiguateContacts(
                query = result.query,
                matches = result.matches
              )
            )
          }
          is ContactCallResult.NotFound -> {
            resultSummary = result.message
            actionExecuted = false
            activeDisambiguation = null
            _uiState.update { it.copy(pendingDisambiguation = null) }
            addLog(
              InteractionLog.ActionExecuted(
                actionName = "callContact",
                target = contactName,
                success = false,
                message = result.message
              )
            )
          }
          is ContactCallResult.PermissionRequired -> {
            resultSummary = result.message
            actionExecuted = false
            activeDisambiguation = null
            _uiState.update { it.copy(pendingDisambiguation = null) }
            addLog(
              InteractionLog.ActionExecuted(
                actionName = "callContact",
                target = contactName,
                success = false,
                message = result.message
              )
            )
          }
          is ContactCallResult.Error -> {
            resultSummary = result.message
            actionExecuted = false
            activeDisambiguation = null
            _uiState.update { it.copy(pendingDisambiguation = null) }
            addLog(
              InteractionLog.ActionExecuted(
                actionName = "callContact",
                target = contactName,
                success = false,
                message = result.message
              )
            )
          }
        }
      }

      "openUrl" -> {
        val url = toolCall.arguments["url"] ?: "https://google.com"
        val result = actionBridge.openUrl(url)
        resultSummary = result.message
        actionExecuted = result.success
        addLog(
          InteractionLog.ActionExecuted(
            actionName = "openUrl",
            target = url,
            success = result.success,
            message = result.message
          )
        )
      }

      else -> {
        resultSummary = "Action not supported."
        actionExecuted = false
      }
    }

    // Build structured tool response payload including disambiguation flags
    val toolResultJson = JSONObject()
      .put("success", actionExecuted)
      .put("message", resultSummary)
      .apply {
        if (disambiguationRequired) {
          put("requiresDisambiguation", true)
          put("query", disambiguationQuery)
          put("matchesCount", disambiguationMatchesCount)
        }
      }
      .toString()

    // Follow up through Gemini conversation loop
    val finalResponse = geminiService.continueAfterToolExecution(
      conversationHistory = conversationHistory,
      functionName = toolCall.functionName,
      toolResultJson = toolResultJson,
      userQuery = originalPrompt
    )

    handleSpokenResponse(finalResponse)
  }

  private fun handleSpokenResponse(response: GeminiTurnResult.SpokenResponse) {
    addLog(
      InteractionLog.AssistantResponse(
        text = response.text,
        language = response.detectedLanguage,
        audioBase64 = response.audioBase64
      )
    )

    _uiState.update {
      it.copy(
        assistantState = if (response.audioBase64 != null) AssistantState.SPEAKING else AssistantState.READY,
        statusText = response.text,
        detectedLanguage = response.detectedLanguage
      )
    }

    if (!response.audioBase64.isNullOrEmpty()) {
      audioPlayer.playBase64Audio(
        base64Data = response.audioBase64,
        mimeType = response.audioMimeType ?: "audio/wav",
        onComplete = {
          _uiState.update {
            it.copy(
              assistantState = AssistantState.READY,
              statusText = "Ready."
            )
          }
        }
      )
    } else {
      _uiState.update {
        it.copy(
          assistantState = AssistantState.READY,
          statusText = response.text
        )
      }
    }
  }

  fun callSpecificContact(contact: ContactInfo) {
    // Interruption check: immediately stop any playing voice
    audioPlayer.stopPlayback()
    activeDisambiguation = null
    _uiState.update {
      it.copy(
        activeCallTarget = "${contact.name} (${contact.phoneNumber})",
        pendingDisambiguation = null,
        statusText = "Calling ${contact.name}...",
        assistantState = AssistantState.READY
      )
    }
    val result = actionBridge.makeCall(contact.phoneNumber)
    addLog(
      InteractionLog.ActionExecuted(
        actionName = "makeCall",
        target = "${contact.name} (${contact.phoneNumber})",
        success = result.success,
        message = result.message
      )
    )
  }

  fun replayAudio(base64: String) {
    interruptSpeaking()
    audioPlayer.playBase64Audio(base64)
  }

  private fun addLog(item: InteractionLog) {
    _uiState.update { it.copy(logs = it.logs + item) }
  }

  override fun onCleared() {
    super.onCleared()
    voiceInputManager.destroyRecognizer()
    audioPlayer.stopPlayback()
  }
}
