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
                        titleRes = R.string.wn_v120_drop_history_title,
                        descRes = R.string.wn_v120_drop_history_desc,
                        category = WhatsNewCategory.FEATURES
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v121_whats_new_title,
                        descRes = R.string.wn_v121_whats_new_desc,
                        category = WhatsNewCategory.FEATURES
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v121_updater_title,
                        descRes = R.string.wn_v121_updater_desc,
                        category = WhatsNewCategory.PERFORMANCE
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v121_perm_title,
                        descRes = R.string.wn_v121_perm_desc,
                        category = WhatsNewCategory.FIXES
                    )
                )
            ),
            VersionRelease(
                version = "1.1.120",
                isCurrent = false,
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
                version = "1.1.119",
                isCurrent = false,
                items = listOf(
                    WhatsNewItem(
                        titleRes = R.string.wn_v119_vault_cache_title,
                        descRes = R.string.wn_v119_vault_cache_desc,
                        category = WhatsNewCategory.FIXES
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v119_decoy_loading_title,
                        descRes = R.string.wn_v119_decoy_loading_desc,
                        category = WhatsNewCategory.FIXES
                    ),
                    WhatsNewItem(
                        titleRes = R.string.wn_v119_atomic_delete_title,
                        descRes = R.string.wn_v119_atomic_delete_desc,
                        category = WhatsNewCategory.SECURITY
                    )
                )
            )
        )
    }
}
