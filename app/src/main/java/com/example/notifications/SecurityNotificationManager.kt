package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

object SecurityNotificationManager {

    const val CHANNEL_CHAT_ID = "security_bps_chat_v2"
    const val CHANNEL_ALERT_ID = "security_bps_alert_v2"
    const val CHANNEL_MONITOR_SERVICE_ID = "security_bps_monitor_v1"
    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val EXTRA_ROOM_CODE = "extra_room_code"
    const val EXTRA_SENDER_NAME = "extra_sender_name"

    private const val CHAT_NOTIFICATION_ID = 4101
    private const val ALERT_NOTIFICATION_ID = 4102
    const val MONITOR_NOTIFICATION_ID = 4100

    private const val ACCENT_BLUE_INT = 0xFF0284C7.toInt()
    private val vibrationPattern = longArrayOf(0L, 210L, 110L, 260L)
    private val urgentVibrationPattern = longArrayOf(0L, 280L, 100L, 280L, 100L, 340L)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeRingtone: Ringtone? = null
    private var stopRingtoneRunnable: Runnable? = null

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun resolveRingtoneUri(settings: NotificationSettings): Uri {
        val raw = settings.ringtoneUri.trim()
        if (raw.isNotEmpty()) {
            runCatching { Uri.parse(raw) }.getOrNull()?.let { return it }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    fun ensureChannels(context: Context, settings: NotificationSettings = NotificationSettings()) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = resolveRingtoneUri(settings)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (manager.getNotificationChannel(CHANNEL_CHAT_ID) == null) {
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_ID,
                "Live Chat & Gelembung Koordinasi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi chat mengambang lengkap dengan nada dering, getar, dan gelembung OS"
                enableLights(true)
                lightColor = ACCENT_BLUE_INT
                enableVibration(true)
                vibrationPattern = this@SecurityNotificationManager.vibrationPattern
                setSound(soundUri, audioAttr)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            manager.createNotificationChannel(chatChannel)
        }

        if (manager.getNotificationChannel(CHANNEL_ALERT_ID) == null) {
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Pemberitahuan & Info Security",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan penting, status tim, dan pembaruan laporan Plot BPS"
                enableLights(true)
                lightColor = ACCENT_BLUE_INT
                enableVibration(true)
                vibrationPattern = urgentVibrationPattern
                setSound(soundUri, audioAttr)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(alertChannel)
        }

        if (manager.getNotificationChannel(CHANNEL_MONITOR_SERVICE_ID) == null) {
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_SERVICE_ID,
                "Siaga Latar Belakang Security BPS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga pemantauan pesan & laporan Plot BPS aktif meski aplikasi ditutup"
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
            manager.createNotificationChannel(monitorChannel)
        }
    }

    fun buildMonitorServiceNotification(context: Context, roomCode: String): Notification {
        ensureChannels(context)
        val appContext = context.applicationContext
        val cleanRoom = roomCode.ifBlank { "PLOT-BPS" }
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, "CHAT")
            putExtra(EXTRA_ROOM_CODE, cleanRoom)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            MONITOR_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(appContext, CHANNEL_MONITOR_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_stat_security_notif)
            .setColor(ACCENT_BLUE_INT)
            .setContentTitle(appContext.getString(R.string.bg_monitor_notif_title))
            .setContentText(appContext.getString(R.string.bg_monitor_notif_body, cleanRoom))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun playRingtoneAndVibrate(
        context: Context,
        settings: NotificationSettings,
        isUrgent: Boolean = false
    ) {
        if (!settings.enabled) return
        if (settings.soundEnabled) {
            playRingtone(context.applicationContext, resolveRingtoneUri(settings))
        }
        if (settings.vibrationEnabled) {
            triggerVibration(context.applicationContext, isUrgent)
        }
    }

    private fun playRingtone(context: Context, uri: Uri) {
        mainHandler.post {
            runCatching {
                stopRingtoneRunnable?.let { mainHandler.removeCallbacks(it) }
                activeRingtone?.let { prev ->
                    if (prev.isPlaying) prev.stop()
                }
                val ringtone = RingtoneManager.getRingtone(context, uri) ?: return@runCatching
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.play()
                activeRingtone = ringtone
                val stopTask = Runnable {
                    runCatching {
                        if (ringtone.isPlaying) ringtone.stop()
                    }
                }
                stopRingtoneRunnable = stopTask
                // Cap playback at 2.6 seconds so phone/alarm tones do not loop indefinitely
                mainHandler.postDelayed(stopTask, 2600L)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerVibration(context: Context, isUrgent: Boolean) {
        val pattern = if (isUrgent) urgentVibrationPattern else vibrationPattern
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vm?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        vibrator.vibrate(pattern, -1)
                    }
                }
            }
        }
    }

