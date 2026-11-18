package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NoteIndexBuilder {
    fun buildUnifiedNotes(
        reports: List<SecurityReport>,
        activitySheets: List<DailyActivitySheet>,
        attendanceSheets: List<AttendanceSheet>,
        pjuSheets: List<PjuChecklistSheet>,
        chatMessages: List<ChatMessage>,
        quickNotes: List<QuickNote>,
        defaultOfficerName: String = "Petugas Security",
        defaultShift: String = ShiftOption.SHIFT_1.label
    ): List<UnifiedNoteItem> {
        val items = mutableListOf<UnifiedNoteItem>()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // 1. Standalone QuickNotes
        quickNotes.forEach { note ->
            val ymd = parseIsoDateTriple(note.noteDate) ?: dateToTriple(note.updatedAt?.toDate() ?: note.createdAt?.toDate())
            val timeStr = note.noteTime.ifBlank {
                note.updatedAt?.toDate()?.let { timeFmt.format(it) } ?: "00:00"
            }
            val hour = timeStr.trim().replace(".", ":").substringBefore(":").toIntOrNull()?.takeIf { it in 0..23 }
            val dateIso = note.noteDate.ifBlank { currentIsoDate() }
            val ts = note.updatedAt?.toDate()?.time ?: note.createdAt?.toDate()?.time ?: estimateMillis(ymd, hour)
            items.add(
                UnifiedNoteItem(
                    id = "qn_${note.id}",
                    source = NoteSourceTab.fromNameOrDefault(note.sourceTab),
                    title = note.title.ifBlank { "Catatan Tersimpan" },
                    content = note.content,
                    shift = note.shift.ifBlank { inferShiftFromHour(hour) },
                    timeWib = timeStr,
                    dateIso = dateIso,
                    dateDisplay = formatIsoDateBadge(dateIso),
                    hour = hour,
                    year = ymd?.first,
                    month = ymd?.second,
                    day = ymd?.third,
                    officerName = note.officerName.ifBlank { defaultOfficerName },
                    creatorUserId = note.userId,
                    quickNoteId = note.id,
                    sortTimestamp = ts
                )
            )
        }

        // 2. Daily Reports (Kategori 1-3 + Photo Captions)
        reports.forEach { r ->
            val ymd = r.resolvedYearMonthDay()
            val dateIso = if (r.reportDate.isNotBlank()) r.reportDate else ymd?.let {
                "${it.first}-${it.second.toString().padStart(2, '0')}-${it.third.toString().padStart(2, '0')}"
            }.orEmpty()
            val captionSummary = r.photoCaptions.filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" • ") { "Foto: $it" }
            val fullContent = listOfNotNull(r.description.takeIf { it.isNotBlank() }, captionSummary)
                .joinToString("\n")
            val ts = r.updatedAt?.toDate()?.time ?: r.createdAt?.toDate()?.time ?: estimateMillis(ymd, r.resolvedHour())
            items.add(
                UnifiedNoteItem(
                    id = "rep_${r.id}",
                    source = NoteSourceTab.LAPORAN,
                    title = "${r.categoryEnum.shortTitle} (${r.status})",
                    content = fullContent.ifBlank { "(Laporan foto tanpa deskripsi)" },
                    shift = r.shift.ifBlank { inferShiftFromHour(r.resolvedHour()) },
                    timeWib = r.time,
                    dateIso = dateIso,
                    dateDisplay = r.formattedDateBadge().ifBlank { formatIsoDateBadge(dateIso) },
                    hour = r.resolvedHour(),
                    year = ymd?.first,
                    month = ymd?.second,
                    day = ymd?.third,
                    officerName = r.officerName.ifBlank { defaultOfficerName },
                    sortTimestamp = ts
                )
            )
        }

        // 3. Daily Activity Table Rows (Tabel Jurnal & Kegiatan)
        activitySheets.forEach { sheet ->
            val ymd = parseIsoDateTriple(sheet.sheetDate)
            val sheetDateBadge = formatIsoDateBadge(sheet.sheetDate)
            val baseTs = sheet.updatedAt?.toDate()?.time ?: sheet.createdAt?.toDate()?.time ?: estimateMillis(ymd, null)
            sheet.activityRows.forEachIndexed { idx, row ->
                val hour = row.resolvedHour()
                items.add(
                    UnifiedNoteItem(
                        id = "act_${sheet.id}_$idx",
                        source = NoteSourceTab.TABEL,
                        title = "Uraian Kegiatan Tabel (${sheet.docNumber.ifBlank { "Jurnal" }})",
                        content = row.uraian,
                        shift = sheet.shift,
                        timeWib = row.jam.replace(".", ":"),
                        dateIso = sheet.sheetDate,
                        dateDisplay = sheetDateBadge,
                        hour = hour,
                        year = ymd?.first,
                        month = ymd?.second,
                        day = ymd?.third,
                        officerName = sheet.officerName.ifBlank { defaultOfficerName },
                        sheetId = sheet.id,
                        sortTimestamp = baseTs + idx
                    )
                )
            }
            val personnelNotes = sheet.personnelRows.filter { it.ket.isNotBlank() }
            if (personnelNotes.isNotEmpty()) {
                val summary = personnelNotes.joinToString("; ") { "${it.nama} (${it.kehadiran}): ${it.ket}" }
                items.add(
                    UnifiedNoteItem(
                        id = "act_pers_${sheet.id}",
                        source = NoteSourceTab.TABEL,
                        title = "Catatan Personel Tabel (${sheet.sheetDate})",
                        content = summary,
                        shift = sheet.shift,
                        timeWib = sheet.updatedAt?.toDate()?.let { timeFmt.format(it) }.orEmpty(),
                        dateIso = sheet.sheetDate,
                        dateDisplay = sheetDateBadge,
                        hour = sheet.activityRows.firstOrNull()?.resolvedHour(),
                        year = ymd?.first,
                        month = ymd?.second,
                        day = ymd?.third,
                        officerName = sheet.officerName.ifBlank { defaultOfficerName },
                        sheetId = sheet.id,
                        sortTimestamp = baseTs
                    )
                )
            }
            if (sheet.activityRows.isEmpty() && personnelNotes.isEmpty()) {
                val timeStr = sheet.updatedAt?.toDate()?.let { timeFmt.format(it) }.orEmpty()
                val hour = timeStr.trim().substringBefore(":").toIntOrNull()?.takeIf { it in 0..23 }
                items.add(
                    UnifiedNoteItem(
                        id = "act_sheet_${sheet.id}",
                        source = NoteSourceTab.TABEL,
                        title = "Rekap Tabel Jurnal (${sheet.docNumber.ifBlank { sheet.sheetDate }})",
                        content = "Personel: ${sheet.personnelRows.size} org • Inventaris: ${sheet.inventoryRows.size} item",
                        shift = sheet.shift,
                        timeWib = timeStr,
                        dateIso = sheet.sheetDate,
                        dateDisplay = sheetDateBadge,
                        hour = hour,
                        year = ymd?.first,
                        month = ymd?.second,
                        day = ymd?.third,
                        officerName = sheet.officerName.ifBlank { defaultOfficerName },
                        sheetId = sheet.id,
                        sortTimestamp = baseTs
                    )
                )
            }
        }

        // 4. 15-Member Attendance Sheets & Member Notes
        attendanceSheets.forEach { sheet ->
            val ymd = parseIsoDateTriple(sheet.sheetDate)
            val sheetDateBadge = formatIsoDateBadge(sheet.sheetDate)
            val baseTs = sheet.updatedAt?.toDate()?.time ?: sheet.createdAt?.toDate()?.time ?: estimateMillis(ymd, null)
            val hCount = sheet.rows.count { it.statusEnum == AttendanceStatus.HADIR }
            val nonHadir = sheet.rows.filter { it.statusEnum != AttendanceStatus.HADIR || it.keterangan.isNotBlank() }
            val detailStr = if (nonHadir.isEmpty()) {
                "Seluruh ${sheet.rows.size} anggota hadir lengkap pada ${sheet.periodLabel}."
            } else {
                nonHadir.joinToString(" • ") { row ->
                    val ketPart = if (row.keterangan.isNotBlank()) " (${row.keterangan})" else ""
                    "${row.nama} [${row.statusEnum.label}]$ketPart"
                }
            }
            val firstTime = sheet.rows.firstOrNull { it.recordedAt.isNotBlank() }?.recordedAt
                ?: sheet.rows.firstOrNull()?.jamMasuk?.replace(".", ":").orEmpty()
            val hour = firstTime.trim().replace(".", ":").substringBefore(":").toIntOrNull()?.takeIf { it in 0..23 }
            items.add(
                UnifiedNoteItem(
                    id = "att_${sheet.id}",
                    source = NoteSourceTab.ABSENSI,
                    title = "Absensi ${sheet.rows.size} Anggota (Hadir: $hCount/${sheet.rows.size})",
                    content = detailStr,
                    shift = sheet.shift,
                    timeWib = firstTime,
                    dateIso = sheet.sheetDate,
                    dateDisplay = sheetDateBadge,
                    hour = hour,
                    year = ymd?.first,
                    month = ymd?.second,
                    day = ymd?.third,
                    officerName = sheet.commanderName.ifBlank { defaultOfficerName },
                    sheetId = sheet.id,
                    sortTimestamp = baseTs
                )
            )
        }

        // 5. PJU Checklist Notes & Issues
        pjuSheets.forEach { sheet ->
            val pjuShift = sheet.resolvedShift().ifBlank { defaultShift }
            val pjuTime = sheet.resolvedUpdatedTime()
            val pjuHour = sheet.resolvedHour()
            val updatedDate = sheet.updatedAt?.toDate()
            val updatedIso = updatedDate?.let { isoFmt.format(it) }
                ?: "${sheet.sheetYear}-${sheet.sheetMonth.toString().padStart(2, '0')}-${sheet.periodStartDay.toString().padStart(2, '0')}"
            val baseTs = updatedDate?.time ?: sheet.createdAt?.toDate()?.time
                ?: estimateMillis(Triple(sheet.sheetYear.toInt(), sheet.sheetMonth.toInt(), sheet.periodStartDay.toInt()), pjuHour)

            val checkedDays = sheet.checkedDays()
            val slotCounts = sheet.countSlotsByStatus()
            val baikCount = slotCounts[PjuLampStatus.BAIK] ?: 0
            val redupCount = slotCounts[PjuLampStatus.REDUP] ?: 0
            val matiCount = slotCounts[PjuLampStatus.MATI] ?: 0
            val hasChecklistOrNotes = sheet.notes.isNotBlank() ||
                checkedDays.isNotEmpty() ||
                sheet.lastShift.isNotBlank() ||
                sheet.lastUpdatedTime.isNotBlank()

            if (hasChecklistOrNotes) {
                val ymd = updatedDate?.let { dateToTriple(it) }
                    ?: Triple(sheet.sheetYear.toInt(), sheet.sheetMonth.toInt(), sheet.latestCheckedDay() ?: sheet.periodStartDay.toInt())
                val statusSummary = "Kondisi PJU: ✓ Nyala $baikCount • ! Redup $redupCount • ✕ Mati $matiCount"
                val daysSummary = if (checkedDays.isNotEmpty()) "Tgl dicek: ${checkedDays.joinToString(", ")}" else null
                val checkedClusterNames = sheet.clusters
                    .filter { row -> checkedDays.any { d -> row.hasAnyCheckOnDay(d) } }
                    .map { it.clusterName }
                    .filter { it.isNotBlank() }
                val clustersSummary = checkedClusterNames.takeIf { it.isNotEmpty() }
                    ?.let { "Cluster: ${it.joinToString(", ")}" }
                val notesPart = sheet.notes.takeIf { it.isNotBlank() }?.let { "Catatan: $it" }
                val combinedContent = listOfNotNull(notesPart, statusSummary, daysSummary, clustersSummary).joinToString("\n")
                items.add(
                    UnifiedNoteItem(
                        id = "pju_note_${sheet.id}",
                        source = NoteSourceTab.PJU,
                        title = "Rekap Checklist PJU (${sheet.monthYear})",
                        content = combinedContent,
                        shift = pjuShift,
                        timeWib = pjuTime,
                        dateIso = updatedIso,
                        dateDisplay = "${sheet.monthYear} (${formatIsoDateBadge(updatedIso)})",
                        hour = pjuHour,
                        year = ymd.first,
                        month = ymd.second,
                        day = ymd.third,
                        officerName = sheet.responsibleNames,
                        sheetId = sheet.id,
                        sortTimestamp = baseTs
                    )
                )
            }

            sheet.issues.forEachIndexed { idx, issue ->
                val ymd = parseIsoDateTriple(issue.tanggal)
                    ?: Triple(sheet.sheetYear.toInt(), sheet.sheetMonth.toInt(), sheet.periodStartDay.toInt())
                val tl = if (issue.tindakLanjut.isNotBlank()) " | Tindak Lanjut: ${issue.tindakLanjut}" else ""
                items.add(
                    UnifiedNoteItem(
                        id = "pju_issue_${sheet.id}_$idx",
                        source = NoteSourceTab.PJU,
                        title = "Masalah PJU: ${issue.cluster} (${issue.titikLampu}) • ${issue.status}",
                        content = "${issue.keluhan}$tl",
                        shift = pjuShift,
                        timeWib = pjuTime,
                        dateIso = issue.tanggal,
                        dateDisplay = formatIsoDateBadge(issue.tanggal),
                        hour = pjuHour,
                        year = ymd.first,
                        month = ymd.second,
                        day = ymd.third,
                        officerName = sheet.responsibleNames,
                        sheetId = sheet.id,
                        sortTimestamp = baseTs + idx + 1
                    )
                )
            }
        }

        // 6. Live Chat Messages
        chatMessages.forEach { msg ->
            val ymd = msg.resolvedYearMonthDay()
            val dateIso = ymd?.let {
                "${it.first}-${it.second.toString().padStart(2, '0')}-${it.third.toString().padStart(2, '0')}"
            }.orEmpty()
            val body = listOfNotNull(
                msg.text.takeIf { it.isNotBlank() },
                msg.imageCaption?.takeIf { it.isNotBlank() }?.let { "Ket. Foto: $it" },
                if (msg.audioType != "none") "Voice Note (${msg.audioDuration ?: "00:05"})" else null
            ).joinToString(" • ")
            if (body.isNotBlank()) {
                val ts = msg.createdAt?.toDate()?.time ?: estimateMillis(ymd, msg.resolvedHour())
                items.add(
                    UnifiedNoteItem(
                        id = "chat_${msg.id.ifBlank { "${msg.senderName}_${msg.time}_${msg.text.hashCode()}" }}",
                        source = NoteSourceTab.CHAT,
                        title = "Pesan Ruang ${msg.roomCode} • ${msg.senderRole}",
                        content = body,
                        shift = msg.resolvedShift(),
                        timeWib = msg.time,
                        dateIso = dateIso,
                        dateDisplay = msg.formattedDateBadge().ifBlank { "Live Chat" },
                        hour = msg.resolvedHour(),
                        year = ymd?.first,
                        month = ymd?.second,
                        day = ymd?.third,
                        officerName = msg.senderName,
                        sortTimestamp = ts
                    )
                )
            }
        }

        return items.sortedByDescending { it.sortTimestamp }
    }

    private fun dateToTriple(date: Date?): Triple<Int, Int, Int>? {
        val d = date ?: return null
        val cal = Calendar.getInstance().apply { time = d }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun estimateMillis(ymd: Triple<Int, Int, Int>?, hour: Int?): Long {
        if (ymd == null) return 0L
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, ymd.first)
            set(Calendar.MONTH, (ymd.second - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, ymd.third.coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, hour ?: 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }
}
