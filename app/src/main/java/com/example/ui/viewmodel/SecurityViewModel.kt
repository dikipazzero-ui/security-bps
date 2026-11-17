package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.SecurityRepository
import com.example.data.model.ChatMessage
import com.example.data.model.OfficerProfile
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.data.model.SecurityReport
import com.example.data.model.ShiftOption
import com.example.data.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainTab {
    LAPORAN,
    CHAT
}

data class ReportFormState(
    val selectedCategory: Int = 1,
    val timeInput: String = currentFormattedTime(),
    val shiftInput: String = ShiftOption.SHIFT_1.label,
    val statusInput: ReportStatus = ReportStatus.AMAN,
    val descriptionInput: String = "",
    val photoSlots: List<String?> = listOf(null, null, null, null),
    val isSubmitting: Boolean = false
)

class SecurityViewModel(
    private val repository: SecurityRepository,
    private val currentUserId: String,
    private val initialDisplayName: String?
) : ViewModel() {

    private val _activeTab = MutableStateFlow(MainTab.LAPORAN)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _unreadChatCount = MutableStateFlow(2)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    private val _formState = MutableStateFlow(ReportFormState())
    val formState: StateFlow<ReportFormState> = _formState.asStateFlow()

    private val _chatTextInput = MutableStateFlow("")
    val chatTextInput: StateFlow<String> = _chatTextInput.asStateFlow()

    private val _pendingChatPhoto = MutableStateFlow<String?>(null)
    val pendingChatPhoto: StateFlow<String?> = _pendingChatPhoto.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var hasSyncedInitialShift = false

    val profileState: StateFlow<UiState<OfficerProfile>> = repository.observeProfile(currentUserId)
        .map<OfficerProfile?, UiState<OfficerProfile>> { profile ->
            val resolved = profile ?: OfficerProfile(
                userId = currentUserId,
                name = initialDisplayName?.ifBlank { null } ?: "Hendra Wijaya"
            )
            UiState.Success(resolved)
        }
        .onEach { state ->
            if (state is UiState.Success && !hasSyncedInitialShift) {
                hasSyncedInitialShift = true
                _formState.update { it.copy(shiftInput = state.data.activeShift) }
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

    val chatState: StateFlow<UiState<List<ChatMessage>>> = repository.observeChatMessages(currentUserId)
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
            repository.ensureInitialSetup(currentUserId, initialDisplayName)
        }
    }

    fun switchTab(tab: MainTab) {
        _activeTab.value = tab
        if (tab == MainTab.CHAT) {
            _unreadChatCount.value = 0
        }
    }

    fun selectCategory(categoryNumber: Int) {
        _formState.update { it.copy(selectedCategory = categoryNumber.coerceIn(1, 3)) }
    }

    fun updateTimeInput(time: String) {
        _formState.update { it.copy(timeInput = time) }
    }

    fun setCurrentTimeNow() {
        _formState.update { it.copy(timeInput = currentFormattedTime()) }
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
        _toastEvents.tryEmit("Foto slot ${slotIndex + 1} berhasil ditambahkan.")
    }

    fun removePhotoAtSlot(slotIndex: Int) {
        if (slotIndex !in 0..3) return
        _formState.update { state ->
            val updated = state.photoSlots.toMutableList()
            updated[slotIndex] = null
            state.copy(photoSlots = updated)
        }
    }

    fun setGlobalShift(shift: String) {
        _formState.update { it.copy(shiftInput = shift) }
        viewModelScope.launch {
            val result = repository.updateActiveShift(shift)
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
            val result = repository.saveProfile(name, nrp, role, currentShift)
            if (result.isSuccess) {
                _toastEvents.tryEmit("Identitas petugas berhasil diperbarui.")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan profil.")
            }
        }
    }

    fun submitReportItem() {
        val form = _formState.value
        val desc = form.descriptionInput.trim()
        if (desc.isEmpty()) {
            _toastEvents.tryEmit("Mohon isi rincian kegiatan terlebih dahulu.")
            return
        }
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        val validPhotos = form.photoSlots.filterNotNull()

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            val result = repository.addReport(
                category = form.selectedCategory,
                time = form.timeInput.ifBlank { currentFormattedTime() },
                shift = form.shiftInput,
                status = form.statusInput.label,
                description = desc,
                photos = validPhotos,
                officerName = profile.name,
                officerNrp = profile.nrp
            )
            if (result.isSuccess) {
                _formState.update {
                    it.copy(
                        descriptionInput = "",
                        photoSlots = listOf(null, null, null, null),
                        timeInput = currentFormattedTime(),
                        isSubmitting = false
                    )
                }
                _toastEvents.tryEmit("Rincian berhasil ditambahkan ke Kategori ${form.selectedCategory}!")
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

    fun updateChatTextInput(text: String) {
        _chatTextInput.value = text
    }

    fun setPendingChatPhoto(base64: String?) {
        _pendingChatPhoto.value = base64
    }

    fun sendTextOrPhotoChat() {
        val text = _chatTextInput.value.trim()
        val photo = _pendingChatPhoto.value
        if (text.isEmpty() && photo == null) return

        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        viewModelScope.launch {
            _isSendingChat.value = true
            val result = repository.sendChatMessage(
                senderName = profile.name,
                senderRole = profile.role,
                fromOfficer = true,
                text = text,
                imageData = photo
            )
            _isSendingChat.value = false
            if (result.isSuccess) {
                _chatTextInput.value = ""
                _pendingChatPhoto.value = null
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengirim pesan.")
            }
        }
    }

    fun sendVoiceNote(audioBase64: String, duration: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: OfficerProfile(userId = currentUserId)
        viewModelScope.launch {
            _isSendingChat.value = true
            val result = repository.sendChatMessage(
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
                _toastEvents.tryEmit("Voice Note berhasil dikirim ke ruang koordinasi!")
            } else {
                _toastEvents.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengirim Voice Note.")
            }
        }
    }

    fun simulateTeamReply() {
        viewModelScope.launch {
            val result = repository.simulateTeamReply()
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
                    sb.appendLine("   ${idx + 1}) [${item.time} WIB | ${item.shift}] (${item.status})")
                    sb.appendLine("      ${item.description}")
                    if (item.photos.isNotEmpty()) {
                        sb.appendLine("      Lampiran: ${item.photos.size} Foto Dokumentasi")
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
    }
}

fun currentFormattedTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

fun currentIndonesianDate(): String =
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
