package com.example.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.data.model.NoteSourceTab
import com.example.data.model.QuickNote
import com.example.data.model.ReportCategory
import com.example.data.model.ReportStatus
import com.example.data.model.SecurityReport
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

/**
 * Play-compliant persistent foreground service (`remoteMessaging`) that listens to
 * incoming team chat messages and reports even after the app UI is closed, triggering
 * high-priority OS heads-up banners and Android OS Conversation Bubbles.
 */
class BackgroundAlertService : Service() {

    private var messagesListener: ListenerRegistration? = null
    private var reportsListener: ListenerRegistration? = null
    private var teamNotesListener: ListenerRegistration? = null
    private var attachedRoomCode: String? = null
    private var attachedUserId: String? = null
    private var activeOfficerName: String = "Petugas Security"

    private var hasSeededMessages = false
    private var hasSeededReports = false
    private var hasSeededTeamNotes = false
    private val knownMessageIds = HashSet<String>()
    private val knownReportIds = HashSet<String>()
    private val knownTeamNoteIds = HashSet<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val prefs = NotificationPreferences(applicationContext)
        val settings = prefs.settings.value
        if (!settings.enabled || !settings.backgroundMonitorEnabled ||
            !SecurityNotificationManager.hasNotificationPermission(applicationContext)
        ) {
            stopMonitoring()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            stopMonitoring()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val roomCode = intent?.getStringExtra(EXTRA_ROOM_CODE)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getSavedRoomCode().ifBlank { DEFAULT_ROOM_CODE }
        activeOfficerName = intent?.getStringExtra(EXTRA_OFFICER_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getSavedOfficerName()
        prefs.saveActiveContext(roomCode, activeOfficerName)

        val foregroundNotif = SecurityNotificationManager.buildMonitorServiceNotification(
            context = applicationContext,
            roomCode = roomCode
        )
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            SecurityNotificationManager.MONITOR_NOTIFICATION_ID,
            foregroundNotif,
            serviceType
        )

        attachListenersIfNeeded(roomCode = roomCode, uid = uid)
        return START_STICKY
    }

