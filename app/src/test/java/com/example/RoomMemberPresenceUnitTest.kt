package com.example

import com.example.data.model.RoomMember
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class RoomMemberPresenceUnitTest {

    @Test
    fun `online member shows active now`() {
        val now = 1_700_000_000_000L
        val member = RoomMember(
            userId = "u1",
            name = "DIKI",
            onlineStatus = "online",
            lastSeenAt = Timestamp(Date(now - 20_000L))
        )
        assertTrue(member.isCurrentlyOnline(now))
        assertEquals("Sedang aktif sekarang", member.lastSeenDisplay(now))
    }

    @Test
    fun `offline member formats minutes today yesterday and older dates`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = baseCal.timeInMillis

        val minsAgo = RoomMember(
            userId = "u2",
            name = "Andrian Sah",
            onlineStatus = "offline",
            lastSeenAt = Timestamp(Date(now - 5 * 60_000L))
        )
        assertTrue(minsAgo.lastSeenDisplay(now).startsWith("Terakhir online 5 mnt lalu"))

        val todayEarlier = RoomMember(
            userId = "u3",
            name = "Chandra Dhanny",
            onlineStatus = "offline",
            lastSeenAt = Timestamp(Date(now - 3 * 3_600_000L))
        )
        assertTrue(todayEarlier.lastSeenDisplay(now).startsWith("Terakhir online hari ini,"))

        val yesterday = RoomMember(
            userId = "u4",
            name = "Hendra Wijaya",
            onlineStatus = "offline",
            lastSeenAt = Timestamp(Date(now - 24 * 3_600_000L))
        )
        assertTrue(yesterday.lastSeenDisplay(now).startsWith("Terakhir online kemarin,"))

        val older = RoomMember(
            userId = "u5",
            name = "Petugas Lama",
            onlineStatus = "offline",
            lastSeenAt = Timestamp(Date(now - 3 * 24 * 3_600_000L))
        )
        assertTrue(older.lastSeenDisplay(now).startsWith("Terakhir online 17 Nov 2026,"))

        val noTimestamp = RoomMember(
            userId = "u6",
            name = "Baru Gabung",
            onlineStatus = "offline"
        )
        assertEquals("Terakhir online belum tercatat", noTimestamp.lastSeenDisplay(now))
    }
}
