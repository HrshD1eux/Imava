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

    fun sanitizeVersion(version: String): String {
        return version.trim().substringBefore("-").substringBefore("+")
    }

    val CURRENT_VERSION_NAME: String
        get() = try {
            sanitizeVersion(com.hrshd1eux.imava.BuildConfig.VERSION_NAME).ifBlank { "1.1.123" }
        } catch (_: Throwable) {
            "1.1.123"
        }

    fun getReleases(currentVersion: String = CURRENT_VERSION_NAME): List<VersionRelease> {
        val v0 = sanitizeVersion(currentVersion).ifBlank { "1.1.123" }
        val v1 = getPreviousVersion(v0, 1)
        val v2 = getPreviousVersion(v0, 2)

        return listOf(
            VersionRelease(
                version = v0,
                isCurrent = true,
                items = getItemsForVersion(v0)
            ),
            VersionRelease(
                version = v1,
                isCurrent = false,
                items = getItemsForVersion(v1)
            ),
            VersionRelease(
                version = v2,
                isCurrent = false,
                items = getItemsForVersion(v2)
            )
        )
    }

    fun getPreviousVersion(version: String, stepsBack: Int): String {
        val clean = sanitizeVersion(version)
        val regex = Regex("""^(\d+\.\d+\.)(\d+)$""")
        val match = regex.matchEntire(clean)
        return if (match != null) {
            val prefix = match.groupValues[1]
            val patch = match.groupValues[2].toIntOrNull() ?: 0
            val prevPatch = maxOf(0, patch - stepsBack)
            "$prefix$prevPatch"
        } else {
            clean
        }
    }

    fun getItemsForVersion(version: String): List<WhatsNewItem> {
        val clean = sanitizeVersion(version)
        val patch = clean.substringAfterLast(".").toIntOrNull() ?: 0
        return when {
            patch >= 123 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v123_adaptive_title,
                    descRes = R.string.wn_v123_adaptive_desc,
                    category = WhatsNewCategory.FEATURES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v120_drop_history_title,
                    descRes = R.string.wn_v120_drop_history_desc,
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
            patch == 122 -> listOf(
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
            patch == 121 || patch == 120 -> listOf(
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
            else -> listOf(
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
        }
    }
}
