package com.hrshd1eux.imava.ui.search

enum class ResolutionFilter(val label: String) {
    ANY("Any Resolution"),
    UHD_4K("4K UHD"),
    FULL_HD_1080("1080p FHD"),
    PANORAMA("Panorama"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

enum class FileSizeFilter(val label: String, val minBytes: Long? = null, val maxBytes: Long? = null) {
    ANY("Any Size"),
    UNDER_1MB("< 1 MB", maxBytes = 1024L * 1024L),
    OVER_10MB("> 10 MB", minBytes = 10L * 1024L * 1024L),
    OVER_100MB("> 100 MB", minBytes = 100L * 1024L * 1024L),
    OVER_500MB("> 500 MB", minBytes = 500L * 1024L * 1024L),
    OVER_1GB("> 1 GB", minBytes = 1024L * 1024L * 1024L)
}

enum class MediaTypeFilterChoice(val label: String) {
    ALL("All Types"),
    PHOTOS_ONLY("Photos"),
    VIDEOS_ONLY("Videos"),
    RAW_DNG("RAW (DNG)"),
    GIFS("GIFs")
}

enum class DatePreset(val label: String) {
    ANY("Any Date"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("Last 3 Months"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom Range")
}

data class AdvancedFilterState(
    val mediaType: MediaTypeFilterChoice = MediaTypeFilterChoice.ALL,
    val fileSize: FileSizeFilter = FileSizeFilter.ANY,
    val resolution: ResolutionFilter = ResolutionFilter.ANY,
    val datePreset: DatePreset = DatePreset.ANY,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val cameraModelQuery: String = ""
) {
    val isActive: Boolean
        get() = mediaType != MediaTypeFilterChoice.ALL ||
                fileSize != FileSizeFilter.ANY ||
                resolution != ResolutionFilter.ANY ||
                datePreset != DatePreset.ANY ||
                customStartDate != null ||
                customEndDate != null ||
                cameraModelQuery.isNotBlank()

    val activeFilterCount: Int
        get() {
            var count = 0
            if (mediaType != MediaTypeFilterChoice.ALL) count++
            if (fileSize != FileSizeFilter.ANY) count++
            if (resolution != ResolutionFilter.ANY) count++
            if (datePreset != DatePreset.ANY || customStartDate != null) count++
            if (cameraModelQuery.isNotBlank()) count++
            return count
        }
}
