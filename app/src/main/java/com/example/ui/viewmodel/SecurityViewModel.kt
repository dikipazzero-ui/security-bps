package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NetworkMonitor
import com.example.data.firebase.SecurityRepository
import com.example.data.model.ActivitySheetTemplates
import com.example.data.model.ActivityTableRow
import com.example.data.model.AttendanceMemberRow
import com.example.data.model.AttendanceSheet
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceTemplates
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.data.model.DailyActivitySheet
import com.example.data.model.InventoryRow
import com.example.data.model.OfficerProfile
import com.example.data.model.PersonnelRow
import com.example.data.model.PjuChecklistSheet
import com.example.data.model.PjuClusterRow
import com.example.data.model.PjuIssueEntry
import com.example.data.model.PjuLampStatus
import com.example.data.model.PjuTemplates
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.data.model.RoomMember
import com.example.data.model.SecurityReport
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UiState
import com.example.data.model.currentIsoDate
import com.example.notifications.AlertKind
import com.example.notifications.IncomingAlertEvent
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class MainTab {
    LAPORAN,
    TABEL,
    ABSENSI,
    PJU,
    CHAT
}

data class ReportFormState(
    val selectedCategory: Int = 1,
    val timeInput: String = currentFormattedTime(),
    val reportDateInput: String = currentIsoDate(),
    val shiftInput: String = ShiftOption.SHIFT_1.label,
    val statusInput: ReportStatus = ReportStatus.AMAN,
    val descriptionInput: String = "",
    val photoSlots: List<String?> = listOf(null, null, null, null),
    val photoCaptionSlots: List<String> = listOf("", "", "", ""),
    val isSubmitting: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModel(
    private val repository: SecurityRepository,
    private val networkMonitor: NetworkMonitor,
    private val currentUserId: String,
    private val initialDisplayName: String?
) : ViewModel() {

    private val _activeTab = MutableStateFlow(MainTab.LAPORAN)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    private val _activeRoomCode = MutableStateFlow(DEFAULT_ROOM_CODE)
    val activeRoomCode: StateFlow<String> = _activeRoomCode.asStateFlow()

    private val _isForeground = MutableStateFlow(true)

    val isInternetConnected: StateFlow<Boolean> = networkMonitor.isConnected

    private val _formState = MutableStateFlow(ReportFormState())
    val formState: StateFlow<ReportFormState> = _formState.asStateFlow()

    private val _reportFilter = MutableStateFlow(TimeKeywordFilter())
    val reportFilter: StateFlow<TimeKeywordFilter> = _reportFilter.asStateFlow()

    private val _tableFilter = MutableStateFlow(TimeKeywordFilter())
    val tableFilter: StateFlow<TimeKeywordFilter> = _tableFilter.asStateFlow()

    private val _selectedSheetId = MutableStateFlow<String?>(null)
    val selectedSheetId: StateFlow<String?> = _selectedSheetId.asStateFlow()

    private val _attendanceFilter = MutableStateFlow(TimeKeywordFilter())
    val attendanceFilter: StateFlow<TimeKeywordFilter> = _attendanceFilter.asStateFlow()

    private val _selectedAttendanceSheetId = MutableStateFlow<String?>(null)
    val selectedAttendanceSheetId: StateFlow<String?> = _selectedAttendanceSheetId.asStateFlow()

    private val _pjuFilter = MutableStateFlow(TimeKeywordFilter())
    val pjuFilter: StateFlow<TimeKeywordFilter> = _pjuFilter.asStateFlow()

    private val _selectedPjuSheetId = MutableStateFlow<String?>(null)
    val selectedPjuSheetId: StateFlow<String?> = _selectedPjuSheetId.asStateFlow()

    private val _chatFilter = MutableStateFlow(TimeKeywordFilter())
    val chatFilter: StateFlow<TimeKeywordFilter> = _chatFilter.asStateFlow()

    private val _chatTextInput = MutableStateFlow("")
    val chatTextInput: StateFlow<String> = _chatTextInput.asStateFlow()

    private val _pendingChatPhoto = MutableStateFlow<String?>(null)
    val pendingChatPhoto: StateFlow<String?> = _pendingChatPhoto.asStateFlow()

    private val _pendingChatPhotoCaption = MutableStateFlow("")
    val pendingChatPhotoCaption: StateFlow<String> = _pendingChatPhotoCaption.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    private val _isJoiningRoom = MutableStateFlow(false)
    val isJoiningRoom: StateFlow<Boolean> = _isJoiningRoom.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _alertEvents = MutableSharedFlow<IncomingAlertEvent>(extraBufferCapacity = 16)
    val alertEvents: SharedFlow<IncomingAlertEvent> = _alertEvents.asSharedFlow()

    private val _activeHeadsUpAlert = MutableStateFlow<IncomingAlertEvent?>(null)
    val activeHeadsUpAlert: StateFlow<IncomingAlertEvent?> = _activeHeadsUpAlert.asStateFlow()

    private val _notificationHistory = MutableStateFlow<List<IncomingAlertEvent>>(emptyList())
    val notificationHistory: StateFlow<List<IncomingAlertEvent>> = _notificationHistory.asStateFlow()

    private val _isFloatingBubbleVisible = MutableStateFlow(false)
    val isFloatingBubbleVisible: StateFlow<Boolean> = _isFloatingBubbleVisible.asStateFlow()

    private val _isFloatingWindowExpanded = MutableStateFlow(false)
    val isFloatingWindowExpanded: StateFlow<Boolean> = _isFloatingWindowExpanded.asStateFlow()

    private var hasSyncedInitialProfile = false
    private var lastObservedRoomForMessages: String? = null
    private val knownMessageIds = mutableSetOf<String>()
    private var hasSeededRoomMembers = false
    private var lastObservedRoomForMembers: String? = null
    private val knownOnlineMemberIds = mutableSetOf<String>()

    val profileState: StateFlow<UiState<OfficerProfile>> = repository.observeProfile(currentUserId)
        .map<OfficerProfile?, UiState<OfficerProfile>> { profile ->
            val resolved = profile ?: OfficerProfile(
                userId = currentUserId,
                name = initialDisplayName?.ifBlank { null } ?: "Hendra Wijaya"
            )
            UiState.Success(resolved)
        }
        .onEach { state ->
            if (state is UiState.Success && !hasSyncedInitialProfile) {
                hasSyncedInitialProfile = true
                _formState.update { it.copy(shiftInput = state.data.activeShift) }
                val savedRoom = state.data.activeRoomCode.takeIf { repository.isValidRoomCode(it) }
                if (savedRoom != null) {
                    _activeRoomCode.value = savedRoom
                }
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat profil petugas."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val reportsState: StateFlow<UiState<List<SecurityReport>>> = repository.observeReports(currentUserId)
        .map<List<SecurityReport>, UiState<List<SecurityReport>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing reports", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat laporan harian."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activitySheetsState: StateFlow<UiState<List<DailyActivitySheet>>> = repository.observeActivitySheets(currentUserId)
        .map<List<DailyActivitySheet>, UiState<List<DailyActivitySheet>>> { UiState.Success(it) }
        .onEach { state ->
            if (state is UiState.Success) {
                val list = state.data
                val cur = _selectedSheetId.value
                if (list.isNotEmpty() && (cur == null || list.none { it.id == cur })) {
                    _selectedSheetId.value = list.first().id
                }
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing activity sheets", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat tabel pelaporan kegiatan."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val attendanceSheetsState: StateFlow<UiState<List<AttendanceSheet>>> = repository.observeAttendanceSheets(currentUserId)
        .map<List<AttendanceSheet>, UiState<List<AttendanceSheet>>> { UiState.Success(it) }
        .onEach { state ->
            if (state is UiState.Success) {
                val list = state.data
                val cur = _selectedAttendanceSheetId.value
                if (list.isNotEmpty() && (cur == null || list.none { it.id == cur })) {
                    _selectedAttendanceSheetId.value = list.first().id
                }
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing attendance sheets", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat tabel absensi anggota."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val pjuSheetsState: StateFlow<UiState<List<PjuChecklistSheet>>> = repository.observePjuSheets(currentUserId)
        .map<List<PjuChecklistSheet>, UiState<List<PjuChecklistSheet>>> { UiState.Success(it) }
        .onEach { state ->
            if (state is UiState.Success) {
                val list = state.data
                val cur = _selectedPjuSheetId.value
                if (list.isNotEmpty() && (cur == null || list.none { it.id == cur })) {
                    _selectedPjuSheetId.value = list.first().id
                }
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing PJU sheets", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat form checklist PJU."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeChatRoom: StateFlow<ChatRoom?> = _activeRoomCode
        .flatMapLatest { code -> repository.observeChatRoom(code) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val roomMembersState: StateFlow<List<RoomMember>> = _activeRoomCode
        .flatMapLatest { code -> repository.observeRoomMembers(code) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val chatState: StateFlow<UiState<List<ChatMessage>>> = _activeRoomCode
        .flatMapLatest { code -> repository.observeRoomMessages(code) }
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing chat", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat pesan koordinasi."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            val result = repository.ensureInitialSetup(currentUserId, initialDisplayName)
            result.getOrNull()?.let { roomCode ->
                _activeRoomCode.value = roomCode
            }
        }

        // Presence heartbeat synced with real internet status & foreground lifecycle
        viewModelScope.launch {
            combine(_isForeground, isInternetConnected, _activeRoomCode) { fg, online, room ->
                Triple(fg, online, room)
            }.collect { (fg, online, room) ->
                val profile = (profileState.value as? UiState.Success)?.data
                    ?: OfficerProfile(userId = currentUserId, name = initialDisplayName?.ifBlank { null } ?: "Hendra Wijaya")
                repository.updatePresence(room, profile, isOnline = fg && online)
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (_isForeground.value && isInternetConnected.value) {
                    val profile = (profileState.value as? UiState.Success)?.data
                        ?: OfficerProfile(userId = currentUserId, name = initialDisplayName?.ifBlank { null } ?: "Hendra Wijaya")
                    repository.updatePresence(_activeRoomCode.value, profile, isOnline = true)
                }
            }
        }

        // Observe incoming chat messages and emit alerts for new messages from teammates/team alerts
        viewModelScope.launch {
            combine(chatState, _activeRoomCode) { state, room ->
                state to room
            }.collect { (state, room) ->
                if (state !is UiState.Success) return@collect
                val messages = state.data
                if (lastObservedRoomForMessages != room) {
                    lastObservedRoomForMessages = room
                    knownMessageIds.clear()
                    messages.forEach { msg ->
                        val key = msg.id.ifBlank { "${msg.senderName}_${msg.time}_${msg.text.hashCode()}" }
                        knownMessageIds.add(key)
                    }
                    return@collect
                }

                val newlyArrived = mutableListOf<ChatMessage>()
                messages.forEach { msg ->
                    val key = msg.id.ifBlank { "${msg.senderName}_${msg.time}_${msg.text.hashCode()}" }
                    if (knownMessageIds.add(key)) {
                        newlyArrived.add(msg)
                    }
                }

                val incomingFromOthers = newlyArrived.filter { it.userId != currentUserId || !it.fromOfficer }
                if (incomingFromOthers.isNotEmpty()) {
                    if (_activeTab.value != MainTab.CHAT || !_isForeground.value) {
                        _unreadChatCount.update { it + incomingFromOthers.size }
                    }
                    val latest = incomingFromOthers.last()
                    val preview = when {
                        latest.text.isNotBlank() -> latest.text
                        !latest.imageData.isNullOrBlank() -> "📷 Foto ${latest.imageCaption.orEmpty()}".trim()
                        latest.audioType != "none" -> "🎤 Voice Note (${latest.audioDuration ?: "00:05"})"
                        else -> "Pesan koordinasi baru"
                    }
                    val upperText = latest.text.uppercase(Locale.getDefault())
                    val isAnnouncement = latest.text.contains("🚨") ||
                        latest.text.contains("⚠️") ||
                        upperText.contains("PEMBERITAHUAN") ||
                        upperText.contains("PENGUMUMAN") ||
                        upperText.contains("DARURAT")

                    val event = IncomingAlertEvent(
                        id = latest.id.ifBlank { "msg_${System.currentTimeMillis()}" },
                        kind = if (isAnnouncement) AlertKind.ANNOUNCEMENT else AlertKind.CHAT,
                        title = if (isAnnouncement) "Pemberitahuan • $room" else "Chat Masuk • $room",
                        senderName = latest.senderName,
                        senderRole = latest.senderRole,
                        senderPhoto = latest.senderPhoto,
                        roomCode = room,
                        messagePreview = preview,
                        timeLabel = "${latest.time} WIB",
                        isUrgent = isAnnouncement
                    )
                    dispatchIncomingAlert(event, showBubbleOnChat = _activeTab.value != MainTab.CHAT)
                }
            }
        }

        // Observe teammates coming online as lightweight pemberitahuan
        viewModelScope.launch {
            combine(roomMembersState, _activeRoomCode) { members, room ->
                members to room
            }.collect { (members, room) ->
                if (lastObservedRoomForMembers != room) {
                    lastObservedRoomForMembers = room
                    hasSeededRoomMembers = false
                    knownOnlineMemberIds.clear()
                }
                val currentOnline = members
                    .filter { it.userId != currentUserId && it.isCurrentlyOnline() }
                    .map { it.userId }
                    .toSet()

                if (!hasSeededRoomMembers) {
                    hasSeededRoomMembers = true
                    knownOnlineMemberIds.addAll(currentOnline)
                    return@collect
                }

                val newlyOnline = currentOnline - knownOnlineMemberIds
                knownOnlineMemberIds.clear()
                knownOnlineMemberIds.addAll(currentOnline)

                if (newlyOnline.isNotEmpty()) {
                    val member = members.firstOrNull { it.userId == newlyOnline.first() }
                    if (member != null) {
                        val event = IncomingAlertEvent(
                            id = "online_${member.userId}_${System.currentTimeMillis()}",
                            kind = AlertKind.ANNOUNCEMENT,
                            title = "Pemberitahuan Tim • $room",
                            senderName = member.name,
                            senderRole = member.role,
                            senderPhoto = member.photoData,
                            roomCode = room,
                            messagePreview = "${member.name} (${member.role}) sedang aktif online di ruang koordinasi.",
                            timeLabel = "${currentFormattedTime()} WIB",
                            isUrgent = false
                        )
                        dispatchIncomingAlert(event, showBubbleOnChat = false)
                    }
                }
            }
        }
    }

    private fun dispatchIncomingAlert(event: IncomingAlertEvent, showBubbleOnChat: Boolean) {
        _notificationHistory.update { (listOf(event) + it).take(25) }
        _activeHeadsUpAlert.value = event
        if (showBubbleOnChat) {
            _isFloatingBubbleVisible.value = true
        }
        _alertEvents.tryEmit(event)
        viewModelScope.launch {
            val snapshotId = event.id
            delay(5500L)
            if (_activeHeadsUpAlert.value?.id == snapshotId) {
                _activeHeadsUpAlert.value = null
            }
        }
    }

    fun dismissHeadsUpAlert() {
        _activeHeadsUpAlert.value = null
    }

    fun openFloatingChatWindow() {
        _isFloatingBubbleVisible.value = true
        _isFloatingWindowExpanded.value = true
        _activeHeadsUpAlert.value = null
    }

    fun minimizeFloatingChatWindow() {
        _isFloatingBubbleVisible.value = true
        _isFloatingWindowExpanded.value = false
    }

    fun closeFloatingBubble() {
        _isFloatingWindowExpanded.value = false
        _isFloatingBubbleVisible.value = false
    }

    fun clearNotificationHistory() {
        _notificationHistory.value = emptyList()
    }

    fun triggerTestChatNotification() {
        val room = _activeRoomCode.value
        val event = IncomingAlertEvent(
            id = "test_chat_${System.currentTimeMillis()}",
            kind = AlertKind.CHAT,
            title = "Chat Masuk • $room",
            senderName = "Bapak Suroto (Korlap)",
            senderRole = "Korlap Security BPS",
            roomCode = room,
            messagePreview = "Siaga 1 Pos Gerbang! Mohon cek kondisi area parkir depan & laporan situasi terbaru. 🫡",
            timeLabel = "${currentFormattedTime()} WIB",
            isUrgent = false
        )
        _unreadChatCount.update { it + 1 }
        dispatchIncomingAlert(event, showBubbleOnChat = true)
    }

    fun triggerTestAnnouncementNotification() {
        val room = _activeRoomCode.value
        val event = IncomingAlertEvent(
            id = "test_alert_${System.currentTimeMillis()}",
            kind = AlertKind.ANNOUNCEMENT,
            title = "PEMBERITAHUAN PENTING",
            senderName = "Komandan Regu Plot BPS",
            senderRole = "Danru Plot BPS",
            roomCode = room,
            messagePreview = "🚨 Apel serah terima shift & pengecekan inventaris HT dilakukan 15 menit sebelum pergantian jaga.",
            timeLabel = "${currentFormattedTime()} WIB",
            isUrgent = true
        )
        dispatchIncomingAlert(event, showBubbleOnChat = true)
    }

    fun sendQuickFloatingReply(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val roomCode = _activeRoomCode.value
        viewModelScope.launch {
            val result = repository.sendRoomChatMessage(
                roomCode = roomCode,
                senderName = profile.name,
                senderRole = profile.role,
                fromOfficer = true,
                text = clean,
                senderPhoto = profile.photoData
            )
            if (result.isFailure) {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengirim balasan cepat.")
            }
        }
    }

    fun setAppForeground(inForeground: Boolean) {
        _isForeground.value = inForeground
    }

    suspend fun markOfflineBeforeSignOut() {
        val profile = (profileState.value as? UiState.Success)?.data
            ?: OfficerProfile(userId = currentUserId)
        repository.updatePresence(_activeRoomCode.value, profile, isOnline = false)
    }

    fun switchTab(tab: MainTab) {
        _activeTab.value = tab
        if (tab == MainTab.CHAT) {
            _unreadChatCount.value = 0
            _isFloatingWindowExpanded.value = false
            _isFloatingBubbleVisible.value = false
        }
    }

    // --- Report Form Actions ---

    fun selectCategory(categoryNumber: Int) {
        _formState.update { it.copy(selectedCategory = categoryNumber.coerceIn(1, 3)) }
    }

    fun updateTimeInput(time: String) {
        _formState.update { it.copy(timeInput = time) }
    }

    fun updateReportDateInput(dateIso: String) {
        _formState.update { it.copy(reportDateInput = dateIso) }
    }

    fun setCurrentTimeNow() {
        _formState.update {
            it.copy(
                timeInput = currentFormattedTime(),
                reportDateInput = currentIsoDate()
            )
        }
    }

    fun updateFormShift(shift: String) {
        _formState.update { it.copy(shiftInput = shift) }
    }

    fun updateFormStatus(status: ReportStatus) {
        _formState.update { it.copy(statusInput = status) }
    }

    fun updateDescription(text: String) {
        _formState.update { it.copy(descriptionInput = text) }
    }

    fun fillTemplateDescription() {
        val cat = ReportCategory.fromNumber(_formState.value.selectedCategory)
        _formState.update { it.copy(descriptionInput = cat.templateText) }
    }

    fun setPhotoAtSlot(slotIndex: Int, base64: String) {
        if (slotIndex !in 0..3) return
        _formState.update { state ->
            val updated = state.photoSlots.toMutableList()
            updated[slotIndex] = base64
            state.copy(photoSlots = updated)
        }
        _toastEvents.tryEmit("Foto slot ${slotIndex + 1} ditambahkan. Silakan isi keterangan di bawah foto.")
    }

    fun updatePhotoCaptionAtSlot(slotIndex: Int, caption: String) {
        if (slotIndex !in 0..3) return
        _formState.update { state ->
            val updated = state.photoCaptionSlots.toMutableList()
            updated[slotIndex] = caption.take(200)
            state.copy(photoCaptionSlots = updated)
        }
    }

    fun removePhotoAtSlot(slotIndex: Int) {
        if (slotIndex !in 0..3) return
        _formState.update { state ->
            val updatedPhotos = state.photoSlots.toMutableList()
            val updatedCaptions = state.photoCaptionSlots.toMutableList()
            updatedPhotos[slotIndex] = null
            updatedCaptions[slotIndex] = ""
            state.copy(photoSlots = updatedPhotos, photoCaptionSlots = updatedCaptions)
        }
    }

    // --- Report Filter & Search Actions ---

    fun updateReportKeyword(keyword: String) {
        _reportFilter.update { it.copy(keyword = keyword) }
    }

    fun updateReportFilterHour(hour: Int?) {
        _reportFilter.update { it.copy(hour = hour) }
    }

    fun updateReportFilterDay(day: Int?) {
        _reportFilter.update { it.copy(day = day) }
    }

    fun updateReportFilterMonth(month: Int?) {
        _reportFilter.update { it.copy(month = month) }
    }

    fun updateReportFilterYear(year: Int?) {
        _reportFilter.update { it.copy(year = year) }
    }

    fun filterReportsToday() {
        val cal = Calendar.getInstance()
        _reportFilter.update {
            it.copy(
                day = cal.get(Calendar.DAY_OF_MONTH),
                month = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR)
            )
        }
    }

    fun resetReportFilter() {
        _reportFilter.value = TimeKeywordFilter()
    }

    // --- Chat Filter & Search Actions ---

    fun updateChatFilterKeyword(keyword: String) {
        _chatFilter.update { it.copy(keyword = keyword) }
    }

    fun updateChatFilterHour(hour: Int?) {
        _chatFilter.update { it.copy(hour = hour) }
    }

    fun updateChatFilterDay(day: Int?) {
        _chatFilter.update { it.copy(day = day) }
    }

    fun updateChatFilterMonth(month: Int?) {
        _chatFilter.update { it.copy(month = month) }
    }

    fun updateChatFilterYear(year: Int?) {
        _chatFilter.update { it.copy(year = year) }
    }

    fun resetChatFilter() {
        _chatFilter.value = TimeKeywordFilter()
    }

    // --- Profile & Room Actions ---

    fun setGlobalShift(shift: String) {
        _formState.update { it.copy(shiftInput = shift) }
        viewModelScope.launch {
            val result = repository.updateActiveShift(shift, _activeRoomCode.value)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Shift aktif diatur ke $shift")
            } else {
                _toastEvents.tryEmit("Gagal menyimpan shift: ${result.exceptionOrNull()?.localizedMessage ?: "Error"}")
            }
        }
    }

    fun saveOfficerProfile(name: String, nrp: String, role: String, photoData: String? = null) {
        val existingProfile = (profileState.value as? UiState.Success)?.data
        val currentShift = existingProfile?.activeShift ?: _formState.value.shiftInput
        viewModelScope.launch {
            val result = repository.saveProfile(
                name = name,
                nrp = nrp,
                role = role,
                activeShift = currentShift,
                activeRoomCode = _activeRoomCode.value,
                photoData = photoData
            )
            if (result.isSuccess) {
                _toastEvents.tryEmit("Identitas petugas berhasil diperbarui.")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan profil.")
            }
        }
    }

    fun updateProfilePhoto(photoBase64: String) {
        viewModelScope.launch {
            val result = repository.updateProfilePhoto(photoBase64, _activeRoomCode.value)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Foto profil berhasil diperbarui!")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengganti foto profil.")
            }
        }
    }

    fun clearProfilePhoto() {
        viewModelScope.launch {
            val result = repository.updateProfilePhoto("", _activeRoomCode.value)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Foto profil dikembalikan ke inisial default.")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus foto profil.")
            }
        }
    }

    fun joinRoomByCode(code: String, onSuccess: () -> Unit = {}) {
        if (!isInternetConnected.value) {
            _toastEvents.tryEmit("Tidak ada koneksi internet. Pastikan internet aktif untuk bergabung.")
            return
        }
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        viewModelScope.launch {
            _isJoiningRoom.value = true
            val result = repository.joinRoomByCode(code, profile, previousRoomCode = _activeRoomCode.value)
            _isJoiningRoom.value = false
            if (result.isSuccess) {
                val room = result.getOrThrow()
                _activeRoomCode.value = room.roomCode
                _toastEvents.tryEmit("Berhasil bergabung ke ruang ${room.title} (${room.roomCode})")
                onSuccess()
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal bergabung ke ruang.")
            }
        }
    }

    fun createAndJoinRoom(code: String, title: String, onSuccess: () -> Unit = {}) {
        if (!isInternetConnected.value) {
            _toastEvents.tryEmit("Tidak ada koneksi internet. Pastikan internet aktif untuk membuat ruang.")
            return
        }
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        viewModelScope.launch {
            _isJoiningRoom.value = true
            val result = repository.createAndJoinRoom(code, title, profile, previousRoomCode = _activeRoomCode.value)
            _isJoiningRoom.value = false
            if (result.isSuccess) {
                val room = result.getOrThrow()
                _activeRoomCode.value = room.roomCode
                _toastEvents.tryEmit("Ruang ${room.title} (${room.roomCode}) dibuat & siap koordinasi!")
                onSuccess()
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal membuat ruang baru.")
            }
        }
    }

    fun generateRandomRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars.random() }.joinToString("")
        return "BPS-$suffix"
    }

    fun submitReportItem() {
        val form = _formState.value
        val desc = form.descriptionInput.trim()
        if (desc.isEmpty()) {
            _toastEvents.tryEmit("Mohon isi rincian kegiatan terlebih dahulu.")
            return
        }
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val validPhotos = mutableListOf<String>()
        val validCaptions = mutableListOf<String>()
        form.photoSlots.forEachIndexed { idx, base64 ->
            if (base64 != null) {
                validPhotos.add(base64)
                validCaptions.add(form.photoCaptionSlots.getOrNull(idx)?.trim()?.take(200).orEmpty())
            }
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            val result = repository.addReport(
                category = form.selectedCategory,
                time = form.timeInput.ifBlank { currentFormattedTime() },
                reportDate = form.reportDateInput.ifBlank { currentIsoDate() },
                shift = form.shiftInput,
                status = form.statusInput.label,
                description = desc,
                photos = validPhotos,
                photoCaptions = validCaptions,
                officerName = profile.name,
                officerNrp = profile.nrp
            )
            if (result.isSuccess) {
                _formState.update {
                    it.copy(
                        descriptionInput = "",
                        photoSlots = listOf(null, null, null, null),
                        photoCaptionSlots = listOf("", "", "", ""),
                        timeInput = currentFormattedTime(),
                        reportDateInput = currentIsoDate(),
                        isSubmitting = false
                    )
                }
                val suffix = if (isInternetConnected.value) "" else " (antrean offline)"
                _toastEvents.tryEmit("Rincian berhasil ditambahkan ke Kategori ${form.selectedCategory}$suffix!")
            } else {
                _formState.update { it.copy(isSubmitting = false) }
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan laporan.")
            }
        }
    }

    fun deleteReportItem(reportId: String) {
        viewModelScope.launch {
            val result = repository.deleteReport(reportId)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Rincian kegiatan dihapus dari laporan.")
            } else {
                _toastEvents.tryEmit("Gagal menghapus rincian.")
            }
        }
    }

    fun resetShiftReports() {
        val currentReports = (reportsState.value as? UiState.Success)?.data.orEmpty()
        if (currentReports.isEmpty()) {
            _toastEvents.tryEmit("Data laporan sudah kosong.")
            return
        }
        viewModelScope.launch {
            val result = repository.resetAllReports(currentReports.map { it.id })
            if (result.isSuccess) {
                _toastEvents.tryEmit("Data laporan shift berhasil direset bersih.")
            } else {
                _toastEvents.tryEmit("Gagal mereset data laporan.")
            }
        }
    }

    // --- Chat Actions ---

    fun updateChatTextInput(text: String) {
        _chatTextInput.value = text
    }

    fun setPendingChatPhoto(base64: String?) {
        _pendingChatPhoto.value = base64
        if (base64 == null) {
            _pendingChatPhotoCaption.value = ""
        }
    }

    fun updatePendingChatPhotoCaption(caption: String) {
        _pendingChatPhotoCaption.value = caption.take(300)
    }

    fun sendTextOrPhotoChat() {
        val text = _chatTextInput.value.trim()
        val photo = _pendingChatPhoto.value
        val photoCaption = _pendingChatPhotoCaption.value.trim()
        if (text.isEmpty() && photo == null) return
        if (!isInternetConnected.value) {
            _toastEvents.tryEmit("Koneksi internet terputus. Pesan akan terkirim otomatis saat online.")
        }

        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val roomCode = _activeRoomCode.value
        viewModelScope.launch {
            _isSendingChat.value = true
            val result = repository.sendRoomChatMessage(
                roomCode = roomCode,
                senderName = profile.name,
                senderRole = profile.role,
                fromOfficer = true,
                text = text,
                imageData = photo,
                imageCaption = photoCaption,
                senderPhoto = profile.photoData
            )
            _isSendingChat.value = false
            if (result.isSuccess) {
                _chatTextInput.value = ""
                _pendingChatPhoto.value = null
                _pendingChatPhotoCaption.value = ""
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengirim pesan.")
            }
        }
    }

    fun sendVoiceNote(audioBase64: String, duration: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val roomCode = _activeRoomCode.value
        viewModelScope.launch {
            _isSendingChat.value = true
            val result = repository.sendRoomChatMessage(
                roomCode = roomCode,
                senderName = profile.name,
                senderRole = profile.role,
                fromOfficer = true,
                text = "",
                audioType = "recorded",
                audioData = audioBase64,
                audioDuration = duration,
                senderPhoto = profile.photoData
            )
            _isSendingChat.value = false
            if (result.isSuccess) {
                _toastEvents.tryEmit("Voice Note berhasil dikirim ke ruang $roomCode!")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengirim Voice Note.")
            }
        }
    }

    fun editChatMessage(messageId: String, newText: String, newImageCaption: String?) {
        val roomCode = _activeRoomCode.value
        viewModelScope.launch {
            val result = repository.editRoomChatMessage(
                roomCode = roomCode,
                messageId = messageId,
                newText = newText,
                newImageCaption = newImageCaption
            )
            if (result.isSuccess) {
                _toastEvents.tryEmit("Pesan berhasil diperbarui.")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengedit pesan.")
            }
        }
    }

    fun deleteChatMessage(messageId: String) {
        val roomCode = _activeRoomCode.value
        viewModelScope.launch {
            val result = repository.deleteRoomChatMessage(roomCode, messageId)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Pesan dihapus.")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus pesan.")
            }
        }
    }

    fun clearAllChatMessages() {
        val messages = (chatState.value as? UiState.Success)?.data.orEmpty()
        if (messages.isEmpty()) {
            _toastEvents.tryEmit("Live chat sudah kosong.")
            return
        }
        val room = activeChatRoom.value
        val isRoomCreator = room?.createdBy == currentUserId
        val deletableIds = messages.filter { msg ->
            msg.id.isNotBlank() && (msg.userId == currentUserId || isRoomCreator)
        }.map { it.id }

        if (deletableIds.isEmpty()) {
            _toastEvents.tryEmit("Hanya pembuat ruang atau pengirim pesan yang dapat menghapus pesan tersebut.")
            return
        }

        val skippedCount = messages.size - deletableIds.size
        viewModelScope.launch {
            val result = repository.clearRoomChatMessages(_activeRoomCode.value, deletableIds)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                if (skippedCount > 0) {
                    _toastEvents.tryEmit("$count pesan Anda dihapus ($skippedCount pesan anggota lain tetap tersimpan).")
                } else {
                    _toastEvents.tryEmit("Semua chat ($count pesan) berhasil dibersihkan.")
                }
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus semua chat.")
            }
        }
    }

    fun simulateTeamReply() {
        viewModelScope.launch {
            val result = repository.simulateTeamReply(_activeRoomCode.value)
            if (result.isFailure) {
                _toastEvents.tryEmit("Gagal mengirim simulasi respon tim.")
            }
        }
    }

    fun emitToast(message: String) {
        _toastEvents.tryEmit(message)
    }

    fun buildWhatsAppRecap(
        dateLabel: String,
        profile: OfficerProfile,
        reports: List<SecurityReport>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("*LAPORAN HARIAN SECURITY - PLOT BPS*")
        sb.appendLine("Tanggal : $dateLabel")
        sb.appendLine("Shift   : ${profile.activeShift}")
        sb.appendLine("Petugas : ${profile.name} (${profile.nrp})")
        sb.appendLine("====================================")
        sb.appendLine()

        ReportCategory.entries.forEach { cat ->
            val items = reports.filter { it.category.toInt() == cat.number }
            sb.appendLine("*${cat.number}. ${cat.shortTitle.uppercase()}*")
            if (items.isEmpty()) {
                sb.appendLine("   - Nihil / Belum ada rincian diinput.")
                sb.appendLine()
            } else {
                items.forEachIndexed { idx, item ->
                    val datePart = item.formattedDateBadge().takeIf { it.isNotBlank() }?.let { "$it • " }.orEmpty()
                    sb.appendLine("   ${idx + 1}) [$datePart${item.time} WIB | ${item.shift}] (${item.status})")
                    sb.appendLine("      ${item.description}")
                    if (item.photos.isNotEmpty()) {
                        sb.appendLine("      Lampiran: ${item.photos.size} Foto Dokumentasi")
                        item.photos.indices.forEach { pIdx ->
                            val cap = item.photoCaptions.getOrNull(pIdx)?.takeIf { it.isNotBlank() }
                            if (cap != null) {
                                sb.appendLine("        - Foto ${pIdx + 1}: $cap")
                            }
                        }
                    }
                }
                sb.appendLine()
            }
        }
        sb.appendLine("====================================")
        sb.append("Demikian laporan situasi Plot BPS dilaporkan. Komandan & Korlap mohon arahannya. Selamat bertugas!")
        return sb.toString()
    }

    // --- Activity Table Sheet Actions ---

    fun selectActivitySheet(sheetId: String) {
        _selectedSheetId.value = sheetId
    }

    fun updateTableKeyword(keyword: String) {
        _tableFilter.update { it.copy(keyword = keyword) }
    }

    fun updateTableFilterHour(hour: Int?) {
        _tableFilter.update { it.copy(hour = hour) }
    }

    fun updateTableFilterDay(day: Int?) {
        _tableFilter.update { it.copy(day = day) }
    }

    fun updateTableFilterMonth(month: Int?) {
        _tableFilter.update { it.copy(month = month) }
    }

    fun updateTableFilterYear(year: Int?) {
        _tableFilter.update { it.copy(year = year) }
    }

    fun filterTableToday() {
        val cal = Calendar.getInstance()
        _tableFilter.update {
            it.copy(
                day = cal.get(Calendar.DAY_OF_MONTH),
                month = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR)
            )
        }
    }

    fun resetTableFilter() {
        _tableFilter.value = TimeKeywordFilter()
    }

    private fun currentActiveSheet(): DailyActivitySheet? {
        val list = (activitySheetsState.value as? UiState.Success)?.data.orEmpty()
        val id = _selectedSheetId.value
        return list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    fun createNewActivitySheet(
        sheetDate: String,
        shift: String,
        docNumber: String,
        includeDefaultTemplate: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val cleanDate = sheetDate.trim().ifBlank { currentIsoDate() }
        if (!cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            _toastEvents.tryEmit("Format tanggal harus yyyy-MM-dd (contoh: 2026-11-18).")
            return
        }
        val newSheet = DailyActivitySheet(
            id = "",
            userId = currentUserId,
            docNumber = docNumber.trim().take(40),
            sheetDate = cleanDate,
            shift = shift,
            personnelRows = if (includeDefaultTemplate) ActivitySheetTemplates.starterPersonnelRows(profile.name) else emptyList(),
            inventoryRows = if (includeDefaultTemplate) ActivitySheetTemplates.defaultInventoryRows else emptyList(),
            activityRows = if (includeDefaultTemplate) ActivitySheetTemplates.starterActivityRows(shift) else emptyList(),
            officerName = profile.name,
            officerNrp = profile.nrp
        )
        viewModelScope.launch {
            val result = repository.saveActivitySheet(newSheet)
            if (result.isSuccess) {
                _selectedSheetId.value = result.getOrThrow()
                _toastEvents.tryEmit("Lembar tabel baru ($cleanDate • $shift) berhasil dibuat.")
                onSuccess()
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal membuat lembar tabel.")
            }
        }
    }

    private fun saveMutatedSheet(successToast: String, mutator: (DailyActivitySheet) -> DailyActivitySheet) {
        val current = currentActiveSheet() ?: return
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val updated = mutator(current).copy(
            officerName = profile.name,
            officerNrp = profile.nrp
        )
        viewModelScope.launch {
            val result = repository.saveActivitySheet(updated)
            if (result.isSuccess) {
                if (successToast.isNotBlank()) _toastEvents.tryEmit(successToast)
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan perubahan tabel.")
            }
        }
    }

    fun updateSheetHeader(sheetDate: String, shift: String, docNumber: String) {
        val cleanDate = sheetDate.trim().ifBlank { currentIsoDate() }
        if (!cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            _toastEvents.tryEmit("Format tanggal harus yyyy-MM-dd.")
            return
        }
        saveMutatedSheet("Header lembar diperbarui.") { sheet ->
            sheet.copy(
                sheetDate = cleanDate,
                shift = shift,
                docNumber = docNumber.trim().take(40)
            )
        }
    }

    fun upsertPersonnelRow(editIndex: Int?, row: PersonnelRow) {
        if (row.nama.trim().isEmpty()) {
            _toastEvents.tryEmit("Nama anggota tidak boleh kosong.")
            return
        }
        saveMutatedSheet(if (editIndex == null) "Anggota ditambahkan ke tabel." else "Data anggota diperbarui.") { sheet ->
            val list = sheet.personnelRows.toMutableList()
            if (editIndex != null && editIndex in list.indices) {
                list[editIndex] = row
            } else {
                list.add(row)
            }
            sheet.copy(personnelRows = list)
        }
    }

    fun deletePersonnelRow(index: Int) {
        saveMutatedSheet("Baris anggota dihapus.") { sheet ->
            val list = sheet.personnelRows.toMutableList()
            if (index in list.indices) list.removeAt(index)
            sheet.copy(personnelRows = list)
        }
    }

    fun upsertInventoryRow(editIndex: Int?, row: InventoryRow) {
        if (row.inventaris.trim().isEmpty()) {
            _toastEvents.tryEmit("Nama barang inventaris tidak boleh kosong.")
            return
        }
        saveMutatedSheet(if (editIndex == null) "Inventaris ditambahkan." else "Data inventaris diperbarui.") { sheet ->
            val list = sheet.inventoryRows.toMutableList()
            if (editIndex != null && editIndex in list.indices) {
                list[editIndex] = row
            } else {
                list.add(row)
            }
            sheet.copy(inventoryRows = list)
        }
    }

    fun deleteInventoryRow(index: Int) {
        saveMutatedSheet("Baris inventaris dihapus.") { sheet ->
            val list = sheet.inventoryRows.toMutableList()
            if (index in list.indices) list.removeAt(index)
            sheet.copy(inventoryRows = list)
        }
    }

    fun loadDefaultInventoryTemplate() {
        saveMutatedSheet("12 item inventaris standar Pos BPS dimuat.") { sheet ->
            sheet.copy(inventoryRows = ActivitySheetTemplates.defaultInventoryRows)
        }
    }

    fun upsertActivityRow(editIndex: Int?, row: ActivityTableRow) {
        if (row.uraian.trim().isEmpty()) {
            _toastEvents.tryEmit("Uraian kegiatan tidak boleh kosong.")
            return
        }
        val cleanJam = row.jam.trim().ifBlank { currentFormattedTime().replace(":", ".") }
        val cleanRow = row.copy(jam = cleanJam)
        saveMutatedSheet(if (editIndex == null) "Kegiatan ditambahkan ke tabel." else "Uraian kegiatan diperbarui.") { sheet ->
            val list = sheet.activityRows.toMutableList()
            if (editIndex != null && editIndex in list.indices) {
                list[editIndex] = cleanRow
            } else {
                list.add(cleanRow)
            }
            sheet.copy(activityRows = list)
        }
    }

    fun deleteActivityRow(index: Int) {
        saveMutatedSheet("Baris uraian kegiatan dihapus.") { sheet ->
            val list = sheet.activityRows.toMutableList()
            if (index in list.indices) list.removeAt(index)
            sheet.copy(activityRows = list)
        }
    }

    fun deleteActiveActivitySheet() {
        val current = currentActiveSheet() ?: return
        if (current.id.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteActivitySheet(current.id)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Lembar tabel (${current.sheetDate}) berhasil dihapus.")
            } else {
                _toastEvents.tryEmit("Gagal menghapus lembar tabel.")
            }
        }
    }

    fun buildTableWhatsAppRecap(sheet: DailyActivitySheet, profile: OfficerProfile): String {
        val sb = StringBuilder()
        sb.appendLine("*BUKU JURNAL / TABEL PELAPORAN SECURITY BPS*")
        sb.appendLine("Tanggal : ${sheet.formattedDisplayDate()}")
        sb.appendLine("Shift   : ${sheet.shift}")
        if (sheet.docNumber.isNotBlank()) sb.appendLine("No. Dok : ${sheet.docNumber}")
        sb.appendLine("Penyusun: ${profile.name} (${profile.nrp})")
        sb.appendLine("====================================")
        sb.appendLine()
        sb.appendLine("*1. DAFTAR PERSONEL JAGA*")
        if (sheet.personnelRows.isEmpty()) {
            sb.appendLine("   - Kosong")
        } else {
            sheet.personnelRows.forEachIndexed { idx, p ->
                val ketSuffix = if (p.ket.isNotBlank()) " | Ket: ${p.ket}" else ""
                sb.appendLine("   ${idx + 1}. ${p.nama} (${p.jabatan}) • Plot ${p.plot} • ${p.kehadiran}$ketSuffix")
            }
        }
        sb.appendLine()
        sb.appendLine("*2. DAFTAR INVENTARIS POS*")
        if (sheet.inventoryRows.isEmpty()) {
            sb.appendLine("   - Kosong")
        } else {
            sheet.inventoryRows.forEachIndexed { idx, inv ->
                val ketSuffix = if (inv.keterangan.isNotBlank()) " (${inv.keterangan})" else ""
                sb.appendLine("   ${idx + 1}. ${inv.inventaris} | Jml: ${inv.jumlah} | ${inv.kondisi}$ketSuffix")
            }
        }
        sb.appendLine()
        sb.appendLine("*3. URAIAN KEGIATAN*")
        if (sheet.activityRows.isEmpty()) {
            sb.appendLine("   - Belum ada uraian kegiatan.")
        } else {
            sheet.activityRows.forEachIndexed { idx, act ->
                sb.appendLine("   ${idx + 1}. [${act.jam} WIB] ${act.uraian}")
            }
        }
        sb.appendLine()
        sb.append("====================================")
        return sb.toString()
    }

    // --- 15-Member Attendance Table Actions ---

    fun selectAttendanceSheet(sheetId: String) {
        _selectedAttendanceSheetId.value = sheetId
    }

    fun updateAttendanceKeyword(keyword: String) {
        _attendanceFilter.update { it.copy(keyword = keyword) }
    }

    fun updateAttendanceFilterHour(hour: Int?) {
        _attendanceFilter.update { it.copy(hour = hour) }
    }

    fun updateAttendanceFilterDay(day: Int?) {
        _attendanceFilter.update { it.copy(day = day) }
    }

    fun updateAttendanceFilterMonth(month: Int?) {
        _attendanceFilter.update { it.copy(month = month) }
    }

    fun updateAttendanceFilterYear(year: Int?) {
        _attendanceFilter.update { it.copy(year = year) }
    }

    fun filterAttendanceToday() {
        val cal = Calendar.getInstance()
        _attendanceFilter.update {
            it.copy(
                day = cal.get(Calendar.DAY_OF_MONTH),
                month = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR)
            )
        }
    }

    fun resetAttendanceFilter() {
        _attendanceFilter.value = TimeKeywordFilter()
    }

    private fun currentActiveAttendanceSheet(): AttendanceSheet? {
        val list = (attendanceSheetsState.value as? UiState.Success)?.data.orEmpty()
        val id = _selectedAttendanceSheetId.value
        return list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    fun createNewAttendanceSheet(
        sheetDate: String,
        shift: String,
        periodLabel: String,
        commanderName: String,
        copyExistingNames: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val cleanDate = sheetDate.trim().ifBlank { currentIsoDate() }
        if (!cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            _toastEvents.tryEmit("Format tanggal harus yyyy-MM-dd (contoh: 2026-11-18).")
            return
        }
        val (masuk, pulang) = AttendanceTemplates.shiftTimes(shift)
        val nowStr = currentFormattedTime()
        val existingRows = currentActiveAttendanceSheet()?.rows.orEmpty()
        val rows = if (copyExistingNames && existingRows.isNotEmpty()) {
            existingRows.take(25).map { prev ->
                prev.copy(
                    status = AttendanceStatus.HADIR.code,
                    jamMasuk = masuk,
                    jamPulang = pulang,
                    recordedAt = nowStr,
                    keterangan = ""
                )
            }
        } else {
            AttendanceTemplates.default15Members(
                commanderName = commanderName.ifBlank { profile.name },
                shift = shift
            )
        }

        val newSheet = AttendanceSheet(
            id = "",
            userId = currentUserId,
            sheetDate = cleanDate,
            shift = shift,
            periodLabel = periodLabel.trim().ifBlank { "Regu Plot BPS (15 Anggota)" }.take(60),
            commanderName = commanderName.trim().ifBlank { profile.name }.take(80),
            rows = rows
        )
        viewModelScope.launch {
            val result = repository.saveAttendanceSheet(newSheet)
            if (result.isSuccess) {
                _selectedAttendanceSheetId.value = result.getOrThrow()
                _toastEvents.tryEmit("Lembar absensi 15 anggota ($cleanDate • $shift) dibuat.")
                onSuccess()
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal membuat lembar absensi.")
            }
        }
    }

    private fun saveMutatedAttendanceSheet(
        successToast: String,
        mutator: (AttendanceSheet) -> AttendanceSheet
    ) {
        val current = currentActiveAttendanceSheet() ?: return
        val updated = mutator(current)
        viewModelScope.launch {
            val result = repository.saveAttendanceSheet(updated)
            if (result.isSuccess) {
                if (successToast.isNotBlank()) _toastEvents.tryEmit(successToast)
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan absensi.")
            }
        }
    }

    fun updateAttendanceHeader(sheetDate: String, shift: String, periodLabel: String, commanderName: String) {
        val cleanDate = sheetDate.trim().ifBlank { currentIsoDate() }
        if (!cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            _toastEvents.tryEmit("Format tanggal harus yyyy-MM-dd.")
            return
        }
        saveMutatedAttendanceSheet("Header absensi diperbarui.") { sheet ->
            sheet.copy(
                sheetDate = cleanDate,
                shift = shift,
                periodLabel = periodLabel.trim().ifBlank { "Regu Plot BPS" }.take(60),
                commanderName = commanderName.trim().ifBlank { "Danru Plot BPS" }.take(80)
            )
        }
    }

    fun upsertAttendanceRow(editIndex: Int?, row: AttendanceMemberRow) {
        if (row.nama.trim().isEmpty()) {
            _toastEvents.tryEmit("Nama anggota tidak boleh kosong.")
            return
        }
        val stampedRow = row.copy(
            recordedAt = row.recordedAt.ifBlank { currentFormattedTime() }
        )
        saveMutatedAttendanceSheet(
            if (editIndex == null) "Anggota ditambahkan ke tabel absensi." else "Absensi ${row.nama} tercatat."
        ) { sheet ->
            val list = sheet.rows.toMutableList()
            if (editIndex != null && editIndex in list.indices) {
                list[editIndex] = stampedRow
            } else if (list.size < 25) {
                list.add(stampedRow)
            }
            sheet.copy(rows = list)
        }
    }

    fun quickCycleAttendanceStatus(rowIndex: Int) {
        val nowStr = currentFormattedTime()
        saveMutatedAttendanceSheet("") { sheet ->
            val list = sheet.rows.toMutableList()
            if (rowIndex in list.indices) {
                val cur = list[rowIndex]
                val nextStatus = cur.statusEnum.next()
                list[rowIndex] = cur.copy(
                    status = nextStatus.code,
                    recordedAt = nowStr
                )
            }
            sheet.copy(rows = list)
        }
    }

    fun markAllAttendanceStatus(statusCode: String) {
        val status = AttendanceStatus.fromCode(statusCode)
        val nowStr = currentFormattedTime()
        saveMutatedAttendanceSheet("Semua anggota ditandai ${status.label} ($nowStr WIB).") { sheet ->
            val (masuk, pulang) = AttendanceTemplates.shiftTimes(sheet.shift)
            val updated = sheet.rows.map { row ->
                row.copy(
                    status = status.code,
                    jamMasuk = if (status == AttendanceStatus.HADIR) row.jamMasuk.ifBlank { masuk } else "-",
                    jamPulang = if (status == AttendanceStatus.HADIR) row.jamPulang.ifBlank { pulang } else "-",
                    recordedAt = nowStr
                )
            }
            sheet.copy(rows = updated)
        }
    }

    fun fillAttendanceShiftTimes() {
        val nowStr = currentFormattedTime()
        saveMutatedAttendanceSheet("Jam masuk & pulang disesuaikan ke standar shift aktif.") { sheet ->
            val (masuk, pulang) = AttendanceTemplates.shiftTimes(sheet.shift)
            sheet.copy(
                rows = sheet.rows.map { row ->
                    row.copy(
                        jamMasuk = masuk,
                        jamPulang = pulang,
                        recordedAt = nowStr
                    )
                }
            )
        }
    }

    fun deleteAttendanceRow(rowIndex: Int) {
        saveMutatedAttendanceSheet("Baris anggota dihapus.") { sheet ->
            val list = sheet.rows.toMutableList()
            if (rowIndex in list.indices) list.removeAt(rowIndex)
            sheet.copy(rows = list)
        }
    }

    fun ensure15AttendanceRows() {
        val current = currentActiveAttendanceSheet() ?: return
        if (current.rows.size >= 15) {
            _toastEvents.tryEmit("Tabel sudah memuat ${current.rows.size} anggota.")
            return
        }
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val template = AttendanceTemplates.default15Members(profile.name, current.shift)
        saveMutatedAttendanceSheet("Daftar anggota dilengkapi menjadi 15 personel.") { sheet ->
            val combined = sheet.rows.toMutableList()
            for (idx in combined.size until 15) {
                combined.add(template[idx])
            }
            sheet.copy(rows = combined)
        }
    }

    fun deleteActiveAttendanceSheet() {
        val current = currentActiveAttendanceSheet() ?: return
        if (current.id.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteAttendanceSheet(current.id)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Lembar absensi (${current.sheetDate}) berhasil dihapus.")
            } else {
                _toastEvents.tryEmit("Gagal menghapus lembar absensi.")
            }
        }
    }

    fun buildAttendanceWhatsAppRecap(sheet: AttendanceSheet, profile: OfficerProfile): String {
        val hCount = sheet.rows.count { it.statusEnum == AttendanceStatus.HADIR }
        val iCount = sheet.rows.count { it.statusEnum == AttendanceStatus.IZIN }
        val sCount = sheet.rows.count { it.statusEnum == AttendanceStatus.SAKIT }
        val aCount = sheet.rows.count { it.statusEnum == AttendanceStatus.ALPHA }
        val lCount = sheet.rows.count { it.statusEnum == AttendanceStatus.LIBUR }

        val sb = StringBuilder()
        sb.appendLine("*DAFTAR ABSENSI ANGGOTA SECURITY - PLOT BPS*")
        sb.appendLine("Tanggal : ${sheet.formattedDisplayDate()}")
        sb.appendLine("Shift   : ${sheet.shift} (${sheet.periodLabel})")
        sb.appendLine("Danru   : ${sheet.commanderName}")
        sb.appendLine("Rekap   : Total ${sheet.rows.size} | Hadir $hCount | Izin $iCount | Sakit $sCount | Alpha $aCount | Libur $lCount")
        sb.appendLine("====================================")
        sheet.rows.forEachIndexed { idx, row ->
            val no = (idx + 1).toString().padStart(2, '0')
            val ketPart = if (row.keterangan.isNotBlank()) " | Ket: ${row.keterangan}" else ""
            val recPart = if (row.recordedAt.isNotBlank()) " (Terekam ${row.recordedAt})" else ""
            sb.appendLine("$no. ${row.nama} [${row.nrp}] • ${row.jabatan} (${row.plot}) -> *${row.statusEnum.label}* [${row.jamMasuk}-${row.jamPulang}]$recPart$ketPart")
        }
        sb.appendLine("====================================")
        sb.append("Dilaporkan oleh: ${profile.name} (${profile.nrp})")
        return sb.toString()
    }

    // --- PJU Checklist Actions ---

    fun selectPjuSheet(sheetId: String) {
        _selectedPjuSheetId.value = sheetId
    }

    fun updatePjuKeyword(keyword: String) {
        _pjuFilter.update { it.copy(keyword = keyword) }
    }

    fun updatePjuFilterDay(day: Int?) {
        _pjuFilter.update { it.copy(day = day) }
    }

    fun updatePjuFilterMonth(month: Int?) {
        _pjuFilter.update { it.copy(month = month) }
    }

    fun updatePjuFilterYear(year: Int?) {
        _pjuFilter.update { it.copy(year = year) }
    }

    fun filterPjuCurrentMonth() {
        val cal = Calendar.getInstance()
        _pjuFilter.update {
            it.copy(
                day = null,
                month = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR)
            )
        }
    }

    fun resetPjuFilter() {
        _pjuFilter.value = TimeKeywordFilter()
    }

    private fun currentActivePjuSheet(): PjuChecklistSheet? {
        val list = (pjuSheetsState.value as? UiState.Success)?.data.orEmpty()
        val id = _selectedPjuSheetId.value
        return list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    fun createNewPjuSheet(
        month: Int,
        year: Int,
        startDay: Int,
        endDay: Int,
        responsibleNames: String,
        copyExistingClusters: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val cleanMonth = month.coerceIn(1, 12)
        val cleanYear = year.coerceIn(2020, 2100)
        val cleanStart = startDay.coerceIn(1, 31)
        val cleanEnd = endDay.coerceIn(cleanStart, 31)
        val cleanResp = responsibleNames.trim().ifBlank { PjuTemplates.DEFAULT_RESPONSIBLE }.take(120)

        val existingClusters = currentActivePjuSheet()?.clusters.orEmpty()
        val clusters = if (copyExistingClusters && existingClusters.isNotEmpty()) {
            existingClusters.map { it.copy(checks = emptyMap()) }
        } else {
            PjuTemplates.defaultClusterRows()
        }
        val monthYearLabel = PjuTemplates.monthYearLabel(cleanMonth, cleanYear)
        val newSheet = PjuChecklistSheet(
            id = "",
            userId = currentUserId,
            monthYear = monthYearLabel,
            sheetMonth = cleanMonth.toLong(),
            sheetYear = cleanYear.toLong(),
            periodStartDay = cleanStart.toLong(),
            periodEndDay = cleanEnd.toLong(),
            clusters = clusters,
            issues = emptyList(),
            notes = "",
            responsibleNames = cleanResp
        )
        viewModelScope.launch {
            val result = repository.savePjuSheet(newSheet)
            if (result.isSuccess) {
                _selectedPjuSheetId.value = result.getOrThrow()
                _toastEvents.tryEmit("Lembar Checklist PJU $monthYearLabel (Tgl $cleanStart–$cleanEnd) dibuat.")
                onSuccess()
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal membuat lembar PJU baru.")
            }
        }
    }

    private fun saveMutatedPjuSheet(
        successToast: String,
        mutator: (PjuChecklistSheet) -> PjuChecklistSheet
    ) {
        val current = currentActivePjuSheet() ?: return
        val updated = mutator(current)
        viewModelScope.launch {
            val result = repository.savePjuSheet(updated)
            if (result.isSuccess) {
                if (successToast.isNotBlank()) _toastEvents.tryEmit(successToast)
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan checklist PJU.")
            }
        }
    }

    fun updatePjuHeader(
        month: Int,
        year: Int,
        startDay: Int,
        endDay: Int,
        responsibleNames: String
    ) {
        val cleanMonth = month.coerceIn(1, 12)
        val cleanYear = year.coerceIn(2020, 2100)
        val cleanStart = startDay.coerceIn(1, 31)
        val cleanEnd = endDay.coerceIn(cleanStart, 31)
        saveMutatedPjuSheet("Periode & Penanggung Jawab PJU diperbarui.") { sheet ->
            sheet.copy(
                monthYear = PjuTemplates.monthYearLabel(cleanMonth, cleanYear),
                sheetMonth = cleanMonth.toLong(),
                sheetYear = cleanYear.toLong(),
                periodStartDay = cleanStart.toLong(),
                periodEndDay = cleanEnd.toLong(),
                responsibleNames = responsibleNames.trim().ifBlank { PjuTemplates.DEFAULT_RESPONSIBLE }.take(120)
            )
        }
    }

    fun cyclePjuSlot(clusterIndex: Int, day: Int, slotIndex: Int) {
        saveMutatedPjuSheet("") { sheet ->
            val list = sheet.clusters.toMutableList()
            if (clusterIndex in list.indices) {
                list[clusterIndex] = list[clusterIndex].cycleSlot(day, slotIndex)
            }
            sheet.copy(clusters = list)
        }
    }

    fun setPjuCellSlots(clusterIndex: Int, day: Int, slots: List<PjuLampStatus>) {
        saveMutatedPjuSheet("Kondisi PJU tanggal $day diperbarui.") { sheet ->
            val list = sheet.clusters.toMutableList()
            if (clusterIndex in list.indices) {
                list[clusterIndex] = list[clusterIndex].withDaySlots(day, slots)
            }
            sheet.copy(clusters = list)
        }
    }

    fun markPjuDayAllBaik(day: Int) {
        val allBaik = listOf(PjuLampStatus.BAIK, PjuLampStatus.BAIK, PjuLampStatus.BAIK, PjuLampStatus.BAIK)
        saveMutatedPjuSheet("Semua cluster PJU tanggal $day ditandai Nyala / Baik (✓).") { sheet ->
            sheet.copy(
                clusters = sheet.clusters.map { row ->
                    row.withDaySlots(day, allBaik)
                }
            )
        }
    }

    fun upsertPjuCluster(editIndex: Int?, clusterName: String) {
        val clean = clusterName.trim().uppercase(Locale.getDefault()).take(60)
        if (clean.isEmpty()) {
            _toastEvents.tryEmit("Nama cluster PJU tidak boleh kosong.")
            return
        }
        saveMutatedPjuSheet(
            if (editIndex == null) "Cluster '$clean' ditambahkan." else "Nama cluster diperbarui."
        ) { sheet ->
            val list = sheet.clusters.toMutableList()
            if (editIndex != null && editIndex in list.indices) {
                list[editIndex] = list[editIndex].copy(clusterName = clean)
            } else if (list.size < 40) {
                list.add(PjuClusterRow(clusterName = clean))
            }
            sheet.copy(clusters = list)
        }
    }

    fun deletePjuCluster(index: Int) {
        saveMutatedPjuSheet("Cluster PJU dihapus.") { sheet ->
            val list = sheet.clusters.toMutableList()
            if (index in list.indices) list.removeAt(index)
            sheet.copy(clusters = list)
        }
    }

    fun upsertPjuIssue(editIndex: Int?, issue: PjuIssueEntry) {
        if (issue.keluhan.trim().isEmpty()) {
            _toastEvents.tryEmit("Keluhan / permasalahan PJU wajib diisi.")
            return
        }
        val cleanIssue = issue.copy(
            tanggal = issue.tanggal.trim().ifBlank { currentIsoDate() }.take(24),
            cluster = issue.cluster.trim().ifBlank { "PJU KRISAN" }.take(60),
            titikLampu = issue.titikLampu.trim().ifBlank { "Lampu 1" }.take(40),
            keluhan = issue.keluhan.trim().take(400),
            tindakLanjut = issue.tindakLanjut.trim().take(400),
            status = issue.status.trim().ifBlank { "Belum Ditangani" }.take(32)
        )
        saveMutatedPjuSheet(
            if (editIndex == null) "Permasalahan PJU dicatat ke form." else "Catatan masalah PJU diperbarui."
        ) { sheet ->
            val list = sheet.issues.toMutableList()
            if (editIndex != null && editIndex in list.indices) {
                list[editIndex] = cleanIssue
            } else if (list.size < 100) {
                list.add(cleanIssue)
            }
            sheet.copy(issues = list)
        }
    }

    fun deletePjuIssue(index: Int) {
        saveMutatedPjuSheet("Laporan masalah PJU dihapus.") { sheet ->
            val list = sheet.issues.toMutableList()
            if (index in list.indices) list.removeAt(index)
            sheet.copy(issues = list)
        }
    }

    fun updatePjuNotes(notes: String) {
        saveMutatedPjuSheet("Catatan checklist PJU disimpan.") { sheet ->
            sheet.copy(notes = notes.trim().take(2000))
        }
    }

    fun deleteActivePjuSheet() {
        val current = currentActivePjuSheet() ?: return
        if (current.id.isBlank()) return
        viewModelScope.launch {
            val result = repository.deletePjuSheet(current.id)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Lembar Checklist PJU (${current.monthYear}) dihapus.")
            } else {
                _toastEvents.tryEmit("Gagal menghapus lembar PJU.")
            }
        }
    }

    fun buildPjuWhatsAppRecap(sheet: PjuChecklistSheet, profile: OfficerProfile): String {
        val counts = sheet.countSlotsByStatus()
        val compliance = sheet.complianceInfo()
        val sb = StringBuilder()
        sb.appendLine("*FORM CHECKLIST PJU — PERUMAHAN BUMI PAPAN SELARAS*")
        sb.appendLine("Bulan   : ${sheet.monthYear} (Tgl ${sheet.periodStartDay}–${sheet.periodEndDay})")
        sb.appendLine("Status  : ${compliance.badgeText} — ${compliance.detailText}")
        sb.appendLine("Rekap   : ✓ Nyala ${counts[PjuLampStatus.BAIK] ?: 0} | ! Redup ${counts[PjuLampStatus.REDUP] ?: 0} | ✕ Mati ${counts[PjuLampStatus.MATI] ?: 0}")
        sb.appendLine("====================================")
        val checkedDays = sheet.checkedDays()
        sheet.clusters.forEachIndexed { idx, row ->
            val health = sheet.clusterHealthPercent(row)?.let { "$it% Baik" } ?: "Belum dicek"
            val problemDays = checkedDays.filter { day ->
                val st = row.daySummaryStatus(day)
                st == PjuLampStatus.MATI || st == PjuLampStatus.REDUP
            }
            val issueDaysNote = if (problemDays.isEmpty()) "" else " | Kendala Tgl: ${problemDays.joinToString(",")}"
            sb.appendLine("${idx + 1}. ${row.clusterName} ($health)$issueDaysNote")
        }
        if (sheet.issues.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("*PERMASALAHAN PJU (${sheet.issues.size}):*")
            sheet.issues.forEachIndexed { idx, item ->
                val tl = if (item.tindakLanjut.isNotBlank()) " -> ${item.tindakLanjut}" else ""
                sb.appendLine("${idx + 1}. [${item.tanggal}] ${item.cluster} (${item.titikLampu}): ${item.keluhan} [${item.status}]$tl")
            }
        }
        if (sheet.notes.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("*CATATAN:* ${sheet.notes}")
        }
        sb.appendLine("====================================")
        sb.appendLine("Penanggung Jawab : ${sheet.responsibleNames}")
        sb.append("Dilaporkan oleh  : ${profile.name} (${profile.nrp})")
        return sb.toString()
    }

    private companion object {
        const val TAG = "SecurityVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val HEARTBEAT_INTERVAL_MS = 25_000L
    }
}

fun currentFormattedTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

fun currentIndonesianDate(): String =
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