    fun showIncomingChatNotification(
        context: Context,
        event: IncomingAlertEvent,
        settings: NotificationSettings,
        officerName: String
    ) {
        if (!settings.enabled || !hasNotificationPermission(context)) return
        ensureChannels(context, settings)

        val appContext = context.applicationContext
        val roomCode = event.roomCode.ifBlank { "PLOT-BPS" }
        val shortcutId = "security_bps_chat_$roomCode"
        val locusId = LocusIdCompat(shortcutId)

        val avatarIcon = createAvatarIconCompat(appContext, event.senderPhoto)
            ?: IconCompat.createWithResource(appContext, R.mipmap.ic_launcher_round)
        val senderPerson = Person.Builder()
            .setName(event.senderName)
            .setKey("${roomCode}_${event.senderName}")
            .setImportant(true)
            .setIcon(avatarIcon)
            .build()
        val mePerson = Person.Builder()
            .setName(officerName.ifBlank { "Petugas Security" })
            .setKey("officer_self")
            .build()

        val mainChatIntent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, "CHAT")
            putExtra(EXTRA_ROOM_CODE, roomCode)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            appContext,
            CHAT_NOTIFICATION_ID,
            mainChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortcut = ShortcutInfoCompat.Builder(appContext, shortcutId)
            .setShortLabel("Chat $roomCode")
            .setLongLabel("Live Chat ${event.senderName} ($roomCode)")
            .setIntent(mainChatIntent)
            .setLongLived(true)
            .setLocusId(locusId)
            .setCategories(setOf("android.shortcut.conversation"))
            .setPerson(senderPerson)
            .setIcon(avatarIcon)
            .build()
        runCatching {
            ShortcutManagerCompat.pushDynamicShortcut(appContext, shortcut)
        }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_CHAT_ID)
            .setSmallIcon(R.drawable.ic_stat_security_notif)
            .setColor(ACCENT_BLUE_INT)
            .setContentTitle("${event.senderName} • $roomCode")
            .setContentText(event.messagePreview)
            .setStyle(
                NotificationCompat.MessagingStyle(mePerson)
                    .setConversationTitle("Live Chat $roomCode")
                    .setGroupConversation(true)
                    .addMessage(event.messagePreview, System.currentTimeMillis(), senderPerson)
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShortcutId(shortcutId)
            .setLocusId(locusId)
            .setShortcutInfo(shortcut)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .addAction(
                R.drawable.ic_stat_security_notif,
                "Buka Live Chat",
                mainPendingIntent
            )

        if (settings.floatingBubbleEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bubbleIntent = Intent(appContext, BubbleChatActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_ROOM_CODE, roomCode)
                putExtra(EXTRA_SENDER_NAME, event.senderName)
            }
            val bubbleFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val bubblePendingIntent = PendingIntent.getActivity(
                appContext,
                CHAT_NOTIFICATION_ID + 50,
                bubbleIntent,
                bubbleFlags
            )
            val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(bubblePendingIntent, avatarIcon)
                .setDesiredHeight(560)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()
            builder.setBubbleMetadata(bubbleMetadata)
        }

        if (settings.soundEnabled) {
            builder.setSound(resolveRingtoneUri(settings))
        } else {
            builder.setSilent(true)
        }
        if (settings.vibrationEnabled) {
            builder.setVibrate(if (event.isUrgent) urgentVibrationPattern else vibrationPattern)
        }

        runCatching {
            NotificationManagerCompat.from(appContext).notify(CHAT_NOTIFICATION_ID, builder.build())
        }
    }

    fun showAnnouncementNotification(
        context: Context,
        event: IncomingAlertEvent,
        settings: NotificationSettings
    ) {
        if (!settings.enabled || !hasNotificationPermission(context)) return
        ensureChannels(context, settings)

        val appContext = context.applicationContext
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, if (event.roomCode.isNotBlank()) "CHAT" else "LAPORAN")
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            ALERT_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_stat_security_notif)
            .setColor(ACCENT_BLUE_INT)
            .setContentTitle(event.title)
            .setContentText(event.messagePreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${event.senderName}: ${event.messagePreview}"))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (settings.soundEnabled) {
            builder.setSound(resolveRingtoneUri(settings))
        } else {
            builder.setSilent(true)
        }
        if (settings.vibrationEnabled) {
            builder.setVibrate(urgentVibrationPattern)
        }

        runCatching {
            NotificationManagerCompat.from(appContext).notify(ALERT_NOTIFICATION_ID, builder.build())
        }
    }

    fun openSystemBubbleSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_CHAT_ID)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    private fun createAvatarIconCompat(context: Context, base64Photo: String?): IconCompat? {
        if (base64Photo.isNullOrBlank()) return null
        return runCatching {
            val clean = base64Photo.substringAfter("base64,", base64Photo)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val size = 96
            val scaled = Bitmap.createScaledBitmap(raw, size, size, true)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = Rect(0, 0, size, size)
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, rect, rect, paint)
            if (raw != scaled) raw.recycle()
            scaled.recycle()
            IconCompat.createWithBitmap(output)
        }.getOrNull()
    }
}
