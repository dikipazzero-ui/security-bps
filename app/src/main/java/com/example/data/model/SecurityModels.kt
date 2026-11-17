package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val DEFAULT_ROOM_CODE = "PLOT-BPS"

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
    val activeRoomCode: String = DEFAULT_ROOM_CODE,
    val onlineStatus: String = "online",
    val lastSeenAt: Timestamp? = null,
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
    val reportDate: String = "",
    val shift: String = "Shift 1",
    val status: String = "Aman & Terkendali",
    val description: String = "",
    val photos: List<String> = emptyList(),
    val photoCaptions: List<String> = emptyList(),
    val officerName: String = "",
    val officerNrp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: ReportCategory
        get() = ReportCategory.fromNumber(category.toInt())

    val statusEnum: ReportStatus
        get() = ReportStatus.fromLabel(status)

    fun resolvedHour(): Int? {
        val fromTime = time.trim().substringBefore(":").toIntOrNull()
        if (fromTime != null && fromTime in 0..23) return fromTime
        val date = createdAt?.toDate() ?: return null
        return Calendar.getInstance().apply { this.time = date }.get(Calendar.HOUR_OF_DAY)
    }

    fun resolvedYearMonthDay(): Triple<Int, Int, Int>? {
        val parts = reportDate.trim().split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null && m in 1..12 && d in 1..31) {
                return Triple(y, m, d)
            }
        }
        val date = createdAt?.toDate() ?: return null
        val cal = Calendar.getInstance().apply { this.time = date }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formattedDateBadge(): String {
        val ymd = resolvedYearMonthDay() ?: return ""
        val months = IndonesianMonths.shortNames
        val monthName = months.getOrElse(ymd.second - 1) { ymd.second.toString() }
        return "${ymd.third.toString().padStart(2, '0')} $monthName ${ymd.first}"
    }
}

