package com.hrshd1eux.imava.ui.albums

import android.content.Context
import android.net.Uri
import androidx.compose.ui.res.stringResource
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.findActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.OutlinedCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.automirrored.filled.ViewList
import com.hrshd1eux.imava.ui.common.ScrubAlbumDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hrshd1eux.imava.data.media.BucketInfo
import com.hrshd1eux.imava.ui.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onStorageDoctorClick: () -> Unit = {}
) {
    val favorites by viewModel.favorites.collectAsState()
    val trashed by viewModel.trashed.collectAsState()
    val hidden by viewModel.hidden.collectAsState()
    val videosCount by viewModel.videosCount.collectAsState()
    val allMedia by viewModel.visibleMediaItems.collectAsState()
    val throwbackMemories by viewModel.throwbackMemories.collectAsState()

    val context = LocalContext.current
    var showHiddenLockedDialog by remember { mutableStateOf(false) }

    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    val systemFolderNames = remember { setOf("Camera", "Screenshots", "Download", "WhatsApp Images", "WhatsApp Video", "Pictures", "Movies", "DCIM", "Instagram", "Snapchat", "Telegram") }
    var activeOptionsBucket by remember { mutableStateOf<BucketInfo?>(null) }
    var showDeleteAlbumConfirm by remember { mutableStateOf(false) }
    var bucketToScrub by remember { mutableStateOf<BucketInfo?>(null) }
    var isScrubbingAlbum by remember { mutableStateOf(false) }
    var scrubProgressCurrent by remember { mutableStateOf(0) }
    var scrubProgressTotal by remember { mutableStateOf(0) }

    val visibleBuckets by viewModel.visibleBuckets.collectAsState()
    val pinnedBucketIds by viewModel.pinnedBucketIds.collectAsState()
    val customAlbumCovers by viewModel.customAlbumCovers.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val albumLayoutMode by viewModel.albumLayoutMode.collectAsState()
    val vaultConfigVersion by viewModel.vaultConfigVersion.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()

    // Real-time lookup of newest media item URI by bucket ID directly from live timeline
    val latestMediaByBucketId = remember(allMedia) {
        val map = mutableMapOf<Long, Uri>()
        for (item in allMedia) {
            if (!map.containsKey(item.bucketId)) {
                map[item.bucketId] = item.uri
            }
        }
        map
    }

    val resolveAlbumCount = remember(allMedia) {
        { bucket: BucketInfo ->
            if (allMedia.isNotEmpty()) {
                allMedia.count { it.bucketId == bucket.id || (bucket.mergedBucketIds.isNotEmpty() && it.bucketId in bucket.mergedBucketIds) }
            } else {
                bucket.count
            }
        }
    }

    val resolveAlbumCoverUri = remember(allMedia, customAlbumCovers, latestMediaByBucketId) {
        { bucket: BucketInfo ->
            val liveCount = resolveAlbumCount(bucket)
            if (liveCount <= 0) {
                null
            } else {
                val customMediaId = customAlbumCovers[bucket.id]
                val customUri = if (customMediaId != null) allMedia.find { it.id == customMediaId }?.uri else null
                val liveCoverUri = bucket.mergedBucketIds.firstNotNullOfOrNull { latestMediaByBucketId[it] }
                customUri ?: liveCoverUri ?: (if (allMedia.isEmpty()) bucket.coverUri else null)
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var showLayoutMenu by remember { mutableStateOf(false) }

    val pinnedBuckets = remember(visibleBuckets, pinnedBucketIds) {
        visibleBuckets.filter { pinnedBucketIds.contains(it.id.toString()) }
    }
    val unpinnedBuckets = remember(visibleBuckets, pinnedBucketIds) {
        visibleBuckets.filter { !pinnedBucketIds.contains(it.id.toString()) }
    }

    val openBucket: (BucketInfo) -> Unit = { bucket ->
        if (com.hrshd1eux.imava.core.util.AppLockManager.isAlbumLocked(context, bucket.id, bucket.name)) {
            com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                activity = context,
                title = "Locked Album",
                subtitle = "Authenticate to open ${bucket.name}",
                onSuccess = {
                    com.hrshd1eux.imava.core.util.AppLockManager.markAlbumUnlockedForSession(bucket.id)
                    viewModel.selectBucket(bucket.id, bucket.name, bucket.mergedBucketIds)
                },
                onError = { err ->
                    android.widget.Toast.makeText(context, "Album locked: $err", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            viewModel.selectBucket(bucket.id, bucket.name, bucket.mergedBucketIds)
        }
    }

    val vaultPrefs = remember(context) { context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE) }
    val isStealthMode = remember(vaultConfigVersion, isVaultUnlocked) { vaultPrefs.getBoolean("vault_stealth_mode", false) }
    val isVaultDisabled = remember(vaultConfigVersion, isVaultUnlocked) { vaultPrefs.getBoolean("vault_disabled", false) }
    val isPinConfigured = remember(vaultConfigVersion, isVaultUnlocked) {
        vaultPrefs.getString("vault_pin_hash", null) != null || vaultPrefs.getString("vault_pin", null) != null
    }

    val storageStats by viewModel.storageBreakdown.collectAsState()
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var bucketToWiFiDrop by remember { mutableStateOf<BucketInfo?>(null) }

    val numCols = when (albumLayoutMode) {
        MainViewModel.AlbumLayoutMode.LIST -> 1
        MainViewModel.AlbumLayoutMode.GRID_2 -> 2
        MainViewModel.AlbumLayoutMode.GRID_3 -> 3
        MainViewModel.AlbumLayoutMode.GRID_4 -> 4
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Search }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            if (throwbackMemories.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        com.hrshd1eux.imava.ui.timeline.MemoriesStoriesHeader(
                            memories = throwbackMemories,
                            onStoryClick = { story ->
                                viewModel.activeMemoryStory = story
                            },
                            onDismiss = {
                                viewModel.dismissMemoriesFor24Hours()
                            }
                        )
                    }
                }
            }


            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { onStorageDoctorClick() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.doctor_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = stringResource(R.string.doctor_analyze, storageStats.formattedTotal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.doctor_photos_count, storageStats.photosCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageStats.formattedPhotos,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.doctor_videos_count, storageStats.videosCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageStats.formattedVideos,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.doctor_vault_count, storageStats.vaultCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageStats.formattedVault,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.doctor_trash_count, storageStats.trashCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageStats.formattedTrash,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }


            item {
                AlbumRowItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.category_favorites),
                    count = favorites.size,
                    onClick = {
                        viewModel.currentBucketId = null
                        viewModel.currentBucketName = null
                        viewModel.currentCategoryName = "Favorites"
                        viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Photos
                    }
                )
            }


            item {
                AlbumRowItem(
                    icon = Icons.Default.PlayCircle,
                    title = stringResource(R.string.category_videos),
                    count = videosCount,
                    onClick = {
                        viewModel.currentBucketId = null
                        viewModel.currentBucketName = null
                        viewModel.currentCategoryName = "Videos"
                        viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Photos
                    }
                )
            }


            if (!isStealthMode) {
                item {
                    val unlockVaultTitle = stringResource(R.string.unlock_vault_title)
                    val unlockVaultSubtitle = stringResource(R.string.unlock_vault_desc)
                    AlbumRowItem(
                        icon = if (isVaultDisabled || !isPinConfigured) Icons.Default.LockOpen else Icons.Default.Lock,
                        title = stringResource(R.string.category_vault),
                        count = hidden.size,
                        onClick = {
                            if (isVaultDisabled || !isPinConfigured) {
                                viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.VaultSetup
                                return@AlbumRowItem
                            }

                            val activity = context.findActivity()
                            val isBiometricEnabled = vaultPrefs.getBoolean("vault_biometric_enabled", false)
                            if (isBiometricEnabled && activity != null) {
                                com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    title = unlockVaultTitle,
                                    subtitle = unlockVaultSubtitle,
                                    onSuccess = {
                                        viewModel.unlockVault()
                                        viewModel.currentBucketId = null
                                        viewModel.currentBucketName = null
                                        viewModel.currentCategoryName = "Hidden Vault"
                                        viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Photos
                                    },
                                    onError = { _ ->
                                        showHiddenLockedDialog = true
                                    }
                                )
                            } else {
                                showHiddenLockedDialog = true
                            }
                        }
                    )
                }
            }


            item {
                val isTrashLocked = com.hrshd1eux.imava.core.util.AppLockManager.isTrashLocked(context)
                val isTrashConfigured = com.hrshd1eux.imava.core.util.AppLockManager.isTrashLockActive(context)
                val unlockTrashTitle = stringResource(R.string.unlock_trash_title)
                val unlockTrashSubtitle = stringResource(R.string.unlock_trash_desc)
                AlbumRowItem(
                    icon = Icons.Default.Delete,
                    title = if (isTrashLocked) "${stringResource(R.string.category_trash)} 🔒" else stringResource(R.string.category_trash),
                    count = if (isTrashLocked) 0 else trashed.size,
                    subtitle = if (isTrashConfigured) {
                        if (isTrashLocked) stringResource(R.string.trash_locked_subtitle) else stringResource(R.string.trash_unlocked_subtitle)
                    } else null,
                    actionLabel = if (isTrashLocked) stringResource(R.string.label_locked) else null,
                    onClick = {
                        if (isTrashLocked) {
                            val activity = context.findActivity()
                            if (activity != null) {
                                com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    title = unlockTrashTitle,
                                    subtitle = unlockTrashSubtitle,
                                    onSuccess = {
                                        com.hrshd1eux.imava.core.util.AppLockManager.markTrashUnlockedForSession()
                                        com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                        viewModel.currentBucketId = null
                                        viewModel.currentBucketName = null
                                        viewModel.currentCategoryName = "Trash"
                                        viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Photos
                                    },
                                    onError = { err ->
                                        android.widget.Toast.makeText(context, "Trash locked: $err", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                android.widget.Toast.makeText(context, "Biometric required to view Trash", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.currentBucketId = null
                            viewModel.currentBucketName = null
                            viewModel.currentCategoryName = "Trash"
                            viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Photos
                        }
                    }
                )
            }


            item {
                AlbumRowItem(
                    icon = Icons.Default.CleaningServices,
                    title = stringResource(R.string.duplicate_finder_title),
                    count = 0,
                    subtitle = stringResource(R.string.duplicate_finder_desc),
                    onClick = {
                        viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.DuplicateFinder
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
            }


            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.albums_count_format, visibleBuckets.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Box {
                            IconButton(onClick = { showLayoutMenu = true }) {
                                Icon(
                                    imageVector = if (numCols == 1) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Change Layout",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showLayoutMenu,
                                onDismissRequest = { showLayoutMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.layout_list_view)) },
                                    onClick = {
                                        viewModel.setAlbumLayoutMode(MainViewModel.AlbumLayoutMode.LIST)
                                        showLayoutMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.layout_grid_2)) },
                                    onClick = {
                                        viewModel.setAlbumLayoutMode(MainViewModel.AlbumLayoutMode.GRID_2)
                                        showLayoutMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.layout_grid_3)) },
                                    onClick = {
                                        viewModel.setAlbumLayoutMode(MainViewModel.AlbumLayoutMode.GRID_3)
                                        showLayoutMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.layout_grid_4)) },
                                    onClick = {
                                        viewModel.setAlbumLayoutMode(MainViewModel.AlbumLayoutMode.GRID_4)
                                        showLayoutMenu = false
                                    }
                                )
                            }
                        }


                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort Albums",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text((if (albumSortOrder == MainViewModel.AlbumSortOrder.NAME_ASC) "✓ " else "") + stringResource(R.string.sort_name_asc)) },
                                    onClick = {
                                        viewModel.setAlbumSortOrder(MainViewModel.AlbumSortOrder.NAME_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text((if (albumSortOrder == MainViewModel.AlbumSortOrder.COUNT_DESC) "✓ " else "") + stringResource(R.string.sort_count_desc)) },
                                    onClick = {
                                        viewModel.setAlbumSortOrder(MainViewModel.AlbumSortOrder.COUNT_DESC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text((if (albumSortOrder == MainViewModel.AlbumSortOrder.RECENT) "✓ " else "") + stringResource(R.string.sort_recent)) },
                                    onClick = {
                                        viewModel.setAlbumSortOrder(MainViewModel.AlbumSortOrder.RECENT)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }


            if (pinnedBuckets.isNotEmpty()) {
                item {
                    Text(
                        text = "Pinned 📌",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (numCols == 1) {
                    items(pinnedBuckets, key = { it.id }) { bucket ->
                        val liveCount = resolveAlbumCount(bucket)
                        val coverUri = resolveAlbumCoverUri(bucket)
                        AlbumRowItem(
                            icon = Icons.Default.Folder,
                            coverUri = coverUri,
                            title = "${bucket.name} 📌",
                            count = liveCount,
                            subtitle = if (bucket.paths.size > 1) "${bucket.paths.size} folder locations" else null,
                            onClick = { openBucket(bucket) },
                            onLongClick = { activeOptionsBucket = bucket }
                        )
                    }
                } else {
                    val chunkedPinned = pinnedBuckets.chunked(numCols)
                    items(chunkedPinned, key = { row -> row.map { it.id }.joinToString("-") }) { rowBuckets ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBuckets.forEach { bucket ->
                                val liveCount = resolveAlbumCount(bucket)
                                val coverUri = resolveAlbumCoverUri(bucket)
                                Box(modifier = Modifier.weight(1f)) {
                                    AlbumGridCard(
                                        bucket = bucket.copy(count = liveCount),
                                        coverUri = coverUri,
                                        isPinned = true,
                                        onClick = { openBucket(bucket) },
                                        onLongClick = { activeOptionsBucket = bucket },
                                        layoutMode = albumLayoutMode
                                    )
                                }
                            }
                            for (i in rowBuckets.size until numCols) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (visibleBuckets.isEmpty()) {
                if (allMedia.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_albums_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (numCols == 1) {
                    items(unpinnedBuckets, key = { it.id }) { bucket ->
                        val liveCount = resolveAlbumCount(bucket)
                        val coverUri = resolveAlbumCoverUri(bucket)
                        AlbumRowItem(
                            icon = Icons.Default.Folder,
                            coverUri = coverUri,
                            title = bucket.name,
                            count = liveCount,
                            subtitle = if (bucket.paths.size > 1) "${bucket.paths.size} folder locations" else null,
                            onClick = { openBucket(bucket) },
                            onLongClick = { activeOptionsBucket = bucket }
                        )
                    }
                } else {
                    val chunkedUnpinned = unpinnedBuckets.chunked(numCols)
                    items(chunkedUnpinned, key = { row -> row.map { it.id }.joinToString("-") }) { rowBuckets ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBuckets.forEach { bucket ->
                                val liveCount = resolveAlbumCount(bucket)
                                val coverUri = resolveAlbumCoverUri(bucket)
                                Box(modifier = Modifier.weight(1f)) {
                                    AlbumGridCard(
                                        bucket = bucket.copy(count = liveCount),
                                        coverUri = coverUri,
                                        isPinned = false,
                                        onClick = { openBucket(bucket) },
                                        onLongClick = { activeOptionsBucket = bucket },
                                        layoutMode = albumLayoutMode
                                    )
                                }
                            }
                            for (i in rowBuckets.size until numCols) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (activeOptionsBucket != null) {
            val bucket = activeOptionsBucket!!
            val isCustomAlbum = !systemFolderNames.contains(bucket.name)
            val isPinned = pinnedBucketIds.contains(bucket.id.toString())

            val isAlbumLockedConfigured = com.hrshd1eux.imava.core.util.AppLockManager.isAlbumConfiguredLocked(context, bucket.id, bucket.name)
            val isAutoScrub = viewModel.isAutoScrubEnabled(bucket.id)
            val isRootSystemAlbum = bucket.name.equals("Camera", ignoreCase = true) ||
                    bucket.name.equals("DCIM", ignoreCase = true) ||
                    bucket.name.equals("Pictures", ignoreCase = true) ||
                    bucket.name.equals("Download", ignoreCase = true) ||
                    bucket.name.equals("Downloads", ignoreCase = true) ||
                    bucket.name.equals("Movies", ignoreCase = true) ||
                    bucket.name.equals("Screenshots", ignoreCase = true) ||
                    bucket.path.equals(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).absolutePath, ignoreCase = true) ||
                    bucket.path.equals(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM).absolutePath, ignoreCase = true) ||
                    bucket.path.equals(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath, ignoreCase = true) ||
                    bucket.path.equals(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES).absolutePath, ignoreCase = true)

            AlertDialog(
                onDismissRequest = { activeOptionsBucket = null },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bucket.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${bucket.count} items",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section 1: Organization
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.togglePinBucket(bucket.id)
                                            activeOptionsBucket = null
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isPinned) stringResource(R.string.album_unpin) else stringResource(R.string.album_pin_top), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(R.string.album_pin_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.excludeBucket(bucket.id)
                                            activeOptionsBucket = null
                                            android.widget.Toast.makeText(context, "Album hidden from gallery", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.album_exclude), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(R.string.album_exclude_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (isCustomAlbum) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                activeOptionsBucket = null
                                                viewModel.shareAlbum(context, bucket.id, bucket.name)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(stringResource(R.string.album_share), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(stringResource(R.string.album_share_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Section 2: Privacy & Protection
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (isAlbumLockedConfigured) {
                                                com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                                                    activity = context,
                                                    title = "Unlock Album",
                                                    subtitle = "Verify biometric to unlock ${bucket.name}",
                                                    onSuccess = {
                                                        com.hrshd1eux.imava.core.util.AppLockManager.unlockAlbum(context, bucket.id, bucket.name, bucket.path)
                                                        viewModel.refreshAll()
                                                        activeOptionsBucket = null
                                                        android.widget.Toast.makeText(context, "Album unlocked", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { err ->
                                                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                                                    activity = context,
                                                    title = "Lock Album",
                                                    subtitle = "Verify biometric to lock ${bucket.name}",
                                                    onSuccess = {
                                                        com.hrshd1eux.imava.core.util.AppLockManager.lockAlbum(context, bucket.id, bucket.name, bucket.path)
                                                        viewModel.refreshAll()
                                                        activeOptionsBucket = null
                                                        android.widget.Toast.makeText(context, "Album locked with Biometrics", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { err ->
                                                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAlbumLockedConfigured) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isAlbumLockedConfigured) stringResource(R.string.album_unlock) else stringResource(R.string.album_lock_bio), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(if (isAlbumLockedConfigured) stringResource(R.string.album_unlock_desc) else stringResource(R.string.album_lock_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val b = bucket
                                            activeOptionsBucket = null
                                            bucketToScrub = b
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.album_scrub_meta), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(R.string.album_scrub_meta_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val newState = viewModel.toggleAutoScrubBucket(bucket.id)
                                            activeOptionsBucket = null
                                            com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                            android.widget.Toast.makeText(
                                                context,
                                                if (newState) "Auto-Scrub enabled for ${bucket.name}" else "Auto-Scrub disabled for ${bucket.name}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.album_auto_scrub), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(if (isAutoScrub) stringResource(R.string.album_auto_scrub_on) else stringResource(R.string.album_auto_scrub_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val b = bucket
                                            activeOptionsBucket = null
                                            bucketToWiFiDrop = b
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.album_wifi_drop), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(R.string.album_wifi_drop_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Section 3: Danger Zone (Custom Albums only)
                        if (!isRootSystemAlbum) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            showDeleteAlbumConfirm = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.delete_album), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                        Text(stringResource(R.string.album_delete_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeOptionsBucket = null }) { Text(stringResource(R.string.action_done)) }
                }
            )

            if (showEmptyTrashConfirm) {
                AlertDialog(
                    onDismissRequest = { showEmptyTrashConfirm = false },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text(stringResource(R.string.album_empty_trash_title)) },
                    text = { Text(stringResource(R.string.album_empty_trash_desc)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEmptyTrashConfirm = false
                                viewModel.emptyTrash(context)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.empty_trash))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEmptyTrashConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
                    }
                )
            }

            if (showDeleteAlbumConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteAlbumConfirm = false },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text(stringResource(R.string.album_delete_confirm_title, bucket.name)) },
                    text = {
                        Column {
                            if (bucket.paths.size > 1) {
                                Text(
                                    text = "⚠️ This album unifies ${bucket.paths.size} separate folder locations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Text(stringResource(R.string.album_delete_choose_desc))
                            Spacer(modifier = Modifier.height(16.dp))

                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    showDeleteAlbumConfirm = false
                                    viewModel.deleteAlbum(context, bucket.id, bucket.name, bucket.path, deleteMedia = false)
                                    activeOptionsBucket = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(stringResource(R.string.album_delete_only), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(R.string.album_delete_only_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    showDeleteAlbumConfirm = false
                                    viewModel.deleteAlbum(context, bucket.id, bucket.name, bucket.path, deleteMedia = true)
                                    activeOptionsBucket = null
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(stringResource(R.string.album_delete_media), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(R.string.album_delete_media_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showDeleteAlbumConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
                    }
                )
            }
        }

        if (bucketToScrub != null) {
            val b = bucketToScrub!!
            ScrubAlbumDialog(
                albumName = b.name,
                itemCount = b.count,
                isScrubbing = isScrubbingAlbum,
                progressCurrent = scrubProgressCurrent,
                progressTotal = scrubProgressTotal,
                onDismiss = {
                    if (!isScrubbingAlbum) {
                        bucketToScrub = null
                    }
                },
                onConfirmScrub = { options ->
                    isScrubbingAlbum = true
                    scrubProgressCurrent = 0
                    scrubProgressTotal = b.count
                    viewModel.scrubAlbumMetadata(
                        context = context,
                        bucketId = b.id,
                        options = options,
                        onProgress = { current, total ->
                            scrubProgressCurrent = current
                            scrubProgressTotal = total
                        },
                        onComplete = { count ->
                            isScrubbingAlbum = false
                            bucketToScrub = null
                            if (count > 0) {
                                com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                android.widget.Toast.makeText(context, "Sanitized $count photos in ${b.name}", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "No photos required sanitization in ${b.name}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            )
        }

        if (bucketToWiFiDrop != null) {
            val b = bucketToWiFiDrop!!
            val allVisible by viewModel.visibleMediaItems.collectAsState()
            val albumItems = remember(b, allVisible) {
                allVisible.filter { it.bucketId == b.id || (b.mergedBucketIds.isNotEmpty() && it.bucketId in b.mergedBucketIds) }
            }
            com.hrshd1eux.imava.ui.p2p.ImavaDropDialog(
                itemsToShare = albumItems,
                onDismissRequest = { bucketToWiFiDrop = null }
            )
        }

        FloatingActionButton(
            onClick = { showCreateAlbumDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Create Album")
        }

        if (showCreateAlbumDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCreateAlbumDialog = false 
                    newAlbumName = ""
                },
                title = { Text(stringResource(R.string.create_new_album)) },
                text = {
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text(stringResource(R.string.album_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newAlbumName.isNotBlank()) {
                                viewModel.createEmptyAlbum(context, newAlbumName.trim())
                                showCreateAlbumDialog = false
                                newAlbumName = ""
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_create))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showCreateAlbumDialog = false 
                            newAlbumName = ""
                        }
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showHiddenLockedDialog) {
            com.hrshd1eux.imava.ui.vault.VaultUnlockDialog(
                onDismiss = { showHiddenLockedDialog = false },
                onUnlockSuccess = {
                    viewModel.unlockVault()
                    showHiddenLockedDialog = false
                    viewModel.currentBucketId = null
                    viewModel.currentBucketName = null
                    viewModel.currentCategoryName = "Hidden Vault"
                    viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Photos
                },
                onUnlockDecoy = {
                    viewModel.unlockDecoyVault()
                    showHiddenLockedDialog = false
                    viewModel.currentBucketId = null
                    viewModel.currentBucketName = null
                    viewModel.currentCategoryName = "Hidden Vault"
                    viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.Photos
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumGridCard(
    bucket: BucketInfo,
    coverUri: Uri?,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    layoutMode: MainViewModel.AlbumLayoutMode
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = bucket.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (isPinned) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📌", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                val isAlbumLocked = com.hrshd1eux.imava.core.util.AppLockManager.isAlbumConfiguredLocked(LocalContext.current, bucket.id, bucket.name)
                if (isAlbumLocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(if (isPinned) Alignment.TopStart else Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = bucket.name,
                    style = if (layoutMode == MainViewModel.AlbumLayoutMode.GRID_4) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (bucket.paths.size > 1) "${bucket.count} • ${bucket.paths.size} locations" else "${bucket.count}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumRowItem(
    icon: ImageVector,
    coverUri: Uri? = null,
    title: String,
    count: Int,
    subtitle: String? = null,
    actionLabel: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (coverUri != null) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        } else if (count > 0 || subtitle == null) {
            Text(
                text = if (subtitle != null && count == 0) "Scan" else count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
