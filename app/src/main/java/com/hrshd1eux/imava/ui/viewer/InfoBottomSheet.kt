package com.hrshd1eux.imava.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.hrshd1eux.imava.R
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.exifinterface.media.ExifInterface
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.core.util.ExifLocationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExifDetails(
    val fileName: String,
    val filePath: String,
    val dateTaken: String?,
    val dateModified: String? = null,
    val cameraModel: String?,
    val resolution: String?,
    val fileSize: String?,
    val location: String?,
    val hasGps: Boolean,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val lensModel: String? = null,
    val software: String? = null,
    val exposureBias: String? = null,
    val meteringMode: String? = null,
    val colorSpace: String? = null,
    val digitalZoom: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    item: MediaItem,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onUpdateDateTaken: ((Long) -> Unit)? = null,
    onUpdateTags: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        MediaInfoContent(
            item = item,
            onUpdateDateTaken = onUpdateDateTaken,
            onUpdateTags = onUpdateTags,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Composable
fun MediaInfoContent(
    item: MediaItem,
    onUpdateDateTaken: ((Long) -> Unit)? = null,
    onUpdateTags: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val currentLocale = remember(configuration) {
        ConfigurationCompat.getLocales(configuration).get(0) ?: Locale.getDefault()
    }
    var details by remember(item) { mutableStateOf<ExifDetails?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var tagsList by remember(item.id, item.tags) { mutableStateOf(item.tags) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var showEditLocationDialog by remember { mutableStateOf(false) }
    var customLocationNameInput by remember { mutableStateOf("") }
    var newLatInput by remember { mutableStateOf("") }
    var newLngInput by remember { mutableStateOf("") }
    var locationSearchQuery by remember { mutableStateOf("") }
    var isSearchingLocation by remember { mutableStateOf(false) }
    var resolvedLocationName by remember { mutableStateOf<String?>(null) }
    var showRemoveGpsConfirm by remember { mutableStateOf(false) }
    var displayedPlaceName by remember(item.id) { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(item, currentLocale) {
        withContext(Dispatchers.IO) {
            details = readExifDetails(context, item, currentLocale)
        }
    }

    LaunchedEffect(item.id, details?.latitude, details?.longitude) {
        val customLoc = withContext(Dispatchers.IO) {
            ExifLocationUtil.getCustomLocation(context, item.id, item.path)
        }
        if (!customLoc.isNullOrBlank()) {
            displayedPlaceName = customLoc
        } else {
            val lat = details?.latitude
            val lng = details?.longitude
            if (details?.hasGps == true && lat != null && lng != null) {
                val name = withContext(Dispatchers.IO) { ExifLocationUtil.reverseGeocode(context, lat, lng) }
                displayedPlaceName = name
            } else {
                displayedPlaceName = null
            }
        }
    }

    var paletteColors by remember(item.id) { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(item.id) {
        if (item is com.hrshd1eux.imava.data.model.MediaItem.Photo) {
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = context.contentResolver.openInputStream(item.uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 8
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).maximumColorCount(8).generate()
                        val colors = mutableListOf<Int>()
                        palette.dominantSwatch?.rgb?.let { colors.add(it) }
                        palette.vibrantSwatch?.rgb?.let { if (!colors.contains(it)) colors.add(it) }
                        palette.darkVibrantSwatch?.rgb?.let { if (!colors.contains(it)) colors.add(it) }
                        palette.lightVibrantSwatch?.rgb?.let { if (!colors.contains(it)) colors.add(it) }
                        palette.mutedSwatch?.rgb?.let { if (!colors.contains(it)) colors.add(it) }
                        palette.darkMutedSwatch?.rgb?.let { if (!colors.contains(it)) colors.add(it) }
                        palette.lightMutedSwatch?.rgb?.let { if (!colors.contains(it)) colors.add(it) }
                        if (colors.isEmpty() && palette.swatches.isNotEmpty()) {
                            colors.addAll(palette.swatches.map { it.rgb })
                        }
                        paletteColors = colors.distinct().take(6)
                        bitmap.recycle()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    if (showDatePicker) {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = if (item.dateTaken > 0) item.dateTaken else System.currentTimeMillis()
        }
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val newCal = java.util.Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute)
                        }
                        val newTimestamp = newCal.timeInMillis
                        onUpdateDateTaken?.invoke(newTimestamp)
                        details = details?.copy(dateTaken = formatDateTaken(null, newTimestamp, currentLocale))
                        showDatePicker = false
                    },
                    currentHour,
                    currentMinute,
                    android.text.format.DateFormat.is24HourFormat(context)
                )
                timePickerDialog.show()
            },
            currentYear,
            currentMonth,
            currentDay
        )
        datePickerDialog.setOnDismissListener { showDatePicker = false }
        datePickerDialog.show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            details?.let { info ->
                // File Name
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    title = stringResource(R.string.info_file_name),
                    subtitle = info.fileName
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Album Name
                val resolvedAlbumName = if (item.bucketName.isNotBlank() && item.bucketName != "Unknown") {
                    item.bucketName
                } else {
                    java.io.File(item.path).parentFile?.name ?: stringResource(R.string.label_unknown)
                }
                InfoRow(
                    icon = Icons.Default.CollectionsBookmark,
                    title = stringResource(R.string.album_default_label),
                    subtitle = resolvedAlbumName
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Path
                InfoRow(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.info_path),
                    subtitle = info.filePath
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Section with Edit Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        InfoRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.info_date_taken),
                            subtitle = info.dateTaken ?: java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, currentLocale).format(Date(item.dateTaken))
                        )
                    }
                    if (onUpdateDateTaken != null) {
                        androidx.compose.material3.IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_date_time_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!info.dateModified.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.info_date_modified),
                        subtitle = info.dateModified
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera model section
                InfoRow(
                    icon = Icons.Default.PhotoCamera,
                    title = stringResource(R.string.info_camera_model),
                    subtitle = info.cameraModel ?: stringResource(R.string.info_no_exif_camera)
                )

                val shotDetails = listOfNotNull(
                    info.aperture?.let { "f/$it" },
                    info.shutterSpeed,
                    info.focalLength?.let { stringResource(R.string.exif_focal_mm_fmt, it) },
                    info.iso?.let { rawIso ->
                        rawIso.toIntOrNull()?.let { stringResource(R.string.exif_iso_fmt, it) } ?: "ISO $rawIso"
                    },
                    info.exposureBias
                ).joinToString("  ·  ")

                if (shotDetails.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.info_shot_details),
                        subtitle = shotDetails
                    )
                }

                val extraExif = listOfNotNull(
                    info.flash,
                    info.whiteBalance?.let { stringResource(R.string.info_wb_fmt, it) },
                    info.meteringMode?.let { stringResource(R.string.info_metering_fmt, it) },
                    info.colorSpace,
                    info.digitalZoom,
                    info.lensModel?.let { stringResource(R.string.info_lens_fmt, it) },
                    info.software?.let { stringResource(R.string.info_software_fmt, it) }
                ).joinToString("  ·  ")

                if (extraExif.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        icon = Icons.Default.CameraAlt,
                        title = stringResource(R.string.info_hardware_processing),
                        subtitle = extraExif
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File metadata details
                InfoRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.info_properties),
                    subtitle = "${info.resolution}  ·  ${info.fileSize}"
                )

                if (paletteColors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.info_dominant_colors),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        paletteColors.forEach { colorInt ->
                            val hex = String.format(Locale.US, "#%06X", 0xFFFFFF and colorInt)
                            val composeColor = androidx.compose.ui.graphics.Color(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(composeColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText(context.getString(R.string.info_hex_color_cd), hex)
                                        clipboard.setPrimaryClip(clip)
                                        com.hrshd1eux.imava.core.util.HapticUtil.performClick(context)
                                        android.widget.Toast.makeText(context, context.getString(R.string.info_copied_hex_toast, hex), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Offline Tags Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.info_tags_hashtags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { showAddTagDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.timeline_add_tag),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    item {
                        InputChip(
                            selected = false,
                            onClick = { showAddTagDialog = true },
                            label = { Text(stringResource(R.string.info_add_tag_btn)) }
                        )
                    }
                    if (tagsList.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.info_no_tags_yet),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }
                    } else {
                        items(tagsList) { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text("#$tag") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.info_remove_tag_cd),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                val updated = tagsList - tag
                                                tagsList = updated
                                                onUpdateTags?.invoke(updated)
                                            }
                                    )
                                }
                            )
                        }
                    }
                }

                if (info.hasGps || !displayedPlaceName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val gpsLine = if (info.hasGps) {
                        stringResource(R.string.info_gps_prefix_fmt, info.location ?: "") +
                            if (!info.altitude.isNullOrBlank()) stringResource(R.string.info_alt_prefix_fmt, info.altitude) else ""
                    } else ""
                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.info_location_title),
                        subtitle = when {
                            !displayedPlaceName.isNullOrBlank() && info.hasGps -> "${displayedPlaceName}\n$gpsLine"
                            !displayedPlaceName.isNullOrBlank() -> displayedPlaceName!!
                            info.hasGps -> gpsLine
                            else -> stringResource(R.string.info_location_added)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val uriStr = if (info.hasGps && info.latitude != 0.0) {
                                    "geo:${info.latitude},${info.longitude}?q=${info.latitude},${info.longitude}(${Uri.encode(displayedPlaceName ?: context.getString(R.string.info_location_title))})"
                                } else {
                                    "geo:0,0?q=${Uri.encode(displayedPlaceName ?: "")}"
                                }
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                                mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: android.content.ActivityNotFoundException) {
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.info_open_maps))
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                customLocationNameInput = displayedPlaceName ?: ""
                                newLatInput = if (info.hasGps && info.latitude != 0.0) info.latitude.toString() else ""
                                newLngInput = if (info.hasGps && info.longitude != 0.0) info.longitude.toString() else ""
                                locationSearchQuery = ""
                                resolvedLocationName = displayedPlaceName
                                showEditLocationDialog = true
                            },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(stringResource(R.string.action_edit))
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = { showRemoveGpsConfirm = true },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(stringResource(R.string.action_remove))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            customLocationNameInput = ""
                            newLatInput = ""
                            newLngInput = ""
                            locationSearchQuery = ""
                            resolvedLocationName = null
                            showEditLocationDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.info_add_location_btn))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } ?: Text(
                text = stringResource(R.string.info_loading_metadata),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showAddTagDialog) {
            AlertDialog(
                onDismissRequest = { showAddTagDialog = false },
                title = { Text(stringResource(R.string.info_add_tag_dialog_title)) },
                text = {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        label = { Text(stringResource(R.string.info_tag_name_label)) },
                        placeholder = { Text(stringResource(R.string.info_tag_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        enabled = newTagInput.isNotBlank(),
                        onClick = {
                            val clean = newTagInput.trim().removePrefix("#").lowercase()
                            if (clean.isNotEmpty() && !tagsList.contains(clean)) {
                                val updated = tagsList + clean
                                tagsList = updated
                                onUpdateTags?.invoke(updated)
                            }
                            newTagInput = ""
                            showAddTagDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.timeline_add_btn))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTagDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showRemoveGpsConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveGpsConfirm = false },
                title = { Text(stringResource(R.string.info_remove_location_title)) },
                text = { Text(stringResource(R.string.info_remove_location_desc)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showRemoveGpsConfirm = false
                            scope.launch {
                                val success = com.hrshd1eux.imava.core.util.ExifLocationUtil.removeAllLocation(context, item.id, item.uri, item.path)
                                if (success) {
                                    displayedPlaceName = null
                                    details = withContext(Dispatchers.IO) { readExifDetails(context, item, currentLocale) }
                                    android.widget.Toast.makeText(context, context.getString(R.string.info_location_removed_toast), android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, context.getString(R.string.info_location_remove_failed_toast), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_remove))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveGpsConfirm = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showEditLocationDialog) {
            val latVal = newLatInput.trim().toDoubleOrNull()
            val lngVal = newLngInput.trim().toDoubleOrNull()
            val hasValidCoords = latVal != null && latVal in -90.0..90.0 && lngVal != null && lngVal in -180.0..180.0
            val hasCustomName = customLocationNameInput.trim().isNotEmpty()
            val canSave = hasCustomName || hasValidCoords

            val executeLocationSearch: () -> Unit = {
                val queryToSearch = locationSearchQuery.ifBlank { customLocationNameInput }.trim()
                if (queryToSearch.isNotEmpty()) {
                    isSearchingLocation = true
                    scope.launch {
                        val res = ExifLocationUtil.geocode(context, queryToSearch)
                        isSearchingLocation = false
                        if (res != null) {
                            newLatInput = res.latitude.toString()
                            newLngInput = res.longitude.toString()
                            resolvedLocationName = res.displayName
                            if (customLocationNameInput.isBlank()) {
                                customLocationNameInput = res.displayName
                            }
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.info_online_lookup_fallback_toast),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showEditLocationDialog = false },
                title = { Text(stringResource(R.string.info_edit_location_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                    ) {
                        Text(
                            text = stringResource(R.string.info_edit_location_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customLocationNameInput,
                            onValueChange = { customLocationNameInput = it },
                            label = { Text(stringResource(R.string.info_place_name_label)) },
                            placeholder = { Text(stringResource(R.string.info_place_name_hint)) },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = locationSearchQuery,
                            onValueChange = { locationSearchQuery = it },
                            label = { Text(stringResource(R.string.info_find_gps_label)) },
                            placeholder = { Text(stringResource(R.string.info_find_gps_hint)) },
                            singleLine = true,
                            trailingIcon = {
                                if (isSearchingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        enabled = locationSearchQuery.isNotBlank() || customLocationNameInput.isNotBlank(),
                                        onClick = executeLocationSearch
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.info_search_gps_btn))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { executeLocationSearch() }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!resolvedLocationName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.info_pinpointed_fmt, resolvedLocationName ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.info_gps_coords_optional),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newLatInput,
                                onValueChange = { newLatInput = it },
                                label = { Text(stringResource(R.string.info_lat_label)) },
                                placeholder = { Text("28.6139") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newLngInput,
                                onValueChange = { newLngInput = it },
                                label = { Text(stringResource(R.string.info_lng_label)) },
                                placeholder = { Text("77.2090") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = canSave,
                        onClick = {
                            val locName = customLocationNameInput.trim().ifEmpty { resolvedLocationName }
                            val lat = if (hasValidCoords) latVal else null
                            val lng = if (hasValidCoords) lngVal else null
                            showEditLocationDialog = false
                            scope.launch {
                                val success = com.hrshd1eux.imava.core.util.ExifLocationUtil.setCustomLocation(
                                    context = context,
                                    mediaId = item.id,
                                    uri = item.uri,
                                    path = item.path,
                                    locationName = locName,
                                    latitude = lat,
                                    longitude = lng
                                )
                                if (success) {
                                    displayedPlaceName = locName ?: if (lat != null && lng != null) {
                                        withContext(Dispatchers.IO) { ExifLocationUtil.reverseGeocode(context, lat, lng) }
                                    } else null
                                    details = withContext(Dispatchers.IO) { readExifDetails(context, item, currentLocale) }
                                    android.widget.Toast.makeText(context, context.getString(R.string.info_location_saved_toast), android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, context.getString(R.string.info_location_save_failed_toast), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditLocationDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun formatDateTaken(rawExifDate: String?, timestampMs: Long, locale: Locale = Locale.getDefault()): String {
    val outputFormatter = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, locale)
    if (!rawExifDate.isNullOrEmpty()) {
        val formatsToTry = arrayOf(
            "yyyy:MM:dd HH:mm:ss",
            "yyyy:MM:dd HH:mm",
            "yyyy:MM:dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (format in formatsToTry) {
            try {
                val parser = SimpleDateFormat(format, Locale.US)
                val parsed = parser.parse(rawExifDate)
                if (parsed != null) {
                    return outputFormatter.format(parsed)
                }
            } catch (_: Exception) { }
        }
    }
    val safeMs = if (timestampMs > 0) timestampMs else System.currentTimeMillis()
    return outputFormatter.format(Date(safeMs))
}

private fun readExifDetails(context: Context, item: MediaItem, locale: Locale = Locale.getDefault()): ExifDetails {
    val file = File(item.path)
    val fileName = file.name.ifEmpty { item.uri.lastPathSegment ?: context.getString(R.string.label_unknown) }
    val filePath = if (file.exists()) file.absolutePath else item.path

    var exif: ExifInterface? = null
    if (file.exists() && file.canRead()) {
        try {
            exif = ExifInterface(file.absolutePath)
        } catch (_: Exception) {}
    }

    if (exif == null) {
        try {
            val contentUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && item.uri.scheme == "content") {
                try {
                    android.provider.MediaStore.setRequireOriginal(item.uri)
                } catch (_: Exception) {
                    item.uri
                }
            } else {
                item.uri
            }
            context.contentResolver.openInputStream(contentUri)?.use { stream ->
                exif = ExifInterface(stream)
            }
        } catch (_: Exception) {}
    }

    val fileObj = File(filePath)
    val lastModMs = if (fileObj.exists()) fileObj.lastModified() else 0L
    val diffFromTaken = if (lastModMs > 0L && item.dateTaken > 0L) kotlin.math.abs(lastModMs - item.dateTaken) else 0L
    val dateModifiedText = if (diffFromTaken > 120_000L) {
        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, locale).format(Date(lastModMs))
    } else null

    if (exif == null) {
        return ExifDetails(
            fileName = fileName,
            filePath = filePath,
            dateTaken = formatDateTaken(null, item.dateTaken, locale),
            dateModified = dateModifiedText,
            cameraModel = null,
            resolution = "${item.width} × ${item.height}",
            fileSize = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(item.size),
            location = null,
            hasGps = false
        )
    }

    return try {
        val nonNullExif = exif!!
        val latLong = nonNullExif.latLong
        val hasGps = latLong != null && latLong.size >= 2
        
        val cameraModel = nonNullExif.getAttribute(ExifInterface.TAG_MODEL)
        val cameraMake = nonNullExif.getAttribute(ExifInterface.TAG_MAKE)
        val modelText = if (cameraModel != null) "${cameraMake ?: ""} $cameraModel".trim() else null

        val resolutionText = "${item.width} × ${item.height}"
        val sizeText = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(item.size)
        val rawDate = nonNullExif.getAttribute(ExifInterface.TAG_DATETIME) ?: nonNullExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        val formattedDate = formatDateTaken(rawDate, item.dateTaken, locale)

        val fNumber = nonNullExif.getAttribute(ExifInterface.TAG_F_NUMBER)
        val expTime = nonNullExif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()?.let { sec ->
            if (sec < 1.0 && sec > 0.0) {
                val denom = Math.round(1.0 / sec)
                context.getString(R.string.exif_shutter_frac_fmt, denom)
            } else {
                context.getString(R.string.exif_shutter_sec_fmt, sec)
            }
        }
        val iso = nonNullExif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ?: nonNullExif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        val focalLen = nonNullExif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).let { if (it > 0) String.format(locale, "%.1f", it) else null }
        val flashVal = nonNullExif.getAttributeInt(ExifInterface.TAG_FLASH, -1).let {
            if (it == -1) null else if ((it and 1) != 0) context.getString(R.string.exif_flash_fired) else context.getString(R.string.exif_flash_off)
        }
        val wbVal = nonNullExif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1).let {
            when (it) {
                ExifInterface.WHITE_BALANCE_AUTO.toInt() -> context.getString(R.string.exif_auto)
                ExifInterface.WHITE_BALANCE_MANUAL.toInt() -> context.getString(R.string.exif_manual)
                else -> null
            }
        }
        val lensModel = nonNullExif.getAttribute(ExifInterface.TAG_LENS_MODEL)
        val software = nonNullExif.getAttribute(ExifInterface.TAG_SOFTWARE)
        val altitudeVal = nonNullExif.getAltitude(0.0).let { if (it > 0.0) context.getString(R.string.exif_alt_m_fmt, it) else null }
        val expBias = nonNullExif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE)?.toDoubleOrNull()?.let {
            if (it != 0.0) context.getString(R.string.exif_ev_fmt, it) else null
        }
        val meterVal = nonNullExif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1).let {
            when (it) {
                1 -> context.getString(R.string.exif_metering_average)
                2 -> context.getString(R.string.exif_metering_center_weighted)
                3 -> context.getString(R.string.exif_metering_spot)
                4 -> context.getString(R.string.exif_metering_multi_spot)
                5 -> context.getString(R.string.exif_metering_pattern)
                6 -> context.getString(R.string.exif_metering_partial)
                else -> null
            }
        }
        val colorSpaceVal = nonNullExif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1).let {
            when (it) {
                1 -> "sRGB"
                65535 -> "Adobe RGB"
                else -> null
            }
        }
        val digitalZoomVal = nonNullExif.getAttributeDouble(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, 0.0).let {
            if (it > 1.0) context.getString(R.string.exif_zoom_fmt, it) else null
        }

        ExifDetails(
            fileName = fileName,
            filePath = filePath,
            dateTaken = formattedDate,
            dateModified = dateModifiedText,
            cameraModel = modelText,
            resolution = resolutionText,
            fileSize = sizeText,
            location = if (hasGps && latLong != null) String.format(locale, "%.4f, %.4f", latLong[0], latLong[1]) else null,
            hasGps = hasGps,
            latitude = if (hasGps && latLong != null) latLong[0] else 0.0,
            longitude = if (hasGps && latLong != null) latLong[1] else 0.0,
            altitude = altitudeVal,
            aperture = fNumber,
            shutterSpeed = expTime,
            iso = iso,
            focalLength = focalLen,
            flash = flashVal,
            whiteBalance = wbVal,
            lensModel = lensModel,
            software = software,
            exposureBias = expBias,
            meteringMode = meterVal,
            colorSpace = colorSpaceVal,
            digitalZoom = digitalZoomVal
        )
    } catch (e: Exception) {
        ExifDetails(
            fileName = fileName,
            filePath = filePath,
            dateTaken = formatDateTaken(null, item.dateTaken, locale),
            dateModified = dateModifiedText,
            cameraModel = null,
            resolution = "${item.width} × ${item.height}",
            fileSize = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(item.size),
            location = null,
            hasGps = false
        )
    }
}
