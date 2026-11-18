package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatMessage
import com.example.notifications.AlertKind
import com.example.notifications.IncomingAlertEvent
import com.example.notifications.NotificationSettings
import com.example.notifications.RingtonePreset
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
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
import kotlin.math.roundToInt

private val QUICK_BUBBLE_EMOJIS = listOf("👍", "🫡", "✅", "⚠️", "🚨", "🛡️")

@Composable
fun FloatingNotificationOverlay(
    activeHeadsUpAlert: IncomingAlertEvent?,
    isFloatingBubbleVisible: Boolean,
    isFloatingWindowExpanded: Boolean,
    unreadChatCount: Int,
    activeRoomCode: String,
    currentUserId: String,
    messages: List<ChatMessage>,
    onDismissHeadsUp: () -> Unit,
    onOpenFullChatFromAlert: () -> Unit,
    onExpandFloatingWindow: () -> Unit,
    onMinimizeFloatingWindow: () -> Unit,
    onCloseFloatingBubble: () -> Unit,
    onSendQuickReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizeDp = 56.dp
        val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }
        val edgeMarginPx = with(density) { 10.dp.toPx() }

        var offsetX by remember(maxWidthPx) {
            mutableFloatStateOf((maxWidthPx - bubbleSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx))
        }
        var offsetY by remember(maxHeightPx) {
            mutableFloatStateOf((maxHeightPx * 0.62f).coerceIn(edgeMarginPx, (maxHeightPx - bubbleSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)))
        }

        // 1. WhatsApp-style Floating Heads-Up Notification Window Banner at top
        AnimatedVisibility(
            visible = activeHeadsUpAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .widthIn(max = 540.dp)
        ) {
            activeHeadsUpAlert?.let { alert ->
                val isUrgent = alert.isUrgent || alert.kind == AlertKind.ANNOUNCEMENT
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUrgent) Navy950 else Navy900.copy(alpha = 0.97f),
                    border = BorderStroke(1.5.dp, if (isUrgent) Red600 else Gold500),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("floating_headsup_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (alert.kind == AlertKind.ANNOUNCEMENT) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (alert.isUrgent) Red600 else Gold500),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = if (alert.isUrgent) Color.White else Navy950,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                OfficerAvatarWithPresence(
                                    name = alert.senderName,
                                    isOnline = true,
                                    photoData = alert.senderPhoto,
                                    size = 38.dp,
                                    containerColor = Navy800,
                                    textColor = Gold400,
                                    borderColor = Gold500,
                                    ringColor = Navy900
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (isUrgent) Red600 else Gold500.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = alert.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isUrgent) Color.White else Gold400,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = alert.timeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = alert.senderName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = alert.messagePreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate100,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onDismissHeadsUp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Notifikasi",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismissHeadsUp()
                                    onExpandFloatingWindow()
                                },
                                border = BorderStroke(1.dp, Gold500.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("headsup_open_bubble_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = Gold400,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Balas di Gelembung", style = MaterialTheme.typography.labelSmall, color = Gold400)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDismissHeadsUp()
                                    onOpenFullChatFromAlert()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold500,
                                    contentColor = Navy950
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("headsup_open_chat_button")
                            ) {
                                Text("Buka", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Draggable WhatsApp/Messenger-style Floating Bubble OR Expanded Mini Chat Window
        if (isFloatingBubbleVisible) {
            if (isFloatingWindowExpanded) {
                FloatingMiniChatWindow(
                    activeRoomCode = activeRoomCode,
                    currentUserId = currentUserId,
                    messages = messages,
                    onMinimize = onMinimizeFloatingWindow,
                    onOpenFullChat = onOpenFullChatFromAlert,
                    onClose = onCloseFloatingBubble,
                    onSendQuickReply = onSendQuickReply,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                )
            } else {
                val maxX = (maxWidthPx - bubbleSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                val maxY = (maxHeightPx - bubbleSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                val clampedX = offsetX.coerceIn(edgeMarginPx, maxX)
                val clampedY = offsetY.coerceIn(edgeMarginPx, maxY)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                        .size(bubbleSizeDp + 6.dp)
                        .pointerInput(maxX, maxY) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(edgeMarginPx, maxX)
                                offsetY = (offsetY + dragAmount.y).coerceIn(edgeMarginPx, maxY)
                            }
                        }
                        .testTag("floating_chat_bubble")
                ) {
                    Surface(
                        onClick = onExpandFloatingWindow,
                        shape = CircleShape,
                        color = Navy900,
                        border = BorderStroke(2.dp, Gold500),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(bubbleSizeDp)
                            .align(Alignment.CenterStart)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Buka Gelembung Live Chat",
                                tint = Gold400,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    if (unreadChatCount > 0) {
                        Surface(
                            color = Red600,
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = if (unreadChatCount > 99) "99+" else unreadChatCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingMiniChatWindow(
    activeRoomCode: String,
    currentUserId: String,
    messages: List<ChatMessage>,
    onMinimize: () -> Unit,
    onOpenFullChat: () -> Unit,
    onClose: () -> Unit,
    onSendQuickReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickInput by remember { mutableStateOf("") }
    val recentMessages = remember(messages) { messages.takeLast(18) }
    val listState = rememberLazyListState()

    LaunchedEffect(recentMessages.size) {
        if (recentMessages.isNotEmpty()) {
            listState.animateScrollToItem(recentMessages.size - 1)
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Gold500),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        modifier = modifier
            .widthIn(min = 270.dp, max = 330.dp)
            .height(356.dp)
            .testTag("floating_mini_chat_window")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Window Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy900)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Gold400, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Gelembung Chat • $activeRoomCode",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenFullChat, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Buka Penuh",
                            tint = Gold400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onMinimize, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Minimize,
                            contentDescription = "Perkecil ke Gelembung",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Jendela",
                            tint = Red100,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Recent Messages inside floating window
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Slate100),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (recentMessages.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada pesan di ruang $activeRoomCode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    items(
                        recentMessages,
                        key = { it.id.ifBlank { "${it.senderName}_${it.time}_${it.text.hashCode()}" } }
                    ) { msg ->
                        val isMe = msg.userId == currentUserId && msg.fromOfficer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isMe) Navy900 else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isMe) Navy800 else Slate200),
                                modifier = Modifier.widthIn(max = 240.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                    Text(
                                        text = "${msg.senderName} • ${msg.time}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMe) Gold400 else Navy800,
                                        fontWeight = FontWeight.Bold
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
                                }
                            }
                        }
                    }
                }
            }

            // Quick Emoji Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate50),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(QUICK_BUBBLE_EMOJIS) { emoji ->
                    Surface(
                        onClick = { quickInput += emoji },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 14.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate200)

            // Quick Reply Composer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = quickInput,
                    onValueChange = { if (it.length <= 1000) quickInput = it },
                    placeholder = { Text("Balas cepat...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Slate100,
                        focusedBorderColor = Gold500,
                        unfocusedBorderColor = Slate200
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("floating_quick_reply_input")
                )
                Surface(
                    onClick = {
                        val trimmed = quickInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendQuickReply(trimmed)
                            quickInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Navy900,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("floating_quick_reply_send")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Balasan",
                            tint = Gold400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationSettingsDialog(
    settings: NotificationSettings,
    hasOsNotificationPermission: Boolean,
    history: List<IncomingAlertEvent>,
    onRequestPermission: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit,
    onToggleSystemInForeground: (Boolean) -> Unit,
    onSelectRingtonePreset: (RingtonePreset) -> Unit,
    onPickSystemRingtone: () -> Unit,
    onTestChatNotification: () -> Unit,
    onTestAnnouncementNotification: () -> Unit,
    onOpenFloatingBubbleWindow: () -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column {
                // Header
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
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Gold400)
                        Column {
                            Text(
                                text = "Notifikasi, Nada Dering & Gelembung",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Atur suara, getar, dan jendela pop-up seperti WA",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold400
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!hasOsNotificationPermission) {
                        Surface(
                            color = Amber100,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Gold500)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Izin notifikasi sistem belum aktif. Aktifkan agar notifikasi muncul di layar HP.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Amber800,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = onRequestPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Izinkan", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    SettingSwitchRow(
                        title = "Aktifkan Notifikasi Masuk",
                        subtitle = "Bunyi & pemberitahuan saat chat atau info baru masuk",
                        checked = settings.enabled,
                        onCheckedChange = onToggleEnabled
                    )
                    SettingSwitchRow(
                        title = "Nada Dering (Suara)",
                        subtitle = "Putar nada notifikasi perangkat secara otomatis",
                        checked = settings.soundEnabled,
                        enabled = settings.enabled,
                        onCheckedChange = onToggleSound
                    )
                    SettingSwitchRow(
                        title = "Getar (Vibrasi)",
                        subtitle = "Getarkan HP saat pesan chat atau pemberitahuan tiba",
                        checked = settings.vibrationEnabled,
                        enabled = settings.enabled,
                        onCheckedChange = onToggleVibration
                    )
                    SettingSwitchRow(
                        title = "Gelembung Jendela & Pop-up (Gaya WA)",
                        subtitle = "Tampilkan banner pop-up & gelembung chat melayang",
                        checked = settings.floatingBubbleEnabled,
                        enabled = settings.enabled,
                        onCheckedChange = onToggleFloatingBubble
                    )
                    SettingSwitchRow(
                        title = "Notif Bar Sistem Saat Aplikasi Dibuka",
                        subtitle = "Kirim juga ke bilah notifikasi Android saat aktif",
                        checked = settings.systemBannerWhenActive,
                        enabled = settings.enabled,
                        onCheckedChange = onToggleSystemInForeground
                    )

                    // Ringtone Picker Card
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Navy800, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Nada Dering Aktif", style = MaterialTheme.typography.labelMedium, color = Navy900)
                                        Text(settings.ringtoneTitle, style = MaterialTheme.typography.labelSmall, color = Emerald600)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onPickSystemRingtone,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Pilih dari HP", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RingtonePreset.entries.forEach { preset ->
                                    val selected = settings.ringtoneTitle == preset.label
                                    Surface(
                                        onClick = { onSelectRingtonePreset(preset) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) Navy900 else Color.White,
                                        border = BorderStroke(1.dp, if (selected) Gold500 else Slate200)
                                    ) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) Gold400 else Slate700,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Test & Bubble Launch Buttons
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onTestChatNotification,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900, contentColor = Gold400),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("test_chat_notif_button")
                        ) {
                            Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Tes Chat & Gelembung", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = onTestAnnouncementNotification,
                            colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("test_announcement_notif_button")
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Tes Pemberitahuan", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                onOpenFloatingBubbleWindow()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Buka Jendela Gelembung", style = MaterialTheme.typography.labelSmall, color = Navy900)
                        }
                    }

                    // Recent History
                    HorizontalDivider(color = Slate200)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Pemberitahuan (${history.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Navy900
                        )
                        if (history.isNotEmpty()) {
                            Text(
                                text = "Bersihkan",
                                style = MaterialTheme.typography.labelSmall,
                                color = Red600,
                                modifier = Modifier.clickable(onClick = onClearHistory)
                            )
                        }
                    }
                    if (history.isEmpty()) {
                        Text(
                            text = "Belum ada pemberitahuan masuk pada sesi ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    } else {
                        history.take(8).forEach { item ->
                            Surface(
                                color = if (item.isUrgent) Red100.copy(alpha = 0.5f) else Slate50,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (item.isUrgent) Red600.copy(alpha = 0.4f) else Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.senderName} • ${item.title}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isUrgent) Red800 else Navy900
                                        )
                                        Text(
                                            text = item.timeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate500
                                        )
                                    }
                                    Text(
                                        text = item.messagePreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate700,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Slate50,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) Navy900 else Slate400
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Navy950,
                    checkedTrackColor = Gold500
                )
            )
        }
    }
}
