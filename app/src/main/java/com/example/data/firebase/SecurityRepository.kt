package com.example.data.firebase

import android.content.Context
import com.example.R
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
import com.example.data.model.NoteSourceTab
import com.example.data.model.OfficerProfile
import com.example.data.model.OfficerRoles
import com.example.data.model.PersonnelRow
import com.example.data.model.PjuChecklistSheet
import com.example.data.model.PjuClusterRow
import com.example.data.model.PjuIssueEntry
import com.example.data.model.PjuTemplates
import com.example.data.model.QuickNote
import com.example.data.model.ReportStatus
import com.example.data.model.RoomMember
import com.example.data.model.SecurityReport
import com.example.data.model.ShiftOption
import com.example.data.model.currentIsoDate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SecurityRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "Petugas harus masuk dengan akun Google terlebih dahulu." }
        return uid
    }

    fun normalizeRoomCode(raw: String): String {
        return raw.trim()
            .uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_\\-]"), "")
            .take(24)
    }

    fun isValidRoomCode(code: String): Boolean {
        return code.length in 3..24 && code.matches(Regex("^[A-Z0-9_\\-]+$"))
    }

    fun observeProfile(userId: String): Flow<OfficerProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        OfficerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeReports(roomCode: String): Flow<List<SecurityReport>> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/reports"
        return db.collection("chat_rooms").document(cleanCode).collection("reports")
            .whereEqualTo("roomCode", cleanCode)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SecurityReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeActivitySheets(roomCode: String): Flow<List<DailyActivitySheet>> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/activity_sheets"
        return db.collection("chat_rooms").document(cleanCode).collection("activity_sheets")
            .whereEqualTo("roomCode", cleanCode)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DailyActivitySheet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending {
                    it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAttendanceSheets(roomCode: String): Flow<List<AttendanceSheet>> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/attendance_sheets"
        return db.collection("chat_rooms").document(cleanCode).collection("attendance_sheets")
            .whereEqualTo("roomCode", cleanCode)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AttendanceSheet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending {
                    it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observePjuSheets(roomCode: String): Flow<List<PjuChecklistSheet>> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/pju_sheets"
        return db.collection("chat_rooms").document(cleanCode).collection("pju_sheets")
            .whereEqualTo("roomCode", cleanCode)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PjuChecklistSheet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending {
                    it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeQuickNotes(roomCode: String): Flow<List<QuickNote>> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/quick_notes"
        return db.collection("chat_rooms").document(cleanCode).collection("quick_notes")
            .whereEqualTo("roomCode", cleanCode)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    QuickNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending {
                    it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeChatRoom(roomCode: String): Flow<ChatRoom?> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode"
        return db.collection("chat_rooms").document(cleanCode)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        ChatRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeRoomMembers(roomCode: String): Flow<List<RoomMember>> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/members"
        return db.collection("chat_rooms").document(cleanCode).collection("members")
            .whereEqualTo("roomCode", cleanCode)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RoomMember::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<RoomMember> { it.isCurrentlyOnline() }
                        .thenByDescending { it.lastSeenMillis() ?: 0L }
                        .thenBy { it.name.lowercase(Locale.getDefault()) }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeRoomMessages(roomCode: String): Flow<List<ChatMessage>> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/messages"
        return db.collection("chat_rooms").document(cleanCode).collection("messages")
            .whereEqualTo("roomCode", cleanCode)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun ensureInitialSetup(userId: String, googleDisplayName: String?): Result<String> {
        val profilePath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi petugas berubah; membatalkan inisialisasi." }
            val profileRef = db.collection("users").document(uid)
            val snapshot = profileRef.get().await()

            val defaultName = googleDisplayName?.trim()?.take(80)?.ifBlank { null } ?: "Hendra Wijaya"
            var resolvedRoom = DEFAULT_ROOM_CODE
            var resolvedProfile = OfficerProfile(
                userId = uid,
                name = defaultName,
                nrp = "SEC-BPS-042",
                role = OfficerRoles.options.first(),
                activeShift = ShiftOption.SHIFT_1.label,
                activeRoomCode = DEFAULT_ROOM_CODE,
                onlineStatus = "online"
            )

            if (!snapshot.exists()) {
                val profilePayload = mapOf(
                    "userId" to uid,
                    "name" to defaultName,
                    "nrp" to "SEC-BPS-042",
                    "role" to OfficerRoles.options.first(),
                    "activeShift" to ShiftOption.SHIFT_1.label,
                    "activeRoomCode" to DEFAULT_ROOM_CODE,
                    "onlineStatus" to "online",
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(profilePayload).await()
            } else {
                val existing = snapshot.toObject(
                    OfficerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    resolvedProfile = existing
                    resolvedRoom = existing.activeRoomCode.takeIf { isValidRoomCode(it) } ?: DEFAULT_ROOM_CODE
                }
            }

            // Ensure default shared PLOT-BPS room exists if user is in default room
            if (resolvedRoom == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            } else {
                val targetRoomSnap = db.collection("chat_rooms").document(resolvedRoom).get().await()
                if (!targetRoomSnap.exists()) {
                    resolvedRoom = DEFAULT_ROOM_CODE
                    ensureDefaultChatRoomExists(uid)
                }
            }

            // One-time best-effort migration of any earlier personal records into the shared room
            runCatching { migrateLegacyUserDataToRoom(uid, resolvedRoom) }

            // Ensure starter shared workspace templates exist in chat_rooms/{resolvedRoom} if empty
            ensureRoomStarterData(resolvedRoom, uid, resolvedProfile)

            // Join & mark online inside the active shared room
            upsertRoomMember(resolvedRoom, resolvedProfile, isOnline = true)
            Result.success(resolvedRoom)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profilePath)
            Result.failure(e)
        }
    }

    private suspend fun ensureRoomStarterData(
        roomCode: String,
        uid: String,
        profile: OfficerProfile
    ) {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val roomRef = db.collection("chat_rooms").document(cleanCode)
        val starterShift = profile.activeShift.takeIf { s ->
            s in ShiftOption.entries.map { it.label }
        } ?: ShiftOption.SHIFT_1.label
        val cleanName = profile.name.trim().ifBlank { "Petugas Security" }.take(80)
        val cleanNrp = profile.nrp.trim().ifBlank { "SEC-BPS" }.take(40)

        // 1. Starter shared report if room has none
        val reportsCol = roomRef.collection("reports")
        if (reportsCol.whereEqualTo("roomCode", cleanCode).limit(1).get().await().isEmpty) {
            val initReportRef = reportsCol.document("init_1")
            if (!initReportRef.get().await().exists()) {
                initReportRef.set(
                    mapOf(
                        "userId" to uid,
                        "roomCode" to cleanCode,
                        "category" to 1,
                        "time" to "07:05",
                        "reportDate" to currentIsoDate(),
                        "shift" to ShiftOption.SHIFT_1.label,
                        "status" to ReportStatus.AMAN.label,
                        "description" to "Apel serah terima jaga pagi di Pos Utama Plot BPS dari petugas Shift 3 ke Shift 1. Pengecekan inventaris HT (3 unit), kunci-kunci ruangan, dan buku mutasi lengkap dalam kondisi baik.",
                        "photos" to emptyList<String>(),
                        "photoCaptions" to emptyList<String>(),
                        "officerName" to cleanName,
                        "officerNrp" to cleanNrp,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        // 2. Starter shared DailyActivitySheet
        val sheetsCol = roomRef.collection("activity_sheets")
        if (sheetsCol.whereEqualTo("roomCode", cleanCode).limit(1).get().await().isEmpty) {
            val initSheetRef = sheetsCol.document("init_sheet_1")
            if (!initSheetRef.get().await().exists()) {
                val starterPayload = buildActivitySheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    docNumber = "001",
                    sheetDate = currentIsoDate(),
                    shift = starterShift,
                    personnelRows = ActivitySheetTemplates.starterPersonnelRows(cleanName),
                    inventoryRows = ActivitySheetTemplates.defaultInventoryRows,
                    activityRows = ActivitySheetTemplates.starterActivityRows(starterShift),
                    officerName = cleanName,
                    officerNrp = cleanNrp,
                    includeCreatedAt = true
                )
                initSheetRef.set(starterPayload).await()
            }
        }

        // 3. Starter shared 4-member-per-shift AttendanceSheet
        val attCol = roomRef.collection("attendance_sheets")
        if (attCol.whereEqualTo("roomCode", cleanCode).limit(1).get().await().isEmpty) {
            val initAttRef = attCol.document("init_att_1")
            if (!initAttRef.get().await().exists()) {
                val starterAttPayload = buildAttendanceSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    sheetDate = currentIsoDate(),
                    shift = starterShift,
                    periodLabel = "Regu Plot BPS (4 Orang/Shift)",
                    commanderName = cleanName,
                    rows = AttendanceTemplates.defaultShiftMembers(cleanName, starterShift),
                    includeCreatedAt = true
                )
                initAttRef.set(starterAttPayload).await()
            }
        }

        // 4. Starter shared PJU Checklist Sheet
        val pjuCol = roomRef.collection("pju_sheets")
        if (pjuCol.whereEqualTo("roomCode", cleanCode).limit(1).get().await().isEmpty) {
            val initPjuRef = pjuCol.document("init_pju_1")
            if (!initPjuRef.get().await().exists()) {
                val cal = Calendar.getInstance()
                val curMonth = cal.get(Calendar.MONTH) + 1
                val curYear = cal.get(Calendar.YEAR)
                val curDay = cal.get(Calendar.DAY_OF_MONTH)
                val (startDay, endDay) = PjuTemplates.defaultPeriodBounds(curMonth, curYear, curDay)
                val starterPjuPayload = buildPjuSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    monthYear = PjuTemplates.monthYearLabel(curMonth, curYear),
                    sheetMonth = curMonth.toLong(),
                    sheetYear = curYear.toLong(),
                    periodStartDay = startDay,
                    periodEndDay = endDay,
                    clusters = PjuTemplates.defaultClusterRows(),
                    issues = emptyList(),
                    notes = "",
                    responsibleNames = PjuTemplates.DEFAULT_RESPONSIBLE,
                    includeCreatedAt = true
                )
                initPjuRef.set(starterPjuPayload).await()
            }
        }
    }

    private suspend fun migrateLegacyUserDataToRoom(uid: String, roomCode: String) {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val userRef = db.collection("users").document(uid)
        val roomRef = db.collection("chat_rooms").document(cleanCode)
        val uidTag = uid.take(8)

        // 1. Migrate legacy reports (skip untouched init_1)
        val oldReports = userRef.collection("reports").get().await()
        oldReports.documents.forEach { doc ->
            if (doc.id == "init_1") return@forEach
            val targetId = "mig_${uidTag}_${doc.id}".take(120)
            val targetRef = roomRef.collection("reports").document(targetId)
            if (!targetRef.get().await().exists()) {
                val rep = doc.toObject(SecurityReport::class.java) ?: return@forEach
                val payload = mapOf(
                    "userId" to uid,
                    "roomCode" to cleanCode,
                    "category" to rep.category.toInt().coerceIn(1, 3),
                    "time" to rep.time.ifBlank { "07:00" }.take(10),
                    "reportDate" to (rep.reportDate.takeIf { it.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) } ?: currentIsoDate()),
                    "shift" to (rep.shift.takeIf { it in ShiftOption.entries.map { s -> s.label } } ?: ShiftOption.SHIFT_1.label),
                    "status" to (rep.status.takeIf { it in ReportStatus.entries.map { s -> s.label } } ?: ReportStatus.AMAN.label),
                    "description" to rep.description.ifBlank { "Laporan security" }.take(2000),
                    "photos" to rep.photos.take(4),
                    "photoCaptions" to rep.photoCaptions.take(4),
                    "officerName" to rep.officerName.ifBlank { "Petugas Security" }.take(80),
                    "officerNrp" to rep.officerNrp.ifBlank { "SEC-BPS" }.take(40),
                    "createdAt" to (rep.createdAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                targetRef.set(payload).await()
            }
        }

        // 2. Migrate legacy activity_sheets (skip untouched init_sheet_1 unless user customized activities > 2 rows)
        val oldSheets = userRef.collection("activity_sheets").get().await()
        oldSheets.documents.forEach { doc ->
            val sheet = doc.toObject(DailyActivitySheet::class.java) ?: return@forEach
            if (doc.id == "init_sheet_1" && sheet.activityRows.size <= 2) return@forEach
            val targetId = "mig_${uidTag}_${doc.id}".take(120)
            val targetRef = roomRef.collection("activity_sheets").document(targetId)
            if (!targetRef.get().await().exists()) {
                val payload = buildActivitySheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    docNumber = sheet.docNumber,
                    sheetDate = sheet.sheetDate,
                    shift = sheet.shift,
                    personnelRows = sheet.personnelRows,
                    inventoryRows = sheet.inventoryRows,
                    activityRows = sheet.activityRows,
                    officerName = sheet.officerName,
                    officerNrp = sheet.officerNrp,
                    includeCreatedAt = true
                )
                targetRef.set(payload).await()
            }
        }

        // 3. Migrate legacy attendance_sheets (skip untouched init_att_1 unless non-default remarks/non-hadir)
        val oldAtt = userRef.collection("attendance_sheets").get().await()
        oldAtt.documents.forEach { doc ->
            val sheet = doc.toObject(AttendanceSheet::class.java) ?: return@forEach
            val modified = sheet.rows.any { it.status != AttendanceStatus.HADIR.code || it.keterangan.isNotBlank() }
            if (doc.id == "init_att_1" && !modified) return@forEach
            val targetId = "mig_${uidTag}_${doc.id}".take(120)
            val targetRef = roomRef.collection("attendance_sheets").document(targetId)
            if (!targetRef.get().await().exists()) {
                val payload = buildAttendanceSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    sheetDate = sheet.sheetDate,
                    shift = sheet.shift,
                    periodLabel = sheet.periodLabel,
                    commanderName = sheet.commanderName,
                    rows = sheet.rows,
                    includeCreatedAt = true
                )
                targetRef.set(payload).await()
            }
        }

        // 4. Migrate legacy pju_sheets (skip untouched init_pju_1 unless checked/issues/notes exist)
        val oldPju = userRef.collection("pju_sheets").get().await()
        oldPju.documents.forEach { doc ->
            val sheet = doc.toObject(PjuChecklistSheet::class.java) ?: return@forEach
            val modified = sheet.checkedDays().isNotEmpty() || sheet.issues.isNotEmpty() || sheet.notes.isNotBlank()
            if (doc.id == "init_pju_1" && !modified) return@forEach
            val targetId = "mig_${uidTag}_${doc.id}".take(120)
            val targetRef = roomRef.collection("pju_sheets").document(targetId)
            if (!targetRef.get().await().exists()) {
                val payload = buildPjuSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    monthYear = sheet.monthYear,
                    sheetMonth = sheet.sheetMonth,
                    sheetYear = sheet.sheetYear,
                    periodStartDay = sheet.periodStartDay,
                    periodEndDay = sheet.periodEndDay,
                    clusters = sheet.clusters,
                    issues = sheet.issues,
                    notes = sheet.notes,
                    responsibleNames = sheet.responsibleNames,
                    lastShift = sheet.lastShift,
                    lastUpdatedTime = sheet.lastUpdatedTime,
                    includeCreatedAt = true
                )
                targetRef.set(payload).await()
            }
        }

        // 5. Migrate legacy quick_notes
        val oldNotes = userRef.collection("quick_notes").get().await()
        oldNotes.documents.forEach { doc ->
            val note = doc.toObject(QuickNote::class.java) ?: return@forEach
            if (note.content.isBlank()) return@forEach
            val targetId = "mig_${uidTag}_${doc.id}".take(120)
            val targetRef = roomRef.collection("quick_notes").document(targetId)
            if (!targetRef.get().await().exists()) {
                val cleanTitle = note.title.trim().take(120).ifBlank {
                    "Catatan ${NoteSourceTab.fromNameOrDefault(note.sourceTab).label}"
                }
                val cleanDate = note.noteDate.trim().takeIf {
                    it.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
                } ?: currentIsoDate()
                val cleanTime = note.noteTime.trim().ifBlank { "07:00" }.take(10)
                val validShift = note.shift.takeIf { it in ShiftOption.entries.map { s -> s.label } } ?: ShiftOption.SHIFT_1.label
                targetRef.set(
                    mapOf(
                        "userId" to uid,
                        "roomCode" to cleanCode,
                        "sourceTab" to NoteSourceTab.fromNameOrDefault(note.sourceTab).name,
                        "title" to cleanTitle,
                        "content" to note.content.trim().take(2000),
                        "noteDate" to cleanDate,
                        "noteTime" to cleanTime,
                        "shift" to validShift,
                        "officerName" to note.officerName.trim().ifBlank { "Petugas Security" }.take(80),
                        "createdAt" to (note.createdAt ?: FieldValue.serverTimestamp()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    private suspend fun ensureDefaultChatRoomExists(uid: String) {
        val roomRef = db.collection("chat_rooms").document(DEFAULT_ROOM_CODE)
        val snap = roomRef.get().await()
        if (!snap.exists()) {
            roomRef.set(
                mapOf(
                    "roomCode" to DEFAULT_ROOM_CODE,
                    "title" to "Channel Koordinasi Plot BPS",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val msgCol = roomRef.collection("messages")
            val welcomeMessages = listOf(
                mapOf(
                    "userId" to uid,
                    "roomCode" to DEFAULT_ROOM_CODE,
                    "senderName" to "Bapak Suroto",
                    "senderRole" to "Korlap Security BPS",
                    "fromOfficer" to false,
                    "time" to "07:15",
                    "text" to "Selamat pagi rekan-rekan Plot BPS. Silakan koordinasi online melalui channel ini atau bagikan kode ruang untuk tim regu.",
                    "audioType" to "none",
                    "createdAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "userId" to uid,
                    "roomCode" to DEFAULT_ROOM_CODE,
                    "senderName" to "Dedi Kurniawan",
                    "senderRole" to "Patroli Gedung Belakang",
                    "fromOfficer" to false,
                    "time" to "07:30",
                    "text" to "Laporan suara pengecekan area genset & ruang arsip belakang dalam keadaan aman terkendali.",
                    "audioType" to "simulated",
                    "audioDuration" to "00:08",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            welcomeMessages.forEachIndexed { idx, payload ->
                msgCol.document("seed_${idx + 1}").set(payload).await()
            }
        }
    }

    private suspend fun upsertRoomMember(
        roomCode: String,
        profile: OfficerProfile,
        isOnline: Boolean
    ) {
        val uid = requireUserId()
        val cleanCode = normalizeRoomCode(roomCode)
        val memberRef = db.collection("chat_rooms").document(cleanCode)
            .collection("members").document(uid)
        val statusStr = if (isOnline) "online" else "offline"
        val cleanName = profile.name.trim().ifBlank { "Petugas Security" }.take(80)
        val cleanNrp = profile.nrp.trim().ifBlank { "SEC-BPS" }.take(40)
        val cleanRole = if (profile.role in OfficerRoles.options) profile.role else OfficerRoles.options.first()
        val cleanShift = if (profile.activeShift in ShiftOption.entries.map { it.label }) profile.activeShift else ShiftOption.SHIFT_1.label
        val cleanPhoto = profile.photoData.takeIf { it.length <= 120_000 } ?: ""

        val snap = memberRef.get().await()
        if (snap.exists()) {
            memberRef.update(
                mapOf(
                    "name" to cleanName,
                    "nrp" to cleanNrp,
                    "role" to cleanRole,
                    "activeShift" to cleanShift,
                    "onlineStatus" to statusStr,
                    "photoData" to cleanPhoto,
                    "lastSeenAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            memberRef.set(
                mapOf(
                    "userId" to uid,
                    "roomCode" to cleanCode,
                    "name" to cleanName,
                    "nrp" to cleanNrp,
                    "role" to cleanRole,
                    "activeShift" to cleanShift,
                    "onlineStatus" to statusStr,
                    "photoData" to cleanPhoto,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun joinRoomByCode(
        rawCode: String,
        profile: OfficerProfile,
        previousRoomCode: String? = null
    ): Result<ChatRoom> {
        val cleanCode = normalizeRoomCode(rawCode)
        val path = "chat_rooms/$cleanCode"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) {
                "Kode ruang harus 3-24 karakter (huruf A-Z, angka 0-9, tanda - atau _)."
            }

            if (cleanCode == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            }

            val roomRef = db.collection("chat_rooms").document(cleanCode)
            val snap = roomRef.get().await()
            if (!snap.exists()) {
                return Result.failure(
                    IllegalStateException("Kode ruang '$cleanCode' tidak ditemukan. Periksa kode atau buat ruang baru.")
                )
            }

            val room = snap.toObject(
                ChatRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: ChatRoom(roomCode = cleanCode, title = "Ruang $cleanCode")

            if (!previousRoomCode.isNullOrBlank() && normalizeRoomCode(previousRoomCode) != cleanCode) {
                runCatching {
                    val oldRef = db.collection("chat_rooms").document(normalizeRoomCode(previousRoomCode))
                        .collection("members").document(uid)
                    if (oldRef.get().await().exists()) {
                        oldRef.update(
                            mapOf(
                                "onlineStatus" to "offline",
                                "lastSeenAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            upsertRoomMember(cleanCode, profile, isOnline = true)
            db.collection("users").document(uid).update(
                mapOf(
                    "activeRoomCode" to cleanCode,
                    "onlineStatus" to "online",
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createAndJoinRoom(
        rawCode: String,
        title: String,
        profile: OfficerProfile,
        previousRoomCode: String? = null
    ): Result<ChatRoom> {
        val cleanCode = normalizeRoomCode(rawCode)
        val cleanTitle = title.trim().take(60)
        val path = "chat_rooms/$cleanCode"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) {
                "Kode ruang harus 3-24 karakter (huruf A-Z, angka 0-9, tanda - atau _)."
            }
            require(cleanTitle.length in 1..60) {
                "Nama ruang koordinasi harus 1-60 karakter."
            }

            val roomRef = db.collection("chat_rooms").document(cleanCode)
            if (roomRef.get().await().exists()) {
                return Result.failure(
                    IllegalStateException("Kode ruang '$cleanCode' sudah ada. Gunakan menu Gabung Ruang untuk masuk.")
                )
            }

            roomRef.set(
                mapOf(
                    "roomCode" to cleanCode,
                    "title" to cleanTitle,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (!previousRoomCode.isNullOrBlank() && normalizeRoomCode(previousRoomCode) != cleanCode) {
                runCatching {
                    val oldRef = db.collection("chat_rooms").document(normalizeRoomCode(previousRoomCode))
                        .collection("members").document(uid)
                    if (oldRef.get().await().exists()) {
                        oldRef.update(
                            mapOf(
                                "onlineStatus" to "offline",
                                "lastSeenAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            upsertRoomMember(cleanCode, profile, isOnline = true)
            db.collection("users").document(uid).update(
                mapOf(
                    "activeRoomCode" to cleanCode,
                    "onlineStatus" to "online",
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ensureRoomStarterData(cleanCode, uid, profile)

            Result.success(ChatRoom(id = cleanCode, roomCode = cleanCode, title = cleanTitle, createdBy = uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePresence(
        roomCode: String,
        profile: OfficerProfile,
        isOnline: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/members/$uid"
        return try {
            val verifiedUid = requireUserId()
            val statusStr = if (isOnline) "online" else "offline"

            val userRef = db.collection("users").document(verifiedUid)
            if (userRef.get().await().exists()) {
                userRef.update(
                    mapOf(
                        "onlineStatus" to statusStr,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val roomSnap = db.collection("chat_rooms").document(cleanCode).get().await()
            if (roomSnap.exists()) {
                upsertRoomMember(cleanCode, profile, isOnline = isOnline)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(
        name: String,
        nrp: String,
        role: String,
        activeShift: String,
        activeRoomCode: String = DEFAULT_ROOM_CODE,
        photoData: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        val path = "users/$uid"
        return try {
            val verifiedUid = requireUserId()
            val cleanName = name.trim()
            val cleanNrp = nrp.trim()
            val cleanRoom = normalizeRoomCode(activeRoomCode).ifBlank { DEFAULT_ROOM_CODE }
            require(cleanName.length in 1..80) { "Nama petugas harus 1-80 karakter." }
            require(cleanNrp.length in 1..40) { "NRP/ID harus 1-40 karakter." }
            require(role in OfficerRoles.options) { "Jabatan tidak valid." }
            require(activeShift in ShiftOption.entries.map { it.label }) { "Shift tidak valid." }
            if (photoData != null) {
                require(photoData.length <= 120_000) { "Ukuran foto profil melebihi batas." }
            }

            val docRef = db.collection("users").document(verifiedUid)
            val snap = docRef.get().await()
            val resolvedPhoto = photoData ?: (snap.getString("photoData") ?: "")
            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "nrp" to cleanNrp,
                    "role" to role,
                    "activeShift" to activeShift,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (photoData != null) {
                    updates["photoData"] = photoData
                }
                docRef.update(updates).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to verifiedUid,
                        "name" to cleanName,
                        "nrp" to cleanNrp,
                        "role" to role,
                        "activeShift" to activeShift,
                        "activeRoomCode" to cleanRoom,
                        "onlineStatus" to "online",
                        "photoData" to resolvedPhoto,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Sync updated identity into active chat room member profile
            val roomSnap = db.collection("chat_rooms").document(cleanRoom).get().await()
            if (roomSnap.exists()) {
                upsertRoomMember(
                    cleanRoom,
                    OfficerProfile(
                        userId = verifiedUid,
                        name = cleanName,
                        nrp = cleanNrp,
                        role = role,
                        activeShift = activeShift,
                        activeRoomCode = cleanRoom,
                        photoData = resolvedPhoto
                    ),
                    isOnline = true
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePhoto(
        photoData: String,
        activeRoomCode: String = DEFAULT_ROOM_CODE
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        val path = "users/$uid"
        return try {
            val verifiedUid = requireUserId()
            require(photoData.length <= 120_000) { "Ukuran foto profil melebihi batas maksimal." }
            val userRef = db.collection("users").document(verifiedUid)
            val snap = userRef.get().await()
            if (snap.exists()) {
                userRef.update(
                    mapOf(
                        "photoData" to photoData,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val cleanRoom = normalizeRoomCode(activeRoomCode).ifBlank { DEFAULT_ROOM_CODE }
            runCatching {
                val memberRef = db.collection("chat_rooms").document(cleanRoom)
                    .collection("members").document(verifiedUid)
                if (memberRef.get().await().exists()) {
                    memberRef.update(
                        mapOf(
                            "photoData" to photoData,
                            "lastSeenAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateActiveShift(shift: String, activeRoomCode: String = DEFAULT_ROOM_CODE): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        val path = "users/$uid"
        return try {
            val verifiedUid = requireUserId()
            require(shift in ShiftOption.entries.map { it.label }) { "Shift tidak valid." }
            db.collection("users").document(verifiedUid)
                .update(
                    mapOf(
                        "activeShift" to shift,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            val cleanRoom = normalizeRoomCode(activeRoomCode).ifBlank { DEFAULT_ROOM_CODE }
            runCatching {
                val memberRef = db.collection("chat_rooms").document(cleanRoom)
                    .collection("members").document(verifiedUid)
                if (memberRef.get().await().exists()) {
                    memberRef.update(
                        mapOf(
                            "activeShift" to shift,
                            "lastSeenAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addReport(
        roomCode: String,
        category: Int,
        time: String,
        reportDate: String,
        shift: String,
        status: String,
        description: String,
        photos: List<String>,
        photoCaptions: List<String>,
        officerName: String,
        officerNrp: String
    ): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/reports"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            val cleanDesc = description.trim()
            val cleanTime = time.trim()
            val cleanDate = reportDate.trim().ifBlank { currentIsoDate() }
            val cleanName = officerName.trim().ifBlank { "Petugas Security" }.take(80)
            val cleanNrp = officerNrp.trim().ifBlank { "SEC-BPS" }.take(40)
            val normalizedCaptions = photos.indices.map { idx ->
                photoCaptions.getOrNull(idx)?.trim()?.take(200).orEmpty()
            }
            require(category in 1..3) { "Kategori harus 1, 2, atau 3." }
            require(cleanTime.length in 4..10) { "Format waktu tidak valid." }
            require(cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) { "Format tanggal harus yyyy-MM-dd." }
            require(shift in ShiftOption.entries.map { it.label }) { "Shift tidak valid." }
            require(status in ReportStatus.entries.map { it.label }) { "Status kondisi tidak valid." }
            require(cleanDesc.length in 1..2000) { "Rincian kegiatan harus 1-2000 karakter." }
            require(photos.size <= 4) { "Maksimal 4 foto per rincian." }
            require(photos.all { it.length <= 200000 }) { "Ukuran foto terkompresi melebihi batas." }

            if (cleanCode == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            }

            val docRef = db.collection("chat_rooms").document(cleanCode).collection("reports").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "roomCode" to cleanCode,
                "category" to category,
                "time" to cleanTime,
                "reportDate" to cleanDate,
                "shift" to shift,
                "status" to status,
                "description" to cleanDesc,
                "photos" to photos,
                "photoCaptions" to normalizedCaptions,
                "officerName" to cleanName,
                "officerNrp" to cleanNrp,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReport(roomCode: String, reportId: String): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/reports/$reportId"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            require(reportId.isNotBlank()) { "ID laporan tidak valid." }
            val docRef = db.collection("chat_rooms").document(cleanCode).collection("reports").document(reportId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun resetAllReports(roomCode: String, reportIds: List<String>): Result<Int> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/reports"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            val col = db.collection("chat_rooms").document(cleanCode).collection("reports")
            val validIds = reportIds.filter { it.isNotBlank() }
            val chunks = validIds.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { id -> batch.delete(col.document(id)) }
                batch.commit().await()
            }
            Result.success(validIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun sendRoomChatMessage(
        roomCode: String,
        senderName: String,
        senderRole: String,
        fromOfficer: Boolean,
        text: String,
        imageData: String? = null,
        imageCaption: String? = null,
        audioType: String = "none",
        audioData: String? = null,
        audioDuration: String? = null,
        senderPhoto: String? = null,
        shift: String? = null
    ): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/messages"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            val cleanText = text.trim()
            val cleanCaption = imageCaption?.trim()?.take(300)?.ifBlank { null }
            val cleanName = senderName.trim().ifBlank { "Petugas Security" }.take(80)
            val cleanRole = senderRole.trim().ifBlank { "Plot BPS" }.take(80)
            val cleanSenderPhoto = senderPhoto?.takeIf { it.isNotBlank() && it.length <= 120_000 }
            val validShift = shift?.takeIf { s -> s in ShiftOption.entries.map { it.label } }
            require(cleanText.length <= 2000) { "Pesan maksimal 2000 karakter." }
            require(audioType in listOf("none", "recorded", "simulated")) { "Tipe audio tidak valid." }
            if (imageData != null) require(imageData.length <= 200000) { "Ukuran foto melebihi batas." }
            if (audioData != null) require(audioData.length <= 250000) { "Durasi rekaman terlalu besar." }
            require(cleanText.isNotEmpty() || !imageData.isNullOrEmpty() || audioType != "none") {
                "Pesan tidak boleh kosong."
            }

            if (cleanCode == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            }

            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val docRef = db.collection("chat_rooms").document(cleanCode).collection("messages").document()
            path = docRef.path

            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "roomCode" to cleanCode,
                "senderName" to cleanName,
                "senderRole" to cleanRole,
                "fromOfficer" to fromOfficer,
                "time" to timeStr,
                "text" to cleanText,
                "imageData" to imageData?.takeIf { it.isNotEmpty() },
                "imageCaption" to if (!imageData.isNullOrEmpty()) cleanCaption else null,
                "audioType" to audioType,
                "audioData" to audioData?.takeIf { it.isNotEmpty() },
                "audioDuration" to audioDuration?.takeIf { it.isNotEmpty() },
                "senderPhoto" to cleanSenderPhoto,
                "edited" to false,
                "shift" to validShift,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun editRoomChatMessage(
        roomCode: String,
        messageId: String,
        newText: String,
        newImageCaption: String? = null
    ): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/messages/$messageId"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            require(messageId.isNotBlank()) { "ID pesan tidak valid." }
            val cleanText = newText.trim()
            require(cleanText.length <= 2000) { "Pesan maksimal 2000 karakter." }
            val docRef = db.collection("chat_rooms").document(cleanCode)
                .collection("messages").document(messageId)
            path = docRef.path

            val updates = mutableMapOf<String, Any>(
                "text" to cleanText,
                "edited" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newImageCaption != null) {
                updates["imageCaption"] = newImageCaption.trim().take(300)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRoomChatMessage(roomCode: String, messageId: String): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/messages/$messageId"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            require(messageId.isNotBlank()) { "ID pesan tidak valid." }
            val docRef = db.collection("chat_rooms").document(cleanCode)
                .collection("messages").document(messageId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearRoomChatMessages(roomCode: String, messageIds: List<String>): Result<Int> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val path = "chat_rooms/$cleanCode/messages"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            val col = db.collection("chat_rooms").document(cleanCode).collection("messages")
            val validIds = messageIds.filter { it.isNotBlank() }
            val chunks = validIds.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { id -> batch.delete(col.document(id)) }
                batch.commit().await()
            }
            Result.success(validIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun simulateTeamReply(roomCode: String): Result<Unit> {
        val replies = listOf(
            Triple("Bapak Suroto", "Korlap Security BPS", "Terima kasih laporannya, lanjutkan pemantauan berkala di seluruh titik Plot BPS. 👍"),
            Triple("Rizky Pratama", "Pos Gerbang Depan", "Copy Danru! Gerbang utama terpantau aman, situasi kondusif. 🫡"),
            Triple("Dedi Kurniawan", "Patroli Gedung Belakang", "Siap laksanakan, area parkir belakang & ruang genset sudah dicek. 🛡️")
        )
        val pick = replies.random()
        return sendRoomChatMessage(
            roomCode = roomCode,
            senderName = pick.first,
            senderRole = pick.second,
            fromOfficer = false,
            text = pick.third
        )
    }

    private fun buildActivitySheetPayload(
        uid: String,
        roomCode: String,
        docNumber: String,
        sheetDate: String,
        shift: String,
        personnelRows: List<PersonnelRow>,
        inventoryRows: List<InventoryRow>,
        activityRows: List<ActivityTableRow>,
        officerName: String,
        officerNrp: String,
        includeCreatedAt: Boolean
    ): Map<String, Any> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val cleanDocNum = docNumber.trim().take(40)
        val cleanDate = sheetDate.trim().ifBlank { currentIsoDate() }
        val cleanShift = if (shift in ShiftOption.entries.map { it.label }) shift else ShiftOption.SHIFT_1.label
        val cleanOfficer = officerName.trim().ifBlank { "Petugas Security" }.take(80)
        val cleanNrp = officerNrp.trim().ifBlank { "SEC-BPS" }.take(40)

        require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
        require(cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) { "Format tanggal harus yyyy-MM-dd." }
        require(personnelRows.size <= 30) { "Maksimal 30 baris personel." }
        require(inventoryRows.size <= 50) { "Maksimal 50 baris inventaris." }
        require(activityRows.size <= 60) { "Maksimal 60 baris uraian kegiatan." }

        val personnelMaps = personnelRows.map { row ->
            mapOf(
                "nama" to row.nama.trim().take(80),
                "jabatan" to row.jabatan.trim().take(40),
                "plot" to row.plot.trim().take(24),
                "kehadiran" to row.kehadiran.trim().take(20),
                "ket" to row.ket.trim().take(120)
            )
        }
        val inventoryMaps = inventoryRows.map { row ->
            mapOf(
                "inventaris" to row.inventaris.trim().take(120),
                "jumlah" to row.jumlah.trim().take(40),
                "kondisi" to row.kondisi.trim().take(40),
                "keterangan" to row.keterangan.trim().take(200)
            )
        }
        val activityMaps = activityRows.map { row ->
            mapOf(
                "jam" to row.jam.trim().take(16),
                "uraian" to row.uraian.trim().take(1000)
            )
        }

        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "roomCode" to cleanCode,
            "docNumber" to cleanDocNum,
            "sheetDate" to cleanDate,
            "shift" to cleanShift,
            "personnelRows" to personnelMaps,
            "inventoryRows" to inventoryMaps,
            "activityRows" to activityMaps,
            "officerName" to cleanOfficer,
            "officerNrp" to cleanNrp,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (includeCreatedAt) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    suspend fun saveActivitySheet(roomCode: String, sheet: DailyActivitySheet): Result<String> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/activity_sheets"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            if (cleanCode == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            }
            val col = db.collection("chat_rooms").document(cleanCode).collection("activity_sheets")
            val isNew = sheet.id.isBlank()
            val docRef = if (isNew) col.document() else col.document(sheet.id)
            path = docRef.path

            val exists = if (isNew) false else docRef.get().await().exists()
            if (!exists) {
                val payload = buildActivitySheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    docNumber = sheet.docNumber,
                    sheetDate = sheet.sheetDate,
                    shift = sheet.shift,
                    personnelRows = sheet.personnelRows,
                    inventoryRows = sheet.inventoryRows,
                    activityRows = sheet.activityRows,
                    officerName = sheet.officerName,
                    officerNrp = sheet.officerNrp,
                    includeCreatedAt = true
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = buildActivitySheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    docNumber = sheet.docNumber,
                    sheetDate = sheet.sheetDate,
                    shift = sheet.shift,
                    personnelRows = sheet.personnelRows,
                    inventoryRows = sheet.inventoryRows,
                    activityRows = sheet.activityRows,
                    officerName = sheet.officerName,
                    officerNrp = sheet.officerNrp,
                    includeCreatedAt = false
                ).toMutableMap().apply {
                    remove("userId")
                    remove("roomCode")
                }
                docRef.update(updatePayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteActivitySheet(roomCode: String, sheetId: String): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/activity_sheets/$sheetId"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            require(sheetId.isNotBlank()) { "ID lembar tabel tidak valid." }
            val docRef = db.collection("chat_rooms").document(cleanCode).collection("activity_sheets").document(sheetId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun buildAttendanceSheetPayload(
        uid: String,
        roomCode: String,
        sheetDate: String,
        shift: String,
        periodLabel: String,
        commanderName: String,
        rows: List<AttendanceMemberRow>,
        includeCreatedAt: Boolean
    ): Map<String, Any> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val cleanDate = sheetDate.trim().ifBlank { currentIsoDate() }
        val cleanShift = if (shift in ShiftOption.entries.map { it.label }) shift else ShiftOption.SHIFT_1.label
        val cleanPeriod = periodLabel.trim().take(60)
        val cleanCommander = commanderName.trim().ifBlank { "Danru Plot BPS" }.take(80)

        require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
        require(cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) { "Format tanggal harus yyyy-MM-dd." }
        require(rows.size <= 25) { "Maksimal 25 baris anggota per lembar absensi." }

        val rowMaps = rows.map { row ->
            mapOf(
                "nama" to row.nama.trim().take(80),
                "nrp" to row.nrp.trim().take(40),
                "jabatan" to row.jabatan.trim().take(40),
                "plot" to row.plot.trim().take(30),
                "status" to AttendanceStatus.fromCode(row.status).code,
                "jamMasuk" to row.jamMasuk.trim().take(16),
                "jamPulang" to row.jamPulang.trim().take(16),
                "recordedAt" to row.recordedAt.trim().take(24),
                "keterangan" to row.keterangan.trim().take(150)
            )
        }

        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "roomCode" to cleanCode,
            "sheetDate" to cleanDate,
            "shift" to cleanShift,
            "periodLabel" to cleanPeriod,
            "commanderName" to cleanCommander,
            "rows" to rowMaps,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (includeCreatedAt) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    suspend fun saveAttendanceSheet(roomCode: String, sheet: AttendanceSheet): Result<String> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/attendance_sheets"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            if (cleanCode == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            }
            val col = db.collection("chat_rooms").document(cleanCode).collection("attendance_sheets")
            val isNew = sheet.id.isBlank()
            val docRef = if (isNew) col.document() else col.document(sheet.id)
            path = docRef.path

            val exists = if (isNew) false else docRef.get().await().exists()
            if (!exists) {
                val payload = buildAttendanceSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    sheetDate = sheet.sheetDate,
                    shift = sheet.shift,
                    periodLabel = sheet.periodLabel,
                    commanderName = sheet.commanderName,
                    rows = sheet.rows,
                    includeCreatedAt = true
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = buildAttendanceSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    sheetDate = sheet.sheetDate,
                    shift = sheet.shift,
                    periodLabel = sheet.periodLabel,
                    commanderName = sheet.commanderName,
                    rows = sheet.rows,
                    includeCreatedAt = false
                ).toMutableMap().apply {
                    remove("userId")
                    remove("roomCode")
                }
                docRef.update(updatePayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAttendanceSheet(roomCode: String, sheetId: String): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/attendance_sheets/$sheetId"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            require(sheetId.isNotBlank()) { "ID lembar absensi tidak valid." }
            val docRef = db.collection("chat_rooms").document(cleanCode).collection("attendance_sheets").document(sheetId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun buildPjuSheetPayload(
        uid: String,
        roomCode: String,
        monthYear: String,
        sheetMonth: Long,
        sheetYear: Long,
        periodStartDay: Long,
        periodEndDay: Long,
        clusters: List<PjuClusterRow>,
        issues: List<PjuIssueEntry>,
        notes: String,
        responsibleNames: String,
        lastShift: String = "",
        lastUpdatedTime: String = "",
        includeCreatedAt: Boolean
    ): Map<String, Any> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        val cleanMonthYear = monthYear.trim().ifBlank { PjuTemplates.currentMonthYearLabel() }.take(40)
        val cleanMonth = sheetMonth.coerceIn(1L, 12L)
        val cleanYear = sheetYear.coerceIn(2020L, 2100L)
        val cleanStartDay = periodStartDay.coerceIn(1L, 31L)
        val cleanEndDay = periodEndDay.coerceIn(cleanStartDay, 31L)
        val cleanNotes = notes.trim().take(2000)
        val cleanResp = responsibleNames.trim().ifBlank { PjuTemplates.DEFAULT_RESPONSIBLE }.take(120)
        val validLastShift = lastShift.takeIf { s -> s in ShiftOption.entries.map { it.label } }
        val cleanLastTime = lastUpdatedTime.trim().take(16).takeIf { it.isNotEmpty() }

        require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
        require(clusters.size <= 40) { "Maksimal 40 cluster PJU per lembar." }
        require(issues.size <= 100) { "Maksimal 100 entri permasalahan PJU per lembar." }

        val clusterMaps = clusters.map { row ->
            val sanitizedChecks = row.checks
                .filterKeys { key ->
                    val d = key.toIntOrNull()
                    d != null && d in 1..31
                }
                .mapValues { (_, v) -> v.trim().take(32) }
            mapOf(
                "clusterName" to row.clusterName.trim().ifBlank { "PJU BARU" }.take(60),
                "checks" to sanitizedChecks
            )
        }

        val issueMaps = issues.map { entry ->
            mapOf(
                "tanggal" to entry.tanggal.trim().take(24),
                "cluster" to entry.cluster.trim().take(60),
                "titikLampu" to entry.titikLampu.trim().take(40),
                "keluhan" to entry.keluhan.trim().take(400),
                "tindakLanjut" to entry.tindakLanjut.trim().take(400),
                "status" to entry.status.trim().take(32)
            )
        }

        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "roomCode" to cleanCode,
            "monthYear" to cleanMonthYear,
            "sheetMonth" to cleanMonth,
            "sheetYear" to cleanYear,
            "periodStartDay" to cleanStartDay,
            "periodEndDay" to cleanEndDay,
            "clusters" to clusterMaps,
            "issues" to issueMaps,
            "notes" to cleanNotes,
            "responsibleNames" to cleanResp,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (validLastShift != null) {
            map["lastShift"] = validLastShift
        }
        if (cleanLastTime != null) {
            map["lastUpdatedTime"] = cleanLastTime
        }
        if (includeCreatedAt) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    suspend fun savePjuSheet(roomCode: String, sheet: PjuChecklistSheet): Result<String> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/pju_sheets"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            if (cleanCode == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            }
            val col = db.collection("chat_rooms").document(cleanCode).collection("pju_sheets")
            val isNew = sheet.id.isBlank()
            val docRef = if (isNew) col.document() else col.document(sheet.id)
            path = docRef.path

            val exists = if (isNew) false else docRef.get().await().exists()
            if (!exists) {
                val payload = buildPjuSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    monthYear = sheet.monthYear,
                    sheetMonth = sheet.sheetMonth,
                    sheetYear = sheet.sheetYear,
                    periodStartDay = sheet.periodStartDay,
                    periodEndDay = sheet.periodEndDay,
                    clusters = sheet.clusters,
                    issues = sheet.issues,
                    notes = sheet.notes,
                    responsibleNames = sheet.responsibleNames,
                    lastShift = sheet.lastShift,
                    lastUpdatedTime = sheet.lastUpdatedTime,
                    includeCreatedAt = true
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = buildPjuSheetPayload(
                    uid = uid,
                    roomCode = cleanCode,
                    monthYear = sheet.monthYear,
                    sheetMonth = sheet.sheetMonth,
                    sheetYear = sheet.sheetYear,
                    periodStartDay = sheet.periodStartDay,
                    periodEndDay = sheet.periodEndDay,
                    clusters = sheet.clusters,
                    issues = sheet.issues,
                    notes = sheet.notes,
                    responsibleNames = sheet.responsibleNames,
                    lastShift = sheet.lastShift,
                    lastUpdatedTime = sheet.lastUpdatedTime,
                    includeCreatedAt = false
                ).toMutableMap().apply {
                    remove("userId")
                    remove("roomCode")
                }
                docRef.update(updatePayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePjuSheet(roomCode: String, sheetId: String): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/pju_sheets/$sheetId"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            require(sheetId.isNotBlank()) { "ID lembar PJU tidak valid." }
            val docRef = db.collection("chat_rooms").document(cleanCode).collection("pju_sheets").document(sheetId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveQuickNote(roomCode: String, note: QuickNote): Result<String> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/quick_notes"
        return try {
            val uid = requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            val validSource = NoteSourceTab.fromNameOrDefault(note.sourceTab).name
            val cleanTitle = note.title.trim().take(120).ifBlank {
                "Catatan ${NoteSourceTab.fromNameOrDefault(note.sourceTab).label}"
            }
            val cleanContent = note.content.trim().take(2000)
            require(cleanContent.isNotEmpty()) { "Isi catatan tidak boleh kosong." }
            val cleanDate = note.noteDate.trim().ifBlank { currentIsoDate() }
            require(cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) { "Format tanggal catatan harus yyyy-MM-dd." }
            val cleanTime = note.noteTime.trim().ifBlank {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            }.take(10)
            val validShift = if (note.shift in ShiftOption.entries.map { it.label }) {
                note.shift
            } else {
                ShiftOption.SHIFT_1.label
            }
            val cleanOfficer = note.officerName.trim().ifBlank { "Petugas Security" }.take(80)

            if (cleanCode == DEFAULT_ROOM_CODE) {
                ensureDefaultChatRoomExists(uid)
            }

            val col = db.collection("chat_rooms").document(cleanCode).collection("quick_notes")
            val isNew = note.id.isBlank()
            val docRef = if (isNew) col.document() else col.document(note.id)
            path = docRef.path

            val baseMap = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "content" to cleanContent,
                "sourceTab" to validSource,
                "noteDate" to cleanDate,
                "noteTime" to cleanTime,
                "shift" to validShift,
                "officerName" to cleanOfficer,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isNew || !docRef.get().await().exists()) {
                baseMap["userId"] = uid
                baseMap["roomCode"] = cleanCode
                baseMap["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(baseMap).await()
            } else {
                docRef.update(baseMap).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuickNote(roomCode: String, noteId: String): Result<Unit> {
        val cleanCode = normalizeRoomCode(roomCode).ifBlank { DEFAULT_ROOM_CODE }
        var path = "chat_rooms/$cleanCode/quick_notes/$noteId"
        return try {
            requireUserId()
            require(isValidRoomCode(cleanCode)) { "Kode ruang tidak valid." }
            require(noteId.isNotBlank()) { "ID catatan tidak valid." }
            val docRef = db.collection("chat_rooms").document(cleanCode).collection("quick_notes").document(noteId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