@Keep
@IgnoreExtraProperties
data class ChatRoom(
    @DocumentId val id: String = "",
    val roomCode: String = DEFAULT_ROOM_CODE,
    val title: String = "Channel Koordinasi Plot BPS",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = DEFAULT_ROOM_CODE,
    val name: String = "",
    val nrp: String = "",
    val role: String = "Danru Plot BPS",
    val activeShift: String = "Shift 1",
    val onlineStatus: String = "offline",
    val lastSeenAt: Timestamp? = null,
    val joinedAt: Timestamp? = null
) {
    fun isCurrentlyOnline(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (onlineStatus != "online") return false
        val seenMillis = lastSeenAt?.toDate()?.time ?: return true
        return (nowMillis - seenMillis) <= 120_000L
    }
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = DEFAULT_ROOM_CODE,
    val senderName: String = "",
    val senderRole: String = "",
    val fromOfficer: Boolean = true,
    val time: String = "07:15",
    val text: String = "",
    val imageData: String? = null,
    val imageCaption: String? = null,
    val audioType: String = "none", // "none", "recorded", "simulated"
    val audioData: String? = null,
    val audioDuration: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolvedHour(): Int? {
        val fromTime = time.trim().substringBefore(":").toIntOrNull()
        if (fromTime != null && fromTime in 0..23) return fromTime
        val date = createdAt?.toDate() ?: return null
        return Calendar.getInstance().apply { this.time = date }.get(Calendar.HOUR_OF_DAY)
    }

    fun resolvedYearMonthDay(): Triple<Int, Int, Int>? {
        val date = createdAt?.toDate() ?: return null
        val cal = Calendar.getInstance().apply { this.time = date }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formattedDateBadge(): String {
        val ymd = resolvedYearMonthDay() ?: return ""
        val monthName = IndonesianMonths.shortNames.getOrElse(ymd.second - 1) { ymd.second.toString() }
        return "${ymd.third.toString().padStart(2, '0')} $monthName ${ymd.first}"
    }
}

data class TimeKeywordFilter(
    val keyword: String = "",
    val hour: Int? = null,
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null
) {
    val isActive: Boolean
        get() = keyword.isNotBlank() || hour != null || day != null || month != null || year != null

    fun matchesReport(report: SecurityReport): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val matchesKeyword = report.description.lowercase(Locale.getDefault()).contains(q) ||
                report.officerName.lowercase(Locale.getDefault()).contains(q) ||
                report.officerNrp.lowercase(Locale.getDefault()).contains(q) ||
                report.status.lowercase(Locale.getDefault()).contains(q) ||
                report.shift.lowercase(Locale.getDefault()).contains(q) ||
                report.time.lowercase(Locale.getDefault()).contains(q) ||
                report.reportDate.lowercase(Locale.getDefault()).contains(q) ||
                report.photoCaptions.any { it.lowercase(Locale.getDefault()).contains(q) }
            if (!matchesKeyword) return false
        }
        if (hour != null && report.resolvedHour() != hour) return false
        if (day != null || month != null || year != null) {
            val ymd = report.resolvedYearMonthDay() ?: return false
            if (year != null && ymd.first != year) return false
            if (month != null && ymd.second != month) return false
            if (day != null && ymd.third != day) return false
        }
        return true
    }

    fun matchesChatMessage(msg: ChatMessage): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val matchesKeyword = msg.text.lowercase(Locale.getDefault()).contains(q) ||
                msg.senderName.lowercase(Locale.getDefault()).contains(q) ||
                msg.senderRole.lowercase(Locale.getDefault()).contains(q) ||
                (msg.imageCaption?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                msg.time.lowercase(Locale.getDefault()).contains(q)
            if (!matchesKeyword) return false
        }
        if (hour != null && msg.resolvedHour() != hour) return false
        if (day != null || month != null || year != null) {
            val ymd = msg.resolvedYearMonthDay() ?: return false
            if (year != null && ymd.first != year) return false
            if (month != null && ymd.second != month) return false
            if (day != null && ymd.third != day) return false
        }
        return true
    }

    fun matchesActivityRow(row: ActivityTableRow, sheetDate: String): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val matchesKeyword = row.jam.lowercase(Locale.getDefault()).contains(q) ||
                row.uraian.lowercase(Locale.getDefault()).contains(q) ||
                sheetDate.lowercase(Locale.getDefault()).contains(q)
            if (!matchesKeyword) return false
        }
        if (hour != null) {
            val rowHour = row.resolvedHour() ?: return false
            if (rowHour != hour) return false
        }
        if (day != null || month != null || year != null) {
            val ymd = parseIsoDateTriple(sheetDate) ?: return false
            if (year != null && ymd.first != year) return false
            if (month != null && ymd.second != month) return false
            if (day != null && ymd.third != day) return false
        }
        return true
    }

    fun matchesSheetHeader(sheet: DailyActivitySheet): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val inMeta = sheet.docNumber.lowercase(Locale.getDefault()).contains(q) ||
                sheet.sheetDate.lowercase(Locale.getDefault()).contains(q) ||
                sheet.shift.lowercase(Locale.getDefault()).contains(q) ||
                sheet.officerName.lowercase(Locale.getDefault()).contains(q)
            val inPersonnel = sheet.personnelRows.any {
                it.nama.lowercase(Locale.getDefault()).contains(q) ||
                    it.jabatan.lowercase(Locale.getDefault()).contains(q) ||
                    it.plot.lowercase(Locale.getDefault()).contains(q) ||
                    it.kehadiran.lowercase(Locale.getDefault()).contains(q) ||
                    it.ket.lowercase(Locale.getDefault()).contains(q)
            }
            val inInventory = sheet.inventoryRows.any {
                it.inventaris.lowercase(Locale.getDefault()).contains(q) ||
                    it.kondisi.lowercase(Locale.getDefault()).contains(q) ||
                    it.keterangan.lowercase(Locale.getDefault()).contains(q)
            }
            val inActivities = sheet.activityRows.any {
                it.jam.lowercase(Locale.getDefault()).contains(q) ||
                    it.uraian.lowercase(Locale.getDefault()).contains(q)
            }
            if (!inMeta && !inPersonnel && !inInventory && !inActivities) return false
        }
        if (day != null || month != null || year != null) {
            val ymd = parseIsoDateTriple(sheet.sheetDate) ?: return false
            if (year != null && ymd.first != year) return false
            if (month != null && ymd.second != month) return false
            if (day != null && ymd.third != day) return false
        }
        if (hour != null) {
            if (sheet.activityRows.none { it.resolvedHour() == hour }) return false
        }
        return true
    }
}

@Keep
@IgnoreExtraProperties
data class PersonnelRow(
    val nama: String = "",
    val jabatan: String = "ANGGOTA",
    val plot: String = "BPS",
    val kehadiran: String = "Hadir",
    val ket: String = ""
)

@Keep
@IgnoreExtraProperties
data class InventoryRow(
    val inventaris: String = "",
    val jumlah: String = "1",
    val kondisi: String = "Baik",
    val keterangan: String = "Lengkap"
)