    private fun attachListenersIfNeeded(roomCode: String, uid: String) {
        if (attachedRoomCode == roomCode && attachedUserId == uid &&
            messagesListener != null && reportsListener != null && teamNotesListener != null
        ) {
            return
        }
        stopMonitoring()
        attachedRoomCode = roomCode
        attachedUserId = uid
        hasSeededMessages = false
        hasSeededReports = false
        hasSeededTeamNotes = false
        knownMessageIds.clear()
        knownReportIds.clear()
        knownTeamNoteIds.clear()

        val dbId = getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        val roomRef = db.collection("chat_rooms").document(roomCode)

        messagesListener = roomRef.collection("messages")
            .whereEqualTo("roomCode", roomCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (!hasSeededMessages) {
                    hasSeededMessages = true
                    snapshot.documents.forEach { doc -> knownMessageIds.add(doc.id) }
                    return@addSnapshotListener
                }
                if (NotificationRuntimeState.isAppUiForeground) {
                    snapshot.documents.forEach { doc -> knownMessageIds.add(doc.id) }
                    return@addSnapshotListener
                }

                snapshot.documentChanges.forEach { change ->
                    if (change.type != DocumentChange.Type.ADDED) return@forEach
                    val doc = change.document
                    if (!knownMessageIds.add(doc.id)) return@forEach
                    val msg = doc.toObject(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (msg.userId == uid && msg.fromOfficer) return@forEach

                    val eventId = msg.id.ifBlank { doc.id }
                    if (!NotificationRuntimeState.shouldDispatchAlert(eventId)) return@forEach

                    val prefs = NotificationPreferences(applicationContext)
                    val currentSettings = prefs.settings.value
                    if (!currentSettings.enabled || !currentSettings.backgroundMonitorEnabled) return@forEach

                    val preview = when {
                        msg.text.isNotBlank() -> msg.text
                        !msg.imageData.isNullOrBlank() -> "📷 Foto ${msg.imageCaption.orEmpty()}".trim()
                        msg.audioType != "none" -> "🎤 Voice Note (${msg.audioDuration ?: "00:05"})"
                        else -> "Pesan koordinasi baru"
                    }
                    val upper = msg.text.uppercase(Locale.getDefault())
                    val isUrgent = msg.text.contains("🚨") ||
                        msg.text.contains("⚠️") ||
                        upper.contains("PEMBERITAHUAN") ||
                        upper.contains("PENGUMUMAN") ||
                        upper.contains("DARURAT")

                    val event = IncomingAlertEvent(
                        id = eventId,
                        kind = if (isUrgent) AlertKind.ANNOUNCEMENT else AlertKind.CHAT,
                        title = if (isUrgent) "Pemberitahuan • $roomCode" else "Chat Masuk • $roomCode",
                        senderName = msg.senderName.ifBlank { "Petugas $roomCode" },
                        senderRole = msg.senderRole,
                        senderPhoto = msg.senderPhoto,
                        roomCode = roomCode,
                        messagePreview = preview,
                        timeLabel = "${msg.time} WIB",
                        isUrgent = isUrgent
                    )
                    SecurityNotificationManager.playRingtoneAndVibrate(
                        context = applicationContext,
                        settings = currentSettings,
                        isUrgent = isUrgent
                    )
                    SecurityNotificationManager.showIncomingChatNotification(
                        context = applicationContext,
                        event = event,
                        settings = currentSettings,
                        officerName = activeOfficerName
                    )
                }
            }

        reportsListener = roomRef.collection("reports")
            .whereEqualTo("roomCode", roomCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (!hasSeededReports) {
                    hasSeededReports = true
                    snapshot.documents.forEach { doc -> knownReportIds.add(doc.id) }
                    return@addSnapshotListener
                }
                if (NotificationRuntimeState.isAppUiForeground) {
                    snapshot.documents.forEach { doc -> knownReportIds.add(doc.id) }
                    return@addSnapshotListener
                }

                snapshot.documentChanges.forEach { change ->
                    if (change.type != DocumentChange.Type.ADDED) return@forEach
                    val doc = change.document
                    if (!knownReportIds.add(doc.id)) return@forEach
                    val rep = doc.toObject(
                        SecurityReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (rep.userId == uid) return@forEach

                    val eventId = "rep_${rep.id.ifBlank { doc.id }}"
                    if (!NotificationRuntimeState.shouldDispatchAlert(eventId)) return@forEach

                    val prefs = NotificationPreferences(applicationContext)
                    val currentSettings = prefs.settings.value
                    if (!currentSettings.enabled || !currentSettings.backgroundMonitorEnabled) return@forEach

                    val cat = ReportCategory.fromNumber(rep.category.toInt())
                    val isUrgent = rep.statusEnum == ReportStatus.TINDAK_LANJUT
                    val event = IncomingAlertEvent(
                        id = eventId,
                        kind = AlertKind.ANNOUNCEMENT,
                        title = "Laporan Masuk • $roomCode",
                        senderName = rep.officerName.ifBlank { "Petugas $roomCode" },
                        senderRole = "${cat.shortTitle} • ${rep.shift}",
                        roomCode = roomCode,
                        messagePreview = "[${rep.time} WIB • ${rep.status}] ${rep.description}",
                        timeLabel = "${rep.time} WIB",
                        isUrgent = isUrgent
                    )
                    SecurityNotificationManager.playRingtoneAndVibrate(
                        context = applicationContext,
                        settings = currentSettings,
                        isUrgent = isUrgent
                    )
                    SecurityNotificationManager.showAnnouncementNotification(
                        context = applicationContext,
                        event = event,
                        settings = currentSettings
                    )
                }
            }

        teamNotesListener = roomRef.collection("quick_notes")
            .whereEqualTo("roomCode", roomCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (!hasSeededTeamNotes) {
                    hasSeededTeamNotes = true
                    snapshot.documents.forEach { doc -> knownTeamNoteIds.add(doc.id) }
                    return@addSnapshotListener
                }
                if (NotificationRuntimeState.isAppUiForeground) {
                    snapshot.documents.forEach { doc -> knownTeamNoteIds.add(doc.id) }
                    return@addSnapshotListener
                }

                snapshot.documentChanges.forEach { change ->
                    if (change.type != DocumentChange.Type.ADDED) return@forEach
                    val doc = change.document
                    if (!knownTeamNoteIds.add(doc.id)) return@forEach
                    val note = doc.toObject(
                        QuickNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (note.userId == uid) return@forEach
                    if (NoteSourceTab.fromNameOrDefault(note.sourceTab) != NoteSourceTab.CATATAN) return@forEach

                    val eventId = "note_${note.id.ifBlank { doc.id }}"
                    if (!NotificationRuntimeState.shouldDispatchAlert(eventId)) return@forEach

                    val prefs = NotificationPreferences(applicationContext)
                    val currentSettings = prefs.settings.value
                    if (!currentSettings.enabled || !currentSettings.backgroundMonitorEnabled) return@forEach

                    val event = IncomingAlertEvent(
                        id = eventId,
                        kind = AlertKind.ANNOUNCEMENT,
                        title = "Catatan & Instruksi Atasan • $roomCode",
                        senderName = note.officerName.ifBlank { "Atasan / Petugas $roomCode" },
                        senderRole = "${note.title} • ${note.shift}",
                        roomCode = roomCode,
                        messagePreview = note.content,
                        timeLabel = "${note.noteTime.ifBlank { "--:--" }} WIB",
                        isUrgent = true
                    )
                    SecurityNotificationManager.playRingtoneAndVibrate(
                        context = applicationContext,
                        settings = currentSettings,
                        isUrgent = true
                    )
                    SecurityNotificationManager.showAnnouncementNotification(
                        context = applicationContext,
                        event = event,
                        settings = currentSettings
                    )
                }
            }
    }

    private fun stopMonitoring() {
        messagesListener?.remove()
        messagesListener = null
        reportsListener?.remove()
        reportsListener = null
        teamNotesListener?.remove()
        teamNotesListener = null
        attachedRoomCode = null
        attachedUserId = null
    }

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.example.notifications.ACTION_START_MONITOR"
        private const val ACTION_STOP = "com.example.notifications.ACTION_STOP_MONITOR"
        private const val EXTRA_ROOM_CODE = "extra_room_code"
        private const val EXTRA_OFFICER_NAME = "extra_officer_name"

        fun start(context: Context, roomCode: String, officerName: String) {
            if (!SecurityNotificationManager.hasNotificationPermission(context)) return
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, BackgroundAlertService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ROOM_CODE, roomCode)
                putExtra(EXTRA_OFFICER_NAME, officerName)
            }
            runCatching {
                ContextCompat.startForegroundService(appCtx, intent)
            }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, BackgroundAlertService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching {
                appCtx.stopService(intent)
            }
        }
    }
}
