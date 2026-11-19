package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DEFAULT_ROOM_CODE
import com.example.notifications.NotificationPreferences
import com.example.notifications.NotificationRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationPreferencesUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("security_bps_notification_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        NotificationRuntimeState.clearSeenForTesting()
    }

    @Test
    fun `background monitor defaults to enabled and persists across instances`() {
        val initial = NotificationPreferences(context)
        assertTrue(initial.settings.value.backgroundMonitorEnabled)
        assertTrue(initial.settings.value.floatingBubbleEnabled)

        initial.setBackgroundMonitorEnabled(false)
        assertFalse(initial.settings.value.backgroundMonitorEnabled)

        val reloaded = NotificationPreferences(context)
        assertFalse(reloaded.settings.value.backgroundMonitorEnabled)

        reloaded.setBackgroundMonitorEnabled(true)
        assertTrue(NotificationPreferences(context).settings.value.backgroundMonitorEnabled)
    }

    @Test
    fun `active context trims values and applies fallback defaults`() {
        val prefs = NotificationPreferences(context)

        prefs.saveActiveContext("  BPS-02  ", "  Budi Santoso  ")
        assertEquals("BPS-02", prefs.getSavedRoomCode())
        assertEquals("Budi Santoso", prefs.getSavedOfficerName())

        prefs.saveActiveContext("   ", " ")
        assertEquals(DEFAULT_ROOM_CODE, prefs.getSavedRoomCode())
        assertEquals("Petugas Security", prefs.getSavedOfficerName())
    }

    @Test
    fun `runtime state deduplicates non-blank alert ids`() {
        assertTrue(NotificationRuntimeState.shouldDispatchAlert("evt-1"))
        assertFalse(NotificationRuntimeState.shouldDispatchAlert("evt-1"))
        assertTrue(NotificationRuntimeState.shouldDispatchAlert("evt-2"))

        assertTrue(NotificationRuntimeState.shouldDispatchAlert(""))
        assertTrue(NotificationRuntimeState.shouldDispatchAlert("   "))

        NotificationRuntimeState.clearSeenForTesting()
        assertTrue(NotificationRuntimeState.shouldDispatchAlert("evt-1"))
    }
}
