package com.example.notifications

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Radio
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.R
import com.example.data.firebase.SecurityRepository
import com.example.data.model.ChatMessage
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.data.model.OfficerProfile
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.SecurityBpsTheme
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Embedded Activity displayed inside Android OS Conversation Bubbles ("gelembung notifikasi jendela").
 */
class BubbleChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val roomCode = intent.getStringExtra(SecurityNotificationManager.EXTRA_ROOM_CODE)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_ROOM_CODE

        setContent {
            SecurityBpsTheme {
                BubbleChatContent(
                    roomCode = roomCode,
                    onOpenFullApp = {
                        val fullIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(SecurityNotificationManager.EXTRA_OPEN_TAB, "CHAT")
                            putExtra(SecurityNotificationManager.EXTRA_ROOM_CODE, roomCode)
                        }
                        startActivity(fullIntent)
                    }
                )
            }
        }
    }
}

@Composable
private fun BubbleChatContent(
    roomCode: String,
    onOpenFullApp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = Firebase.auth.currentUser?.uid
    val repository = remember(context) {
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        SecurityRepository(FirebaseFirestore.getInstance(dbId))
    }

    if (uid.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate100)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Silakan masuk di aplikasi Security BPS terlebih dahulu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Navy900
                )
                Surface(
                    onClick = onOpenFullApp,
                    color = Navy900,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Buka Security BPS",
                        color = Gold400,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
        return
    }

    val messagesFlow = remember(roomCode) { repository.observeRoomMessages(roomCode) }
    val profileFlow = remember(uid) { repository.observeProfile(uid) }
    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val profile by profileFlow.collectAsStateWithLifecycle(initialValue = OfficerProfile(userId = uid))
    val activeProfile = profile ?: OfficerProfile(userId = uid)

    var replyInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate100)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy900)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Radio, contentDescription = null, tint = Gold400, modifier = Modifier.size(18.dp))
                Column {
                    Text(
                        text = "Live Chat • $roomCode",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeProfile.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold400,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onOpenFullApp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Buka Layar Penuh",
                    tint = Gold400
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id.ifBlank { "${it.senderName}_${it.time}_${it.text.hashCode()}" } }) { msg ->
                val isMe = msg.userId == uid && msg.fromOfficer
                BubbleRowMessage(msg = msg, isMe = isMe)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = replyInput,
                onValueChange = { if (it.length <= 1000) replyInput = it },
                placeholder = { Text("Balas cepat...", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Slate100,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = Slate200
                ),
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = {
                    val clean = replyInput.trim()
                    if (clean.isNotEmpty() && !isSending) {
                        isSending = true
                        scope.launch {
                            val res = repository.sendRoomChatMessage(
                                roomCode = roomCode,
                                senderName = activeProfile.name,
                                senderRole = activeProfile.role,
                                fromOfficer = true,
                                text = clean,
                                senderPhoto = activeProfile.photoData
                            )
                            isSending = false
                            if (res.isSuccess) replyInput = ""
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = Navy900,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = Gold400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleRowMessage(msg: ChatMessage, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) Navy900 else Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isMe) Navy800 else Slate200)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text(
                    text = msg.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) Gold400 else Navy800
                )
                val body = when {
                    msg.text.isNotBlank() -> msg.text
                    !msg.imageData.isNullOrBlank() -> "[Foto] ${msg.imageCaption.orEmpty()}".trim()
                    msg.audioType != "none" -> "[Voice Note ${msg.audioDuration.orEmpty()}]".trim()
                    else -> ""
                }
                if (body.isNotEmpty()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMe) Color.White else Slate800
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${msg.time} WIB",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) Slate400 else Slate500
                )
            }
        }
    }
}
