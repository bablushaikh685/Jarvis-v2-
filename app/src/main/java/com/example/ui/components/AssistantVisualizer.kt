package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AssistantVisualizer(
  state: AssistantState,
  amplitude: Float,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "jarvis_reactor")

  // Primary HUD Rotation
  val primaryRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (state == AssistantState.THINKING) 2500 else 7500,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "primary_rotation"
  )

  // Secondary Counter-Rotation
  val counterRotation by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (state == AssistantState.THINKING) 3500 else 10500,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "counter_rotation"
  )

  // Radar Scan Sweep for Thinking / Processing
  val scanAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "scan_angle"
  )

  // Reactor Core Breathing Pulse
  val breathingPulse by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "breathing_pulse"
  )

  val smoothedAmplitude = remember { Animatable(0f) }
  LaunchedEffect(amplitude) {
    smoothedAmplitude.animateTo(
      targetValue = amplitude,
      animationSpec = tween(durationMillis = 60, easing = LinearEasing)
    )
  }

  // Color Palette Matrix
  val (primaryColor, secondaryColor, accentColor) = when (state) {
    AssistantState.READY -> Triple(Color(0xFF00E5FF), Color(0xFF0091EA), Color(0xFF2979FF))
    AssistantState.LISTENING -> Triple(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF7C4DFF))
    AssistantState.THINKING -> Triple(Color(0xFFFF9100), Color(0xFFFF3D00), Color(0xFFFFD600))
    AssistantState.SPEAKING -> Triple(Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFF1DE9B6))
    AssistantState.EXECUTING_ACTION -> Triple(Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF7C4DFF))
  }

  Box(
    modifier = modifier.testTag("assistant_visualizer_orb"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val maxRadius = size.minDimension / 2f - 8.dp.toPx()
      val amp = smoothedAmplitude.value
      val coreScale = breathingPulse * (1f + amp * 0.4f)
      val coreRadius = (maxRadius * 0.38f) * coreScale

      // 1. Ambient Background Glow Field
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.35f + amp * 0.25f),
            secondaryColor.copy(alpha = 0.12f),
            Color.Transparent
          ),
          center = center,
          radius = maxRadius * 1.1f
        ),
        radius = maxRadius * 1.1f,
        center = center
      )

      // 2. Outer Tactical HUD Reticle (Compass Ticks & Hash Marks)
      rotate(primaryRotation * 0.25f, pivot = center) {
        val tickRadius = maxRadius * 0.95f
        for (i in 0 until 72) {
          val isMajor = i % 6 == 0
          val tickLen = if (isMajor) 9.dp.toPx() else 4.dp.toPx()
          val angleRad = (i * 5) * (PI / 180.0)
          val start = Offset(
            (center.x + (tickRadius - tickLen) * cos(angleRad)).toFloat(),
            (center.y + (tickRadius - tickLen) * sin(angleRad)).toFloat()
          )
          val end = Offset(
            (center.x + tickRadius * cos(angleRad)).toFloat(),
            (center.y + tickRadius * sin(angleRad)).toFloat()
          )
          drawLine(
            color = if (isMajor) primaryColor.copy(alpha = 0.85f) else primaryColor.copy(alpha = 0.35f),
            start = start,
            end = end,
            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round
          )
        }

        // Thin outer bounding ring
        drawCircle(
          color = primaryColor.copy(alpha = 0.25f),
          radius = tickRadius,
          center = center,
          style = Stroke(width = 1.dp.toPx())
        )
      }

      // 3. Segmented Arc Rings (Clockwise & Counter-Clockwise)
      rotate(primaryRotation, pivot = center) {
        // 3 Segmented Glowing Outer Arcs
        val arcRadius = maxRadius * 0.82f
        val arcSize = Size(arcRadius * 2, arcRadius * 2)
        val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

        for (i in 0 until 3) {
          drawArc(
            brush = Brush.sweepGradient(
              colors = listOf(primaryColor, primaryColor.copy(alpha = 0.1f)),
              center = center
            ),
            startAngle = i * 120f + 10f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
          )
        }
      }

      rotate(counterRotation, pivot = center) {
        // 4 Dashed Middle Arcs
        val midRadius = maxRadius * 0.68f
        val midSize = Size(midRadius * 2, midRadius * 2)
        val midTopLeft = Offset(center.x - midRadius, center.y - midRadius)

        for (i in 0 until 4) {
          drawArc(
            color = secondaryColor.copy(alpha = 0.75f),
            startAngle = i * 90f + 12f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = midTopLeft,
            size = midSize,
            style = Stroke(
              width = 2.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
          )
        }
      }

      // 4. Thinking/Processing Radar Sweep Beam
      if (state == AssistantState.THINKING) {
        rotate(scanAngle, pivot = center) {
          drawArc(
            brush = Brush.sweepGradient(
              colors = listOf(
                Color.Transparent,
                accentColor.copy(alpha = 0.05f),
                accentColor.copy(alpha = 0.5f)
              ),
              center = center
            ),
            startAngle = 0f,
            sweepAngle = 60f,
            useCenter = true,
            topLeft = Offset(center.x - maxRadius * 0.82f, center.y - maxRadius * 0.82f),
            size = Size(maxRadius * 1.64f, maxRadius * 1.64f)
          )
        }
      }

      // 5. Arc Reactor Coil Blocks (10 Electromagnetic Coils)
      rotate(primaryRotation * 0.5f, pivot = center) {
        val numCoils = 10
        val coilInnerRadius = coreRadius * 1.15f
        val coilOuterRadius = coreRadius * 1.45f

        for (i in 0 until numCoils) {
          val angleDeg = i * (360f / numCoils)
          val angleRad = angleDeg * (PI / 180.0)

          val cX = center.x + (coilInnerRadius + coilOuterRadius) / 2f * cos(angleRad).toFloat()
          val cY = center.y + (coilInnerRadius + coilOuterRadius) / 2f * sin(angleRad).toFloat()

          // Draw coil block
          rotate(angleDeg + 90f, pivot = Offset(cX, cY)) {
            val coilW = 10.dp.toPx()
            val coilH = (coilOuterRadius - coilInnerRadius)

            // Coil container
            drawRoundRect(
              color = primaryColor.copy(alpha = 0.4f),
              topLeft = Offset(cX - coilW / 2f, cY - coilH / 2f),
              size = Size(coilW, coilH),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
              style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner glowing filament
            drawRoundRect(
              color = Color.White.copy(alpha = 0.7f + amp * 0.3f),
              topLeft = Offset(cX - coilW * 0.25f, cY - coilH * 0.35f),
              size = Size(coilW * 0.5f, coilH * 0.7f),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
          }
        }
      }

      // 6. Dynamic Soundwave Frequency Filaments (Reacting to Voice Amplitude)
      if (amp > 0.05f || state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
        val waveRadius = coreRadius * 1.65f
        val wavePath = Path()
        val numPoints = 64
        val waveFactor = 16.dp.toPx() * amp

        for (i in 0..numPoints) {
          val angleRad = (i * (360.0 / numPoints)) * (PI / 180.0)
          val harmonic = sin(angleRad * 6.0 + primaryRotation * 0.08).toFloat() * waveFactor
          val r = waveRadius + harmonic
          val ptX = (center.x + r * cos(angleRad)).toFloat()
          val ptY = (center.y + r * sin(angleRad)).toFloat()

          if (i == 0) wavePath.moveTo(ptX, ptY) else wavePath.lineTo(ptX, ptY)
        }
        wavePath.close()

        drawPath(
          path = wavePath,
          color = primaryColor.copy(alpha = 0.65f + amp * 0.35f),
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
      }

      // 7. Central Arc Reactor Core
      // Multilayer high-intensity radial plasma core
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.98f),
            primaryColor.copy(alpha = 0.92f),
            secondaryColor.copy(alpha = 0.65f),
            accentColor.copy(alpha = 0.2f),
            Color.Transparent
          ),
          center = center,
          radius = coreRadius
        ),
        radius = coreRadius,
        center = center
      )

      // Inner Reactor Aperture Ring
      drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = coreRadius * 0.65f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // Triangular Arc Reactor Inner Geometry (Tony Stark / JARVIS iconic core)
      rotate(-primaryRotation * 0.8f, pivot = center) {
        val triRadius = coreRadius * 0.55f
        val triPath = Path()
        for (i in 0 until 3) {
          val a = (i * 120.0 - 90.0) * (PI / 180.0)
          val tX = (center.x + triRadius * cos(a)).toFloat()
          val tY = (center.y + triRadius * sin(a)).toFloat()
          if (i == 0) triPath.moveTo(tX, tY) else triPath.lineTo(tX, tY)
        }
        triPath.close()

        drawPath(
          path = triPath,
          color = primaryColor.copy(alpha = 0.75f),
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Center High-Density Singularity Point
        drawCircle(
          color = Color.White,
          radius = 5.dp.toPx() * (1f + amp * 0.5f),
          center = center
        )
      }
    }
  }
}
