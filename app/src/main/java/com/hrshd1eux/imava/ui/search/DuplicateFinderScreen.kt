package com.hrshd1eux.imava.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hrshd1eux.imava.core.util.DuplicateFinder
import com.hrshd1eux.imava.core.util.DuplicateGroup
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFinderScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val visibleMedia by viewModel.visibleMediaItems.collectAsState()

    var isScanning by remember { mutableStateOf(true) }
    var scanCount by remember { mutableIntStateOf(0) }
    var scanTotal by remember { mutableIntStateOf(0) }
    var groups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    val selectedToDelete = remember { mutableStateListOf<Long>() }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var previewMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var comparePair by remember { mutableStateOf<Pair<MediaItem, MediaItem>?>(null) }

    if (comparePair != null) {
        val (first, second) = comparePair!!
        com.hrshd1eux.imava.ui.compare.PhotoCompareScreen(
            item1 = first,
            item2 = second,
            onBack = { comparePair = null }
        )
        return
    }

    LaunchedEffect(Unit) {
        isScanning = true
        val allPhotos = viewModel.getAllPhotosForDuplicateScan()
        val photosToScan = if (allPhotos.isNotEmpty()) allPhotos else visibleMedia.filterIsInstance<MediaItem.Photo>()
        scanTotal = photosToScan.size
        val result = DuplicateFinder.findDuplicates(context, photosToScan) { current, total ->
            scanCount = current
            scanTotal = total
        }
        groups = result

        selectedToDelete.clear()
        result.forEach { group ->
            group.items.forEach { item ->
                if (item.id != group.bestItem.id) {
                    selectedToDelete.add(item.id)
                }
            }
        }
        isScanning = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Finder 🧹") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedToDelete.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete ${selectedToDelete.size} Duplicates")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isScanning) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (scanTotal > 0) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { (scanCount.toFloat() / scanTotal.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (scanTotal > 0) "Scanning $scanCount / $scanTotal items..." else "Analyzing library for duplicates...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (groups.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No duplicate photos found 🎉",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your gallery is clean and optimized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(groups) { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${group.items.size} Similar Photos",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(group.bestItem.size)} · ${group.bestItem.width}×${group.bestItem.height}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            DuplicateFinder.ignoreGroup(context, group)
                                            groups = groups.filter { it.id != group.id }
                                            group.items.forEach { selectedToDelete.remove(it.id) }
                                            android.widget.Toast.makeText(context, "Group marked as not duplicate (ignored next time)", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Not Same", style = MaterialTheme.typography.labelMedium)
                                    }

                                    if (group.items.size >= 2) {
                                        FilledTonalButton(
                                            onClick = { comparePair = Pair(group.items[0], group.items[1]) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Compare Loupe ⚖️", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(group.items) { item ->
                                    val isSelected = selectedToDelete.contains(item.id)
                                    val isBest = item.id == group.bestItem.id

                                    Box(
                                        modifier = Modifier
                                            .size(115.dp)
                                            .clip(MaterialTheme.shapes.small)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .clickable {
                                                com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                                if (isSelected) {
                                                    selectedToDelete.remove(item.id)
                                                } else {
                                                    selectedToDelete.add(item.id)
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = item.uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        if (isBest) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("Keep", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                        )

                                        IconButton(
                                            onClick = { previewMediaItem = item },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Preview",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
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

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Confirm Duplicate Deletion") },
                text = {
                    Text("Are you sure you want to delete ${selectedToDelete.size} selected duplicate photos? They will be moved to Trash.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.selectionState.clear()
                            selectedToDelete.forEach { id ->
                                viewModel.selectionState.select(id)
                            }
                            viewModel.deleteSelectedMedia(context)
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Duplicates")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        if (previewMediaItem != null) {
            val pItem = previewMediaItem!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { previewMediaItem = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    var pScale by remember { mutableFloatStateOf(1f) }
                    var pOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    pScale = (pScale * zoom).coerceIn(1f, 5f)
                                    pOffset += pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = pItem.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pScale
                                    scaleY = pScale
                                    translationX = pOffset.x
                                    translationY = pOffset.y
                                }
                        )
                    }

                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { previewMediaItem = null },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${pItem.width} × ${pItem.height}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(pItem.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Bottom Action Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xDD1E1E2E)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isMarkedForDelete = selectedToDelete.contains(pItem.id)
                            Button(
                                onClick = {
                                    if (isMarkedForDelete) {
                                        selectedToDelete.remove(pItem.id)
                                    } else {
                                        selectedToDelete.add(pItem.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMarkedForDelete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (isMarkedForDelete) Icons.Default.CheckCircle else Icons.Default.Delete,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isMarkedForDelete) "Keep This Photo (Unmark Delete)" else "Mark to Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
}
