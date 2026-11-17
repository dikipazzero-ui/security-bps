package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NetworkMonitor
import com.example.data.firebase.SecurityRepository
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.data.model.OfficerProfile
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.data.model.RoomMember
import com.example.data.model.SecurityReport
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.UiState
import com.example.data.model.currentIsoDate
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

    private var hasSyncedInitialProfile = false

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
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
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
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
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

    fun saveOfficerProfile(name: String, nrp: String, role: String) {
        val currentShift = (profileState.value as? UiState.Success)?.data?.activeShift
            ?: _formState.value.shiftInput
        viewModelScope.launch {
            val result = repository.saveProfile(name, nrp, role, currentShift, _activeRoomCode.value)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Identitas petugas berhasil diperbarui.")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan profil.")
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
                imageCaption = photoCaption
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
                audioDuration = duration
            )
            _isSendingChat.value = false
            if (result.isSuccess) {
                _toastEvents.tryEmit("Voice Note berhasil dikirim ke ruang $roomCode!")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengirim Voice Note.")
            }
        }
    }

    fun simulateTeamReply() {
        viewModelScope.launch {
            val result = repository.simulateTeamReply(_activeRoomCode.value)
            if (result.isSuccess) {
                if (_activeTab.value != MainTab.CHAT) {
                    _unreadChatCount.update { it + 1 }
                }
            } else {
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