@Keep
@IgnoreExtraProperties
data class ActivityTableRow(
    val jam: String = "",
    val uraian: String = ""
) {
    fun resolvedHour(): Int? {
        val cleaned = jam.trim().replace(".", ":")
        val h = cleaned.substringBefore(":").toIntOrNull()
        return if (h != null && h in 0..23) h else null
    }
}

@Keep
@IgnoreExtraProperties
data class DailyActivitySheet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val docNumber: String = "",
    val sheetDate: String = currentIsoDate(),
    val shift: String = ShiftOption.SHIFT_1.label,
    val personnelRows: List<PersonnelRow> = emptyList(),
    val inventoryRows: List<InventoryRow> = emptyList(),
    val activityRows: List<ActivityTableRow> = emptyList(),
    val officerName: String = "",
    val officerNrp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun formattedDisplayDate(): String {
        val ymd = parseIsoDateTriple(sheetDate) ?: return sheetDate
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(sheetDate)
            if (parsed != null) {
                SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("id", "ID")).format(parsed)
            } else {
                "${ymd.third.toString().padStart(2, '0')}/${ymd.second.toString().padStart(2, '0')}/${ymd.first}"
            }
        } catch (_: Exception) {
            sheetDate
        }
    }
}

object ActivitySheetTemplates {
    val defaultInventoryRows: List<InventoryRow> = listOf(
        InventoryRow("B. Jurnal * B. Tamu * B. Inventaris", "1 * 1 * 1", "Baik", "Lengkap"),
        InventoryRow("HT * Charger * Senter", "6 * 4 * 1", "Baik", "Lengkap"),
        InventoryRow("Kipas Angin * Jam Dinding", "1 * 1", "Baik", "Lengkap"),
        InventoryRow("Telepon * Hp Call Center", "1 * 1", "Baik", "Lengkap"),
        InventoryRow("Pompa Air * Pemanas Air", "1 * 1", "Baik", "Lengkap"),
        InventoryRow("Galon Air * Appar * Cermin", "2 * 1 * 1", "Baik", "Lengkap"),
        InventoryRow("Lampu Lalin * Rompi Lalin", "2 * 5", "Baik", "Lengkap"),
        InventoryRow("Lampu Rotari * Rambu Stop", "3 * 2", "Baik", "Lengkap"),
        InventoryRow("Sapu * Pel Lantai", "1 * 1", "Baik", "Lengkap"),
        InventoryRow("Papan Tulis * Meja * Kursi", "1 * 1 * 7", "Baik", "Lengkap"),
        InventoryRow("Sepatu Boot * Mantel / Jas Hujan", "7 * 1", "Baik", "Lengkap"),
        InventoryRow("Bolpoin * Penggaris", "1 * 1", "Baik", "Lengkap")
    )

    fun starterPersonnelRows(officerName: String): List<PersonnelRow> = listOf(
        PersonnelRow(officerName.ifBlank { "DANRU JAGA" }, "DANRU", "B", "Hadir", ""),
        PersonnelRow("KA. SHIFT", "KA. SHIF", "P", "Hadir", ""),
        PersonnelRow("ANGGOTA 1", "ANGGOTA", "S", "Hadir", ""),
        PersonnelRow("ANGGOTA 2", "ANGGOTA", "S", "Hadir", "")
    )

    fun starterActivityRows(shift: String): List<ActivityTableRow> {
        val startJam = when (shift) {
            ShiftOption.SHIFT_2.label -> "15.00"
            ShiftOption.SHIFT_3.label -> "23.00"
            else -> "07.00"
        }
        return listOf(
            ActivityTableRow(
                jam = startJam,
                uraian = "Menerima tugas dan tanggung jawab $shift dalam keadaan aman. Barang inventaris lengkap siap pakai."
            ),
            ActivityTableRow(
                jam = if (shift == ShiftOption.SHIFT_2.label) "15.30" else if (shift == ShiftOption.SHIFT_3.label) "23.30" else "07.30",
                uraian = "Petugas jaga melaksanakan standby di Portal utama BPS. Situasi terpantau aman kondusif."
            )
        )
    }
}

fun parseIsoDateTriple(isoDate: String): Triple<Int, Int, Int>? {
    val parts = isoDate.trim().split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..31) return null
    return Triple(y, m, d)
}

object IndonesianMonths {
    val fullNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val shortNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )
}

fun currentIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
