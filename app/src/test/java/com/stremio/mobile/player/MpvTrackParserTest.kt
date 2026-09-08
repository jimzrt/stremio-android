package com.stremio.mobile.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpvTrackParserTest {
    @Test
    fun `parses embedded and external audio and subtitle tracks`() {
        val reader = FakeMpvTrackReader(
            ints = mapOf(
                "track-list/count" to 4,
                "track-list/0/id" to 1,
                "track-list/1/id" to 2,
                "track-list/2/id" to 3,
                "track-list/3/id" to 4,
            ),
            strings = mapOf(
                "track-list/0/type" to "audio",
                "track-list/0/lang" to "eng",
                "track-list/0/title" to "Stereo",
                "track-list/1/type" to "sub",
                "track-list/1/lang" to "spa",
                "track-list/1/title" to "Spanish",
                "track-list/2/type" to "sub",
                "track-list/2/lang" to "fre",
                "track-list/3/type" to "audio",
            ),
            booleans = mapOf(
                "track-list/0/external" to false,
                "track-list/0/selected" to true,
                "track-list/1/external" to false,
                "track-list/2/external" to true,
                "track-list/2/selected" to true,
                "track-list/3/external" to false,
            )
        )

        val (audio, subtitles) = MpvTrackParser.parse(reader)

        assertEquals(2, audio.size)
        assertEquals("mpv:audio:1", audio[0].id)
        assertEquals("Stereo (ENG) (Embedded #1)", audio[0].label)
        assertEquals("eng", audio[0].language)
        assertTrue(audio[0].selected)
        assertEquals("Track 2 (Embedded #4)", audio[1].label)
        assertFalse(audio[1].selected)

        assertEquals(2, subtitles.size)
        assertEquals("mpv:sub:2", subtitles[0].id)
        assertEquals("Spanish (SPA) (Embedded #2)", subtitles[0].label)
        assertEquals("mpv:sub:3", subtitles[1].id)
        assertEquals("FRE (External #3)", subtitles[1].label)
        assertTrue(subtitles[1].selected)
    }

    @Test
    fun `adds one downmixed option for each multichannel audio track`() {
        val reader = FakeMpvTrackReader(
            ints = mapOf(
                "track-list/count" to 3,
                "track-list/0/id" to 1,
                "track-list/0/audio-channels" to 2,
                "track-list/1/id" to 2,
                "track-list/1/audio-channels" to 6,
                "track-list/2/id" to 3,
                "track-list/2/audio-channels" to 8,
            ),
            strings = mapOf(
                "track-list/0/type" to "audio",
                "track-list/0/lang" to "eng",
                "track-list/0/title" to "Stereo",
                "track-list/1/type" to "audio",
                "track-list/1/lang" to "eng",
                "track-list/1/title" to "Surround 5.1",
                "track-list/2/type" to "audio",
                "track-list/2/title" to "Surround 7.1",
            ),
            booleans = mapOf(
                "track-list/0/selected" to true,
                "track-list/1/selected" to false,
                "track-list/2/selected" to false,
            )
        )

        val (audio, subtitles) = MpvTrackParser.parse(reader, downmixedAudioId = 2)

        assertTrue(subtitles.isEmpty())
        assertEquals(
            listOf("mpv:audio:1", "mpv:audio:2", "mpv:audio:-2", "mpv:audio:3", "mpv:audio:-3"),
            audio.map { it.id },
        )
        assertEquals("Surround 5.1 (Downmixed) (ENG) (Embedded #-2)", audio[2].label)
        assertEquals("Surround 7.1 (Downmixed) (Embedded #-3)", audio[4].label)
        assertFalse(audio[1].selected)
        assertTrue(audio[2].selected)
        assertFalse(audio[4].selected)
    }


    private class FakeMpvTrackReader(
        private val ints: Map<String, Int> = emptyMap(),
        private val strings: Map<String, String> = emptyMap(),
        private val booleans: Map<String, Boolean> = emptyMap(),
    ) : MpvTrackPropertyReader {
        override fun getInt(property: String): Int? = ints[property]
        override fun getString(property: String): String? = strings[property]
        override fun getBoolean(property: String): Boolean? = booleans[property]
    }
}
