package com.example

import com.example.data.model.PjuChecklistSheet
import com.example.data.model.PjuClusterRow
import com.example.data.model.PjuComplianceLevel
import com.example.data.model.PjuIssueEntry
import com.example.data.model.PjuLampStatus
import com.example.data.model.PjuTemplates
import com.example.data.model.TimeKeywordFilter
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PjuChecklistUnitTest {

    @Test
    fun `defaultClusterNames matches all 9 physical form clusters`() {
        assertEquals(
            listOf(
                "PJU KRISAN",
                "PJU CEMPAKA",
                "PJU CENDANA",
                "PJU CEMARA",
                "PJU FLAMBOYAN 1",
                "PJU FLAMBOYAN 2",
                "PJU FLAMBOYAN FASUM",
                "PJU AKASIA",
                "RUKO"
            ),
            PjuTemplates.defaultClusterNames
        )
    }

    @Test
    fun `clusterRow slotsForDay and cycleSlot roundtrip accurately`() {
        var row = PjuClusterRow(clusterName = "PJU KRISAN")
        assertEquals(
            listOf(PjuLampStatus.NONE, PjuLampStatus.NONE, PjuLampStatus.NONE, PjuLampStatus.NONE),
            row.slotsForDay(16)
        )
        row = row.withDaySlots(
            16,
            listOf(PjuLampStatus.BAIK, PjuLampStatus.BAIK, PjuLampStatus.REDUP, PjuLampStatus.BAIK)
        )
        assertEquals("✓|✓|!|✓", row.checks["16"])
        assertEquals(PjuLampStatus.REDUP, row.daySummaryStatus(16))

        row = row.cycleSlot(16, 2) // REDUP -> MATI
        assertEquals(PjuLampStatus.MATI, row.slotsForDay(16)[2])
        assertEquals(PjuLampStatus.MATI, row.daySummaryStatus(16))
    }

    @Test
    fun `complianceInfo evaluates 2-day rule accurately`() {
        val baseSheet = PjuChecklistSheet(
            sheetMonth = 11L,
            sheetYear = 2026L,
            periodStartDay = 16L,
            periodEndDay = 30L,
            clusters = PjuTemplates.defaultClusterRows()
        )
        val today20 = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 20)
        }

        assertEquals(PjuComplianceLevel.EMPTY, baseSheet.complianceInfo(today20).level)

        val checked19 = baseSheet.copy(
            clusters = baseSheet.clusters.mapIndexed { idx, row ->
                if (idx == 0) row.withDaySlots(19, List(4) { PjuLampStatus.BAIK }) else row
            }
        )
        assertEquals(PjuComplianceLevel.COMPLIANT, checked19.complianceInfo(today20).level)

        val checked18 = baseSheet.copy(
            clusters = baseSheet.clusters.mapIndexed { idx, row ->
                if (idx == 0) row.withDaySlots(18, List(4) { PjuLampStatus.BAIK }) else row
            }
        )
        assertEquals(PjuComplianceLevel.DUE_SOON, checked18.complianceInfo(today20).level)

        val checked16 = baseSheet.copy(
            clusters = baseSheet.clusters.mapIndexed { idx, row ->
                if (idx == 0) row.withDaySlots(16, List(4) { PjuLampStatus.BAIK }) else row
            }
        )
        val overdue = checked16.complianceInfo(today20)
        assertEquals(PjuComplianceLevel.OVERDUE, overdue.level)
        assertEquals(4, overdue.daysGap)
    }

    @Test
    fun `clusterHealthPercent and countSlotsByStatus aggregate sheet data`() {
        val row1 = PjuClusterRow("PJU KRISAN").withDaySlots(
            16,
            listOf(PjuLampStatus.BAIK, PjuLampStatus.BAIK, PjuLampStatus.BAIK, PjuLampStatus.MATI)
        )
        val row2 = PjuClusterRow("RUKO")
        val sheet = PjuChecklistSheet(
            sheetMonth = 11L,
            sheetYear = 2026L,
            periodStartDay = 16L,
            periodEndDay = 17L,
            clusters = listOf(row1, row2)
        )
        assertEquals(75, sheet.clusterHealthPercent(row1))
        assertNull(sheet.clusterHealthPercent(row2))

        val counts = sheet.countSlotsByStatus()
        assertEquals(3, counts[PjuLampStatus.BAIK])
        assertEquals(1, counts[PjuLampStatus.MATI])
        assertEquals(0, counts[PjuLampStatus.REDUP])
    }

    @Test
    fun `timeKeywordFilter filters PJU sheet by keyword month year and day`() {
        val sheet = PjuChecklistSheet(
            monthYear = "November 2026",
            sheetMonth = 11L,
            sheetYear = 2026L,
            periodStartDay = 16L,
            periodEndDay = 30L,
            clusters = listOf(PjuClusterRow("PJU CEMPAKA")),
            issues = listOf(PjuIssueEntry(cluster = "PJU CEMPAKA", keluhan = "Timer rusak"))
        )
        assertTrue(TimeKeywordFilter(keyword = "cempaka", month = 11, year = 2026, day = 20).matchesPjuSheet(sheet))
        assertTrue(TimeKeywordFilter(keyword = "timer").matchesPjuSheet(sheet))
        assertFalse(TimeKeywordFilter(month = 10).matchesPjuSheet(sheet))
        assertFalse(TimeKeywordFilter(day = 5).matchesPjuSheet(sheet))
    }
}
