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
            sanitizeVersion(com.hrshd1eux.imava.BuildConfig.VERSION_NAME).ifBlank { "1.1.133" }
        } catch (_: Throwable) {
            "1.1.133"
        }

    fun getReleases(currentVersion: String = CURRENT_VERSION_NAME): List<VersionRelease> {
        val v0 = sanitizeVersion(currentVersion).ifBlank { "1.1.133" }
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
            patch >= 133 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v133_room_db_persistence_title,
                    descRes = R.string.wn_v133_room_db_persistence_desc,
                    category = WhatsNewCategory.STORAGE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v133_zero_rescan_boot_title,
                    descRes = R.string.wn_v133_zero_rescan_boot_desc,
                    category = WhatsNewCategory.PERFORMANCE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v133_database_migration_v11_title,
                    descRes = R.string.wn_v133_database_migration_v11_desc,
                    category = WhatsNewCategory.FEATURES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v133_lazy_deletion_sync_title,
                    descRes = R.string.wn_v133_lazy_deletion_sync_desc,
                    category = WhatsNewCategory.FIXES
                )
            )
            patch == 132 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v132_ghost_thumb_purger_title,
                    descRes = R.string.wn_v132_ghost_thumb_purger_desc,
                    category = WhatsNewCategory.STORAGE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v132_target_app_discovery_title,
                    descRes = R.string.wn_v132_target_app_discovery_desc,
                    category = WhatsNewCategory.FEATURES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v132_delete_resurrection_fix_title,
                    descRes = R.string.wn_v132_delete_resurrection_fix_desc,
                    category = WhatsNewCategory.FIXES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v132_cache_init_perf_title,
                    descRes = R.string.wn_v132_cache_init_perf_desc,
                    category = WhatsNewCategory.PERFORMANCE
                )
            )
            patch == 131 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v131_nomedia_accuracy_title,
                    descRes = R.string.wn_v131_nomedia_accuracy_desc,
                    category = WhatsNewCategory.FEATURES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v131_instant_hidden_title,
                    descRes = R.string.wn_v131_instant_hidden_desc,
                    category = WhatsNewCategory.PERFORMANCE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v131_broad_format_title,
                    descRes = R.string.wn_v131_broad_format_desc,
                    category = WhatsNewCategory.STORAGE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v131_perf_optimization_title,
                    descRes = R.string.wn_v131_perf_optimization_desc,
                    category = WhatsNewCategory.FIXES
                )
            )
            patch == 130 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v129_whats_new_rule_title,
                    descRes = R.string.wn_v129_whats_new_rule_desc,
                    category = WhatsNewCategory.FEATURES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v129_audit_hook_title,
                    descRes = R.string.wn_v129_audit_hook_desc,
                    category = WhatsNewCategory.SECURITY
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v129_lang_parity_title,
                    descRes = R.string.wn_v129_lang_parity_desc,
                    category = WhatsNewCategory.FEATURES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v129_unit_test_title,
                    descRes = R.string.wn_v129_unit_test_desc,
                    category = WhatsNewCategory.FIXES
                )
            )
            patch == 129 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v128_deepscan_persist_title,
                    descRes = R.string.wn_v128_deepscan_persist_desc,
                    category = WhatsNewCategory.STORAGE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v128_lowend_perf_title,
                    descRes = R.string.wn_v128_lowend_perf_desc,
                    category = WhatsNewCategory.PERFORMANCE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v128_btree_index_title,
                    descRes = R.string.wn_v128_btree_index_desc,
                    category = WhatsNewCategory.PERFORMANCE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v128_album_streamline_title,
                    descRes = R.string.wn_v128_album_streamline_desc,
                    category = WhatsNewCategory.FEATURES
                )
            )
            patch == 128 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v127_reload_loop_title,
                    descRes = R.string.wn_v127_reload_loop_desc,
                    category = WhatsNewCategory.FIXES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v127_sql_index_title,
                    descRes = R.string.wn_v127_sql_index_desc,
                    category = WhatsNewCategory.PERFORMANCE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v127_memory_cache_title,
                    descRes = R.string.wn_v127_memory_cache_desc,
                    category = WhatsNewCategory.PERFORMANCE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v127_album_stall_title,
                    descRes = R.string.wn_v127_album_stall_desc,
                    category = WhatsNewCategory.PERFORMANCE
                )
            )
            patch in 124..127 -> listOf(
                WhatsNewItem(
                    titleRes = R.string.wn_v125_autobackup_title,
                    descRes = R.string.wn_v125_autobackup_desc,
                    category = WhatsNewCategory.STORAGE
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v125_restore_prompt_title,
                    descRes = R.string.wn_v125_restore_prompt_desc,
                    category = WhatsNewCategory.FEATURES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v125_video_share_title,
                    descRes = R.string.wn_v125_video_share_desc,
                    category = WhatsNewCategory.FIXES
                ),
                WhatsNewItem(
                    titleRes = R.string.wn_v125_instant_timeline_title,
                    descRes = R.string.wn_v125_instant_timeline_desc,
                    category = WhatsNewCategory.PERFORMANCE
                )
            )
            patch == 123 -> listOf(
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
