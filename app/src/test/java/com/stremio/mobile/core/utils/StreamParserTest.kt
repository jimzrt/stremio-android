package com.stremio.mobile.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamParserTest {
    @Test
    fun `parses torrentio-style stream metadata`() {
        val parsed = parseStreamMetadata(
            name = "1080p HEVC",
            description = "👤 84 💾 4.21 GB ⚙️ RARBG\nAndor.S01E03.1080p.BluRay.x265.Atmos.HDR10.ENG.GER",
            filename = "Andor.S01E03.1080p.BluRay.x265.Atmos.HDR10.ENG.GER.mkv",
            videoSizeBytes = 4_521_164_800L,
            subtitleLanguages = listOf("eng", "ger"),
        )

        assertEquals("84", parsed.seeds)
        assertEquals("4.2 GB", parsed.size)
        assertEquals("RARBG", parsed.origin)
        assertEquals("1080p", parsed.quality)
        assertEquals("HEVC", parsed.videoCodec)
        assertEquals("Atmos", parsed.audio)
        assertEquals("HDR10", parsed.hdr)
        assertEquals("Andor.S01E03.1080p.BluRay.x265.Atmos.HDR10.ENG.GER", parsed.filename)
        assertTrue(parsed.languages.contains("English"))
        assertTrue(parsed.languages.contains("German"))
    }

    @Test
    fun `prefers behavior hint size and filename`() {
        val parsed = parseStreamMetadata(
            name = "720p",
            description = "💾 700 MB",
            filename = "/downloads/Show Name S02E01.mkv",
            videoSizeBytes = 1_572_864_000L,
        )

        assertEquals("1.5 GB", parsed.size)
        assertEquals("Show Name S02E01", parsed.filename)
        assertEquals("720p", parsed.quality)
    }

    @Test
    fun `stream identity ignores volatile seed counts`() {
        val first = streamStableId(
            addonTitle = "Torrentio",
            name = "1080p",
            description = "👤 84 💾 4.21 GB ⚙️ RARBG",
            filename = "Andor.S01E03.1080p.mkv",
        )
        val second = streamStableId(
            addonTitle = "Torrentio",
            name = "1080p",
            description = "👤 12 💾 4.21 GB ⚙️ RARBG",
            filename = "Andor.S01E03.1080p.mkv",
        )
        val otherFile = streamStableId(
            addonTitle = "Torrentio",
            name = "1080p",
            description = "👤 84 💾 4.21 GB ⚙️ RARBG",
            filename = "Andor.S01E03.720p.mkv",
        )

        assertEquals(first, second)
        assertTrue(first != otherFile)
    }

    @Test
    fun `stream identity prefers info hash over description`() {
        val first = streamIdentityKeys(
            addonTitle = "Torrentio",
            name = "1080p",
            description = "👤 84",
            filename = null,
            infoHash = "ABCDEF",
            fileIdx = 1,
        )
        val second = streamIdentityKeys(
            addonTitle = "Torrentio",
            name = "720p",
            description = "👤 3",
            filename = null,
            infoHash = "abcdef",
            fileIdx = 1,
        )

        assertTrue(first.any { it in second })
    }

    @Test
    fun `clears description when it is only the filename`() {
        val parsed = parseStreamMetadata(
            name = "1080p",
            description = "Movie.Name.2024.1080p.mkv",
            filename = "Movie.Name.2024.1080p.mkv",
        )
        assertEquals("", parsed.cleanDescription)
        assertEquals("Movie.Name.2024.1080p", parsed.filename)
    }

    @Test
    fun `extracts dolby vision and 2160p`() {
        val parsed = parseStreamMetadata(
            name = "4K DV Atmos",
            description = "2160p BluRay HEVC Dolby Vision TrueHD",
        )
        assertEquals("2160p", parsed.quality)
        assertEquals("HEVC", parsed.videoCodec)
        assertEquals("Dolby Vision", parsed.hdr)
        assertEquals("Atmos", parsed.audio)
        assertNull(parsed.filename)
    }
}
