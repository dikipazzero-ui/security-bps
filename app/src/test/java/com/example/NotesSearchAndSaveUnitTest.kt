package com.example

import com.example.data.model.ActivityTableRow
import com.example.data.model.AttendanceMemberRow
import com.example.data.model.AttendanceSheet
import com.example.data.model.AttendanceStatus
import com.example.data.model.ChatMessage
import com.example.data.model.DailyActivitySheet
import com.example.data.model.NoteIndexBuilder
import com.example.data.model.NoteSourceTab
import com.example.data.model.PersonnelRow
import com.example.data.model.PjuChecklistSheet
import com.example.data.model.PjuIssueEntry
import com.example.data.model.QuickNote
import com.example.data.model.SecurityReport
import com.example.data.model.ShiftOption
import com.example.data.model.TimeKeywordFilter
import com.example.data.model.inferShiftFromHour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesSearchAndSaveUnitTest {

    @Test
    fun buildUnifiedNotes_indexesAllSixSourcesWithTimeAndShift() {
        val report = SecurityReport(
            id = "r1",
            category = 2L,
            time = "08:15",
            reportDate = "2026-11-18",
            shift = ShiftOption.SHIFT_1.label,
            status = "Aman & Terkendali",
            description = "Patroli ruang server terkendali",
            photoCaptions = listOf("Kunci server tersegel"),
            officerName = "Hendra"
        )
        val tableSheet = DailyActivitySheet(
            id = "t1",
            docNumber = "014/BPS/2026",
            sheetDate = "2026-11-18",
            shift = ShiftOption.SHIFT_2.label,
            personnelRows = listOf(
                PersonnelRow(nama = "Budi", kehadiran = "Hadir", ket = "Serah terima HT aman")
            ),
            activityRows = listOf(
                ActivityTableRow(jam = "16.30", uraian = "Pengecekan portal belakang")
            ),
            officerName = "Budi"
        )
        val attendanceSheet = AttendanceSheet(
            id = "a1",
            sheetDate = "2026-11-18",
            shift = ShiftOption.SHIFT_3.label,
            periodLabel = "Regu Malam",
            commanderName = "Andi",
            rows = listOf(
                AttendanceMemberRow(
                    nama = "Andi",
                    status = AttendanceStatus.HADIR.code,
                    jamMasuk = "23.00",
                    recordedAt = "23:02"
                ),
                AttendanceMemberRow(
                    nama = "Siti",
                    status = AttendanceStatus.IZIN.code,
                    jamMasuk = "23.00",
                    recordedAt = "23:02",
                    keterangan = "Izin keluarga"
                )
            )
        )
        val pjuSheet = PjuChecklistSheet(
            id = "p1",
            monthYear = "November 2026",
            sheetMonth = 11L,
            sheetYear = 2026L,
            periodStartDay = 16L,
            periodEndDay = 30L,
            notes = "Lampu Krisan normal semua",
            issues = listOf(
                PjuIssueEntry(
                    tanggal = "2026-11-18",
                    cluster = "PJU FLAMBOYAN 1",
                    titikLampu = "Lampu 2",
                    keluhan = "Kabel kendor",
                    tindakLanjut = "Sudah dilaporkan teknisi",
                    status = "Dalam Perbaikan"
                )
            ),
            lastShift = ShiftOption.SHIFT_2.label,
            lastUpdatedTime = "19:45"
        )
        val chatMessage = ChatMessage(
            id = "c1",
            roomCode = "PLOT-BPS",
            senderName = "Danru",
            senderRole = "Danru Plot BPS",
            time = "20:10",
            text = "Tamu dinas keluar gerbang",
            shift = ShiftOption.SHIFT_2.label
        )
        val quickNote = QuickNote(
            id = "q1",
            title = "Catatan Kunci Gudang",
            content = "Anak kunci cadangan ditaruh di kotak besi",
            sourceTab = NoteSourceTab.CATATAN.name,
            noteDate = "2026-11-18",
            noteTime = "09:20",
            shift = ShiftOption.SHIFT_1.label,
            officerName = "Hendra"
        )

        val notes = NoteIndexBuilder.buildUnifiedNotes(
            reports = listOf(report),
            activitySheets = listOf(tableSheet),
            attendanceSheets = listOf(attendanceSheet),
            pjuSheets = listOf(pjuSheet),
            chatMessages = listOf(chatMessage),
            quickNotes = listOf(quickNote)
        )

        val sources = notes.map { it.source }.toSet()
        assertTrue(NoteSourceTab.LAPORAN in sources)
        assertTrue(NoteSourceTab.TABEL in sources)
        assertTrue(NoteSourceTab.ABSENSI in sources)
        assertTrue(NoteSourceTab.PJU in sources)
        assertTrue(NoteSourceTab.CHAT in sources)
        assertTrue(NoteSourceTab.CATATAN in sources)

        val pjuNote = notes.first { it.id == "pju_note_p1" }
        assertEquals("Shift 2", pjuNote.shift)
        assertEquals("19:45", pjuNote.timeWib)
        assertEquals(19, pjuNote.hour)

        val chatNote = notes.first { it.id == "chat_c1" }
        assertEquals("Shift 2", chatNote.shift)
        assertEquals("20:10", chatNote.timeWib)
    }

    @Test
    fun unifiedNoteMatches_filtersByKeywordShiftHourDateAndSource() {
        val pjuSheet = PjuChecklistSheet(
            id = "p1",
            monthYear = "November 2026",
            sheetMonth = 11L,
            sheetYear = 2026L,
            notes = "Cek gardu utama Cendana",
            lastShift = ShiftOption.SHIFT_3.label,
            lastUpdatedTime = "23:15"
        )
        val report = SecurityReport(
            id = "r1",
            time = "08:00",
            reportDate = "2026-11-18",
            shift = ShiftOption.SHIFT_1.label,
            description = "Cek gardu listrik lobi"
        )

        val notes = NoteIndexBuilder.buildUnifiedNotes(
            reports = listOf(report),
            activitySheets = emptyList(),
            attendanceSheets = emptyList(),
            pjuSheets = listOf(pjuSheet),
            chatMessages = emptyList(),
            quickNotes = emptyList()
        )

        // Keyword + Shift 3 matches only the PJU note
        val shift3Filter = TimeKeywordFilter(keyword = "gardu", shift = ShiftOption.SHIFT_3.label)
        val matchedShift3 = notes.filter { it.matches(shift3Filter) }
        assertEquals(1, matchedShift3.size)
        assertEquals(NoteSourceTab.PJU, matchedShift3.first().source)

        // Hour 23 + Month 11 + Source PJU
        val hour23Filter = TimeKeywordFilter(hour = 23, month = 11, year = 2026)
        assertTrue(notes.first { it.source == NoteSourceTab.PJU }.matches(hour23Filter, NoteSourceTab.PJU))
        assertFalse(notes.first { it.source == NoteSourceTab.LAPORAN }.matches(hour23Filter, NoteSourceTab.PJU))
    }

    @Test
    fun timeKeywordFilter_enforcesShiftAcrossTabMatchers() {
        val filterS2 = TimeKeywordFilter(shift = ShiftOption.SHIFT_2.label)

        val reportS1 = SecurityReport(id = "r1", shift = ShiftOption.SHIFT_1.label, description = "Patroli")
        val reportS2 = SecurityReport(id = "r2", shift = ShiftOption.SHIFT_2.label, description = "Patroli")
        assertFalse(filterS2.matchesReport(reportS1))
        assertTrue(filterS2.matchesReport(reportS2))

        val chatS2 = ChatMessage(id = "c1", time = "16:10", text = "Aman", shift = ShiftOption.SHIFT_2.label)
        val chatS3Inferred = ChatMessage(id = "c2", time = "01:20", text = "Patroli malam", shift = "")
        assertTrue(filterS2.matchesChatMessage(chatS2))
        assertFalse(filterS2.matchesChatMessage(chatS3Inferred))
        assertEquals(ShiftOption.SHIFT_3.label, chatS3Inferred.resolvedShift())

        val pjuS2 = PjuChecklistSheet(
            id = "p2",
            lastShift = ShiftOption.SHIFT_2.label,
            lastUpdatedTime = "18:00"
        )
        assertTrue(filterS2.matchesPjuSheet(pjuS2))
        assertEquals(18, pjuS2.resolvedHour())
        assertEquals(ShiftOption.SHIFT_1.label, inferShiftFromHour(8))
        assertEquals(ShiftOption.SHIFT_2.label, inferShiftFromHour(17))
        assertEquals(ShiftOption.SHIFT_3.label, inferShiftFromHour(2))
    }
}
