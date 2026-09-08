package com.stremio.mobile.data.model

import com.stremio.mobile.core.CoreStream
import com.stremio.mobile.core.utils.streamIdentityKeys
import com.stremio.mobile.core.utils.streamStableId

data class StreamOption(
    val key: String,
    val name: String,
    val description: String?,
    val addonTitle: String,
    val quality: String?,
    val core: CoreStream,
    val seeds: String? = null,
    val size: String? = null,
    val origin: String? = null,
    val cleanDescription: String? = null,
    val filename: String? = null,
    val rawFilename: String? = null,
    val videoCodec: String? = null,
    val audio: String? = null,
    val hdr: String? = null,
    val languages: List<String> = emptyList(),
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val bingeGroup: String? = null,
    val sourceUrl: String? = null,
    val played: Boolean = false,
    val lastPlayed: Boolean = false,
) {
    val identityKeys: Set<String>
        get() = streamIdentityKeys(
            addonTitle = addonTitle,
            name = name,
            description = description,
            filename = rawFilename ?: filename,
            infoHash = infoHash,
            fileIdx = fileIdx,
            bingeGroup = bingeGroup,
            sourceUrl = sourceUrl,
        )

    val stableId: String
        get() = streamStableId(
            addonTitle = addonTitle,
            name = name,
            description = description,
            filename = rawFilename ?: filename,
            infoHash = infoHash,
            fileIdx = fileIdx,
            bingeGroup = bingeGroup,
            sourceUrl = sourceUrl,
        )

    val displayTitle: String
        get() = filename?.takeIf { it.isNotBlank() } ?: name

    fun wasPlayed(playedIds: Set<String>): Boolean {
        if (identityKeys.any { it in playedIds }) return true
        val file = (rawFilename ?: filename)?.trim().orEmpty()
        if (file.isEmpty()) return false
        return playedIds.any { saved ->
            saved.contains('\u001f') && saved.substringAfterLast('\u001f').equals(file, ignoreCase = true)
        }
    }
}
