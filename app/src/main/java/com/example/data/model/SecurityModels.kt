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
    val photoData: String = "",
    val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SecurityReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = DEFAULT_ROOM_CODE,
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
    val photoData: String = "",
    val lastSeenAt: Timestamp? = null,
    val joinedAt: Timestamp? = null
) {
    fun isCurrentlyOnline(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (onlineStatus != "online") return false
        val seenMillis = lastSeenAt?.toDate()?.time ?: return true
        return (nowMillis - seenMillis) <= 120_000L
    }

    fun lastSeenMillis(): Long? = lastSeenAt?.toDate()?.time ?: joinedAt?.toDate()?.time

    fun lastSeenDisplay(nowMillis: Long = System.currentTimeMillis()): String {
        if (isCurrentlyOnline(nowMillis)) return "Sedang aktif sekarang"
        val seenMillis = lastSeenMillis() ?: return "Terakhir online belum tercatat"

        val seenDate = Date(seenMillis)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(seenDate)
        val diffMillis = (nowMillis - seenMillis).coerceAtLeast(0L)

        if (diffMillis < 60_000L) {
            return "Terakhir online baru saja ($timeStr)"
        }
        if (diffMillis < 3_600_000L) {
            val mins = (diffMillis / 60_000L).coerceAtLeast(1L)
            return "Terakhir online $mins mnt lalu ($timeStr)"
        }

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val seenCal = Calendar.getInstance().apply { timeInMillis = seenMillis }
        if (nowCal.get(Calendar.YEAR) == seenCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == seenCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Terakhir online hari ini, $timeStr WIB"
        }

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        if (yesterdayCal.get(Calendar.YEAR) == seenCal.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == seenCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Terakhir online kemarin, $timeStr WIB"
        }

        val day = seenCal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val monthShort = IndonesianMonths.shortNames.getOrElse(seenCal.get(Calendar.MONTH)) { "" }
        val year = seenCal.get(Calendar.YEAR)
        return "Terakhir online $day $monthShort $year, $timeStr WIB"
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
    val senderPhoto: String? = null,
    val edited: Boolean = false,
    val shift: String = "",
    val updatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolvedHour(): Int? {
        val fromTime = time.trim().substringBefore(":").toIntOrNull()
        if (fromTime != null && fromTime in 0..23) return fromTime
        val date = createdAt?.toDate() ?: return null
        return Calendar.getInstance().apply { this.time = date }.get(Calendar.HOUR_OF_DAY)
    }

    fun resolvedShift(): String {
        if (shift in ShiftOption.entries.map { it.label }) return shift
        return inferShiftFromHour(resolvedHour())
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
    val year: Int? = null,
    val shift: String? = null
) {
    val isActive: Boolean
        get() = keyword.isNotBlank() || hour != null || day != null || month != null || year != null || shift != null

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
        if (shift != null && report.shift != shift) return false
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
                msg.time.lowercase(Locale.getDefault()).contains(q) ||
                msg.resolvedShift().lowercase(Locale.getDefault()).contains(q)
            if (!matchesKeyword) return false
        }
        if (shift != null && msg.resolvedShift() != shift) return false
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
        if (shift != null && sheet.shift != shift) return false
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

    fun matchesAttendanceSheet(sheet: AttendanceSheet): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val inHeader = sheet.sheetDate.lowercase(Locale.getDefault()).contains(q) ||
                sheet.shift.lowercase(Locale.getDefault()).contains(q) ||
                sheet.periodLabel.lowercase(Locale.getDefault()).contains(q) ||
                sheet.commanderName.lowercase(Locale.getDefault()).contains(q)
            val inRows = sheet.rows.any { matchesAttendanceKeywordOnly(it, q) }
            if (!inHeader && !inRows) return false
        }
        if (shift != null && sheet.shift != shift) return false
        if (day != null || month != null || year != null) {
            val ymd = parseIsoDateTriple(sheet.sheetDate) ?: return false
            if (year != null && ymd.first != year) return false
            if (month != null && ymd.second != month) return false
            if (day != null && ymd.third != day) return false
        }
        if (hour != null) {
            val hourStr = hour.toString().padStart(2, '0')
            val matchesTime = sheet.rows.any {
                it.jamMasuk.startsWith(hourStr) || it.recordedAt.startsWith(hourStr)
            }
            if (!matchesTime) return false
        }
        return true
    }

    fun matchesAttendanceRow(row: AttendanceMemberRow, sheetDate: String): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty() && !matchesAttendanceKeywordOnly(row, q)) {
            return false
        }
        if (day != null || month != null || year != null) {
            val ymd = parseIsoDateTriple(sheetDate) ?: return false
            if (year != null && ymd.first != year) return false
            if (month != null && ymd.second != month) return false
            if (day != null && ymd.third != day) return false
        }
        if (hour != null) {
            val hourStr = hour.toString().padStart(2, '0')
            if (!row.jamMasuk.startsWith(hourStr) && !row.recordedAt.startsWith(hourStr)) return false
        }
        return true
    }

    private fun matchesAttendanceKeywordOnly(row: AttendanceMemberRow, q: String): Boolean {
        return row.nama.lowercase(Locale.getDefault()).contains(q) ||
            row.nrp.lowercase(Locale.getDefault()).contains(q) ||
            row.jabatan.lowercase(Locale.getDefault()).contains(q) ||
            row.plot.lowercase(Locale.getDefault()).contains(q) ||
            row.statusEnum.label.lowercase(Locale.getDefault()).contains(q) ||
            row.status.lowercase(Locale.getDefault()).contains(q) ||
            row.keterangan.lowercase(Locale.getDefault()).contains(q)
    }

    fun matchesPjuSheet(sheet: PjuChecklistSheet): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val inHeader = sheet.monthYear.lowercase(Locale.getDefault()).contains(q) ||
                sheet.responsibleNames.lowercase(Locale.getDefault()).contains(q) ||
                sheet.notes.lowercase(Locale.getDefault()).contains(q) ||
                sheet.lastShift.lowercase(Locale.getDefault()).contains(q) ||
                sheet.lastUpdatedTime.lowercase(Locale.getDefault()).contains(q)
            val inClusters = sheet.clusters.any {
                it.clusterName.lowercase(Locale.getDefault()).contains(q) ||
                    it.checks.values.any { v -> v.lowercase(Locale.getDefault()).contains(q) }
            }
            val inIssues = sheet.issues.any {
                it.cluster.lowercase(Locale.getDefault()).contains(q) ||
                    it.keluhan.lowercase(Locale.getDefault()).contains(q) ||
                    it.tindakLanjut.lowercase(Locale.getDefault()).contains(q) ||
                    it.status.lowercase(Locale.getDefault()).contains(q) ||
                    it.tanggal.lowercase(Locale.getDefault()).contains(q)
            }
            if (!inHeader && !inClusters && !inIssues) return false
        }
        if (shift != null && sheet.resolvedShift() != shift) return false
        if (hour != null) {
            val sheetHour = sheet.resolvedHour() ?: return false
            if (sheetHour != hour) return false
        }
        if (year != null && sheet.sheetYear.toInt() != year) return false
        if (month != null && sheet.sheetMonth.toInt() != month) return false
        if (day != null) {
            val d = day
            if (d < sheet.periodStartDay.toInt() || d > sheet.periodEndDay.toInt()) return false
        }
        return true
    }

    fun matchesPjuCluster(cluster: PjuClusterRow, sheet: PjuChecklistSheet): Boolean {
        val q = keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val matchesCluster = cluster.clusterName.lowercase(Locale.getDefault()).contains(q)
            val matchesRelatedIssue = sheet.issues.any {
                it.cluster.equals(cluster.clusterName, ignoreCase = true) &&
                    (it.keluhan.lowercase(Locale.getDefault()).contains(q) ||
                        it.tindakLanjut.lowercase(Locale.getDefault()).contains(q) ||
                        it.status.lowercase(Locale.getDefault()).contains(q))
            }
            if (!matchesCluster && !matchesRelatedIssue) return false
        }
        if (year != null && sheet.sheetYear.toInt() != year) return false
        if (month != null && sheet.sheetMonth.toInt() != month) return false
        if (day != null) {
            val d = day
            if (d < sheet.periodStartDay.toInt() || d > sheet.periodEndDay.toInt()) return false
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
    val roomCode: String = DEFAULT_ROOM_CODE,
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

enum class AttendanceStatus(val code: String, val label: String) {
    HADIR("H", "Hadir"),
    IZIN("I", "Izin"),
    SAKIT("S", "Sakit"),
    ALPHA("A", "Alpha"),
    LIBUR("L", "Libur");

    fun next(): AttendanceStatus {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromCode(code: String): AttendanceStatus {
            val clean = code.trim()
            return entries.firstOrNull {
                it.code.equals(clean, ignoreCase = true) || it.label.equals(clean, ignoreCase = true)
            } ?: HADIR
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AttendanceMemberRow(
    val nama: String = "",
    val nrp: String = "",
    val jabatan: String = "ANGGOTA",
    val plot: String = "BPS",
    val status: String = AttendanceStatus.HADIR.code,
    val jamMasuk: String = "07.00",
    val jamPulang: String = "15.00",
    val recordedAt: String = "",
    val keterangan: String = ""
) {
    val statusEnum: AttendanceStatus
        get() = AttendanceStatus.fromCode(status)
}

@Keep
@IgnoreExtraProperties
data class AttendanceSheet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = DEFAULT_ROOM_CODE,
    val sheetDate: String = currentIsoDate(),
    val shift: String = ShiftOption.SHIFT_1.label,
    val periodLabel: String = "Regu Plot BPS (4 Orang/Shift)",
    val commanderName: String = "Danru Plot BPS",
    val rows: List<AttendanceMemberRow> = emptyList(),
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

object AttendanceTemplates {
    const val MEMBERS_PER_SHIFT = 4

    fun shiftTimes(shift: String): Pair<String, String> = when (shift) {
        ShiftOption.SHIFT_2.label -> "15.00" to "23.00"
        ShiftOption.SHIFT_3.label -> "23.00" to "07.00"
        else -> "07.00" to "15.00"
    }

    fun isTemplatePlaceholderName(name: String): Boolean {
        val clean = name.trim()
        return clean.matches(Regex("^Anggota Security \\d{2}$")) ||
            clean.matches(Regex("^ANGGOTA \\d+$", RegexOption.IGNORE_CASE)) ||
            clean.equals("KA. SHIFT", ignoreCase = true)
    }

    fun defaultShiftMembers(commanderName: String, shift: String): List<AttendanceMemberRow> {
        val (masuk, pulang) = shiftTimes(shift)
        val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val firstCommander = commanderName.trim().ifBlank { "DANRU PLOT BPS" }
        val presets = listOf(
            "DANRU" to "Pos Utama",
            "KA. SHIFT" to "Lobi & Portal BPS",
            "ANGGOTA" to "Gerbang & Parkir",
            "ANGGOTA" to "Patroli & Pos Belakang"
        )
        return presets.mapIndexed { idx, (jabatan, plot) ->
            val num = (idx + 1).toString().padStart(2, '0')
            val nrpNum = (idx + 1).toString().padStart(3, '0')
            val name = if (idx == 0) firstCommander else "Anggota Security $num"
            AttendanceMemberRow(
                nama = name,
                nrp = "SEC-BPS-$nrpNum",
                jabatan = jabatan,
                plot = plot,
                status = AttendanceStatus.HADIR.code,
                jamMasuk = masuk,
                jamPulang = pulang,
                recordedAt = nowStr,
                keterangan = ""
            )
        }
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

enum class PjuLampStatus(val symbol: String, val shortLabel: String, val label: String) {
    NONE("-", "Kosong", "Belum Dicek"),
    BAIK("✓", "Nyala", "Nyala / Baik"),
    REDUP("!", "Redup", "Redup / Sebagian"),
    MATI("✕", "Mati", "Mati / Rusak");

    fun next(): PjuLampStatus {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromCode(raw: String): PjuLampStatus {
            val clean = raw.trim().uppercase(Locale.getDefault())
            return when {
                clean == "✓" || clean == "V" || clean == "B" || clean == "BAIK" || clean == "OK" -> BAIK
                clean == "!" || clean == "R" || clean == "REDUP" -> REDUP
                clean == "✕" || clean == "X" || clean == "M" || clean == "MATI" || clean == "RUSAK" -> MATI
                else -> NONE
            }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class PjuClusterRow(
    val clusterName: String = "",
    val checks: Map<String, String> = emptyMap()
) {
    fun slotsForDay(day: Int): List<PjuLampStatus> {
        val raw = checks[day.toString()].orEmpty()
        if (raw.isBlank()) return listOf(PjuLampStatus.NONE, PjuLampStatus.NONE, PjuLampStatus.NONE, PjuLampStatus.NONE)
        val parsed = raw.split("|").map { PjuLampStatus.fromCode(it) }
        return List(4) { idx -> parsed.getOrElse(idx) { PjuLampStatus.NONE } }
    }

    fun withDaySlots(day: Int, slots: List<PjuLampStatus>): PjuClusterRow {
        val normalized = List(4) { idx -> slots.getOrElse(idx) { PjuLampStatus.NONE } }
        val updated = checks.toMutableMap()
        if (normalized.all { it == PjuLampStatus.NONE }) {
            updated.remove(day.toString())
        } else {
            updated[day.toString()] = normalized.joinToString("|") { it.symbol }
        }
        return copy(checks = updated)
    }

    fun cycleSlot(day: Int, slotIndex: Int): PjuClusterRow {
        if (slotIndex !in 0..3) return this
        val current = slotsForDay(day).toMutableList()
        current[slotIndex] = current[slotIndex].next()
        return withDaySlots(day, current)
    }

    fun daySummaryStatus(day: Int): PjuLampStatus {
        val slots = slotsForDay(day)
        if (slots.all { it == PjuLampStatus.NONE }) return PjuLampStatus.NONE
        if (slots.any { it == PjuLampStatus.MATI }) return PjuLampStatus.MATI
        if (slots.any { it == PjuLampStatus.REDUP }) return PjuLampStatus.REDUP
        return PjuLampStatus.BAIK
    }

    fun hasAnyCheckOnDay(day: Int): Boolean =
        slotsForDay(day).any { it != PjuLampStatus.NONE }
}

@Keep
@IgnoreExtraProperties
data class PjuIssueEntry(
    val tanggal: String = currentIsoDate(),
    val cluster: String = "",
    val titikLampu: String = "Lampu 1",
    val keluhan: String = "",
    val tindakLanjut: String = "",
    val status: String = "Belum Ditangani"
)

enum class PjuComplianceLevel {
    COMPLIANT,
    DUE_SOON,
    OVERDUE,
    EMPTY
}

data class PjuComplianceInfo(
    val level: PjuComplianceLevel,
    val badgeText: String,
    val detailText: String,
    val latestCheckedDay: Int?,
    val daysGap: Int?
)

@Keep
@IgnoreExtraProperties
data class PjuChecklistSheet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = DEFAULT_ROOM_CODE,
    val monthYear: String = PjuTemplates.currentMonthYearLabel(),
    val sheetMonth: Long = Calendar.getInstance().get(Calendar.MONTH) + 1L,
    val sheetYear: Long = Calendar.getInstance().get(Calendar.YEAR).toLong(),
    val periodStartDay: Long = 16L,
    val periodEndDay: Long = 30L,
    val clusters: List<PjuClusterRow> = emptyList(),
    val issues: List<PjuIssueEntry> = emptyList(),
    val notes: String = "",
    val responsibleNames: String = PjuTemplates.DEFAULT_RESPONSIBLE,
    val lastShift: String = "",
    val lastUpdatedTime: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedShift(): String {
        if (lastShift in ShiftOption.entries.map { it.label }) return lastShift
        val h = resolvedHour()
        return if (h != null) inferShiftFromHour(h) else ""
    }

    fun resolvedUpdatedTime(): String {
        if (lastUpdatedTime.isNotBlank()) return lastUpdatedTime
        val dt = updatedAt?.toDate() ?: return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(dt)
    }

    fun resolvedHour(): Int? {
        val t = resolvedUpdatedTime().trim().replace(".", ":")
        val parsed = t.substringBefore(":").toIntOrNull()
        if (parsed != null && parsed in 0..23) return parsed
        val dt = updatedAt?.toDate() ?: return null
        return Calendar.getInstance().apply { time = dt }.get(Calendar.HOUR_OF_DAY)
    }

    fun daysList(): List<Int> {
        val start = periodStartDay.toInt().coerceIn(1, 31)
        val end = periodEndDay.toInt().coerceIn(start, 31)
        return (start..end).toList()
    }

    fun checkedDays(): List<Int> {
        return daysList().filter { day -> clusters.any { it.hasAnyCheckOnDay(day) } }
    }

    fun latestCheckedDay(): Int? = checkedDays().maxOrNull()

    fun countSlotsByStatus(): Map<PjuLampStatus, Int> {
        var baik = 0
        var redup = 0
        var mati = 0
        var empty = 0
        val days = daysList()
        clusters.forEach { cluster ->
            days.forEach { day ->
                cluster.slotsForDay(day).forEach { st ->
                    when (st) {
                        PjuLampStatus.BAIK -> baik++
                        PjuLampStatus.REDUP -> redup++
                        PjuLampStatus.MATI -> mati++
                        PjuLampStatus.NONE -> empty++
                    }
                }
            }
        }
        return mapOf(
            PjuLampStatus.BAIK to baik,
            PjuLampStatus.REDUP to redup,
            PjuLampStatus.MATI to mati,
            PjuLampStatus.NONE to empty
        )
    }

    fun clusterHealthPercent(cluster: PjuClusterRow): Int? {
        val checkedSlots = daysList().flatMap { cluster.slotsForDay(it) }.filter { it != PjuLampStatus.NONE }
        if (checkedSlots.isEmpty()) return null
        val okCount = checkedSlots.count { it == PjuLampStatus.BAIK }
        return ((okCount * 100f) / checkedSlots.size).toInt()
    }

    fun complianceInfo(todayCal: Calendar = Calendar.getInstance()): PjuComplianceInfo {
        val checked = checkedDays()
        if (checked.isEmpty()) {
            return PjuComplianceInfo(
                level = PjuComplianceLevel.EMPTY,
                badgeText = "Belum Dicek",
                detailText = "Wajib checklist semua PJU maksimal 2 hari sekali.",
                latestCheckedDay = null,
                daysGap = null
            )
        }
        val latest = checked.last()
        val nowMonth = todayCal.get(Calendar.MONTH) + 1
        val nowYear = todayCal.get(Calendar.YEAR)
        val nowDay = todayCal.get(Calendar.DAY_OF_MONTH)

        val gap = if (nowYear == sheetYear.toInt() && nowMonth == sheetMonth.toInt()) {
            (nowDay - latest).coerceAtLeast(0)
        } else {
            (periodEndDay.toInt() - latest).coerceAtLeast(0)
        }

        val maxInternalGap = checked.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0
        return when {
            gap > 2 -> PjuComplianceInfo(
                level = PjuComplianceLevel.OVERDUE,
                badgeText = "Telat Cek ($gap Hari)",
                detailText = "Checklist terakhir tgl $latest • Lewat batas maks 2 hari sekali! Segera cek PJU.",
                latestCheckedDay = latest,
                daysGap = gap
            )
            gap == 2 -> PjuComplianceInfo(
                level = PjuComplianceLevel.DUE_SOON,
                badgeText = "Jadwal Cek Hari Ini",
                detailText = "Checklist terakhir tgl $latest ($gap hari lalu). Waktunya cek rutin hari ini.",
                latestCheckedDay = latest,
                daysGap = gap
            )
            else -> {
                val intervalInfo = if (maxInternalGap > 2) " (riwayat ada jeda $maxInternalGap hr)" else ""
                PjuComplianceInfo(
                    level = PjuComplianceLevel.COMPLIANT,
                    badgeText = "Patuh ≤ 2 Hari",
                    detailText = "Checklist terakhir tgl $latest$intervalInfo • Jadwal terkendali.",
                    latestCheckedDay = latest,
                    daysGap = gap
                )
            }
        }
    }
}

object PjuTemplates {
    const val DEFAULT_RESPONSIBLE = "R. MOH. GANDI / NERI WELAS ANTORO"

    val defaultClusterNames = listOf(
        "PJU KRISAN",
        "PJU CEMPAKA",
        "PJU CENDANA",
        "PJU CEMARA",
        "PJU FLAMBOYAN 1",
        "PJU FLAMBOYAN 2",
        "PJU FLAMBOYAN FASUM",
        "PJU AKASIA",
        "RUKO"
    )

    val issueStatusOptions = listOf(
        "Belum Ditangani",
        "Dalam Perbaikan",
        "Selesai"
    )

    fun monthYearLabel(month: Int, year: Int): String {
        val monthName = IndonesianMonths.fullNames.getOrElse(month - 1) { "November" }
        return "$monthName $year"
    }

    fun currentMonthYearLabel(): String {
        val cal = Calendar.getInstance()
        return monthYearLabel(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    fun defaultClusterRows(): List<PjuClusterRow> =
        defaultClusterNames.map { name -> PjuClusterRow(clusterName = name, checks = emptyMap()) }

    fun defaultPeriodBounds(month: Int, year: Int, referenceDay: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH).toLong()
        return if (referenceDay >= 16) 16L to maxDay else 1L to 15L
    }
}

fun inferShiftFromHour(hour: Int?): String {
    if (hour == null) return ShiftOption.SHIFT_1.label
    return when (hour) {
        in 7..14 -> ShiftOption.SHIFT_1.label
        in 15..22 -> ShiftOption.SHIFT_2.label
        else -> ShiftOption.SHIFT_3.label
    }
}

fun formatIsoDateBadge(isoDate: String): String {
    val ymd = parseIsoDateTriple(isoDate) ?: return isoDate
    val monthName = IndonesianMonths.shortNames.getOrElse(ymd.second - 1) { ymd.second.toString() }
    return "${ymd.third.toString().padStart(2, '0')} $monthName ${ymd.first}"
}

enum class NoteSourceTab(val label: String, val badgeText: String) {
    LAPORAN("Laporan", "Laporan Harian"),
    TABEL("Tabel", "Tabel Jurnal"),
    ABSENSI("Absensi", "Absensi 4/Shift"),
    PJU("PJU", "Checklist PJU"),
    CHAT("Chat", "Live Chat"),
    CATATAN("Catatan", "Catatan Tersimpan");

    companion object {
        fun fromNameOrDefault(raw: String): NoteSourceTab =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: CATATAN
    }
}

@Keep
@IgnoreExtraProperties
data class QuickNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = DEFAULT_ROOM_CODE,
    val title: String = "",
    val content: String = "",
    val sourceTab: String = NoteSourceTab.CATATAN.name,
    val noteDate: String = currentIsoDate(),
    val noteTime: String = "",
    val shift: String = ShiftOption.SHIFT_1.label,
    val officerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class UnifiedNoteItem(
    val id: String,
    val source: NoteSourceTab,
    val title: String,
    val content: String,
    val shift: String,
    val timeWib: String,
    val dateIso: String,
    val dateDisplay: String,
    val hour: Int?,
    val year: Int?,
    val month: Int?,
    val day: Int?,
    val officerName: String,
    val creatorUserId: String = "",
    val sheetId: String? = null,
    val quickNoteId: String? = null,
    val sortTimestamp: Long = 0L
) {
    fun matches(filter: TimeKeywordFilter, sourceFilter: NoteSourceTab? = null): Boolean {
        if (sourceFilter != null && source != sourceFilter) return false
        if (filter.shift != null && shift != filter.shift) return false
        if (filter.hour != null && hour != filter.hour) return false
        if (filter.year != null && year != filter.year) return false
        if (filter.month != null && month != filter.month) return false
        if (filter.day != null && day != filter.day) return false

        val q = filter.keyword.trim().lowercase(Locale.getDefault())
        if (q.isNotEmpty()) {
            val haystack = listOf(
                title,
                content,
                shift,
                timeWib,
                dateIso,
                dateDisplay,
                officerName,
                source.label,
                source.badgeText
            ).joinToString(" ").lowercase(Locale.getDefault())
            if (!haystack.contains(q)) return false
        }
        return true
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
