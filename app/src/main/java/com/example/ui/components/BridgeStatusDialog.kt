package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BridgeStatusDialog(
  isWhatsAppInstalled: Boolean,
  hasCallPermission: Boolean,
  hasContactsPermission: Boolean,
  onRequestCallPermission: () -> Unit,
  onRequestContactsPermission: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("bridge_status_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = Color(0xFF00E5FF),
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Android Action Bridge Status",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Native Android Bridge is connected. Gemini Live calls real Android Intent APIs instead of browser mockups.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // WhatsApp
        BridgeStatusRow(
          title = "WhatsApp Status",
          subtitle = if (isWhatsAppInstalled) "Installed and ready for launch" else "Not installed (will report gracefully)",
          isReady = isWhatsAppInstalled
        )

        // Contacts Permission
        BridgeStatusRow(
          title = "Contacts Search (callContact)",
          subtitle = if (hasContactsPermission) "Permission granted (full search active)" else "Permission needed for name lookup",
          isReady = hasContactsPermission,
          actionLabel = if (!hasContactsPermission) "Grant" else null,
          onAction = onRequestContactsPermission
        )

        // Call Permission
        BridgeStatusRow(
          title = "Direct Calling (makeCall)",
          subtitle = if (hasCallPermission) "Direct call permission granted" else "Safe dialer fallback active",
          isReady = hasCallPermission,
          actionLabel = if (!hasCallPermission) "Grant" else null,
          onAction = onRequestCallPermission
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
          text = "Registered Functions: openApp, makeCall, callContact, openWhatsApp, openUrl.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun BridgeStatusRow(
  title: String,
  subtitle: String,
  isReady: Boolean,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
      contentDescription = null,
      tint = if (isReady) Color(0xFF00E676) else Color(0xFFFFB300),
      modifier = Modifier.size(20.dp)
    )

    Spacer(modifier = Modifier.width(10.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    if (actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(actionLabel, fontSize = 12.sp)
      }
    }
  }
}
