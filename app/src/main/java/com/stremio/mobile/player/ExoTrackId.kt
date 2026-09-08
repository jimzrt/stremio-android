package com.stremio.mobile.player

data class ExoTrackId(
    val type: PlayerTrackType,
    val groupIndex: Int,
    val trackIndex: Int,
) {
    val isDownmixed: Boolean
        get() = type == PlayerTrackType.AUDIO && trackIndex < 0

    val sourceTrackIndex: Int
        get() = if (isDownmixed) -trackIndex - 1 else trackIndex

    fun encode(): String = "$PREFIX:${type.name.lowercase()}:$groupIndex:$trackIndex"

    companion object {
        private const val PREFIX = "exo"

        fun downmixed(groupIndex: Int, trackIndex: Int) =
            ExoTrackId(PlayerTrackType.AUDIO, groupIndex, -trackIndex - 1)

        fun parse(value: String): ExoTrackId? {
            val parts = value.split(':')
            if (parts.size != 4 || parts[0] != PREFIX) return null
            val type = when (parts[1]) {
                "audio" -> PlayerTrackType.AUDIO
                "subtitle" -> PlayerTrackType.SUBTITLE
                else -> return null
            }
            val groupIndex = parts[2].toIntOrNull() ?: return null
            val trackIndex = parts[3].toIntOrNull() ?: return null
            return ExoTrackId(type, groupIndex, trackIndex)
        }
    }
}

