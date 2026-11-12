package com.hrshd1eux.imava.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterDialog(
    initialState: AdvancedFilterState,
    onApply: (AdvancedFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var workingState by remember { mutableStateOf(initialState) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.adv_filter_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.adv_filter_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Media Type
                Text(
                    text = stringResource(R.string.adv_filter_media_type),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaTypeFilterChoice.values().forEach { choice ->
                        FilterChip(
                            selected = workingState.mediaType == choice,
                            onClick = { workingState = workingState.copy(mediaType = choice) },
                            label = { Text(stringResource(choice.labelRes), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 2. File Size Thresholds
                Text(
                    text = stringResource(R.string.adv_filter_file_size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FileSizeFilter.values().forEach { sizeChoice ->
                        FilterChip(
                            selected = workingState.fileSize == sizeChoice,
                            onClick = { workingState = workingState.copy(fileSize = sizeChoice) },
                            label = { Text(stringResource(sizeChoice.labelRes), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 3. Resolution & Orientation
                Text(
                    text = stringResource(R.string.adv_filter_resolution),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ResolutionFilter.values().forEach { resChoice ->
                        FilterChip(
                            selected = workingState.resolution == resChoice,
                            onClick = { workingState = workingState.copy(resolution = resChoice) },
                            label = { Text(stringResource(resChoice.labelRes), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 4. Date Range
                Text(
                    text = stringResource(R.string.adv_filter_date_taken),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DatePreset.values().forEach { dateChoice ->
                        FilterChip(
                            selected = workingState.datePreset == dateChoice,
                            onClick = { workingState = workingState.copy(datePreset = dateChoice) },
                            label = { Text(stringResource(dateChoice.labelRes), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 5. Camera / Device Model
                Text(
                    text = stringResource(R.string.adv_filter_camera_model),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = workingState.cameraModelQuery,
                    onValueChange = { workingState = workingState.copy(cameraModelQuery = it) },
                    placeholder = { Text(stringResource(R.string.adv_filter_camera_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(workingState) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (workingState.isActive) stringResource(R.string.adv_filter_apply_count, workingState.activeFilterCount) else stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (workingState.isActive) {
                    TextButton(onClick = {
                        workingState = AdvancedFilterState()
                        onReset()
                    }) {
                        Text(stringResource(R.string.action_reset_all))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
