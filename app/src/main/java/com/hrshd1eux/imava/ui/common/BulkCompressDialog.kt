package com.hrshd1eux.imava.ui.common

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.core.util.ImageCompressor
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BulkCompressDialog(
    selectedItems: List<MediaItem>,
    onDismiss: () -> Unit,
    onComplete: (compressedCount: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photos = remember(selectedItems) {
        selectedItems.filterIsInstance<MediaItem.Photo>()
    }
    val skippedVideosCount = selectedItems.size - photos.size

    var targetKbInput by remember { mutableStateOf("100") }
    var isCompressing by remember { mutableStateOf(false) }
    var processedCount by remember { mutableIntStateOf(0) }
    var progressFraction by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        onDismissRequest = {
            if (!isCompressing) onDismiss()
        },
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Compress,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Bulk Compress Photos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Info Summary Box
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${photos.size} photo(s) selected for compression",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (skippedVideosCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: $skippedVideosCount video(s) will be skipped",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Compressed copies will be saved in Pictures/Compressed with original metadata preserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Target Size Per Photo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(25, 50, 100, 250, 500, 1000, 2000)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { presetKb ->
                        val isSelected = targetKbInput == presetKb.toString()
                        val label = if (presetKb >= 1000) "${presetKb / 1000} MB" else "$presetKb KB"
                        FilterChip(
                            selected = isSelected,
                            onClick = { targetKbInput = presetKb.toString() },
                            label = { Text(label) },
                            enabled = !isCompressing
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetKbInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 6) {
                            targetKbInput = input
                        }
                    },
                    label = { Text("Custom Target (KB)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCompressing
                )

                if (isCompressing) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Compressing $processedCount of ${photos.size} (${(progressFraction * 100).toInt()}%)...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isCompressing && photos.isNotEmpty() && (targetKbInput.toIntOrNull() ?: 0) > 0,
                onClick = {
                    val targetKb = targetKbInput.toIntOrNull() ?: 100
                    isCompressing = true
                    processedCount = 0
                    progressFraction = 0f

                    scope.launch {
                        var successCount = 0
                        val total = photos.size

                        photos.forEachIndexed { index, photo ->
                            val uri = ImageCompressor.compressToTargetKb(context, photo, targetKb)
                            if (uri != null) {
                                successCount++
                            }
                            processedCount = index + 1
                            progressFraction = (index + 1).toFloat() / total.toFloat()
                        }

                        isCompressing = false
                        if (successCount > 0) {
                            HapticUtil.performSuccess(context)
                            Toast.makeText(
                                context,
                                "Successfully compressed $successCount photos to Pictures/Compressed",
                                Toast.LENGTH_LONG
                            ).show()
                            onComplete(successCount)
                        } else {
                            HapticUtil.performError(context)
                            Toast.makeText(
                                context,
                                "Failed to compress photos. Check target size.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(if (isCompressing) "Compressing..." else "Compress (${photos.size})")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isCompressing,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
