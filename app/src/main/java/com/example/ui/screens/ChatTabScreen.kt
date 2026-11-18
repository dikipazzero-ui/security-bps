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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

private val SECURITY_EMOJIS = listOf(
    "👍", "👌", "✅", "⚠️", "🚨", "🛡️", "👮", "🫡",
    "🙏", "😊", "😂", "🤝", "🔥", "💪", "📸", "🕒",
    "🔒", "🔓", "📍", "🚓", "🌙", "☀️", "🌧️", "📋"
)

@OptIn(ExperimentalLayoutApi::class)
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
    activeShift: String,
    isExplicitSaving: Boolean,
    recordingState: VoiceRecordingState,
    playbackState: VoicePlaybackState,
    onOpenRoomModal: () -> Unit,
    onOpenMembersModal: () -> Unit,
    onChatTextChange: (String) -> Unit,
    onRequestChatPhoto: () -> Unit,
    onPendingPhotoCaptionChange: (String) -> Unit,
    onClearChatPhoto: () -> Unit,
    onSendTextOrPhoto: () -> Unit,
    onExplicitSaveChat: () -> Unit,
    onEditMessage: (messageId: String, newText: String, newCaption: String?) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onClearAllChat: () -> Unit,
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
    onFilterShiftChange: (String?) -> Unit,
    onResetFilter: () -> Unit,
    onOpenGlobalRecap: () -> Unit,
    totalRecapCount: Int
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
    var showEmojiBar by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteConfirmMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.89f)),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact Channel Header with Room Code Pill & Quick Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy900)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Gold500.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = Gold400,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeChatRoom?.title ?: stringResource(R.string.channel_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isInternetConnected) Emerald400 else Red600)
                                )
                                Text(
                                    text = if (isInternetConnected) "$onlineMembersCount/${roomMembers.size} Aktif" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isInternetConnected) Emerald400 else Red100,
                                    maxLines = 1
                                )
                                Surface(
                                    onClick = onOpenRoomModal,
                                    color = Navy800,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Gold500.copy(alpha = 0.55f)),
                                    modifier = Modifier.testTag("open_room_code_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = Gold400, modifier = Modifier.size(11.dp))
                                        Text(
                                            text = activeRoomCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Gold400,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenMembersModal,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("open_members_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Anggota Ruang",
                                tint = Emerald400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { showSearchPanel = !showSearchPanel },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("toggle_chat_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari & Saring Pesan",
                                tint = if (showSearchPanel || chatFilter.isActive) Gold400 else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (allMessages.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearAllConfirm = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("clear_all_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Hapus Semua Chat",
                                    tint = Red100,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onSimulateTeamReply,
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("simulate_reply_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = Gold400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Tes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold400
                            )
                        }
                    }
                }
            }

            // Ultra-slim Live Online Members Pill Strip
            if (roomMembers.isNotEmpty()) {
                Surface(
                    color = Slate50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                                    .clickable(onClick = onOpenMembersModal)
                                    .padding(start = 3.dp, end = 7.dp, top = 2.dp, bottom = 2.dp)
                            ) {
                                OfficerAvatarWithPresence(
                                    name = member.name,
                                    isOnline = online,
                                    photoData = member.photoData,
                                    size = 20.dp,
                                    containerColor = if (online) Navy800 else Slate200,
                                    textColor = if (online) Gold400 else Slate700,
                                    borderColor = if (online) Gold500 else Slate400,
                                    ringColor = Color.White
                                )
                                Text(
                                    text = member.name.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (online) Navy900 else Slate500,
                                    fontWeight = if (online) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Slate200)
            }

            // Collapsible Search & Time Filter Panel
            AnimatedVisibility(visible = showSearchPanel) {
                Box(modifier = Modifier.padding(6.dp)) {
                    SearchAndTimeFilterPanel(
                        title = "Cari Pesan (Kata Kunci / Jam / Hari / Bulan / Tahun / Shift)",
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
                        onShiftChange = onFilterShiftChange,
                        onReset = onResetFilter,
                        onOpenGlobalRecap = onOpenGlobalRecap,
                        globalRecapCount = totalRecapCount
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
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    filteredMessages,
                                    key = { it.id.ifBlank { "${it.senderName}_${it.time}_${it.text.hashCode()}" } }
                                ) { msg ->
                                    val senderMember = roomMembers.firstOrNull { it.userId == msg.userId }
                                    val senderOnline = if (msg.userId == currentUserId && msg.fromOfficer) {
                                        isInternetConnected
                                    } else {
                                        senderMember?.isCurrentlyOnline() == true
                                    }
                                    val senderAvatar = msg.senderPhoto?.takeIf { it.isNotBlank() }
                                        ?: senderMember?.photoData?.takeIf { it.isNotBlank() }
                                    val isAuthor = msg.userId == currentUserId && msg.fromOfficer
                                    val canDelete = msg.userId == currentUserId ||
                                        (activeChatRoom?.createdBy == currentUserId)
                                    ChatBubbleItem(
                                        message = msg,
                                        isMe = isAuthor,
                                        canEdit = msg.userId == currentUserId,
                                        canDelete = canDelete,
                                        isSenderOnline = senderOnline,
                                        senderPhoto = senderAvatar,
                                        isPlaying = playbackState.activeMessageId == msg.id && playbackState.isPlaying,
                                        playbackProgress = if (playbackState.activeMessageId == msg.id) playbackState.progress else 0f,
                                        onTogglePlay = { onTogglePlayVoiceNote(msg) },
                                        onOpenLightbox = onOpenLightbox,
                                        onEditClick = { editingMessage = msg },
                                        onDeleteClick = { deleteConfirmMessage = msg }
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

            // Emoji Quick Picker Panel above input row
            AnimatedVisibility(visible = showEmojiBar && !recordingState.isRecording) {
                Surface(
                    color = Slate50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SECURITY_EMOJIS.forEach { emoji ->
                                Surface(
                                    onClick = {
                                        if (chatTextInput.length + emoji.length <= 2000) {
                                            onChatTextChange(chatTextInput + emoji)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Slate200)
                    }
                }
            }

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
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { showEmojiBar = !showEmojiBar },
                        shape = RoundedCornerShape(10.dp),
                        color = if (showEmojiBar) Navy900 else Slate100,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chat_emoji_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = "Pilih Emoji",
                                tint = if (showEmojiBar) Gold400 else Navy800,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onRequestChatPhoto,
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chat_camera_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Lampirkan Foto",
                                tint = Navy800,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = chatTextInput,
                        onValueChange = { if (it.length <= 2000) onChatTextChange(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.chat_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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
                        shape = RoundedCornerShape(10.dp),
                        color = Amber100,
                        border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("start_voice_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Rekam Voice Note",
                                tint = Amber800,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { if (!isSending) onSendTextOrPhoto() },
                        shape = RoundedCornerShape(10.dp),
                        color = Navy900,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("send_chat_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Gold400,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Kirim Pesan",
                                    tint = Gold400,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dedicated Bottom Save Bar (very bottom of Chat option)
            HorizontalDivider(color = Navy800.copy(alpha = 0.25f))
            Surface(
                color = Navy900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (chatTextInput.trim().isNotEmpty()) {
                            "Ketik selesai? Simpan ke arsip Catatan ($activeShift)"
                        } else {
                            "Arsip Chat $activeRoomCode (${allMessages.size} pesan • $activeShift)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate200,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onOpenGlobalRecap,
                        border = BorderStroke(1.dp, Gold400.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Navy800,
                            contentColor = Gold400
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("bottom_chat_open_recap_button")
                    ) {
                        Icon(Icons.Default.ManageSearch, contentDescription = "Filter Rekap", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rekap",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onExplicitSaveChat,
                        enabled = !isExplicitSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold500,
                            contentColor = Navy950
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("bottom_save_chat_button")
                    ) {
                        if (isExplicitSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                color = Navy950,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (chatTextInput.trim().isNotEmpty()) "Simpan Catatan" else "Simpan",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    editingMessage?.let { target ->
        EditChatMessageDialog(
            message = target,
            onDismiss = { editingMessage = null },
            onSave = { newText, newCaption ->
                onEditMessage(target.id, newText, newCaption)
                editingMessage = null
            }
        )
    }

    deleteConfirmMessage?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmMessage = null },
            title = { Text("Hapus Pesan Chat?", style = MaterialTheme.typography.titleMedium, color = Navy900) },
            text = {
                Text(
                    text = "Pesan dari ${target.senderName} pada ${target.time} WIB akan dihapus dari ruang obrolan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMessage(target.id)
                        deleteConfirmMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmMessage = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Hapus Semua Chat?", style = MaterialTheme.typography.titleMedium, color = Navy900) },
            text = {
                Text(
                    text = "Semua pesan di ruang $activeRoomCode yang menjadi hak Anda akan dibersihkan secara permanen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllChat()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600),
                    modifier = Modifier.testTag("confirm_clear_all_chat_button")
                ) {
                    Text("Ya, Hapus Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isMe: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    isSenderOnline: Boolean,
    senderPhoto: String?,
    isPlaying: Boolean,
    playbackProgress: Float,
    onTogglePlay: () -> Unit,
    onOpenLightbox: (base64: String, caption: String?) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateBadge = remember(message.createdAt) { message.formattedDateBadge() }
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            OfficerAvatarWithPresence(
                name = message.senderName,
                isOnline = isSenderOnline,
                photoData = senderPhoto,
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
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 14.dp
            ),
            border = BorderStroke(1.dp, if (isMe) Navy800 else Slate200),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                        if (canEdit || canDelete) {
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Aksi Pesan",
                                        tint = if (isMe) Gold400 else Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (canEdit) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Pesan") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = Navy800)
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onEditClick()
                                            }
                                        )
                                    }
                                    if (canDelete) {
                                        DropdownMenuItem(
                                            text = { Text("Hapus Pesan", color = Red600) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Red600)
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onDeleteClick()
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
                    val editedSuffix = if (message.edited) " (diedit)" else ""
                    Text(
                        text = "$timePrefix${message.time} WIB$editedSuffix",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditChatMessageDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onSave: (newText: String, newCaption: String?) -> Unit
) {
    var textInput by remember(message) { mutableStateOf(message.text) }
    var captionInput by remember(message) { mutableStateOf(message.imageCaption.orEmpty()) }
    val hasImage = !message.imageData.isNullOrBlank()
    val hasAudio = message.audioType != "none"
    val canSave = textInput.isNotBlank() || hasImage || hasAudio

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Gold400)
                        Text(
                            text = "Edit Pesan Chat",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { if (it.length <= 2000) textInput = it },
                        label = { Text("Isi Pesan") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_chat_text_input")
                    )

                    if (hasImage) {
                        OutlinedTextField(
                            value = captionInput,
                            onValueChange = { if (it.length <= 300) captionInput = it },
                            label = { Text("Keterangan Foto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("Tambah Emoji:", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SECURITY_EMOJIS.take(12).forEach { emoji ->
                            Surface(
                                onClick = {
                                    if (textInput.length + emoji.length <= 2000) {
                                        textInput += emoji
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (canSave) {
                                    onSave(
                                        textInput.trim(),
                                        if (hasImage) captionInput.trim() else null
                                    )
                                }
                            },
                            enabled = canSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy900,
                                contentColor = Gold400
                            ),
                            modifier = Modifier.testTag("save_edit_chat_button")
                        ) {
                            Text("Simpan Perubahan")
                        }
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
