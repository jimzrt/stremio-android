package com.stremio.mobile.core.utils

import java.util.Locale

data class ParsedStreamMetadata(
    val seeds: String?,
    val size: String?,
    val origin: String?,
    val cleanDescription: String,
    val quality: String? = null,
    val videoCodec: String? = null,
    val audio: String? = null,
    val hdr: String? = null,
    val languages: List<String> = emptyList(),
    val filename: String? = null,
)

fun parseStreamDescription(description: String?): ParsedStreamMetadata {
    return parseStreamMetadata(name = null, description = description)
}

fun parseStreamMetadata(
    name: String?,
    description: String?,
    filename: String? = null,
    videoSizeBytes: Long? = null,
    subtitleLanguages: List<String> = emptyList(),
): ParsedStreamMetadata {
    val desc = description ?: ""
    val blob = listOfNotNull(name, description, filename).joinToString("\n")
    var seeds: String? = null
    var size: String? = null
    var origin: String? = null

    val seedsEmojiRegex = Regex("👤\\s*(\\d+)")
    val sizeEmojiRegex = Regex("💾\\s*([\\d.]+\\s*(?:GB|GiB|MB|MiB|KB|KiB|Bytes|B))", RegexOption.IGNORE_CASE)
    val originEmojiRegex = Regex("⚙️\\s*([^\\n💾👤]+)")
    val seedsTextRegex = Regex("(?:seeds|peers):?\\s*(\\d+)", RegexOption.IGNORE_CASE)
    val sizeTextRegex = Regex("(?:size):?\\s*([\\d.]+\\s*(?:GB|GiB|MB|MiB|KB|KiB|Bytes|B))", RegexOption.IGNORE_CASE)
    val sizeBareRegex = Regex("\\b(\\d+(?:\\.\\d+)?\\s*(?:GB|GiB|MB|MiB|KB|KiB))\\b", RegexOption.IGNORE_CASE)

    seedsEmojiRegex.find(blob)?.let { seeds = it.groupValues[1] }
        ?: seedsTextRegex.find(blob)?.let { seeds = it.groupValues[1] }

    size = videoSizeBytes?.takeIf { it > 0 }?.let(::formatByteSize)
        ?: sizeEmojiRegex.find(blob)?.groupValues?.get(1)
        ?: sizeTextRegex.find(blob)?.groupValues?.get(1)
        ?: sizeBareRegex.find(blob)?.groupValues?.get(1)

    originEmojiRegex.find(blob)?.let { origin = it.groupValues[1].trim() }

    var cleanDesc = desc
    cleanDesc = seedsEmojiRegex.replace(cleanDesc, "")
    cleanDesc = sizeEmojiRegex.replace(cleanDesc, "")
    cleanDesc = originEmojiRegex.replace(cleanDesc, "")
    cleanDesc = seedsTextRegex.replace(cleanDesc, "")
    cleanDesc = sizeTextRegex.replace(cleanDesc, "")
    cleanDesc = cleanDesc.replace(Regex("[👤💾⚙️]"), "")

    val cleanedText = cleanDesc.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

    val displayFilename = displayFilename(filename)
    val descriptionIsFilename = displayFilename != null &&
        (cleanedText.equals(displayFilename, ignoreCase = true) ||
            displayFilename(cleanedText) == displayFilename)

    return ParsedStreamMetadata(
        seeds = seeds,
        size = size,
        origin = origin,
        cleanDescription = if (descriptionIsFilename) "" else cleanedText,
        quality = extractQuality(blob),
        videoCodec = extractVideoCodec(blob),
        audio = extractAudio(blob),
        hdr = extractHdr(blob),
        languages = extractLanguages(blob, subtitleLanguages),
        filename = displayFilename,
    )
}

fun streamStableId(
    addonTitle: String,
    name: String,
    description: String?,
    filename: String?,
    infoHash: String? = null,
    fileIdx: Int? = null,
    bingeGroup: String? = null,
    sourceUrl: String? = null,
): String {
    return streamIdentityKeys(
        addonTitle = addonTitle,
        name = name,
        description = description,
        filename = filename,
        infoHash = infoHash,
        fileIdx = fileIdx,
        bingeGroup = bingeGroup,
        sourceUrl = sourceUrl,
    ).first()
}

