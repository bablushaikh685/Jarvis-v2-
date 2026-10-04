package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ActionControlBar(
  assistantState: AssistantState,
  isListening: Boolean,
  isAudioPlaying: Boolean,
  onMicClick: () -> Unit,
  onInterruptClick: () -> Unit,
  onSendTextPrompt: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var textInput by remember { mutableStateOf("") }
  val quickTestPrompts = listOf(
    "Open actual browser",
    "WhatsApp kholo",
    "Open WhatsApp",
    "Mummy ko call karo",
    "Call Rahul",
    "Call 9876543210",
    "Hindi mein baat karo",
    "Talk to me in English",
    "Hinglish mein baat karo",
    "Open YouTube",
    "Open settings"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "cyber_mic_transitions")

  // Outer Reticle Rotation
  val ringRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (isListening) 3000 else if (isAudioPlaying) 2000 else 9000,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "ring_rotation"
  )

  // Cyber Pulsing Glow
  val glowPulse by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (isListening) 600 else 1200,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_pulse"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .padding(bottom = 12.dp, top = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Quick Test Chips (Horizontal Scroll)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      quickTestPrompts.forEach { prompt ->
        AssistChip(
          onClick = { onSendTextPrompt(prompt) },
          label = { Text(prompt, fontSize = 11.sp) },
          colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          modifier = Modifier.testTag("quick_chip_${prompt.replace(" ", "_")}")
        )
      }
    }

    Spacer(modifier = Modifier.height(2.dp))

    // Cyber Visualizer Equalizer Waveform Bars
    Row(
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.height(14.dp)
    ) {
      val barColors = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF00E676))
      val isLive = isListening || isAudioPlaying
      for (i in 0 until 5) {
        val animBar = remember { Animatable(3f) }
        LaunchedEffect(isLive) {
          if (isLive) {
            animBar.animateTo(
              targetValue = (6 + (i * 2) % 6).toFloat(),
              animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 200 + i * 50, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
              )
            )
          } else {
            animBar.snapTo(3f)
          }
        }
        Box(
          modifier = Modifier
            .width(3.dp)
            .height(animBar.value.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(if (isLive) barColors[i] else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
      }
    }

    // Redesigned Cybernetic Reactor Mic Button
    Box(
      modifier = Modifier
        .padding(vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      // 1. Ambient Dynamic Cyber Halo
      val haloColor = when {
        isAudioPlaying -> Color(0xFFFF3D00)
        isListening -> Color(0xFF00E5FF)
        else -> Color(0xFF00E5FF)
      }

      Box(
        modifier = Modifier
          .size(92.dp)
          .scale(if (isListening || isAudioPlaying) glowPulse else 1f)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                haloColor.copy(alpha = if (isListening) 0.35f else if (isAudioPlaying) 0.45f else 0.15f),
                Color.Transparent
              )
            )
          )
      )

      // 2. Rotating HUD Reticle Ring with Precision Tick Marks
      Canvas(
        modifier = Modifier
          .size(86.dp)
          .rotate(ringRotation)
      ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 2.dp.toPx()

        // Segmented Dashed Arc
        drawCircle(
          brush = Brush.sweepGradient(
            colors = listOf(
              haloColor.copy(alpha = 0.85f),
              Color.Transparent,
              haloColor.copy(alpha = 0.4f),
              Color.Transparent
            ),
            center = center
          ),
          radius = radius,
          center = center,
          style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
          )
        )

        // 12 Radar Compass Ticks
        for (i in 0 until 12) {
          val angleRad = (i * 30.0) * (PI / 180.0)
          val tickLen = if (i % 3 == 0) 5.dp.toPx() else 3.dp.toPx()
          val sX = (center.x + (radius - tickLen) * cos(angleRad)).toFloat()
          val sY = (center.y + (radius - tickLen) * sin(angleRad)).toFloat()
          val eX = (center.x + radius * cos(angleRad)).toFloat()
          val eY = (center.y + radius * sin(angleRad)).toFloat()

          drawLine(
            color = haloColor.copy(alpha = if (i % 3 == 0) 0.9f else 0.4f),
            start = Offset(sX, sY),
            end = Offset(eX, eY),
            strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
      }

      // 3. Cyber Reactor Button Surface
      val coreBrush = when {
        isAudioPlaying -> Brush.linearGradient(
          listOf(Color(0xFFD50000), Color(0xFFFF5252))
        )
        isListening -> Brush.linearGradient(
          listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
        )
        else -> Brush.linearGradient(
          listOf(Color(0xFF0D1424), Color(0xFF1E293B))
        )
      }

      val borderColor = when {
        isAudioPlaying -> Color(0xFFFF5252)
        isListening -> Color(0xFF00E5FF)
        else -> Color(0xFF00E5FF).copy(alpha = 0.7f)
      }

      Surface(
        onClick = {
          if (isAudioPlaying) {
            onInterruptClick()
          } else {
            onMicClick()
          }
        },
        shape = CircleShape,
        modifier = Modifier
          .size(68.dp)
          .border(2.dp, borderColor, CircleShape)
          .testTag("main_mic_action_button"),
        color = Color.Transparent,
        shadowElevation = 10.dp
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(coreBrush),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when {
              isAudioPlaying -> Icons.Default.Stop
              isListening -> Icons.Default.Mic
              else -> Icons.Default.Mic
            },
            contentDescription = if (isAudioPlaying) "Interrupt Speaking" else if (isListening) "Stop Listening" else "Start Voice Command",
            tint = if (isAudioPlaying || isListening) Color.White else Color(0xFF00E5FF),
            modifier = Modifier.size(32.dp)
          )
        }
      }
    }

    // Cyber Telemetry Status Indicator
    Text(
      text = when {
        isAudioPlaying -> "PCM STREAM ACTIVE // TAP TO INTERRUPT"
        isListening -> "NEURAL AUDIO MATRIX // LISTENING..."
        else -> "CYBER COMMAND READY // TAP MIC OR TYPE"
      },
      fontFamily = FontFamily.Monospace,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      color = when {
        isAudioPlaying -> Color(0xFFFF5252)
        isListening -> Color(0xFF00E5FF)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
      },
      modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
    )

    // Text Input Bar with Test Tag
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = textInput,
        onValueChange = { textInput = it },
        placeholder = { Text("Ask Jarvis or type command...", fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFF00E5FF),
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
          onSend = {
            if (textInput.isNotBlank()) {
              onSendTextPrompt(textInput)
              textInput = ""
            }
          }
        ),
        trailingIcon = {
          IconButton(
            onClick = {
              if (textInput.isNotBlank()) {
                onSendTextPrompt(textInput)
                textInput = ""
              }
            },
            modifier = Modifier.testTag("send_prompt_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send Command",
              tint = Color(0xFF00E5FF)
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("prompt_text_field")
      )
    }
  }
}
