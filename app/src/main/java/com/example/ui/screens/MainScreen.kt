package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.example.notifications.AlertKind
import com.example.notifications.NotificationPreferences
import com.example.notifications.SecurityNotificationManager
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
    CHAT_ATTACHMENT,
    PROFILE_PHOTO
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

    val notificationPrefs = remember(context) { NotificationPreferences(context) }
    val notifSettings by notificationPrefs.settings.collectAsStateWithLifecycle()
    var hasNotifPermission by remember {
        mutableStateOf(SecurityNotificationManager.hasNotificationPermission(context))
    }

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val unreadChatCount by viewModel.unreadChatCount.collectAsStateWithLifecycle()
    val isInternetConnected by viewModel.isInternetConnected.collectAsStateWithLifecycle()
    val activeRoomCode by viewModel.activeRoomCode.collectAsStateWithLifecycle()
    val activeChatRoom by viewModel.activeChatRoom.collectAsStateWithLifecycle()
    val roomMembers by viewModel.roomMembersState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val activitySheetsState by viewModel.activitySheetsState.collectAsStateWithLifecycle()
    val selectedSheetId by viewModel.selectedSheetId.collectAsStateWithLifecycle()
    val attendanceSheetsState by viewModel.attendanceSheetsState.collectAsStateWithLifecycle()
    val selectedAttendanceSheetId by viewModel.selectedAttendanceSheetId.collectAsStateWithLifecycle()
    val pjuSheetsState by viewModel.pjuSheetsState.collectAsStateWithLifecycle()
    val selectedPjuSheetId by viewModel.selectedPjuSheetId.collectAsStateWithLifecycle()
    val pjuSelectedShift by viewModel.pjuSelectedShift.collectAsStateWithLifecycle()
    val pjuSelectedTime by viewModel.pjuSelectedTime.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val reportFilter by viewModel.reportFilter.collectAsStateWithLifecycle()
    val tableFilter by viewModel.tableFilter.collectAsStateWithLifecycle()
    val attendanceFilter by viewModel.attendanceFilter.collectAsStateWithLifecycle()
    val pjuFilter by viewModel.pjuFilter.collectAsStateWithLifecycle()
    val chatFilter by viewModel.chatFilter.collectAsStateWithLifecycle()
    val chatTextInput by viewModel.chatTextInput.collectAsStateWithLifecycle()
    val pendingChatPhoto by viewModel.pendingChatPhoto.collectAsStateWithLifecycle()
    val pendingChatPhotoCaption by viewModel.pendingChatPhotoCaption.collectAsStateWithLifecycle()
    val isSendingChat by viewModel.isSendingChat.collectAsStateWithLifecycle()
    val isJoiningRoom by viewModel.isJoiningRoom.collectAsStateWithLifecycle()
    val isExplicitSaving by viewModel.isExplicitSaving.collectAsStateWithLifecycle()
    val unifiedNotes by viewModel.unifiedNotesState.collectAsStateWithLifecycle()

    val activeHeadsUpAlert by viewModel.activeHeadsUpAlert.collectAsStateWithLifecycle()
    val notificationHistory by viewModel.notificationHistory.collectAsStateWithLifecycle()
    val isFloatingBubbleVisible by viewModel.isFloatingBubbleVisible.collectAsStateWithLifecycle()
    val isFloatingWindowExpanded by viewModel.isFloatingWindowExpanded.collectAsStateWithLifecycle()

    var isAppInForeground by remember { mutableStateOf(true) }

    val voiceNoteManager = remember(context, currentUserId) {
        VoiceNoteManager(context.applicationContext, scope)
    }
    DisposableEffect(voiceNoteManager) {
        onDispose { voiceNoteManager.release() }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isAppInForeground = true
                    hasNotifPermission = SecurityNotificationManager.hasNotificationPermission(context)
                    viewModel.setAppForeground(true)
                }
                Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                    viewModel.setAppForeground(false)
                }
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
    val activitySheets = (activitySheetsState as? UiState.Success)?.data.orEmpty()
    val attendanceSheets = (attendanceSheetsState as? UiState.Success)?.data.orEmpty()
    val pjuSheets = (pjuSheetsState as? UiState.Success)?.data.orEmpty()
    val chatMessages = (chatState as? UiState.Success)?.data.orEmpty()

    val pagerState = rememberPagerState(
        initialPage = activeTab.ordinal,
        pageCount = { MainTab.entries.size }
    )
    val tabBarListState = rememberLazyListState()

    LaunchedEffect(activeTab) {
        if (pagerState.currentPage != activeTab.ordinal) {
            pagerState.animateScrollToPage(activeTab.ordinal)
        }
        tabBarListState.animateScrollToItem(activeTab.ordinal)
    }

    LaunchedEffect(pagerState.settledPage) {
        val settledTab = MainTab.entries.getOrElse(pagerState.settledPage) { MainTab.LAPORAN }
        if (settledTab != activeTab) {
            viewModel.switchTab(settledTab)
        }
    }

    var showHeaderOptions by remember { mutableStateOf(false) }
    var showShiftDropdown by remember { mutableStateOf(false) }
    var showNotificationModal by remember { mutableStateOf(false) }
    var showRecapModal by remember { mutableStateOf(false) }
    var showOfficerModal by remember { mutableStateOf(false) }
    var showResetModal by remember { mutableStateOf(false) }
    var showRoomModal by remember { mutableStateOf(false) }
    var showMembersModal by remember { mutableStateOf(false) }
    var showGlobalNotesSearch by remember { mutableStateOf(false) }
    var lightboxPayload by remember { mutableStateOf<LightboxPayload?>(null) }

    // Photo picking state
    var showPhotoSourcePicker by remember { mutableStateOf(false) }
    var pendingPhotoTarget by remember { mutableStateOf(PhotoTarget.REPORT_SLOT) }
    var pendingSlotIndex by remember { mutableStateOf(0) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val latestNotifSettings by rememberUpdatedState(notifSettings)
    val latestProfile by rememberUpdatedState(profile)
    val latestForeground by rememberUpdatedState(isAppInForeground)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (!granted) {
            viewModel.emitToast(context.getString(R.string.notification_permission_rationale))
        } else {
            SecurityNotificationManager.ensureChannels(context, latestNotifSettings)
            viewModel.emitToast("Izin notifikasi diaktifkan!")
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                val ringtone = runCatching { RingtoneManager.getRingtone(context, pickedUri) }.getOrNull()
                val title = runCatching { ringtone?.getTitle(context) }.getOrNull().orEmpty()
                notificationPrefs.setRingtone(pickedUri.toString(), title.ifBlank { "Nada Kustom" })
                viewModel.emitToast("Nada dering diatur ke: ${title.ifBlank { "Nada Kustom" }}")
            } else {
                notificationPrefs.setSoundEnabled(false)
                viewModel.emitToast("Nada notifikasi disetel hening.")
            }
        }
    }

    LaunchedEffect(Unit) {
        SecurityNotificationManager.ensureChannels(context, notifSettings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !SecurityNotificationManager.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveRecapToastEvents.collect { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Lihat Rekap",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                showGlobalNotesSearch = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.alertEvents.collect { event ->
            val currentSettings = latestNotifSettings
            if (!currentSettings.enabled) return@collect

            // Ringtone + Vibration
            SecurityNotificationManager.playRingtoneAndVibrate(
                context = context,
                settings = currentSettings,
                isUrgent = event.isUrgent
            )

            // OS Heads-up & Conversation Bubble notification when backgrounded or explicitly requested
            if (!latestForeground || currentSettings.systemBannerWhenActive) {
                if (event.kind == AlertKind.CHAT) {
                    SecurityNotificationManager.showIncomingChatNotification(
                        context = context,
                        event = event,
                        settings = currentSettings,
                        officerName = latestProfile.name
                    )
                } else {
                    SecurityNotificationManager.showAnnouncementNotification(
                        context = context,
                        event = event,
                        settings = currentSettings
                    )
                }
            }
        }
    }

    BackHandler(enabled = activeTab != MainTab.LAPORAN) {
        viewModel.switchTab(MainTab.LAPORAN)
    }

    fun processPickedUri(uri: Uri) {
        scope.launch {
            if (pendingPhotoTarget == PhotoTarget.PROFILE_PHOTO) {
                val base64 = ImageUtils.compressProfilePhotoUri(context = context, uri = uri)
                if (base64 == null) {
                    viewModel.emitToast("Gagal memproses foto profil. Coba pilih foto lain.")
                    return@launch
                }
                viewModel.updateProfilePhoto(base64)
                return@launch
            }

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
                // COMPACT COMMAND HEADER (kept slim so main chat & content are much wider/taller)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Navy950.copy(alpha = 0.90f),
                                    Navy900.copy(alpha = 0.90f),
                                    Navy800.copy(alpha = 0.88f)
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 900.dp)
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Ultra-compact primary bar: Avatar + Name/Online + 1-tap Shift Pill + Notif Bell + Expand Opsi + Logout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Gold500, RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_security_shield_1794947235641),
                                    contentDescription = "Logo Security BPS",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            OfficerAvatarWithPresence(
                                name = profile.name,
                                isOnline = isInternetConnected,
                                photoData = profile.photoData,
                                onClick = {
                                    pendingPhotoTarget = PhotoTarget.PROFILE_PHOTO
                                    showPhotoSourcePicker = true
                                },
                                size = 30.dp,
                                containerColor = Navy700,
                                textColor = Gold400,
                                borderColor = Gold500.copy(alpha = 0.6f),
                                ringColor = Navy950
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PulsingConnectivityDot(isOnline = isInternetConnected)
                                    Text(
                                        text = if (isInternetConnected) {
                                            "Online • Ruang $activeRoomCode"
                                        } else {
                                            "Offline • Ruang $activeRoomCode"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isInternetConnected) Emerald400 else Red100,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 1-Tap Compact Shift Pill + Dropdown
                            Box {
                                val shortShift = when (profile.activeShift) {
                                    ShiftOption.SHIFT_1.label -> "S1"
                                    ShiftOption.SHIFT_2.label -> "S2"
                                    else -> "S3"
                                }
                                Surface(
                                    onClick = { showShiftDropdown = true },
                                    color = Gold500,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("compact_shift_selector")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = shortShift,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Navy950
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Pilih Shift",
                                            tint = Navy950,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showShiftDropdown,
                                    onDismissRequest = { showShiftDropdown = false }
                                ) {
                                    ShiftOption.entries.forEach { shift ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${shift.label} (${shift.timeRange})",
                                                    fontWeight = if (profile.activeShift == shift.label) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setGlobalShift(shift.label)
                                                showShiftDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Notification & Ringtone / Bubble Settings Bell
                            Box {
                                IconButton(
                                    onClick = { showNotificationModal = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("notification_settings_button")
                                ) {
                                    Icon(
                                        imageVector = if (notifSettings.enabled) {
                                            Icons.Default.NotificationsActive
                                        } else {
                                            Icons.Default.NotificationsOff
                                        },
                                        contentDescription = "Notifikasi, Nada Dering & Gelembung",
                                        tint = if (notifSettings.enabled) Gold400 else Slate400,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                if (notificationHistory.isNotEmpty() || unreadChatCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .padding(top = 2.dp, end = 2.dp)
                                            .clip(CircleShape)
                                            .background(Red600)
                                    )
                                }
                            }

                            // Global Notes Search across all 5 options (includes Waktu & Shift)
                            IconButton(
                                onClick = { showGlobalNotesSearch = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("open_global_notes_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ManageSearch,
                                    contentDescription = "Cari Seluruh Catatan",
                                    tint = Gold400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Toggle Detailed Options Drawer (Profile NRP/Role + Date + Full Shift)
                            IconButton(
                                onClick = { showHeaderOptions = !showHeaderOptions },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("toggle_header_options_button")
                            ) {
                                Icon(
                                    imageVector = if (showHeaderOptions) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = "Tampilkan / Sembunyikan Opsi",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.markOfflineBeforeSignOut()
                                        voiceNoteManager.release()
                                        signOutOfficer(credentialManager, scope)
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(R.string.sign_out),
                                    tint = Slate300,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Offline Alert Strip when internet disconnects
                        if (!isInternetConnected) {
                            Surface(
                                color = Red100,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Red600, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Koneksi internet terputus. Data & Live Chat akan sinkron otomatis saat online.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Red800
                                    )
                                }
                            }
                        }

                        // Collapsible Officer Identity, Date & Full Shift Bar (collapsed by default for larger workspace)
                        AnimatedVisibility(visible = showHeaderOptions) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = Navy950.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Navy700.copy(alpha = 0.8f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "$dateLabel • ${stringResource(R.string.header_title)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Gold400,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "NRP: ${profile.nrp} • ${profile.role}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate300,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        OnlineStatusPill(isOnline = isInternetConnected)
                                        IconButton(
                                            onClick = { showOfficerModal = true },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("edit_officer_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Ubah Data Petugas",
                                                tint = Gold400,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Navy700.copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Gold400,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    ShiftOption.entries.forEach { shift ->
                                        val isSelected = profile.activeShift == shift.label
                                        Surface(
                                            onClick = { viewModel.setGlobalShift(shift.label) },
                                            shape = RoundedCornerShape(8.dp),
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
                                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = shift.label,
                                                    style = MaterialTheme.typography.labelSmall,
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
                        }
                    }

                    // Horizontally Scrollable 5-Tab Switcher (Laporan | Tabel | Absensi | PJU | Chat)
                    LazyRow(
                        state = tabBarListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Navy950.copy(alpha = 0.92f)),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val tabItems = MainTab.entries
                        items(tabItems, key = { it.name }) { tab ->
                            val (icon, label, badge, badgeBg, badgeFg) = when (tab) {
                                MainTab.LAPORAN -> TabSpec(
                                    icon = Icons.Default.Assignment,
                                    label = stringResource(R.string.tab_laporan),
                                    badge = reports.size,
                                    badgeBg = Gold500,
                                    badgeFg = Navy950
                                )
                                MainTab.TABEL -> TabSpec(
                                    icon = Icons.Default.TableChart,
                                    label = stringResource(R.string.tab_tabel),
                                    badge = activitySheets.size,
                                    badgeBg = Gold500,
                                    badgeFg = Navy950
                                )
                                MainTab.ABSENSI -> TabSpec(
                                    icon = Icons.Default.FactCheck,
                                    label = stringResource(R.string.tab_absensi),
                                    badge = attendanceSheets.size,
                                    badgeBg = Gold500,
                                    badgeFg = Navy950
                                )
                                MainTab.PJU -> TabSpec(
                                    icon = Icons.Default.Lightbulb,
                                    label = stringResource(R.string.tab_pju),
                                    badge = pjuSheets.sumOf { it.issues.size },
                                    badgeBg = if (pjuSheets.any { it.issues.isNotEmpty() }) Red600 else Gold500,
                                    badgeFg = if (pjuSheets.any { it.issues.isNotEmpty() }) Color.White else Navy950
                                )
                                MainTab.CHAT -> TabSpec(
                                    icon = Icons.Default.Radio,
                                    label = stringResource(R.string.tab_chat),
                                    badge = unreadChatCount,
                                    badgeBg = Red600,
                                    badgeFg = Color.White
                                )
                            }
                            TabHeaderButton(
                                selected = activeTab == tab,
                                icon = icon,
                                label = label,
                                badgeCount = badge,
                                badgeContainerColor = badgeBg,
                                badgeContentColor = badgeFg,
                                onClick = {
                                    viewModel.switchTab(tab)
                                    scope.launch {
                                        pagerState.animateScrollToPage(tab.ordinal)
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(min = 82.dp)
                                    .testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Gold500)
                    )
                }

                // SWIPEABLE HORIZONTAL PAGER TAB CONTENT AREA (Left/Right swipe across all 5 options)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 900.dp)
                            .fillMaxSize()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (MainTab.entries[page]) {
                            MainTab.LAPORAN -> {
                                ReportTabScreen(
                                    formState = formState,
                                    reportsState = reportsState,
                                    reportFilter = reportFilter,
                                    activeShift = profile.activeShift,
                                    currentUserId = currentUserId,
                                    isRoomCreator = activeChatRoom?.createdBy == currentUserId,
                                    isExplicitSaving = isExplicitSaving,
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
                                    onExplicitSave = viewModel::saveReportTabDataExplicitly,
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
                                    onFilterShiftChange = viewModel::updateReportFilterShift,
                                    onFilterToday = viewModel::filterReportsToday,
                                    onResetFilter = viewModel::resetReportFilter,
                                    onOpenGlobalRecap = { showGlobalNotesSearch = true },
                                    totalRecapCount = unifiedNotes.size
                                )
                            }
                            MainTab.TABEL -> {
                                TableReportTabScreen(
                                    sheetsState = activitySheetsState,
                                    selectedSheetId = selectedSheetId,
                                    tableFilter = tableFilter,
                                    defaultShift = profile.activeShift,
                                    currentUserId = currentUserId,
                                    isRoomCreator = activeChatRoom?.createdBy == currentUserId,
                                    isExplicitSaving = isExplicitSaving,
                                    onSelectSheet = viewModel::selectActivitySheet,
                                    onCreateSheet = viewModel::createNewActivitySheet,
                                    onUpdateHeader = viewModel::updateSheetHeader,
                                    onUpsertPersonnel = viewModel::upsertPersonnelRow,
                                    onDeletePersonnel = viewModel::deletePersonnelRow,
                                    onUpsertInventory = viewModel::upsertInventoryRow,
                                    onDeleteInventory = viewModel::deleteInventoryRow,
                                    onLoadDefaultInventory = viewModel::loadDefaultInventoryTemplate,
                                    onUpsertActivity = viewModel::upsertActivityRow,
                                    onDeleteActivity = viewModel::deleteActivityRow,
                                    onDeleteSheet = viewModel::deleteActiveActivitySheet,
                                    onExplicitSave = viewModel::saveActiveActivitySheetExplicitly,
                                    onCopyWhatsApp = { sheet ->
                                        val text = viewModel.buildTableWhatsAppRecap(sheet, profile)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Tabel Pelaporan Security BPS", text))
                                        viewModel.emitToast("Format Tabel Kegiatan siap tempel WhatsApp berhasil disalin!")
                                    },
                                    onPrintPdf = { sheet ->
                                        PdfReportPrinter.printActivityTableSheet(
                                            context = context,
                                            sheet = sheet,
                                            profile = profile
                                        )
                                    },
                                    onFilterKeywordChange = viewModel::updateTableKeyword,
                                    onFilterHourChange = viewModel::updateTableFilterHour,
                                    onFilterDayChange = viewModel::updateTableFilterDay,
                                    onFilterMonthChange = viewModel::updateTableFilterMonth,
                                    onFilterYearChange = viewModel::updateTableFilterYear,
                                    onFilterShiftChange = viewModel::updateTableFilterShift,
                                    onFilterToday = viewModel::filterTableToday,
                                    onResetFilter = viewModel::resetTableFilter,
                                    onOpenGlobalRecap = { showGlobalNotesSearch = true },
                                    totalRecapCount = unifiedNotes.size
                                )
                            }
                            MainTab.ABSENSI -> {
                                AttendanceTabScreen(
                                    sheetsState = attendanceSheetsState,
                                    selectedSheetId = selectedAttendanceSheetId,
                                    attendanceFilter = attendanceFilter,
                                    defaultShift = profile.activeShift,
                                    defaultCommanderName = profile.name,
                                    currentUserId = currentUserId,
                                    isRoomCreator = activeChatRoom?.createdBy == currentUserId,
                                    isExplicitSaving = isExplicitSaving,
                                    onSelectSheet = viewModel::selectAttendanceSheet,
                                    onCreateSheet = { date, shift, period, commander, copy ->
                                        viewModel.createNewAttendanceSheet(date, shift, period, commander, copy)
                                    },
                                    onUpdateHeader = viewModel::updateAttendanceHeader,
                                    onUpsertRow = viewModel::upsertAttendanceRow,
                                    onQuickCycleStatus = viewModel::quickCycleAttendanceStatus,
                                    onMarkAllStatus = viewModel::markAllAttendanceStatus,
                                    onFillShiftTimes = viewModel::fillAttendanceShiftTimes,
                                    onEnsure15Rows = viewModel::ensure15AttendanceRows,
                                    onDeleteRow = viewModel::deleteAttendanceRow,
                                    onDeleteSheet = viewModel::deleteActiveAttendanceSheet,
                                    onExplicitSave = viewModel::saveActiveAttendanceSheetExplicitly,
                                    onCopyWhatsApp = { sheet ->
                                        val text = viewModel.buildAttendanceWhatsAppRecap(sheet, profile)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Absensi 15 Anggota Security BPS", text))
                                        viewModel.emitToast("Rekap Absensi 15 Anggota berhasil disalin ke Clipboard!")
                                    },
                                    onPrintPdf = { sheet ->
                                        PdfReportPrinter.printAttendanceSheet(
                                            context = context,
                                            sheet = sheet,
                                            profile = profile
                                        )
                                    },
                                    onFilterKeywordChange = viewModel::updateAttendanceKeyword,
                                    onFilterHourChange = viewModel::updateAttendanceFilterHour,
                                    onFilterDayChange = viewModel::updateAttendanceFilterDay,
                                    onFilterMonthChange = viewModel::updateAttendanceFilterMonth,
                                    onFilterYearChange = viewModel::updateAttendanceFilterYear,
                                    onFilterShiftChange = viewModel::updateAttendanceFilterShift,
                                    onFilterToday = viewModel::filterAttendanceToday,
                                    onResetFilter = viewModel::resetAttendanceFilter,
                                    onOpenGlobalRecap = { showGlobalNotesSearch = true },
                                    totalRecapCount = unifiedNotes.size
                                )
                            }
                            MainTab.PJU -> {
                                PjuTabScreen(
                                    sheetsState = pjuSheetsState,
                                    selectedSheetId = selectedPjuSheetId,
                                    pjuFilter = pjuFilter,
                                    activeShift = profile.activeShift,
                                    selectedPjuShift = pjuSelectedShift,
                                    selectedPjuTime = pjuSelectedTime,
                                    currentUserId = currentUserId,
                                    isRoomCreator = activeChatRoom?.createdBy == currentUserId,
                                    isExplicitSaving = isExplicitSaving,
                                    onSelectSheet = viewModel::selectPjuSheet,
                                    onSelectPjuShift = viewModel::updatePjuSelectedShift,
                                    onPjuTimeChange = viewModel::updatePjuSelectedTime,
                                    onSetPjuTimeNow = viewModel::setPjuSelectedTimeNow,
                                    onCreateSheet = viewModel::createNewPjuSheet,
                                    onUpdateHeader = viewModel::updatePjuHeader,
                                    onSetCellSlots = viewModel::setPjuCellSlots,
                                    onMarkDayAllBaik = viewModel::markPjuDayAllBaik,
                                    onUpsertCluster = viewModel::upsertPjuCluster,
                                    onDeleteCluster = viewModel::deletePjuCluster,
                                    onUpsertIssue = viewModel::upsertPjuIssue,
                                    onDeleteIssue = viewModel::deletePjuIssue,
                                    onUpdateNotes = viewModel::updatePjuNotes,
                                    onExplicitSave = { latestNotes ->
                                        viewModel.saveActivePjuSheetExplicitly(latestNotes)
                                    },
                                    onDeleteSheet = viewModel::deleteActivePjuSheet,
                                    onCopyWhatsApp = { sheet ->
                                        val text = viewModel.buildPjuWhatsAppRecap(sheet, profile)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Checklist PJU Bumi Papan Selaras", text))
                                        viewModel.emitToast("Rekap Checklist PJU siap tempel WhatsApp berhasil disalin!")
                                    },
                                    onPrintPdf = { sheet ->
                                        PdfReportPrinter.printPjuChecklistSheet(
                                            context = context,
                                            sheet = sheet,
                                            profile = profile
                                        )
                                    },
                                    onFilterKeywordChange = viewModel::updatePjuKeyword,
                                    onFilterHourChange = viewModel::updatePjuFilterHour,
                                    onFilterDayChange = viewModel::updatePjuFilterDay,
                                    onFilterMonthChange = viewModel::updatePjuFilterMonth,
                                    onFilterYearChange = viewModel::updatePjuFilterYear,
                                    onFilterShiftChange = viewModel::updatePjuFilterShift,
                                    onFilterCurrentMonth = viewModel::filterPjuCurrentMonth,
                                    onResetFilter = viewModel::resetPjuFilter,
                                    onOpenGlobalRecap = { showGlobalNotesSearch = true },
                                    totalRecapCount = unifiedNotes.size
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
                                    activeShift = profile.activeShift,
                                    chatTextInput = chatTextInput,
                                    pendingChatPhoto = pendingChatPhoto,
                                    pendingChatPhotoCaption = pendingChatPhotoCaption,
                                    isSending = isSendingChat,
                                    isExplicitSaving = isExplicitSaving,
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
                                    onEditMessage = viewModel::editChatMessage,
                                    onDeleteMessage = viewModel::deleteChatMessage,
                                    onClearAllChat = viewModel::clearAllChatMessages,
                                    onExplicitSaveChat = viewModel::saveChatNoteOrSync,
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
                                    onFilterShiftChange = viewModel::updateChatFilterShift,
                                    onResetFilter = viewModel::resetChatFilter,
                                    onOpenGlobalRecap = { showGlobalNotesSearch = true },
                                    totalRecapCount = unifiedNotes.size
                                )
                            }
                            }
                        }
                    }
                }
            }
        }

        // WHATSAPP-STYLE FLOATING HEADS-UP BANNER & DRAGGABLE FLOATING CHAT BUBBLE WINDOW
        if (notifSettings.enabled && notifSettings.floatingBubbleEnabled) {
            FloatingNotificationOverlay(
                activeHeadsUpAlert = activeHeadsUpAlert,
                isFloatingBubbleVisible = isFloatingBubbleVisible,
                isFloatingWindowExpanded = isFloatingWindowExpanded,
                unreadChatCount = unreadChatCount,
                activeRoomCode = activeRoomCode,
                currentUserId = currentUserId,
                messages = chatMessages,
                onDismissHeadsUp = viewModel::dismissHeadsUpAlert,
                onOpenFullChatFromAlert = {
                    viewModel.closeFloatingBubble()
                    viewModel.switchTab(MainTab.CHAT)
                },
                onExpandFloatingWindow = viewModel::openFloatingChatWindow,
                onMinimizeFloatingWindow = viewModel::minimizeFloatingChatWindow,
                onCloseFloatingBubble = viewModel::closeFloatingBubble,
                onSendQuickReply = viewModel::sendQuickFloatingReply
            )
        }
    }

    // MODALS
    if (showNotificationModal) {
        NotificationSettingsDialog(
            settings = notifSettings,
            hasOsNotificationPermission = hasNotifPermission,
            history = notificationHistory,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onToggleEnabled = { notificationPrefs.setEnabled(it) },
            onToggleSound = { notificationPrefs.setSoundEnabled(it) },
            onToggleVibration = { notificationPrefs.setVibrationEnabled(it) },
            onToggleFloatingBubble = {
                notificationPrefs.setFloatingBubbleEnabled(it)
                if (!it) viewModel.closeFloatingBubble()
            },
            onToggleSystemInForeground = { notificationPrefs.setSystemBannerWhenActive(it) },
            onSelectRingtonePreset = { preset ->
                val uri = RingtoneManager.getDefaultUri(preset.type)
                notificationPrefs.setRingtone(uri?.toString().orEmpty(), preset.label)
                SecurityNotificationManager.playRingtoneAndVibrate(context, notifSettings.copy(
                    ringtoneUri = uri?.toString().orEmpty(),
                    ringtoneTitle = preset.label
                ))
            },
            onPickSystemRingtone = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih Nada Dering Notifikasi")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    val curUri = SecurityNotificationManager.resolveRingtoneUri(notifSettings)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, curUri)
                }
                ringtonePickerLauncher.launch(intent)
            },
            onTestChatNotification = {
                showNotificationModal = false
                viewModel.triggerTestChatNotification()
            },
            onTestAnnouncementNotification = {
                showNotificationModal = false
                viewModel.triggerTestAnnouncementNotification()
            },
            onOpenFloatingBubbleWindow = {
                if (!notifSettings.floatingBubbleEnabled) {
                    notificationPrefs.setFloatingBubbleEnabled(true)
                }
                viewModel.openFloatingChatWindow()
            },
            onClearHistory = viewModel::clearNotificationHistory,
            onDismiss = { showNotificationModal = false }
        )
    }

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
            onPickProfilePhoto = {
                pendingPhotoTarget = PhotoTarget.PROFILE_PHOTO
                showPhotoSourcePicker = true
            },
            onRemoveProfilePhoto = { viewModel.clearProfilePhoto() },
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

    if (showGlobalNotesSearch) {
        GlobalNotesSearchDialog(
            allNotes = unifiedNotes,
            activeRoomCode = activeRoomCode,
            currentUserId = currentUserId,
            isRoomCreator = activeChatRoom?.createdBy == currentUserId,
            defaultShift = profile.activeShift,
            isSavingNote = isExplicitSaving,
            onDismiss = { showGlobalNotesSearch = false },
            onOpenTarget = { note ->
                viewModel.openUnifiedNoteTarget(note)
                showGlobalNotesSearch = false
            },
            onSaveQuickNote = viewModel::saveQuickNote,
            onDeleteQuickNote = viewModel::deleteQuickNote
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
            .size(7.dp)
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
        modifier = modifier
            .height(40.dp)
            .width(IntrinsicSize.Max)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Gold400 else Slate400,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) Gold400 else Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = badgeContainerColor,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeContentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 0.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (selected) Gold500 else Color.Transparent)
            )
        }
    }
}

private data class TabSpec(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val badge: Int,
    val badgeBg: Color,
    val badgeFg: Color
)
