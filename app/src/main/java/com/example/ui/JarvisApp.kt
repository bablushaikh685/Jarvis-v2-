package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bridge.ContactInfo
import com.example.ui.components.ActionControlBar
import com.example.ui.components.AssistantVisualizer
import com.example.ui.components.BridgeStatusDialog
import com.example.ui.components.InteractionFeed
import com.example.ui.theme.MyApplicationTheme

@Composable
fun JarvisApp(
  viewModel: AssistantViewModel,
  onRequestAudioPermission: () -> Unit,
  onRequestCallPermission: () -> Unit,
  onRequestContactsPermission: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()
  var showBridgeDialog by remember { mutableStateOf(false) }

  JarvisScreen(
    uiState = uiState,
    onMicClick = {
      onRequestAudioPermission()
      viewModel.toggleListening()
    },
    onInterruptClick = {
      viewModel.interruptSpeaking()
    },
    onSendTextPrompt = { prompt ->
      viewModel.processUserPrompt(prompt)
    },
    onContactSelected = { contact ->
      viewModel.callSpecificContact(contact)
    },
    onReplayAudio = { audioBase64 ->
      viewModel.replayAudio(audioBase64)
    },
    onCancelCall = {
      viewModel.cancelActiveCall()
    },
    onOpenBrowser = {
      viewModel.processUserPrompt("Open actual browser")
    },
    onOpenBridgeStatus = {
      viewModel.updateBridgeStatus()
      showBridgeDialog = true
    }
  )

  if (showBridgeDialog) {
    BridgeStatusDialog(
      isWhatsAppInstalled = uiState.isWhatsAppInstalled,
      hasCallPermission = uiState.hasCallPermission,
      hasContactsPermission = uiState.hasContactsPermission,
      onRequestCallPermission = {
        onRequestCallPermission()
      },
      onRequestContactsPermission = {
        onRequestContactsPermission()
      },
      onDismiss = { showBridgeDialog = false }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(
  uiState: AssistantUiState,
  onMicClick: () -> Unit,
  onInterruptClick: () -> Unit,
  onSendTextPrompt: (String) -> Unit,
  onContactSelected: (ContactInfo) -> Unit,
  onReplayAudio: (String) -> Unit,
  onCancelCall: () -> Unit,
  onOpenBrowser: () -> Unit,
  onOpenBridgeStatus: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
                  )
                )
                .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Jarvis AI",
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFF00E676).copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "CYBER CORE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00E676),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }

              Text(
                text = "Multilingual Voice • Neural Device Matrix",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = onOpenBrowser,
            modifier = Modifier.testTag("open_browser_header_button")
          ) {
            Icon(
              imageVector = Icons.Default.OpenInBrowser,
              contentDescription = "Open Actual Browser",
              tint = Color(0xFF00E5FF)
            )
          }

          IconButton(
            onClick = onOpenBridgeStatus,
            modifier = Modifier.testTag("bridge_status_button")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Device Action Bridge",
              tint = Color(0xFF00E5FF)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.TopCenter
    ) {
      val isWideScreen = maxWidth > 600.dp
      val orbSize = if (isWideScreen) 190.dp else 145.dp

      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Holographic Reactor Core & Status Area
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            AssistantVisualizer(
              state = uiState.assistantState,
              amplitude = uiState.orbAmplitude,
              modifier = Modifier.size(orbSize)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // State Chip
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = when (uiState.assistantState) {
                AssistantState.LISTENING -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                AssistantState.SPEAKING -> Color(0xFF00E676).copy(alpha = 0.2f)
                AssistantState.THINKING -> Color(0xFFFF9100).copy(alpha = 0.2f)
                AssistantState.EXECUTING_ACTION -> Color(0xFF7C4DFF).copy(alpha = 0.2f)
                AssistantState.READY -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
              },
              modifier = Modifier.padding(horizontal = 16.dp)
            ) {
              Text(
                text = uiState.statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = when (uiState.assistantState) {
                  AssistantState.LISTENING -> Color(0xFF00E5FF)
                  AssistantState.SPEAKING -> Color(0xFF00E676)
                  AssistantState.THINKING -> Color(0xFFFF9100)
                  AssistantState.EXECUTING_ACTION -> Color(0xFFB388FF)
                  AssistantState.READY -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                  .padding(horizontal = 14.dp, vertical = 5.dp)
                  .testTag("assistant_status_text"),
                textAlign = TextAlign.Center,
                maxLines = 2
              )
            }

            // Real-time partial speech preview
            if (uiState.partialSpeech.isNotBlank()) {
              Text(
                text = "\"${uiState.partialSpeech}...\"",
                fontSize = 12.sp,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(top = 2.dp)
              )
            }

            // Active Call Interruption Banner
            if (uiState.activeCallTarget != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFD50000).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 4.dp)
                  .testTag("active_call_banner")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                      imageVector = Icons.Default.Call,
                      contentDescription = null,
                      tint = Color(0xFFFF5252),
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Calling: ${uiState.activeCallTarget}",
                      color = Color.White,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 12.sp,
                      maxLines = 1
                    )
                  }
                  ElevatedButton(
                    onClick = onCancelCall,
                    colors = ButtonDefaults.elevatedButtonColors(
                      containerColor = Color(0xFFFF1744),
                      contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("cancel_call_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.CallEnd,
                      contentDescription = "Cancel Call",
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        // Live Interaction Feed
        InteractionFeed(
          logs = uiState.logs,
          onContactSelected = onContactSelected,
          onReplayAudio = onReplayAudio,
          onCancelCall = onCancelCall,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        )

        // Bottom Controls
        ActionControlBar(
          assistantState = uiState.assistantState,
          isListening = uiState.isListening,
          isAudioPlaying = uiState.isAudioPlaying,
          onMicClick = onMicClick,
          onInterruptClick = onInterruptClick,
          onSendTextPrompt = onSendTextPrompt
        )
      }
    }
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
fun JarvisScreenPreview() {
  MyApplicationTheme {
    JarvisScreen(
      uiState = AssistantUiState(
        assistantState = AssistantState.READY,
        statusText = "Ready. Ask Jarvis anything or give a command.",
        detectedLanguage = "English",
        logs = listOf(
          InteractionLog.AssistantResponse(
            text = "Greetings! I am Jarvis. All cyber defense protocols and device actions are online.",
            language = "English"
          )
        )
      ),
      onMicClick = {},
      onInterruptClick = {},
      onSendTextPrompt = {},
      onContactSelected = {},
      onReplayAudio = {},
      onCancelCall = {},
      onOpenBrowser = {},
      onOpenBridgeStatus = {}
    )
  }
}
