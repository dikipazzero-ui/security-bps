package com.example

import com.example.data.model.AttendanceTemplates
import com.example.data.model.ShiftOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceTemplateUnitTest {

    @Test
    fun `default shift template generates exactly 4 members with shift hours`() {
        val shift1Rows = AttendanceTemplates.defaultShiftMembers("Siswandi", ShiftOption.SHIFT_1.label)
        assertEquals(4, shift1Rows.size)
        assertEquals("Siswandi", shift1Rows[0].nama)
        assertEquals("DANRU", shift1Rows[0].jabatan)
        assertEquals("07.00", shift1Rows[0].jamMasuk)
        assertEquals("15.00", shift1Rows[0].jamPulang)

        val shift2Rows = AttendanceTemplates.defaultShiftMembers("Diki pazzero", ShiftOption.SHIFT_2.label)
        assertEquals(4, shift2Rows.size)
        assertEquals("15.00", shift2Rows[0].jamMasuk)
        assertEquals("23.00", shift2Rows[0].jamPulang)

        val shift3Rows = AttendanceTemplates.defaultShiftMembers("", ShiftOption.SHIFT_3.label)
        assertEquals(4, shift3Rows.size)
        assertEquals("DANRU PLOT BPS", shift3Rows[0].nama)
        assertEquals("23.00", shift3Rows[0].jamMasuk)
        assertEquals("07.00", shift3Rows[0].jamPulang)
    }

    @Test
    fun `placeholder detection distinguishes template placeholders from custom names`() {
        assertTrue(AttendanceTemplates.isTemplatePlaceholderName("Anggota Security 05"))
        assertTrue(AttendanceTemplates.isTemplatePlaceholderName("Anggota Security 15"))
        assertTrue(AttendanceTemplates.isTemplatePlaceholderName("KA. SHIFT"))
        assertFalse(AttendanceTemplates.isTemplatePlaceholderName("Diki pazzero"))
        assertFalse(AttendanceTemplates.isTemplatePlaceholderName("Siswandi"))
    }
}
