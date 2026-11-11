package com.hrshd1eux.imava.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.ExifSanitizerUtil

@Composable
fun ScrubAlbumDialog(
    albumName: String,
    itemCount: Int,
    isScrubbing: Boolean,
    progressCurrent: Int,
    progressTotal: Int,
    onDismiss: () -> Unit,
    onConfirmScrub: (ExifSanitizerUtil.ScrubOptions) -> Unit
) {
    var preserveCaptureDate by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = {
            if (!isScrubbing) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.scrub_album_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isScrubbing) {
                    Text(
                        text = stringResource(R.string.scrubbing_album_format, albumName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val progressFloat = if (progressTotal > 0) progressCurrent.toFloat() / progressTotal.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.scrubbing_progress_format, progressCurrent, progressTotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.scrub_album_warning, itemCount, albumName),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { preserveCaptureDate = !preserveCaptureDate }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Checkbox(
                                checked = preserveCaptureDate,
                                onCheckedChange = { preserveCaptureDate = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.scrub_preserve_date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.scrub_preserve_date_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.scrub_irreversible_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isScrubbing) {
                Button(
                    onClick = {
                        onConfirmScrub(
                            ExifSanitizerUtil.ScrubOptions(
                                preserveCaptureDate = preserveCaptureDate,
                                preserveOrientation = true
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.scrub_photos_action))
                }
            }
        },
        dismissButton = {
            if (!isScrubbing) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
