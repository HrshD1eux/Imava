package com.hrshd1eux.imava.core.util

import com.hrshd1eux.imava.R

enum class WhatsNewCategory(val label: String, val iconChar: String) {
    SECURITY("Security & Privacy", "🛡️"),
    PERFORMANCE("Performance", "⚡"),
    STORAGE("Storage & Sync", "💾"),
    FEATURES("Features & UX", "✨"),
    FIXES("Bug Fixes", "🛠️")
}

data class WhatsNewItem(
    val titleRes: Int,
    val descRes: Int,
    val category: WhatsNewCategory
)

data class VersionRelease(
    val version: String,
    val isCurrent: Boolean = false,
    val items: List<WhatsNewItem>
)

object WhatsNewRepository {

    const val CURRENT_VERSION_NAME = "1.1.121"

    fun getReleases(): List<VersionRelease> {
        return listOf(
            VersionRelease(
                version = CURRENT_VERSION_NAME,
                isCurrent = true,
                items = listOf(
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_keystore_title,
                        descRes = R.string.wn_v120_keystore_desc,
                        category = WhatsNewCategory.SECURITY
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_vault_iso_title,
                        descRes = R.string.wn_v120_vault_iso_desc,
                        category = WhatsNewCategory.SECURITY
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_shake_title,
                        descRes = R.string.wn_v120_shake_desc,
                        category = WhatsNewCategory.SECURITY
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_drop_limit_title,
                        descRes = R.string.wn_v120_drop_limit_desc,
                        category = WhatsNewCategory.SECURITY
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_drop_history_title,
                        descRes = R.string.wn_v120_drop_history_desc,
                        category = WhatsNewCategory.FEATURES
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_trash_title,
                        descRes = R.string.wn_v120_trash_desc,
                        category = WhatsNewCategory.STORAGE
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_dup_title,
                        descRes = R.string.wn_v120_dup_desc,
                        category = WhatsNewCategory.PERFORMANCE
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_zoom_title,
                        descRes = R.string.wn_v120_zoom_desc,
                        category = WhatsNewCategory.PERFORMANCE
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v120_stealth_title,
                        descRes = R.string.wn_v120_stealth_desc,
                        category = WhatsNewCategory.FIXES
                    )
                )
            ),
            VersionRelease(
                version = "1.1.112",
                isCurrent = false,
                items = listOf(
                    WhatsNewItem(
                        titleRes = R.string.wn_v112_decoy_title,
                        descRes = R.string.wn_v112_decoy_desc,
                        category = WhatsNewCategory.SECURITY
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v112_autofit_title,
                        descRes = R.string.wn_v112_autofit_desc,
                        category = WhatsNewCategory.FEATURES
                    )
                )
            ),
            VersionRelease(
                version = "1.1.42",
                isCurrent = false,
                items = listOf(
                    WhatsNewItem(
                        titleRes = R.string.wn_v42_shredder_title,
                        descRes = R.string.wn_v42_shredder_desc,
                        category = WhatsNewCategory.SECURITY
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v42_deepscan_title,
                        descRes = R.string.wn_v42_deepscan_desc,
                        category = WhatsNewCategory.PERFORMANCE
                    )
                )
            )
        )
    }
}
