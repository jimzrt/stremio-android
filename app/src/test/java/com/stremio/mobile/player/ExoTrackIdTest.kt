package com.stremio.mobile.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExoTrackIdTest {
    @Test
    fun `roundtrips audio and subtitle ids`() {
        val audio = ExoTrackId(PlayerTrackType.AUDIO, groupIndex = 3, trackIndex = 1)
        assertEquals(audio, ExoTrackId.parse(audio.encode()))

        val subtitle = ExoTrackId(PlayerTrackType.SUBTITLE, groupIndex = 4, trackIndex = 2)
        assertEquals(subtitle, ExoTrackId.parse(subtitle.encode()))
    }

    @Test
    fun `downmixed id points to its source track`() {
        val downmixed = ExoTrackId.downmixed(groupIndex = 3, trackIndex = 0)

        assertEquals("exo:audio:3:-1", downmixed.encode())
        assertEquals(0, downmixed.sourceTrackIndex)
        assertEquals(downmixed, ExoTrackId.parse(downmixed.encode()))
        assertEquals(true, downmixed.isDownmixed)
    }

    @Test
    fun `rejects malformed ids`() {
        assertNull(ExoTrackId.parse("exo:video:1:0"))
        assertNull(ExoTrackId.parse("mpv:audio:1"))
        assertNull(ExoTrackId.parse("exo:audio:one:0"))
    }
}
