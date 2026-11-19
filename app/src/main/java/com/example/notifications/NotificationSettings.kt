package com.example.notifications

import android.content.Context
import android.media.RingtoneManager
import com.example.data.model.DEFAULT_ROOM_CODE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RingtonePreset(val label: String, val type: Int) {
    NOTIFICATION("Default Notifikasi", RingtoneManager.TYPE_NOTIFICATION),
    RINGTONE("Nada Dering Panggilan", RingtoneManager.TYPE_RINGTONE),
    ALARM("Nada Alarm Siaga", RingtoneManager.TYPE_ALARM)
}

enum class AlertKind {
    CHAT,
    ANNOUNCEMENT
}

data class NotificationSettings(
    val enabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val floatingBubbleEnabled: Boolean = true,
    val backgroundMonitorEnabled: Boolean = true,
    val systemBannerWhenActive: Boolean = false,
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "Default Notifikasi"
)

data class IncomingAlertEvent(
    val id: String,
    val kind: AlertKind,
    val title: String,
    val senderName: String,
    val senderRole: String,
    val senderPhoto: String? = null,
    val roomCode: String = "",
    val messagePreview: String,
    val timeLabel: String,
    val isUrgent: Boolean = false
)

/**
 * Process-level runtime coordination between foreground Compose UI and BackgroundAlertService
 * so incoming chat/report alerts never fire twice across UI & service listeners.
 */
object NotificationRuntimeState {
    @Volatile
    var isAppUiForeground: Boolean = false

    private val seenAlertIds = LinkedHashSet<String>()
    private const val MAX_SEEN_IDS = 200

    @Synchronized
    fun shouldDispatchAlert(id: String): Boolean {
        if (id.isBlank()) return true
        if (seenAlertIds.contains(id)) return false
        seenAlertIds.add(id)
        if (seenAlertIds.size > MAX_SEEN_IDS) {
            val iterator = seenAlertIds.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        return true
    }

    @Synchronized
    fun clearSeenForTesting() {
        seenAlertIds.clear()
    }
}

class NotificationPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    private fun readSettings(): NotificationSettings {
        return NotificationSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            floatingBubbleEnabled = prefs.getBoolean(KEY_FLOATING_BUBBLE, true),
            backgroundMonitorEnabled = prefs.getBoolean(KEY_BG_MONITOR, true),
            systemBannerWhenActive = prefs.getBoolean(KEY_SYSTEM_IN_FG, false),
            ringtoneUri = prefs.getString(KEY_RINGTONE_URI, "").orEmpty(),
            ringtoneTitle = prefs.getString(KEY_RINGTONE_TITLE, "Default Notifikasi").orEmpty()
                .ifBlank { "Default Notifikasi" }
        )
    }

    private fun update(transform: (NotificationSettings) -> NotificationSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putBoolean(KEY_ENABLED, next.enabled)
            .putBoolean(KEY_SOUND, next.soundEnabled)
            .putBoolean(KEY_VIBRATION, next.vibrationEnabled)
            .putBoolean(KEY_FLOATING_BUBBLE, next.floatingBubbleEnabled)
            .putBoolean(KEY_BG_MONITOR, next.backgroundMonitorEnabled)
            .putBoolean(KEY_SYSTEM_IN_FG, next.systemBannerWhenActive)
            .putString(KEY_RINGTONE_URI, next.ringtoneUri)
            .putString(KEY_RINGTONE_TITLE, next.ringtoneTitle)
            .apply()
        _settings.value = next
    }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }
    fun setSoundEnabled(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setVibrationEnabled(enabled: Boolean) = update { it.copy(vibrationEnabled = enabled) }
    fun setFloatingBubbleEnabled(enabled: Boolean) = update { it.copy(floatingBubbleEnabled = enabled) }
    fun setBackgroundMonitorEnabled(enabled: Boolean) = update { it.copy(backgroundMonitorEnabled = enabled) }
    fun setSystemBannerWhenActive(enabled: Boolean) = update { it.copy(systemBannerWhenActive = enabled) }
    fun setRingtone(uri: String, title: String) = update {
        it.copy(
            ringtoneUri = uri,
            ringtoneTitle = title.ifBlank { "Nada Kustom Perangkat" }
        )
    }

    fun saveActiveContext(roomCode: String, officerName: String) {
        val cleanRoom = roomCode.trim().ifBlank { DEFAULT_ROOM_CODE }
        val cleanOfficer = officerName.trim().ifBlank { "Petugas Security" }
        prefs.edit()
            .putString(KEY_SAVED_ROOM_CODE, cleanRoom)
            .putString(KEY_SAVED_OFFICER_NAME, cleanOfficer)
            .apply()
    }

    fun getSavedRoomCode(): String =
        prefs.getString(KEY_SAVED_ROOM_CODE, DEFAULT_ROOM_CODE).orEmpty().ifBlank { DEFAULT_ROOM_CODE }

    fun getSavedOfficerName(): String =
        prefs.getString(KEY_SAVED_OFFICER_NAME, "Petugas Security").orEmpty().ifBlank { "Petugas Security" }

    private companion object {
        const val PREFS_NAME = "security_bps_notification_prefs"
        const val KEY_ENABLED = "notif_enabled"
        const val KEY_SOUND = "notif_sound"
        const val KEY_VIBRATION = "notif_vibration"
        const val KEY_FLOATING_BUBBLE = "notif_floating_bubble"
        const val KEY_BG_MONITOR = "notif_background_monitor"
        const val KEY_SYSTEM_IN_FG = "notif_system_in_fg"
        const val KEY_RINGTONE_URI = "notif_ringtone_uri"
        const val KEY_RINGTONE_TITLE = "notif_ringtone_title"
        const val KEY_SAVED_ROOM_CODE = "notif_saved_room_code"
        const val KEY_SAVED_OFFICER_NAME = "notif_saved_officer_name"
    }
}
