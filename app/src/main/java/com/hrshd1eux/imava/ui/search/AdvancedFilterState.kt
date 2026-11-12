package com.hrshd1eux.imava.ui.search

import androidx.annotation.StringRes
import com.hrshd1eux.imava.R

enum class ResolutionFilter(@StringRes val labelRes: Int) {
    ANY(R.string.filter_res_any),
    UHD_4K(R.string.filter_res_4k),
    FULL_HD_1080(R.string.filter_res_1080p),
    PANORAMA(R.string.filter_res_panorama),
    PORTRAIT(R.string.filter_res_portrait),
    LANDSCAPE(R.string.filter_res_landscape)
}

enum class FileSizeFilter(@StringRes val labelRes: Int, val minBytes: Long? = null, val maxBytes: Long? = null) {
    ANY(R.string.filter_size_any),
    UNDER_1MB(R.string.filter_size_under_1mb, maxBytes = 1024L * 1024L),
    OVER_10MB(R.string.filter_size_over_10mb, minBytes = 10L * 1024L * 1024L),
    OVER_100MB(R.string.filter_size_over_100mb, minBytes = 100L * 1024L * 1024L),
    OVER_500MB(R.string.filter_size_over_500mb, minBytes = 500L * 1024L * 1024L),
    OVER_1GB(R.string.filter_size_over_1gb, minBytes = 1024L * 1024L * 1024L)
}

enum class MediaTypeFilterChoice(@StringRes val labelRes: Int) {
    ALL(R.string.filter_media_all),
    PHOTOS_ONLY(R.string.filter_media_photos),
    VIDEOS_ONLY(R.string.filter_media_videos),
    RAW_DNG(R.string.filter_media_raw),
    GIFS(R.string.filter_media_gifs)
}

enum class DatePreset(@StringRes val labelRes: Int) {
    ANY(R.string.filter_date_any),
    THIS_WEEK(R.string.filter_date_this_week),
    THIS_MONTH(R.string.filter_date_this_month),
    LAST_3_MONTHS(R.string.filter_date_last_3_months),
    THIS_YEAR(R.string.filter_date_this_year),
    CUSTOM(R.string.filter_date_custom)
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
