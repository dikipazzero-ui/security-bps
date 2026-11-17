package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOutOfficer
import com.example.data.model.OfficerProfile
import com.example.data.model.ShiftOption
import com.example.data.model.UiState
import com.example.media.ImageUtils
import com.example.media.PdfReportPrinter
import com.example.media.VoiceNoteManager
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Red800
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.SecurityViewModel
import com.example.ui.viewmodel.currentIndonesianDate
import kotlinx.coroutines.launch

private enum class PhotoTarget {
    REPORT_SLOT,
    CHAT_ATTACHMENT
}

private data class LightboxPayload(
    val base64: String,
    val caption: String?
)

@Composable
fun MainScreen(
    viewModel: SecurityViewModel,
    currentUserId: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val unreadChatCount by viewModel.unreadChatCount.collectAsStateWithLifecycle()
    val isInternetConnected by viewModel.isInternetConnected.collectAsStateWithLifecycle()
    val activeRoomCode by viewModel.activeRoomCode.collectAsStateWithLifecycle()
    val activeChatRoom by viewModel.activeChatRoom.collectAsStateWithLifecycle()
    val roomMembers by viewModel.roomMembersState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val reportFilter by viewModel.reportFilter.collectAsStateWithLifecycle()
    val chatFilter by viewModel.chatFilter.collectAsStateWithLifecycle()
    val chatTextInput by viewModel.chatTextInput.collectAsStateWithLifecycle()
    val pendingChatPhoto by viewModel.pendingChatPhoto.collectAsStateWithLifecycle()
    val pendingChatPhotoCaption by viewModel.pendingChatPhotoCaption.collectAsStateWithLifecycle()
    val isSendingChat by viewModel.isSendingChat.collectAsStateWithLifecycle()
    val isJoiningRoom by viewModel.isJoiningRoom.collectAsStateWithLifecycle()

    val voiceNoteManager = remember(context, currentUserId) {
        VoiceNoteManager(context.applicationContext, scope)
    }
    DisposableEffect(voiceNoteManager) {
        onDispose { voiceNoteManager.release() }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setAppForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.setAppForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val recordingState by voiceNoteManager.recordingState.collectAsStateWithLifecycle()
    val playbackState by voiceNoteManager.playbackState.collectAsStateWithLifecycle()

    val dateLabel = remember { currentIndonesianDate() }
    val profile = (profileState as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()

    var showRecapModal by remember { mutableStateOf(false) }
    var showOfficerModal by remember { mutableStateOf(false) }
    var showResetModal by remember { mutableStateOf(false) }
    var showRoomModal by remember { mutableStateOf(false) }
    var showMembersModal by remember { mutableStateOf(false) }
    var lightboxPayload by remember { mutableStateOf<LightboxPayload?>(null) }

    // Photo picking state
    var showPhotoSourcePicker by remember { mutableStateOf(false) }
    var pendingPhotoTarget by remember { mutableStateOf(PhotoTarget.REPORT_SLOT) }
    var pendingSlotIndex by remember { mutableStateOf(0) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = activeTab == MainTab.CHAT) {
        viewModel.switchTab(MainTab.LAPORAN)
    }

    fun processPickedUri(uri: Uri) {
        scope.launch {
            val base64 = ImageUtils.compressAndWatermarkUri(
                context = context,
                uri = uri,
                activeShift = profile.activeShift
            )
            if (base64 == null) {
                viewModel.emitToast("Gagal memproses foto. Coba pilih foto lain.")
                return@launch
            }
            if (pendingPhotoTarget == PhotoTarget.REPORT_SLOT) {
                viewModel.setPhotoAtSlot(pendingSlotIndex, base64)
            } else {
                viewModel.setPendingChatPhoto(base64)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processPickedUri(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            processPickedUri(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = ImageUtils.createCameraOutputUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            viewModel.emitToast(context.getString(R.string.camera_permission_rationale))
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val started = voiceNoteManager.startRecording()
            if (!started) {
                viewModel.emitToast("Mikrofon tidak dapat dimulai pada perangkat ini.")
            }
        } else {
            viewModel.emitToast(context.getString(R.string.mic_permission_rationale))
        }
    }

    fun launchCameraFlow() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val uri = ImageUtils.createCameraOutputUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchVoiceRecordingFlow() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val started = voiceNoteManager.startRecording()
            if (!started) {
                viewModel.emitToast("Gagal memulai perekaman suara.")
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_bumi_papan_selaras),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Navy950.copy(alpha = 0.22f),
                            Slate100.copy(alpha = 0.28f),
                            Navy950.copy(alpha = 0.18f)
                        )
                    )
                )
        )
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // COMMAND HEADER
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Navy950.copy(alpha = 0.88f),
                                    Navy900.copy(alpha = 0.88f),
                                    Navy800.copy(alpha = 0.86f)
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title + Date + Real-Time Internet Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, Gold500, RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_security_shield_1794947235641),
                                contentDescription = "Logo Security BPS",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.header_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PulsingConnectivityDot(isOnline = isInternetConnected)
                                Text(
                                    text = if (isInternetConnected) {
                                        stringResource(R.string.status_online)
                                    } else {
                                        "Offline (Tanpa Internet)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isInternetConnected) Emerald400 else Red100,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("•", color = Slate400, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gold400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    viewModel.markOfflineBeforeSignOut()
                                    voiceNoteManager.release()
                                    signOutOfficer(credentialManager, scope)
                                }
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(R.string.sign_out),
                                tint = Slate300
                            )
                        }
                    }

                    // Offline Alert Strip when internet disconnects
                    if (!isInternetConnected) {
                        Surface(
                            color = Red100,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, tint = Red600, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Koneksi internet terputus. Aktifkan WiFi/Data untuk sinkronisasi Live Chat.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Red800
                                )
                            }
                        }
                    }

                    // Officer Identity Strip with Online Profile Badge
                    Surface(
                        color = Navy950.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Navy700.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OfficerAvatarWithPresence(
                                name = profile.name,
                                isOnline = isInternetConnected,
                                size = 38.dp,
                                containerColor = Navy700,
                                textColor = Gold400,
                                borderColor = Gold500.copy(alpha = 0.6f),
                                ringColor = Navy950
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    OnlineStatusPill(isOnline = isInternetConnected)
                                }
                                Text(
                                    text = "NRP: ${profile.nrp} • ${profile.role}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate300,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { showOfficerModal = true },
                                modifier = Modifier.testTag("edit_officer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ubah Data Petugas",
                                    tint = Gold400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Global Shift Selector Bar
                    HorizontalDivider(color = Navy700.copy(alpha = 0.7f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Gold400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Shift:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate300
                        )
                        ShiftOption.entries.forEach { shift ->
                            val isSelected = profile.activeShift == shift.label
                            Surface(
                                onClick = { viewModel.setGlobalShift(shift.label) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Gold500 else Navy900,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Gold400 else Navy700
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shift_button_${shift.label.replace(" ", "_").lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = shift.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Navy950 else Slate300
                                    )
                                    Text(
                                        text = shift.timeRange,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Navy950.copy(alpha = 0.8f) else Slate400,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 2-Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy950.copy(alpha = 0.92f))
                ) {
                    TabHeaderButton(
                        selected = activeTab == MainTab.LAPORAN,
                        icon = Icons.Default.Assignment,
                        label = stringResource(R.string.tab_laporan),
                        badgeCount = reports.size,
                        badgeContainerColor = Gold500,
                        badgeContentColor = Navy950,
                        onClick = { viewModel.switchTab(MainTab.LAPORAN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_laporan")
                    )
                    TabHeaderButton(
                        selected = activeTab == MainTab.CHAT,
                        icon = Icons.Default.Radio,
                        label = stringResource(R.string.tab_chat),
                        badgeCount = unreadChatCount,
                        badgeContainerColor = Red600,
                        badgeContentColor = Color.White,
                        onClick = { viewModel.switchTab(MainTab.CHAT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_chat")
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Gold500)
                )
            }

            // TAB CONTENT AREA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.widthIn(max = 860.dp).fillMaxSize()) {
                    when (activeTab) {
                        MainTab.LAPORAN -> {
                            ReportTabScreen(
                                formState = formState,
                                reportsState = reportsState,
                                reportFilter = reportFilter,
                                onSelectCategory = viewModel::selectCategory,
                                onTimeChange = viewModel::updateTimeInput,
                                onReportDateChange = viewModel::updateReportDateInput,
                                onSetTimeNow = viewModel::setCurrentTimeNow,
                                onFormShiftChange = viewModel::updateFormShift,
                                onFormStatusChange = viewModel::updateFormStatus,
                                onDescriptionChange = viewModel::updateDescription,
                                onFillTemplate = viewModel::fillTemplateDescription,
                                onRequestPhotoForSlot = { slotIdx ->
                                    pendingPhotoTarget = PhotoTarget.REPORT_SLOT
                                    pendingSlotIndex = slotIdx
                                    showPhotoSourcePicker = true
                                },
                                onPhotoCaptionChange = viewModel::updatePhotoCaptionAtSlot,
                                onRemovePhotoAtSlot = viewModel::removePhotoAtSlot,
                                onOpenLightbox = { img, caption -> lightboxPayload = LightboxPayload(img, caption) },
                                onSubmitReport = viewModel::submitReportItem,
                                onDeleteReport = viewModel::deleteReportItem,
                                onOpenResetConfirm = { showResetModal = true },
                                onOpenRecap = { showRecapModal = true },
                                onPrintPdf = {
                                    PdfReportPrinter.printDailyReport(
                                        context = context,
                                        dateLabel = dateLabel,
                                        activeShift = profile.activeShift,
                                        profile = profile,
                                        reports = reports
                                    )
                                },
                                onFilterKeywordChange = viewModel::updateReportKeyword,
                                onFilterHourChange = viewModel::updateReportFilterHour,
                                onFilterDayChange = viewModel::updateReportFilterDay,
                                onFilterMonthChange = viewModel::updateReportFilterMonth,
                                onFilterYearChange = viewModel::updateReportFilterYear,
                                onFilterToday = viewModel::filterReportsToday,
                                onResetFilter = viewModel::resetReportFilter
                            )
                        }
                        MainTab.CHAT -> {
                            ChatTabScreen(
                                currentUserId = currentUserId,
                                isInternetConnected = isInternetConnected,
                                activeRoomCode = activeRoomCode,
                                activeChatRoom = activeChatRoom,
                                roomMembers = roomMembers,
                                chatState = chatState,
                                chatFilter = chatFilter,
                                chatTextInput = chatTextInput,
                                pendingChatPhoto = pendingChatPhoto,
                                pendingChatPhotoCaption = pendingChatPhotoCaption,
                                isSending = isSendingChat,
                                recordingState = recordingState,
                                playbackState = playbackState,
                                onOpenRoomModal = { showRoomModal = true },
                                onOpenMembersModal = { showMembersModal = true },
                                onChatTextChange = viewModel::updateChatTextInput,
                                onRequestChatPhoto = {
                                    pendingPhotoTarget = PhotoTarget.CHAT_ATTACHMENT
                                    showPhotoSourcePicker = true
                                },
                                onPendingPhotoCaptionChange = viewModel::updatePendingChatPhotoCaption,
                                onClearChatPhoto = { viewModel.setPendingChatPhoto(null) },
                                onSendTextOrPhoto = viewModel::sendTextOrPhotoChat,
                                onStartVoiceRecording = { launchVoiceRecordingFlow() },
                                onCancelVoiceRecording = {
                                    voiceNoteManager.cancelRecording()
                                    viewModel.emitToast("Perekaman pesan suara dibatalkan.")
                                },
                                onStopAndSendVoiceNote = {
                                    val result = voiceNoteManager.stopAndExtractBase64()
                                    if (result != null) {
                                        viewModel.sendVoiceNote(result.first, result.second)
                                    } else {
                                        viewModel.emitToast("Rekaman suara terlalu pendek atau melebihi batas.")
                                    }
                                },
                                onTogglePlayVoiceNote = { msg ->
                                    voiceNoteManager.togglePlayVoiceNote(
                                        messageId = msg.id,
                                        audioType = msg.audioType,
                                        audioData = msg.audioData
                                    )
                                },
                                onSimulateTeamReply = viewModel::simulateTeamReply,
                                onOpenLightbox = { img, caption -> lightboxPayload = LightboxPayload(img, caption) },
                                onFilterKeywordChange = viewModel::updateChatFilterKeyword,
                                onFilterHourChange = viewModel::updateChatFilterHour,
                                onFilterDayChange = viewModel::updateChatFilterDay,
                                onFilterMonthChange = viewModel::updateChatFilterMonth,
                                onFilterYearChange = viewModel::updateChatFilterYear,
                                onResetFilter = viewModel::resetChatFilter
                            )
                        }
                    }
                }
            }
        }
        }
    }

    // MODALS
    if (showRecapModal) {
        val recapText = remember(dateLabel, profile, reports) {
            viewModel.buildWhatsAppRecap(dateLabel, profile, reports)
        }
        WhatsAppRecapDialog(
            recapText = recapText,
            onDismiss = { showRecapModal = false },
            onCopyText = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Laporan Security BPS", recapText))
                viewModel.emitToast("Format laporan WhatsApp berhasil disalin!")
                showRecapModal = false
            }
        )
    }

    if (showOfficerModal) {
        EditOfficerDialog(
            profile = profile,
            onDismiss = { showOfficerModal = false },
            onSave = { name, nrp, role ->
                viewModel.saveOfficerProfile(name, nrp, role)
                showOfficerModal = false
            }
        )
    }

    if (showResetModal) {
        ResetShiftConfirmDialog(
            onDismiss = { showResetModal = false },
            onConfirm = {
                viewModel.resetShiftReports()
                showResetModal = false
            }
        )
    }

    if (showRoomModal) {
        JoinOrCreateRoomDialog(
            currentRoomCode = activeRoomCode,
            isBusy = isJoiningRoom,
            onGenerateCode = viewModel::generateRandomRoomCode,
            onCopyCode = { code ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Kode Ruang Live Chat Security BPS", code))
                viewModel.emitToast("Kode ruang '$code' berhasil disalin!")
            },
            onJoinRoom = { code ->
                viewModel.joinRoomByCode(code) { showRoomModal = false }
            },
            onCreateRoom = { code, title ->
                viewModel.createAndJoinRoom(code, title) { showRoomModal = false }
            },
            onDismiss = { showRoomModal = false }
        )
    }

    if (showMembersModal) {
        RoomMembersDialog(
            roomCode = activeRoomCode,
            roomTitle = activeChatRoom?.title ?: "Channel Koordinasi Plot BPS",
            members = roomMembers,
            currentUserId = currentUserId,
            onDismiss = { showMembersModal = false }
        )
    }

    if (showPhotoSourcePicker) {
        PhotoSourcePickerDialog(
            onDismiss = { showPhotoSourcePicker = false },
            onPickCamera = {
                showPhotoSourcePicker = false
                launchCameraFlow()
            },
            onPickGallery = {
                showPhotoSourcePicker = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    lightboxPayload?.let { payload ->
        PhotoLightboxDialog(
            base64Image = payload.base64,
            caption = payload.caption,
            onDismiss = { lightboxPayload = null }
        )
    }
}

@Composable
private fun PulsingConnectivityDot(isOnline: Boolean) {
    val transition = rememberInfiniteTransition(label = "online_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(if (isOnline) alpha else 1f)
            .clip(CircleShape)
            .background(if (isOnline) Emerald400 else Red600)
    )
}

@Composable
private fun TabHeaderButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badgeCount: Int,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.height(50.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Gold400 else Slate400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Gold400 else Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = badgeContainerColor,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeContentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (selected) Gold500 else Color.Transparent)
            )
        }
    }
}
