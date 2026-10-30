package com.hrshd1eux.imava.ui.p2p

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.hrshd1eux.imava.core.p2p.ImavaDropServer
import com.hrshd1eux.imava.core.p2p.QrCodeGenerator
import com.hrshd1eux.imava.core.util.FormatUtils
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.data.model.MediaItem

@Composable
fun ImavaDropDialog(
    itemsToShare: List<MediaItem>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val serverState by ImavaDropServer.state.collectAsState()
    var isCopied by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    // Start server on entry if not running, update items if already running
    DisposableEffect(Unit) {
        ImavaDropServer.start(context, itemsToShare)
        onDispose {
            ImavaDropServer.stop()
        }
    }

    val qrBitmap = remember(serverState.url) {
        serverState.url?.let {
            QrCodeGenerator.generateQrBitmap(
                content = it,
                sizePx = 420,
                foregroundColor = android.graphics.Color.BLACK,
                backgroundColor = android.graphics.Color.WHITE
            )
        }
    }

    AlertDialog(
        onDismissRequest = {
            ImavaDropServer.stop()
            onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Imava Drop ⚡",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Direct Wi-Fi · Zero Cloud",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = {
                    ImavaDropServer.stop()
                    onDismissRequest()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(
                            if (serverState.isRunning && serverState.ipAddress != null)
                                Color(0xFF10B981).copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (serverState.isRunning && serverState.ipAddress != null)
                                    Color(0xFF10B981)
                                else
                                    MaterialTheme.colorScheme.error
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (serverState.isRunning && serverState.ipAddress != null)
                            "Ready on local network"
                        else
                            "No local Wi-Fi IP detected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (serverState.isRunning && serverState.ipAddress != null)
                            Color(0xFF10B981)
                        else
                            MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Display
                if (qrBitmap != null) {
                    Card(
                        modifier = Modifier
                            .size(210.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Scan with phone camera or QR scanner",
                                modifier = Modifier.size(186.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connect to Wi-Fi to generate transfer link",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // URL & Quick Actions
                val url = serverState.url ?: "http://..."
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Imava Drop Link", url))
                                    HapticUtil.performSuccess(context)
                                    isCopied = true
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy Link",
                                    tint = if (isCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, url)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Imava Drop Link"))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Link",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Open in Browser",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step-by-Step Sharing Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to Share & Connect",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        InstructionRow(
                            step = "1",
                            title = "Connect to Same Wi-Fi",
                            description = "Ensure the receiving PC, Mac, or phone is on the same Wi-Fi router or hotspot."
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        InstructionRow(
                            step = "2",
                            title = "Scan QR or Open Link",
                            description = "Scan the QR code above with camera, or enter the URL into any web browser."
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        InstructionRow(
                            step = "3",
                            title = "Download or Send Files",
                            description = "Download lossless originals or drag & drop files in the browser to send to this phone."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transfer Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (serverState.sharedItems.isNotEmpty()) "Sharing ${serverState.sharedItems.size} items" else "No photos selected to send",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (serverState.sharedItems.isNotEmpty()) {
                                Text(
                                    text = FormatUtils.formatFileSize(serverState.totalBytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (serverState.sharedItems.isNotEmpty())
                                "⚡ Full uncompressed originals · 50–100 MB/s speed\n📥 Drag & drop files on PC web page to send back to phone"
                            else
                                "💡 Select photos in gallery before tapping Wi-Fi Drop to share them.\nYou can still drop files onto the browser page to send to this phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Expandable Transfer Logs
                if (serverState.logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showLogs = !showLogs }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showLogs) "Hide Transfer Activity" else "View Live Activity (${serverState.logs.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (serverState.filesReceivedCount > 0) {
                            Text(
                                text = "Received ${serverState.filesReceivedCount} file(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AnimatedVisibility(visible = showLogs) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                serverState.logs.takeLast(8).forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ImavaDropServer.stop()
                    onDismissRequest()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    ImavaDropServer.stop()
                    onDismissRequest()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Stop Sharing")
            }
        }
    )
}

@Composable
private fun InstructionRow(
    step: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
