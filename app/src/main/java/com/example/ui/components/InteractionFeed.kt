package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bridge.ContactInfo
import com.example.ui.InteractionLog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractionFeed(
  logs: List<InteractionLog>,
  onContactSelected: (ContactInfo) -> Unit,
  onReplayAudio: (String) -> Unit,
  onCancelCall: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()

  LaunchedEffect(logs.size) {
    if (logs.isNotEmpty()) {
      listState.animateScrollToItem(logs.size - 1)
    }
  }

  LazyColumn(
    state = listState,
    modifier = modifier
      .fillMaxWidth()
      .testTag("interaction_feed_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(logs, key = {
      when (it) {
        is InteractionLog.UserQuery -> it.id
        is InteractionLog.AssistantResponse -> it.id
        is InteractionLog.ActionExecuted -> it.id
        is InteractionLog.DisambiguateContacts -> it.id
      }
    }) { logItem ->
      when (logItem) {
        is InteractionLog.UserQuery -> {
          UserMessageItem(text = logItem.text)
        }
        is InteractionLog.AssistantResponse -> {
          AssistantMessageItem(
            text = logItem.text,
            language = logItem.language,
            audioBase64 = logItem.audioBase64,
            onReplayAudio = onReplayAudio
          )
        }
        is InteractionLog.ActionExecuted -> {
          ActionExecutedItem(
            actionName = logItem.actionName,
            target = logItem.target,
            success = logItem.success,
            message = logItem.message
          )
        }
        is InteractionLog.DisambiguateContacts -> {
          DisambiguationItem(
            query = logItem.query,
            matches = logItem.matches,
            onContactSelected = onContactSelected,
            onCancelCall = onCancelCall
          )
        }
      }
    }
  }
}

@Composable
fun UserMessageItem(text: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("user_message_row"),
    horizontalArrangement = Arrangement.End
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .background(
          Brush.horizontalGradient(
            colors = listOf(Color(0xFF2979FF), Color(0xFF00E5FF))
          )
        )
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
      )
    }
  }
}

@Composable
fun AssistantMessageItem(
  text: String,
  language: String,
  audioBase64: String?,
  onReplayAudio: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("assistant_message_row"),
    horizontalArrangement = Arrangement.Start
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Column {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Jarvis",
              color = Color(0xFF00E5FF),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
              onClick = {},
              label = { Text(language, fontSize = 11.sp) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp)
                )
              },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
              ),
              modifier = Modifier.height(24.dp)
            )

            if (!audioBase64.isNullOrEmpty()) {
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(
                onClick = { onReplayAudio(audioBase64) },
                modifier = Modifier
                  .size(28.dp)
                  .testTag("replay_voice_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = "Replay Gemini Voice",
                  tint = Color(0xFF00E5FF),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = text,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 15.sp,
          lineHeight = 21.sp
        )
      }
    }
  }
}

@Composable
fun ActionExecutedItem(
  actionName: String,
  target: String,
  success: Boolean,
  message: String
) {
  val accentColor = if (success) Color(0xFF00E676) else Color(0xFFFF5252)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("action_executed_card"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ),
    shape = RoundedCornerShape(12.dp),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        listOf(accentColor.copy(alpha = 0.6f), Color.Transparent)
      )
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        val icon = when (actionName) {
          "openWhatsApp" -> Icons.Outlined.ChatBubbleOutline
          "makeCall" -> Icons.Default.Call
          "callContact" -> Icons.Default.Person
          else -> Icons.AutoMirrored.Filled.Launch
        }
        Icon(
          imageVector = icon,
          contentDescription = actionName,
          tint = accentColor,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Action: $actionName",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = if (success) "Success" else "Failed",
            tint = accentColor,
            modifier = Modifier.size(14.dp)
          )
        }

        Text(
          text = message,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisambiguationItem(
  query: String,
  matches: List<ContactInfo>,
  onContactSelected: (ContactInfo) -> Unit,
  onCancelCall: () -> Unit = {}
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("disambiguation_card"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(12.dp),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        listOf(Color(0xFFFF9100), Color(0xFF00E5FF))
      )
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFFF9100),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Multiple contacts found for '$query'",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(
          onClick = onCancelCall,
          modifier = Modifier
            .size(24.dp)
            .testTag("dismiss_disambiguation_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Cancel Call",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Which contact would you like Jarvis to call? Select below or say 'cancel'.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        matches.forEach { contact ->
          ElevatedButton(
            onClick = { onContactSelected(contact) },
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("contact_chip_${contact.name.replace(" ", "_")}")
          ) {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = contact.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
              )
              Text(
                text = "${contact.type}: ${contact.phoneNumber}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }
        }

        ElevatedButton(
          onClick = onCancelCall,
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          ),
          shape = RoundedCornerShape(20.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("cancel_disambiguation_chip")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Cancel Call", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
      }
    }
  }
}
