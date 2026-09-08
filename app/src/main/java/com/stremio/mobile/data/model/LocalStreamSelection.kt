package com.stremio.mobile.data.model

data class LocalStreamSelection(
    val key: String,
    val addonTitle: String,
    val name: String,
    val description: String?,
    val quality: String?,
    val stableId: String = "",
    val filename: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val bingeGroup: String? = null,
    val identityKeys: Set<String> = emptySet(),
) {
    fun matches(option: StreamOption): Boolean {
        val optionKeys = option.identityKeys
        if (stableId.isNotBlank() && stableId in optionKeys) return true
        if (identityKeys.any { it in optionKeys }) return true
        val savedHash = infoHash?.trim()?.lowercase().orEmpty()
        val optionHash = option.infoHash?.trim()?.lowercase().orEmpty()
        if (savedHash.isNotEmpty() && savedHash == optionHash && (fileIdx == null || option.fileIdx == fileIdx)) {
            return true
        }
        val savedFile = filename?.trim().orEmpty().ifEmpty {
            stableId.substringAfterLast('\u001f', missingDelimiterValue = "").trim()
        }
        val optionFile = (option.rawFilename ?: option.filename)?.trim().orEmpty()
        if (savedFile.isNotEmpty() && savedFile.equals(optionFile, ignoreCase = true) &&
            (addonTitle.isBlank() || option.addonTitle == addonTitle)
        ) {
            return true
        }
        val savedBinge = bingeGroup?.trim().orEmpty()
        if (savedBinge.isNotEmpty() && savedBinge == option.bingeGroup?.trim().orEmpty() && option.addonTitle == addonTitle) {
            return true
        }
        return false
    }
}
