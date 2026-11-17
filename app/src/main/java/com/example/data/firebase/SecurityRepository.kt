package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.OfficerProfile
import com.example.data.model.OfficerRoles
import com.example.data.model.ReportStatus
import com.example.data.model.SecurityReport
import com.example.data.model.ShiftOption
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

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> {
        val path = "users/$userId/chat_messages"
        return db.collection("users").document(userId).collection("chat_messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
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

    suspend fun ensureInitialSetup(userId: String, googleDisplayName: String?): Result<Unit> {
        val profilePath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi petugas berubah; membatalkan inisialisasi." }
            val profileRef = db.collection("users").document(uid)
            val snapshot = profileRef.get().await()
            if (!snapshot.exists()) {
                val defaultName = googleDisplayName?.trim()?.take(80)?.ifBlank { null } ?: "Hendra Wijaya"
                val profilePayload = mapOf(
                    "userId" to uid,
                    "name" to defaultName,
                    "nrp" to "SEC-BPS-042",
                    "role" to OfficerRoles.options.first(),
                    "activeShift" to ShiftOption.SHIFT_1.label,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(profilePayload).await()

                // Seed 1 initial report sample & default coordination channel messages
                val initialReportRef = profileRef.collection("reports").document("init_1")
                initialReportRef.set(
                    mapOf(
                        "userId" to uid,
                        "category" to 1,
                        "time" to "07:05",
                        "shift" to ShiftOption.SHIFT_1.label,
                        "status" to ReportStatus.AMAN.label,
                        "description" to "Apel serah terima jaga pagi di Pos Utama Plot BPS dari petugas Shift 3 ke Shift 1. Pengecekan inventaris HT (3 unit), kunci-kunci ruangan, dan buku mutasi lengkap dalam kondisi baik.",
                        "photos" to emptyList<String>(),
                        "officerName" to defaultName,
                        "officerNrp" to "SEC-BPS-042",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                val chatCol = profileRef.collection("chat_messages")
                val defaultMessages = listOf(
                    mapOf(
                        "userId" to uid,
                        "senderName" to "Bapak Suroto",
                        "senderRole" to "Korlap Security BPS",
                        "fromOfficer" to false,
                        "time" to "07:15",
                        "text" to "Selamat pagi rekan-rekan Plot BPS. Mohon monitor area lobi utama dan parkiran tamu hari ini ada agenda rapat pimpinan BPS.",
                        "audioType" to "none",
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    mapOf(
                        "userId" to uid,
                        "senderName" to "Rizky Pratama",
                        "senderRole" to "Pos Gerbang Depan",
                        "fromOfficer" to false,
                        "time" to "07:22",
                        "text" to "Siap monitor Komandan! Barrier gate dan pos depan sudah steril, tamu mulai berdatangan dan diarahkan sesuai SOP.",
                        "audioType" to "none",
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    mapOf(
                        "userId" to uid,
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
                defaultMessages.forEachIndexed { idx, msg ->
                    chatCol.document("seed_${idx + 1}").set(msg).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profilePath)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(
        name: String,
        nrp: String,
        role: String,
        activeShift: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        val path = "users/$uid"
        return try {
            val verifiedUid = requireUserId()
            val cleanName = name.trim()
            val cleanNrp = nrp.trim()
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
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateActiveShift(shift: String): Result<Unit> {
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
        shift: String,
        status: String,
        description: String,
        photos: List<String>,
        officerName: String,
        officerNrp: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reports"
        return try {
            val uid = requireUserId()
            val cleanDesc = description.trim()
            val cleanTime = time.trim()
            val cleanName = officerName.trim().ifBlank { "Petugas Security" }.take(80)
            val cleanNrp = officerNrp.trim().ifBlank { "SEC-BPS" }.take(40)
            require(category in 1..3) { "Kategori harus 1, 2, atau 3." }
            require(cleanTime.length in 4..10) { "Format waktu tidak valid." }
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
                "shift" to shift,
                "status" to status,
                "description" to cleanDesc,
                "photos" to photos,
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

    suspend fun sendChatMessage(
        senderName: String,
        senderRole: String,
        fromOfficer: Boolean,
        text: String,
        imageData: String? = null,
        audioType: String = "none",
        audioData: String? = null,
        audioDuration: String? = null
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/chat_messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            val cleanName = senderName.trim().ifBlank { "Petugas Security" }.take(80)
            val cleanRole = senderRole.trim().ifBlank { "Plot BPS" }.take(80)
            require(cleanText.length <= 2000) { "Pesan maksimal 2000 karakter." }
            require(audioType in listOf("none", "recorded", "simulated")) { "Tipe audio tidak valid." }
            if (imageData != null) require(imageData.length <= 200000) { "Ukuran foto melebihi batas." }
            if (audioData != null) require(audioData.length <= 250000) { "Durasi rekaman terlalu besar." }
            require(cleanText.isNotEmpty() || !imageData.isNullOrEmpty() || audioType != "none") {
                "Pesan tidak boleh kosong."
            }

            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val docRef = db.collection("users").document(uid).collection("chat_messages").document()
            path = docRef.path

            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "senderName" to cleanName,
                "senderRole" to cleanRole,
                "fromOfficer" to fromOfficer,
                "time" to timeStr,
                "text" to cleanText,
                "imageData" to imageData?.takeIf { it.isNotEmpty() },
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

    suspend fun simulateTeamReply(): Result<Unit> {
        val replies = listOf(
            Triple("Bapak Suroto", "Korlap Security BPS", "Terima kasih laporannya, lanjutkan pemantauan berkala di seluruh titik Plot BPS."),
            Triple("Rizky Pratama", "Pos Gerbang Depan", "Copy Danru! Gerbang utama terpantau aman, situasi kondusif."),
            Triple("Dedi Kurniawan", "Patroli Gedung Belakang", "Siap laksanakan, area parkir belakang & ruang genset sudah dicek.")
        )
        val pick = replies.random()
        return sendChatMessage(
            senderName = pick.first,
            senderRole = pick.second,
            fromOfficer = false,
            text = pick.third
        )
    }
}
