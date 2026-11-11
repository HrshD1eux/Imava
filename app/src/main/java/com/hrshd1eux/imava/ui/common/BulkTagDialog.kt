package com.hrshd1eux.imava.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.data.model.MediaItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkTagDialog(
    selectedItems: List<MediaItem>,
    allExistingTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (tagsToAdd: Set<String>, tagsToRemove: Set<String>) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }

    // Collect tags currently present on at least one selected item
    val initialCurrentTags = remember(selectedItems) {
        selectedItems.flatMap { it.tags }.distinct().sorted()
    }

    val tagsToAdd = remember { mutableStateListOf<String>() }
    val tagsToRemove = remember { mutableStateListOf<String>() }

    fun addTag(raw: String) {
        val clean = raw.trim().removePrefix("#")
        if (clean.isNotBlank()) {
            tagsToRemove.remove(clean)
            if (!tagsToAdd.contains(clean)) {
                tagsToAdd.add(clean)
            }
            tagInput = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.menu_manage_tags),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.bulk_tag_applying_to, selectedItems.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.bulk_tag_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text(stringResource(R.string.add_custom_tag)) },
                        placeholder = { Text(stringResource(R.string.add_custom_tag_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { addTag(tagInput) },
                        enabled = tagInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (tagInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Active Tags on Selection
                val visibleCurrentTags = (initialCurrentTags + tagsToAdd).filter { !tagsToRemove.contains(it) }.distinct()

                Text(
                    text = stringResource(R.string.active_tags_format, visibleCurrentTags.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (visibleCurrentTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleCurrentTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                label = { Text("#$tag") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.padding(vertical = 2.dp),
                                onClick = {
                                    if (tagsToAdd.contains(tag)) {
                                        tagsToAdd.remove(tag)
                                    } else {
                                        tagsToRemove.add(tag)
                                    }
                                }
                            )
                        }
                    }
                }

                // Suggestions from existing tags in gallery
                val availableSuggestions = remember(allExistingTags, visibleCurrentTags) {
                    allExistingTags.filter { !visibleCurrentTags.contains(it) }
                }

                if (availableSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.suggested_tags_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSuggestions.forEach { suggestion ->
                            FilterChip(
                                selected = false,
                                onClick = { addTag(suggestion) },
                                label = { Text("+#$suggestion") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = tagInput.trim().removePrefix("#")
                    if (clean.isNotBlank()) {
                        tagsToRemove.remove(clean)
                        if (!tagsToAdd.contains(clean)) {
                            tagsToAdd.add(clean)
                        }
                    }
                    onConfirm(tagsToAdd.toSet(), tagsToRemove.toSet())
                }
            ) {
                Text(stringResource(R.string.apply_tags))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
