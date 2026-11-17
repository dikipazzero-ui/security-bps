package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.UiState
import com.example.media.ImageUtils
import com.example.media.VoicePlaybackState
import com.example.media.VoiceRecordingState
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy50
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Red800
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

@Composable
fun ChatTabScreen(
    chatState: UiState<List<ChatMessage>>,
    chatTextInput: String,
    pendingChatPhoto: String?,
    isSending: Boolean,
    recordingState: VoiceRecordingState,
    playbackState: VoicePlaybackState,
    onChatTextChange: (String) -> Unit,
    onRequestChatPhoto: () -> Unit,
    onClearChatPhoto: () -> Unit,
    onSendTextOrPhoto: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onStopAndSendVoiceNote: () -> Unit,
    onTogglePlayVoiceNote: (ChatMessage) -> Unit,
    onSimulateTeamReply: () -> Unit,
    onOpenLightbox: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val messages = (chatState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Channel Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy900)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Gold500.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = Gold400
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.channel_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                            )
                            Text(
                                text = "4 Personil Aktif • Danru, Pos Depan & Korlap",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onSimulateTeamReply,
                    border = BorderStroke(1.dp, Gold500.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("simulate_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = Gold400,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.test_team_reply),
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold400
                    )
                }
            }

            // Messages area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Slate100)
            ) {
                when (chatState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            color = Navy900,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is UiState.Error -> {
                        Text(
                            text = chatState.message,
                            color = Red800,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    is UiState.Success -> {
                        if (messages.isEmpty()) {
                            Text(
                                text = "Belum ada pesan koordinasi. Kirim pesan teks, foto, atau Voice Note.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(20.dp)
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(messages, key = { it.id.ifBlank { "${it.senderName}_${it.time}_${it.text.hashCode()}" } }) { msg ->
                                    ChatBubbleItem(
                                        message = msg,
                                        isPlaying = playbackState.activeMessageId == msg.id && playbackState.isPlaying,
                                        playbackProgress = if (playbackState.activeMessageId == msg.id) playbackState.progress else 0f,
                                        onTogglePlay = { onTogglePlayVoiceNote(msg) },
                                        onOpenLightbox = onOpenLightbox
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pending photo bar
            if (pendingChatPhoto != null) {
                val bmp = remember(pendingChatPhoto) { ImageUtils.decodeBase64ToImageBitmap(pendingChatPhoto) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy50)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Preview Foto Chat",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        Column {
                            Text(
                                text = "Foto siap dikirim ke grup",
                                style = MaterialTheme.typography.labelMedium,
                                color = Navy900
                            )
                            Text(
                                text = "Tambahkan keterangan atau langsung kirim",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                    IconButton(onClick = onClearChatPhoto) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus Foto", tint = Red600)
                    }
                }
            }

            HorizontalDivider(color = Slate200)

            // Recording bar OR Text Input Row
            if (recordingState.isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Red100)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Red600)
                        )
                        VoiceWaveformBars(amplitude = recordingState.amplitudeNormalized)
                        Text(
                            text = recordingState.formattedTimer,
                            style = MaterialTheme.typography.labelLarge,
                            color = Red800
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onCancelVoiceRecording,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("cancel_vn_button")
                        ) {
                            Text("Batal", style = MaterialTheme.typography.labelMedium, color = Slate700)
                        }
                        Button(
                            onClick = onStopAndSendVoiceNote,
                            colors = ButtonDefaults.buttonColors(containerColor = Red600),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("send_vn_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim VN", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onRequestChatPhoto,
                        shape = RoundedCornerShape(12.dp),
                        color = Slate100,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_camera_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Lampirkan Foto",
                                tint = Navy800
                            )
                        }
                    }

                    OutlinedTextField(
                        value = chatTextInput,
                        onValueChange = { if (it.length <= 2000) onChatTextChange(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.chat_placeholder),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Slate100,
                            focusedBorderColor = Gold500,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input")
                    )

                    Surface(
                        onClick = onStartVoiceRecording,
                        shape = RoundedCornerShape(12.dp),
                        color = Amber100,
                        border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("start_voice_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Rekam Voice Note",
                                tint = Amber800
                            )
                        }
                    }

                    Surface(
                        onClick = { if (!isSending) onSendTextOrPhoto() },
                        shape = RoundedCornerShape(12.dp),
                        color = Navy900,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_chat_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Gold400,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Kirim Pesan",
                                    tint = Gold400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isPlaying: Boolean,
    playbackProgress: Float,
    onTogglePlay: () -> Unit,
    onOpenLightbox: (String) -> Unit
) {
    val isMe = message.fromOfficer
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) Navy900 else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            border = BorderStroke(1.dp, if (isMe) Navy800 else Slate200),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isMe) Gold400 else Navy800
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (isMe) Gold500.copy(alpha = 0.2f) else Slate100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = message.senderRole,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) Gold400 else Slate500,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMe) Color.White else Slate800
                    )
                }

                if (!message.imageData.isNullOrBlank()) {
                    val bmp = remember(message.imageData) { ImageUtils.decodeBase64ToImageBitmap(message.imageData) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Foto Koordinasi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenLightbox(message.imageData) }
                        )
                    }
                }

                if (message.audioType != "none") {
                    Surface(
                        color = if (isMe) Navy800 else Amber100.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isMe) Navy700 else Gold500.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Gold500)
                                    .clickable(onClick = onTogglePlay),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Hentikan Voice Note" else "Putar Voice Note",
                                    tint = Navy950,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = if (isMe) Gold400 else Navy900,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Voice Note",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isMe) Gold400 else Navy900
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { if (isPlaying) playbackProgress else 0f },
                                    color = Gold500,
                                    trackColor = Slate400.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                            Text(
                                text = message.audioDuration ?: "00:05",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMe) Color.White else Slate700
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${message.time} WIB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) Slate400 else Slate500
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Gold400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveformBars(amplitude: Float) {
    val multipliers = listOf(0.55f, 1.0f, 0.45f, 0.85f, 0.65f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(22.dp)
    ) {
        multipliers.forEach { mult ->
            val targetHeight by animateFloatAsState(
                targetValue = (6f + 16f * amplitude * mult).coerceIn(5f, 22f),
                label = "waveform"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(targetHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Red600)
            )
        }
    }
}
