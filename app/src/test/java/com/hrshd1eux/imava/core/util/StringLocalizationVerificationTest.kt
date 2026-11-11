package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringLocalizationVerificationTest {

    private val expectedLocaleFolders = listOf(
        "values",
        "values-es",
        "values-pt",
        "values-pt-rBR",
        "values-de",
        "values-ru",
        "values-fr",
        "values-hi",
        "values-bn",
        "values-mr",
        "values-te",
        "values-ta",
        "values-gu"
    )

    private fun getResDir(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res"),
            File("../app/src/main/res")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Could not find res directory in: $candidates")
    }

    private fun parseStringKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val nameAttr = node.attributes.getNamedItem("name")?.nodeValue
            if (!nameAttr.isNullOrBlank()) {
                keys.add(nameAttr)
            }
        }
        return keys
    }

    @Test
    fun testAllThirteenLocalesExist() {
        val resDir = getResDir()
        for (folder in expectedLocaleFolders) {
            val xmlFile = File(resDir, "$folder/strings.xml")
            assertTrue("Expected strings.xml to exist in $folder", xmlFile.exists())
        }
    }

    @Test
    fun testAllPhase3KeysArePresentAcrossAllLocales() {
        val resDir = getResDir()
        val defaultFile = File(resDir, "values/strings.xml")
        val defaultKeys = parseStringKeys(defaultFile)
        assertTrue("Default strings.xml should not be empty", defaultKeys.isNotEmpty())

        val requiredPhase3Keys = listOf(
            "items_selected_format",
            "lock_vault_now",
            "lock_vault_now_desc",
            "vault_security_settings",
            "vault_security_settings_desc",
            "menu_compress_images",
            "menu_compress_images_desc",
            "menu_manage_tags",
            "menu_manage_tags_desc",
            "menu_move_copy",
            "menu_move_copy_desc",
            "menu_create_pdf",
            "menu_create_pdf_desc",
            "menu_clean_copy",
            "menu_clean_copy_desc",
            "menu_lossless_rotate",
            "menu_lossless_rotate_desc",
            "menu_collage",
            "menu_collage_desc",
            "menu_compare",
            "menu_compare_desc",
            "menu_slideshow",
            "menu_slideshow_desc",
            "menu_print",
            "menu_print_desc",
            "menu_shift_timestamp",
            "menu_shift_timestamp_desc",
            "share_dialog_title",
            "share_choose_privacy",
            "share_strip_all",
            "share_strip_all_desc",
            "share_fuzz_gps",
            "share_fuzz_gps_desc",
            "share_full_metadata",
            "share_full_metadata_desc",
            "vault_hide_dialog_title",
            "vault_hide_dialog_message",
            "action_continue",
            "vault_setup_required_title",
            "vault_setup_required_message",
            "vault_setup_button",
            "restore_items_confirm_title",
            "restore_items_confirm_message",
            "trash_items_confirm_title",
            "trash_items_confirm_message",
            "delete_permanently_confirm_title",
            "delete_permanently_confirm_message",
            "new_album",
            "create_and_select",
            "copy_to_album",
            "move_to_album",
            "rename_file",
            "new_file_name",
            "enter_name",
            "filename_invalid_characters",
            "extension_preserved_format",
            "batch_rename_title",
            "rename_mode_numbered",
            "rename_mode_date_prefix",
            "rename_mode_find_replace",
            "rename_prefix",
            "rename_start_counter",
            "rename_date_prefix_desc",
            "rename_find_text",
            "rename_replace_with",
            "rename_live_preview",
            "rename_all_format",
            "rename_more_files_format",
            "bulk_compress_title",
            "bulk_compress_photos_selected",
            "bulk_compress_skipped_videos",
            "bulk_compress_desc",
            "target_size_per_photo",
            "custom_target_kb",
            "compressing_progress_format",
            "compress_button_format",
            "bulk_tag_applying_to",
            "bulk_tag_desc",
            "add_custom_tag",
            "add_custom_tag_hint",
            "active_tags_format",
            "no_tags_yet",
            "suggested_tags_title",
            "apply_tags",
            "scrub_album_title",
            "scrubbing_album_format",
            "scrubbing_progress_format",
            "scrub_album_warning",
            "scrub_preserve_date",
            "scrub_preserve_date_desc",
            "scrub_irreversible_warning",
            "scrub_photos_action",
            "edit_date_time_title",
            "edit_date_time_desc",
            "shift_relative",
            "set_exact_date",
            "direction_forward",
            "direction_backward",
            "unit_days",
            "unit_hours",
            "unit_mins",
            "unit_year",
            "unit_month",
            "unit_day",
            "unit_hour",
            "unit_minute",
            "action_apply",
            "app_locked_title",
            "app_locked_desc",
            "app_unlock_button"
        )

        for (folder in expectedLocaleFolders) {
            val xmlFile = File(resDir, "$folder/strings.xml")
            val localeKeys = parseStringKeys(xmlFile)
            for (key in requiredPhase3Keys) {
                assertTrue("Key '$key' is missing in $folder/strings.xml", localeKeys.contains(key))
            }
        }
    }

    @Test
    fun testAllPhase2BatchProgressKeysArePresentAcrossAllLocales() {
        val resDir = getResDir()
        val requiredPhase2Keys = listOf(
            "emptying_trash",
            "moving_media",
            "copying_media",
            "deleting_media"
        )

        for (folder in expectedLocaleFolders) {
            val xmlFile = File(resDir, "$folder/strings.xml")
            val localeKeys = parseStringKeys(xmlFile)
            for (key in requiredPhase2Keys) {
                assertTrue("Key '$key' is missing in $folder/strings.xml", localeKeys.contains(key))
            }
        }
    }
}
