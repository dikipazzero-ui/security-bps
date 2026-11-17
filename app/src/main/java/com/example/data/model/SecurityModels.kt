package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ReportCategory(
    val number: Int,
    val shortTitle: String,
    val fullTitle: String,
    val subtitle: String,
    val templateText: String
) {
    APEL_INVENTARIS(
        number = 1,
        shortTitle = "Apel Serah Terima & Inventaris",
        fullTitle = "KATEGORI 1: APEL SERAH TERIMA & PENGECEKAN INVENTARIS",
        subtitle = "Pengecekan personil, HT, kunci & buku mutasi",
        templateText = "Melaksanakan apel serah terima tugas jaga di Pos Utama BPS. Pengecekan inventaris: 3 unit HT berfungsi baik, 1 buku mutasi, kunci ruang server & ruang arsip lengkap. Personil lengkap dan siap bertugas."
    ),
    PATROLI_GEDUNG(
        number = 2,
        shortTitle = "Patroli Area Gedung & Lingkungan BPS",
        fullTitle = "KATEGORI 2: PATROLI AREA GEDUNG & LINGKUNGAN BPS",
        subtitle = "Kontrol ruang server, arsip, parkir & pagar keliling",
        templateText = "Melaksanakan patroli keliling area gedung utama BPS, ruang server lantai 2, gudang arsip, area parkir kendaraan dinas, serta pagar keliling. Pintu terkunci rapat, instalasi listrik & AC aman, situasi terkendali."
    ),
    PELAYANAN_TAMU(
        number = 3,
        shortTitle = "Pelayanan Tamu & Situasi Kamtibmas",
        fullTitle = "KATEGORI 3: PELAYANAN TAMU, PENGAWASAN & SITUASI KAMTIBMAS",
        subtitle = "Pemeriksaan tamu/kendaraan & kejadian menonjol",
        templateText = "Melakukan pemeriksaan dan pendataan tamu dinas di lobi utama BPS serta pengaturan lalu lintas keluar masuk kendaraan. Tidak ditemukan hal mencurigakan, situasi kamtibmas aman dan kondusif."
    );

    companion object {
        fun fromNumber(num: Int): ReportCategory =
            entries.firstOrNull { it.number == num } ?: APEL_INVENTARIS
    }
}

enum class ShiftOption(val label: String, val timeRange: String) {
    SHIFT_1("Shift 1", "07.00 - 15.00"),
    SHIFT_2("Shift 2", "15.00 - 23.00"),
    SHIFT_3("Shift 3", "23.00 - 07.00");

    companion object {
        fun fromLabel(label: String): ShiftOption =
            entries.firstOrNull { it.label == label } ?: SHIFT_1
    }
}

enum class ReportStatus(val label: String) {
    AMAN("Aman & Terkendali"),
    PEMANTAUAN("Dalam Pemantauan"),
    TINDAK_LANJUT("Perlu Tindak Lanjut");

    companion object {
        fun fromLabel(label: String): ReportStatus =
            entries.firstOrNull { it.label == label } ?: AMAN
    }
}

object OfficerRoles {
    val options = listOf(
        "Danru Plot BPS",
        "Anggota Pos Utama BPS",
        "Anggota Patroli Gedung BPS",
        "Korlap Security BPS"
    )
}

@Keep
@IgnoreExtraProperties
data class OfficerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Hendra Wijaya",
    val nrp: String = "SEC-BPS-042",
    val role: String = "Danru Plot BPS",
    val activeShift: String = "Shift 1",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SecurityReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: Long = 1L,
    val time: String = "07:05",
    val shift: String = "Shift 1",
    val status: String = "Aman & Terkendali",
    val description: String = "",
    val photos: List<String> = emptyList(),
    val officerName: String = "",
    val officerNrp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: ReportCategory
        get() = ReportCategory.fromNumber(category.toInt())

    val statusEnum: ReportStatus
        get() = ReportStatus.fromLabel(status)
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val fromOfficer: Boolean = true,
    val time: String = "07:15",
    val text: String = "",
    val imageData: String? = null,
    val audioType: String = "none", // "none", "recorded", "simulated"
    val audioData: String? = null,
    val audioDuration: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
