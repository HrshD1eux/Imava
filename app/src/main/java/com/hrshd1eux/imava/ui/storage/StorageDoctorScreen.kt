package com.hrshd1eux.imava.ui.storage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.core.util.StorageDoctorUtils
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDoctorScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allMediaItems by viewModel.visibleMediaItems.collectAsState(initial = emptyList())

    var report by remember { mutableStateOf<StorageDoctorUtils.StorageReport?>(null) }
    var isAnalyzing by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val selectedItemIds = remember { mutableStateOf(setOf<Long>()) }

    var blurryPhotosList by remember { mutableStateOf<List<StorageDoctorUtils.BlurryPhotoItem>>(emptyList()) }
    var hasScannedBlur by remember { mutableStateOf(false) }
    var isScanningBlur by remember { mutableStateOf(false) }
    var blurScanProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(allMediaItems) {
        isAnalyzing = true
        report = StorageDoctorUtils.analyzeStorage(allMediaItems)
        isAnalyzing = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            TopAppBar(
                title = { Text(stringResource(R.string.doctor_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedItemIds.value.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val ids = selectedItemIds.value.toList()
                                scope.launch {
                                    HapticUtil.performClick(context)
                                    val itemsToDelete = allMediaItems.filter { it.id in ids }
                                    viewModel.deleteMediaItems(context, itemsToDelete)
                                    selectedItemIds.value = emptySet()
                                    Toast.makeText(context, "Moved ${itemsToDelete.size} items to Trash", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Trash Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

            if (isAnalyzing || report == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.doctor_analyzing_storage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val rep = report!!

                // Health Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "${StorageDoctorUtils.formatBytes(rep.totalPotentialReclaimBytes)} " + stringResource(R.string.doctor_cleanable_space),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.doctor_summary_format),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                var isPurgingGhostThumbs by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.doctor_ghost_purger_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.doctor_ghost_purger_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                if (!isPurgingGhostThumbs) {
                                    isPurgingGhostThumbs = true
                                    scope.launch {
                                        HapticUtil.performClick(context)
                                        val res = com.hrshd1eux.imava.core.util.ThumbnailPurgeUtil.purgeAndShredGhostThumbnails(context)
                                        val mb = String.format("%.1f MB", res.bytesReclaimed / (1024.0 * 1024.0))
                                        Toast.makeText(context, "Shredded ${res.filesPurged} ghost thumbnails ($mb freed)!", Toast.LENGTH_SHORT).show()
                                        HapticUtil.performSuccess(context)
                                        isPurgingGhostThumbs = false
                                    }
                                }
                            },
                            enabled = !isPurgingGhostThumbs
                        ) {
                            if (isPurgingGhostThumbs) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.action_purge))
                            }
                        }
                    }
                }

                // Category Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            HapticUtil.performSelection(context)
                        },
                        text = { Text(stringResource(R.string.doctor_large_videos, rep.largeVideos.size)) },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            HapticUtil.performSelection(context)
                        },
                        text = { Text(stringResource(R.string.doctor_screenshots, rep.staleScreenshots.size)) },
                        icon = { Icon(Icons.Default.Screenshot, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            HapticUtil.performSelection(context)
                        },
                        text = { Text(stringResource(R.string.doctor_burst_shots, rep.burstGroups.size)) },
                        icon = { Icon(Icons.Default.BurstMode, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            HapticUtil.performSelection(context)
                            if (!hasScannedBlur && !isScanningBlur) {
                                isScanningBlur = true
                                hasScannedBlur = true
                                scope.launch {
                                    val nonTrashedPhotos = allMediaItems.filter { !it.isTrashed }.filterIsInstance<MediaItem.Photo>()
                                    val detected = StorageDoctorUtils.scanBlurryPhotos(
                                        context = context,
                                        photos = nonTrashedPhotos,
                                        limit = 150,
                                        onProgress = { cur, tot -> blurScanProgress = Pair(cur, tot) }
                                    )
                                    blurryPhotosList = detected
                                    isScanningBlur = false
                                    blurScanProgress = null
                                    if (detected.isNotEmpty()) {
                                        HapticUtil.performSuccess(context)
                                    }
                                }
                            }
                        },
                        text = { Text(if (hasScannedBlur) stringResource(R.string.doctor_blurry_photos, blurryPhotosList.size) else stringResource(R.string.doctor_tab_blurry)) },
                        icon = { Icon(Icons.Default.BlurOn, contentDescription = null) }
                    )
                }

                // Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            if (rep.largeVideos.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.doctor_no_large_videos), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(rep.largeVideos, key = { it.id }) { video ->
                                    val isSelected = selectedItemIds.value.contains(video.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (selectedItemIds.value.isNotEmpty()) {
                                                    selectedItemIds.value = if (isSelected) selectedItemIds.value - video.id else selectedItemIds.value + video.id
                                                } else {
                                                    onMediaClick(video)
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            AsyncImage(
                                                model = video.uri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.align(Alignment.Center).size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = video.path.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${StorageDoctorUtils.formatBytes(video.size)} · ${video.durationMs / 1000}s",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedItemIds.value = if (isSelected) selectedItemIds.value - video.id else selectedItemIds.value + video.id
                                                HapticUtil.performSelection(context)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (rep.staleScreenshots.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.doctor_no_screenshots), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(rep.staleScreenshots, key = { it.id }) { shot ->
                                    val isSelected = selectedItemIds.value.contains(shot.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (selectedItemIds.value.isNotEmpty()) {
                                                    selectedItemIds.value = if (isSelected) selectedItemIds.value - shot.id else selectedItemIds.value + shot.id
                                                } else {
                                                    onMediaClick(shot)
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            AsyncImage(
                                                model = shot.uri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = shot.path.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                StorageDoctorUtils.formatBytes(shot.size),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedItemIds.value = if (isSelected) selectedItemIds.value - shot.id else selectedItemIds.value + shot.id
                                                HapticUtil.performSelection(context)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (rep.burstGroups.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.doctor_no_burst), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(rep.burstGroups) { group ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "${group.burstItems.size} Similar Shots (${StorageDoctorUtils.formatBytes(group.totalSizeBytes)})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.clickable {
                                                        val redundantIds = group.burstItems.drop(1).map { it.id }.toSet()
                                                        selectedItemIds.value = selectedItemIds.value + redundantIds
                                                        HapticUtil.performClick(context)
                                                        Toast.makeText(context, "Selected ${redundantIds.size} redundant shots for cleanup", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(stringResource(R.string.doctor_keep_best), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(group.burstItems, key = { it.id }) { item ->
                                                    val isSelected = selectedItemIds.value.contains(item.id)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(80.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { onMediaClick(item) }
                                                    ) {
                                                        AsyncImage(
                                                            model = item.uri,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (isScanningBlur) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            val (cur, tot) = blurScanProgress ?: Pair(0, 0)
                                            Text(
                                                text = stringResource(R.string.doctor_scanning_blurry, cur, tot),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            } else if (!hasScannedBlur) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BlurOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = stringResource(R.string.doctor_scan_blurry),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = stringResource(R.string.doctor_blurry_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    isScanningBlur = true
                                                    hasScannedBlur = true
                                                    scope.launch {
                                                        val nonTrashedPhotos = allMediaItems.filter { !it.isTrashed }.filterIsInstance<MediaItem.Photo>()
                                                        val detected = StorageDoctorUtils.scanBlurryPhotos(
                                                            context = context,
                                                            photos = nonTrashedPhotos,
                                                            limit = 150,
                                                            onProgress = { cur, tot -> blurScanProgress = Pair(cur, tot) }
                                                        )
                                                        blurryPhotosList = detected
                                                        isScanningBlur = false
                                                        blurScanProgress = null
                                                        if (detected.isNotEmpty()) {
                                                            HapticUtil.performSuccess(context)
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text(stringResource(R.string.doctor_scan_now))
                                            }
                                        }
                                    }
                                }
                            } else if (blurryPhotosList.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.doctor_no_blurry_photos), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${blurryPhotosList.size} blurry shots found",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextButton(
                                            onClick = {
                                                isScanningBlur = true
                                                scope.launch {
                                                    val nonTrashedPhotos = allMediaItems.filter { !it.isTrashed }.filterIsInstance<MediaItem.Photo>()
                                                    val detected = StorageDoctorUtils.scanBlurryPhotos(
                                                        context = context,
                                                        photos = nonTrashedPhotos,
                                                        limit = 150,
                                                        onProgress = { cur, tot -> blurScanProgress = Pair(cur, tot) }
                                                    )
                                                    blurryPhotosList = detected
                                                    isScanningBlur = false
                                                    blurScanProgress = null
                                                }
                                            }
                                        ) {
                                            Text(stringResource(R.string.doctor_rescan))
                                        }
                                    }
                                }
                                items(blurryPhotosList, key = { it.photo.id }) { item ->
                                    val photo = item.photo
                                    val isSelected = selectedItemIds.value.contains(photo.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (selectedItemIds.value.isNotEmpty()) {
                                                    selectedItemIds.value = if (isSelected) selectedItemIds.value - photo.id else selectedItemIds.value + photo.id
                                                } else {
                                                    onMediaClick(photo)
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            AsyncImage(
                                                model = photo.uri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = photo.path.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${StorageDoctorUtils.formatBytes(photo.size)} · Score: ${String.format("%.1f", item.variance)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedItemIds.value = if (isSelected) selectedItemIds.value - photo.id else selectedItemIds.value + photo.id
                                                HapticUtil.performSelection(context)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
