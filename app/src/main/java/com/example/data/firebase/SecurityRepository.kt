package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.data.model.OfficerProfile
import com.example.data.model.OfficerRoles
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

    fun observeReports(userId: String): Flow<List<SecurityReport>> {
        val path = "users/$userId/reports"
        return db.collection("users").document(userId).collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SecurityReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
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

                val initialReportRef = profileRef.collection("reports").document("init_1")
                initialReportRef.set(
                    mapOf(
                        "userId" to uid,
                        "category" to 1,
                        "time" to "07:05",
                        "reportDate" to currentIsoDate(),
                        "shift" to ShiftOption.SHIFT_1.label,
                        "status" to ReportStatus.AMAN.label,
                        "description" to "Apel serah terima jaga pagi di Pos Utama Plot BPS dari petugas Shift 3 ke Shift 1. Pengecekan inventaris HT (3 unit), kunci-kunci ruangan, dan buku mutasi lengkap dalam kondisi baik.",
                        "photos" to emptyList<String>(),
                        "photoCaptions" to emptyList<String>(),
                        "officerName" to defaultName,
                        "officerNrp" to "SEC-BPS-042",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
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

        val snap = memberRef.get().await()
        if (snap.exists()) {
            memberRef.update(
                mapOf(
                    "name" to cleanName,
                    "nrp" to cleanNrp,
                    "role" to cleanRole,
                    "activeShift" to cleanShift,
                    "onlineStatus" to statusStr,
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
        activeRoomCode: String = DEFAULT_ROOM_CODE
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

            val docRef = db.collection("users").document(verifiedUid)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "nrp" to cleanNrp,
                        "role" to role,
                        "activeShift" to activeShift,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
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
                        activeRoomCode = cleanRoom
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
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reports"
        return try {
            val uid = requireUserId()
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

            val docRef = db.collection("users").document(uid).collection("reports").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
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

    suspend fun deleteReport(reportId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reports/$reportId"
        return try {
            val uid = requireUserId()
            require(reportId.isNotBlank()) { "ID laporan tidak valid." }
            val docRef = db.collection("users").document(uid).collection("reports").document(reportId)
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

    suspend fun resetAllReports(reportIds: List<String>): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reports"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("reports")
            val chunks = reportIds.filter { it.isNotBlank() }.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { id -> batch.delete(col.document(id)) }
                batch.commit().await()
            }
            Result.success(Unit)
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
        audioDuration: String? = null
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

    suspend fun simulateTeamReply(roomCode: String): Result<Unit> {
        val replies = listOf(
            Triple("Bapak Suroto", "Korlap Security BPS", "Terima kasih laporannya, lanjutkan pemantauan berkala di seluruh titik Plot BPS."),
            Triple("Rizky Pratama", "Pos Gerbang Depan", "Copy Danru! Gerbang utama terpantau aman, situasi kondusif."),
            Triple("Dedi Kurniawan", "Patroli Gedung Belakang", "Siap laksanakan, area parkir belakang & ruang genset sudah dicek.")
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
}