fun streamIdentityKeys(
    addonTitle: String,
    name: String,
    description: String?,
    filename: String?,
    infoHash: String? = null,
    fileIdx: Int? = null,
    bingeGroup: String? = null,
    sourceUrl: String? = null,
): Set<String> {
    return buildSet {
        val hash = infoHash?.trim()?.lowercase().orEmpty()
        if (hash.isNotEmpty()) {
            add("hash:$hash:${fileIdx ?: -1}")
        }
        val file = filename?.trim().orEmpty()
        if (file.isNotEmpty()) {
            add("file:$addonTitle:$file")
        }
        val binge = bingeGroup?.trim().orEmpty()
        if (binge.isNotEmpty()) {
            add("binge:$addonTitle:$binge")
        }
        val url = sourceUrl?.trim().orEmpty()
        if (url.isNotEmpty()) {
            add("url:$url")
        }
        add("meta:$addonTitle:$name:${stripVolatileStreamText(description)}")
    }
}

private fun stripVolatileStreamText(value: String?): String {
    return (value ?: "")
        .replace(Regex("👤\\s*\\d+"), "")
        .replace(Regex("💾\\s*[\\d.]+\\s*(?:GB|GiB|MB|MiB|KB|KiB|Bytes|B)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("⚙️\\s*[^\\n💾👤]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun displayFilename(filename: String?): String? {
    val raw = filename
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val withoutExt = raw.substringBeforeLast('.')
    return withoutExt.ifBlank { raw }
}

fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (unit == 0) "${value.toInt()} B" else String.format(Locale.US, "%.1f %s", value, units[unit])
}

private fun extractQuality(blob: String): String? {
    val patterns = listOf(
        Regex("\\b(2160p|4k)\\b", RegexOption.IGNORE_CASE) to "2160p",
        Regex("\\b1080p\\b", RegexOption.IGNORE_CASE) to "1080p",
        Regex("\\b720p\\b", RegexOption.IGNORE_CASE) to "720p",
        Regex("\\b480p\\b", RegexOption.IGNORE_CASE) to "480p",
    )
    return patterns.firstOrNull { (regex, _) -> regex.containsMatchIn(blob) }?.second
}

private fun extractVideoCodec(blob: String): String? {
    val patterns = listOf(
        Regex("\\bAV1\\b", RegexOption.IGNORE_CASE) to "AV1",
        Regex("\\b(?:HEVC|H\\.?265|x265)\\b", RegexOption.IGNORE_CASE) to "HEVC",
        Regex("\\b(?:AVC|H\\.?264|x264)\\b", RegexOption.IGNORE_CASE) to "H.264",
        Regex("\\bVP9\\b", RegexOption.IGNORE_CASE) to "VP9",
    )
    return patterns.firstOrNull { (regex, _) -> regex.containsMatchIn(blob) }?.second
}

private fun extractAudio(blob: String): String? {
    val patterns = listOf(
        Regex("\\bAtmos\\b", RegexOption.IGNORE_CASE) to "Atmos",
        Regex("\\bTrueHD\\b", RegexOption.IGNORE_CASE) to "TrueHD",
        Regex("\\bDTS(?:-HD)?(?:\\s*MA)?\\b", RegexOption.IGNORE_CASE) to "DTS",
        Regex("\\b(?:DD\\+|DDP|E-?AC-?3)\\b", RegexOption.IGNORE_CASE) to "DD+",
        Regex("\\b(?:AC-?3|DD)\\b", RegexOption.IGNORE_CASE) to "AC3",
        Regex("\\bAAC\\b", RegexOption.IGNORE_CASE) to "AAC",
        Regex("\\bFLAC\\b", RegexOption.IGNORE_CASE) to "FLAC",
        Regex("\\bOpus\\b", RegexOption.IGNORE_CASE) to "Opus",
        Regex("\\b7\\.1\\b") to "7.1",
        Regex("\\b5\\.1\\b") to "5.1",
    )
    return patterns.firstOrNull { (regex, _) -> regex.containsMatchIn(blob) }?.second
}

private fun extractHdr(blob: String): String? {
    val patterns = listOf(
        Regex("\\bDolby\\s*Vision\\b|\\bDV\\b", RegexOption.IGNORE_CASE) to "Dolby Vision",
        Regex("\\bHDR10\\+\\b", RegexOption.IGNORE_CASE) to "HDR10+",
        Regex("\\bHDR10\\b", RegexOption.IGNORE_CASE) to "HDR10",
        Regex("\\bHDR\\b", RegexOption.IGNORE_CASE) to "HDR",
    )
    return patterns.firstOrNull { (regex, _) -> regex.containsMatchIn(blob) }?.second
}

private val languagePatterns = listOf(
    Regex("\\bMulti(?:lang(?:ual)?)?\\b", RegexOption.IGNORE_CASE) to "Multi",
    Regex("\\bDual(?:\\s*Audio)?\\b", RegexOption.IGNORE_CASE) to "Dual",
    Regex("\\bEnglish\\b|\\bENG\\b", RegexOption.IGNORE_CASE) to "English",
    Regex("\\bGerman\\b|\\bGER\\b|\\bDEU\\b", RegexOption.IGNORE_CASE) to "German",
    Regex("\\bFrench\\b|\\bFRE\\b|\\bFRA\\b", RegexOption.IGNORE_CASE) to "French",
    Regex("\\bSpanish\\b|\\bSPA\\b|\\bESP\\b", RegexOption.IGNORE_CASE) to "Spanish",
    Regex("\\bItalian\\b|\\bITA\\b", RegexOption.IGNORE_CASE) to "Italian",
    Regex("\\bJapanese\\b|\\bJPN\\b", RegexOption.IGNORE_CASE) to "Japanese",
    Regex("\\bKorean\\b|\\bKOR\\b", RegexOption.IGNORE_CASE) to "Korean",
    Regex("\\bRussian\\b|\\bRUS\\b", RegexOption.IGNORE_CASE) to "Russian",
    Regex("\\bPortuguese\\b|\\bPOR\\b|\\bPOB\\b", RegexOption.IGNORE_CASE) to "Portuguese",
    Regex("\\bHindi\\b|\\bHIN\\b", RegexOption.IGNORE_CASE) to "Hindi",
    Regex("\\bChinese\\b|\\bCHI\\b|\\bZHO\\b", RegexOption.IGNORE_CASE) to "Chinese",
    Regex("\\bPolish\\b|\\bPOL\\b", RegexOption.IGNORE_CASE) to "Polish",
    Regex("\\bDutch\\b|\\bNLD\\b|\\bDUT\\b", RegexOption.IGNORE_CASE) to "Dutch",
    Regex("\\bSwedish\\b|\\bSWE\\b", RegexOption.IGNORE_CASE) to "Swedish",
    Regex("\\bNorwegian\\b|\\bNOR\\b", RegexOption.IGNORE_CASE) to "Norwegian",
    Regex("\\bDanish\\b|\\bDAN\\b", RegexOption.IGNORE_CASE) to "Danish",
    Regex("\\bTurkish\\b|\\bTUR\\b", RegexOption.IGNORE_CASE) to "Turkish",
    Regex("\\bArabic\\b|\\bARA\\b", RegexOption.IGNORE_CASE) to "Arabic",
)

private val subtitleLanguageLabels = mapOf(
    "eng" to "English",
    "ger" to "German",
    "fre" to "French",
    "spa" to "Spanish",
    "ita" to "Italian",
    "jpn" to "Japanese",
    "kor" to "Korean",
    "rus" to "Russian",
    "por" to "Portuguese",
    "pob" to "Portuguese",
    "hin" to "Hindi",
    "zho" to "Chinese",
    "chi" to "Chinese",
    "pol" to "Polish",
    "nld" to "Dutch",
    "swe" to "Swedish",
    "nor" to "Norwegian",
    "dan" to "Danish",
    "tur" to "Turkish",
    "ara" to "Arabic",
)

private fun extractLanguages(blob: String, subtitleLanguages: List<String>): List<String> {
    val labels = linkedSetOf<String>()
    for ((regex, label) in languagePatterns) {
        if (regex.containsMatchIn(blob)) {
            labels += label
        }
    }
    for (raw in subtitleLanguages) {
        labels += languageLabel(raw)
    }
    return labels.take(6)
}

private fun languageLabel(raw: String): String {
    val code = raw.trim().lowercase(Locale.ROOT)
    subtitleLanguageLabels[code]?.let { return it }
    val locale = Locale.forLanguageTag(code)
    val display = locale.getDisplayLanguage(Locale.ENGLISH)
    return display.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
        ?: raw.trim().uppercase(Locale.ROOT)
}
