package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.RoomMember
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UiState
import com.example.media.ImageUtils
import com.example.media.VoicePlaybackState
import com.example.media.VoiceRecordingState
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
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
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

@Composable
fun ChatTabScreen(
    currentUserId: String,
    isInternetConnected: Boolean,
    activeRoomCode: String,
    activeChatRoom: ChatRoom?,
    roomMembers: List<RoomMember>,
    chatState: UiState<List<ChatMessage>>,
    chatFilter: TimeKeywordFilter,
    chatTextInput: String,
    pendingChatPhoto: String?,
    pendingChatPhotoCaption: String,
    isSending: Boolean,
    recordingState: VoiceRecordingState,
    playbackState: VoicePlaybackState,
    onOpenRoomModal: () -> Unit,
    onOpenMembersModal: () -> Unit,
    onChatTextChange: (String) -> Unit,
    onRequestChatPhoto: () -> Unit,
    onPendingPhotoCaptionChange: (String) -> Unit,
    onClearChatPhoto: () -> Unit,
    onSendTextOrPhoto: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onStopAndSendVoiceNote: () -> Unit,
    onTogglePlayVoiceNote: (ChatMessage) -> Unit,
    onSimulateTeamReply: () -> Unit,
    onOpenLightbox: (base64: String, caption: String?) -> Unit,
    onFilterKeywordChange: (String) -> Unit,
    onFilterHourChange: (Int?) -> Unit,
    onFilterDayChange: (Int?) -> Unit,
    onFilterMonthChange: (Int?) -> Unit,
    onFilterYearChange: (Int?) -> Unit,
    onResetFilter: () -> Unit
) {
    val listState = rememberLazyListState()
    val allMessages = (chatState as? UiState.Success)?.data.orEmpty()
    val filteredMessages = remember(allMessages, chatFilter) {
        if (!chatFilter.isActive) allMessages else allMessages.filter { chatFilter.matchesChatMessage(it) }
    }
    val availableYears = remember(allMessages) {
        allMessages.mapNotNull { it.resolvedYearMonthDay()?.first }.distinct()
    }
    var showSearchPanel by remember { mutableStateOf(false) }

    val onlineMembersCount = remember(roomMembers, currentUserId, isInternetConnected) {
        roomMembers.count { member ->
            if (member.userId == currentUserId) isInternetConnected else member.isCurrentlyOnline()
        }
    }

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.87f)),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Channel Header with Room Code & Search/Simulate Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy900)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeChatRoom?.title ?: stringResource(R.string.channel_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isInternetConnected) Emerald400 else Red600)
                                )
                                Text(
                                    text = if (isInternetConnected) {
                                        "Online • $onlineMembersCount Anggota Aktif"
                                    } else {
                                        "Offline • Menunggu Koneksi Internet"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isInternetConnected) Slate400 else Red100,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showSearchPanel = !showSearchPanel },
                            modifier = Modifier.testTag("toggle_chat_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari & Saring Pesan",
                                tint = if (showSearchPanel || chatFilter.isActive) Gold400 else Color.White
                            )
                        }
                        OutlinedButton(
                            onClick = onSimulateTeamReply,
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
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
                                text = "Tes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold400
                            )
                        }
                    }
                }

                // Room Code Bar + Join/Create Code + Members Online Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onOpenRoomModal,
                        color = Navy800,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Gold500.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_room_code_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Gold400, modifier = Modifier.size(15.dp))
                            Text(
                                text = "Kode: $activeRoomCode",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Gabung / Ubah",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold400
                            )
                        }
                    }
                    Surface(
                        onClick = onOpenMembersModal,
                        color = Navy950,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Navy700),
                        modifier = Modifier.testTag("open_members_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                            Text(
                                text = "$onlineMembersCount/${roomMembers.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Live Online Members Avatar Strip
            if (roomMembers.isNotEmpty()) {
                Surface(
                    color = Slate50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(roomMembers, key = { it.userId }) { member ->
                            val online = if (member.userId == currentUserId) {
                                isInternetConnected
                            } else {
                                member.isCurrentlyOnline()
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                                    .clickable(onClick = onOpenMembersModal)
                                    .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                OfficerAvatarWithPresence(
                                    name = member.name,
                                    isOnline = online,
                                    size = 28.dp,
                                    containerColor = if (online) Navy800 else Slate200,
                                    textColor = if (online) Gold400 else Slate700,
                                    borderColor = if (online) Gold500 else Slate400,
                                    ringColor = Color.White
                                )
                                Column {
                                    Text(
                                        text = member.name.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Navy900,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (online) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (online) Emerald600 else Slate400
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = Slate200)
            }

            // Collapsible Search & Time Filter Panel
            AnimatedVisibility(visible = showSearchPanel) {
                Box(modifier = Modifier.padding(10.dp)) {
                    SearchAndTimeFilterPanel(
                        title = "Cari Pesan (Kata Kunci / Jam / Hari / Bulan / Tahun)",
                        filter = chatFilter,
                        availableYears = availableYears,
                        totalCount = allMessages.size,
                        filteredCount = filteredMessages.size,
                        testTagPrefix = "chat",
                        onKeywordChange = onFilterKeywordChange,
                        onHourChange = onFilterHourChange,
                        onDayChange = onFilterDayChange,
                        onMonthChange = onFilterMonthChange,
                        onYearChange = onFilterYearChange,
                        onReset = onResetFilter
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
                        if (filteredMessages.isEmpty()) {
                            Text(
                                text = if (chatFilter.isActive) {
                                    "Tidak ada pesan yang cocok dengan kata kunci atau filter waktu."
                                } else {
                                    "Belum ada pesan koordinasi di ruang $activeRoomCode. Kirim pesan teks, foto, atau Voice Note."
                                },
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
                                items(
                                    filteredMessages,
                                    key = { it.id.ifBlank { "${it.senderName}_${it.time}_${it.text.hashCode()}" } }
                                ) { msg ->
                                    val senderOnline = if (msg.userId == currentUserId && msg.fromOfficer) {
                                        isInternetConnected
                                    } else {
                                        roomMembers.firstOrNull { it.userId == msg.userId && msg.fromOfficer }
                                            ?.isCurrentlyOnline() == true
                                    }
                                    ChatBubbleItem(
                                        message = msg,
                                        isMe = msg.fromOfficer && (msg.userId == currentUserId || msg.userId.isBlank()),
                                        isSenderOnline = senderOnline,
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

            // Pending photo bar WITH caption field directly below the photo
            if (pendingChatPhoto != null) {
                val bmp = remember(pendingChatPhoto) { ImageUtils.decodeBase64ToImageBitmap(pendingChatPhoto) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy50)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Preview Foto Chat",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Column {
                                Text(
                                    text = "Foto siap dikirim ke ruang $activeRoomCode",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Navy900
                                )
                                Text(
                                    text = "Isi kolom keterangan foto di bawah ini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                        }
                        IconButton(onClick = onClearChatPhoto) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus Foto", tint = Red600)
                        }
                    }
                    OutlinedTextField(
                        value = pendingChatPhotoCaption,
                        onValueChange = { if (it.length <= 300) onPendingPhotoCaptionChange(it) },
                        placeholder = {
                            Text("Tulis keterangan foto di bawah foto...", style = MaterialTheme.typography.bodySmall)
                        },
                        label = { Text("Keterangan Foto") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Gold500,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_photo_caption_input")
                    )
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
    isMe: Boolean,
    isSenderOnline: Boolean,
    isPlaying: Boolean,
    playbackProgress: Float,
    onTogglePlay: () -> Unit,
    onOpenLightbox: (base64: String, caption: String?) -> Unit
) {
    val dateBadge = remember(message.createdAt) { message.formattedDateBadge() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            OfficerAvatarWithPresence(
                name = message.senderName,
                isOnline = isSenderOnline,
                size = 32.dp,
                containerColor = Navy800,
                textColor = Gold400,
                borderColor = Gold500.copy(alpha = 0.6f),
                ringColor = Slate100
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            color = if (isMe) Navy900 else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            border = BorderStroke(1.dp, if (isMe) Navy800 else Slate200),
            modifier = Modifier.widthIn(max = 300.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isMe) Gold400 else Navy800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSenderOnline) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (isMe) Gold500.copy(alpha = 0.2f) else Slate100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = message.senderRole,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) Gold400 else Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Image(
                                bitmap = bmp,
                                contentDescription = message.imageCaption ?: "Foto Koordinasi",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenLightbox(message.imageData, message.imageCaption) }
                            )
                            // Photo caption directly below chat photo
                            val captionText = message.imageCaption?.trim().orEmpty()
                            if (captionText.isNotEmpty()) {
                                Surface(
                                    color = if (isMe) Navy800 else Slate50,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isMe) Navy700 else Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = captionText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMe) Color.White else Slate800,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
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
                    val timePrefix = if (dateBadge.isNotBlank()) "$dateBadge • " else ""
                    Text(
                        text = "$timePrefix${message.time} WIB",
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
