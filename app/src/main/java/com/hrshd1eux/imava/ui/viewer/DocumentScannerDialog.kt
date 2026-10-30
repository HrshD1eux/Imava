package com.hrshd1eux.imava.ui.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.hrshd1eux.imava.core.util.DocumentScannerUtil
import com.hrshd1eux.imava.core.util.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DocumentScannerDialog(
    photoUri: Uri,
    onDismissRequest: () -> Unit,
    onDocumentSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var flattenedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(DocumentScannerUtil.DocumentFilter.CLEAN_ENHANCED) }
    var isProcessing by remember { mutableStateOf(false) }

    // Normalized coordinates (0f to 1f) for Top-Left, Top-Right, Bottom-Right, Bottom-Left
    var tlNorm by remember { mutableStateOf(Offset(0.12f, 0.12f)) }
    var trNorm by remember { mutableStateOf(Offset(0.88f, 0.12f)) }
    var brNorm by remember { mutableStateOf(Offset(0.88f, 0.88f)) }
    var blNorm by remember { mutableStateOf(Offset(0.12f, 0.88f)) }

    LaunchedEffect(photoUri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(photoUri)?.use { input ->
                    sourceBitmap = BitmapFactory.decodeStream(input)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (flattenedBitmap != null) "Scanned Document" else "Document Keystoning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bmp = sourceBitmap
                if (bmp == null) {
                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                } else if (flattenedBitmap != null) {
                    // Preview flattened & filtered result
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Image(
                            bitmap = flattenedBitmap!!.asImageBitmap(),
                            contentDescription = "Flattened Document",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Document Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedFilter == DocumentScannerUtil.DocumentFilter.CLEAN_ENHANCED,
                            onClick = {
                                selectedFilter = DocumentScannerUtil.DocumentFilter.CLEAN_ENHANCED
                                updateFlattenedPreview(bmp, tlNorm, trNorm, brNorm, blNorm, selectedFilter) {
                                    flattenedBitmap = it
                                }
                            },
                            label = { Text("Color Clean") }
                        )
                        FilterChip(
                            selected = selectedFilter == DocumentScannerUtil.DocumentFilter.BW_HIGH_CONTRAST,
                            onClick = {
                                selectedFilter = DocumentScannerUtil.DocumentFilter.BW_HIGH_CONTRAST
                                updateFlattenedPreview(bmp, tlNorm, trNorm, brNorm, blNorm, selectedFilter) {
                                    flattenedBitmap = it
                                }
                            },
                            label = { Text("B&W Scan") }
                        )
                        FilterChip(
                            selected = selectedFilter == DocumentScannerUtil.DocumentFilter.ORIGINAL,
                            onClick = {
                                selectedFilter = DocumentScannerUtil.DocumentFilter.ORIGINAL
                                updateFlattenedPreview(bmp, tlNorm, trNorm, brNorm, blNorm, selectedFilter) {
                                    flattenedBitmap = it
                                }
                            },
                            label = { Text("Original") }
                        )
                    }
                } else {
                    // Interactive 4-point corner pinning canvas
                    Text(
                        text = "Drag the 4 corners to fit the document boundaries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val aspect = bmp.width.toFloat() / maxOf(1, bmp.height).toFloat()
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val containerWidth = maxWidth.value
                        val containerHeight = maxHeight.value

                        // Compute displayed image bounding box inside the letterboxed container
                        val containerAspect = containerWidth / containerHeight
                        val (imgW, imgH) = if (aspect > containerAspect) {
                            Pair(containerWidth, containerWidth / aspect)
                        } else {
                            Pair(containerHeight * aspect, containerHeight)
                        }
                        val offsetX = (containerWidth - imgW) / 2f
                        val offsetY = (containerHeight - imgH) / 2f

                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Source Document",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Overlay quad lines and corner drag pins
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val touchX = change.position.x
                                        val touchY = change.position.y

                                        // Convert touch into normalized image space
                                        val normX = ((touchX - offsetX) / imgW).coerceIn(0f, 1f)
                                        val normY = ((touchY - offsetY) / imgH).coerceIn(0f, 1f)
                                        val normTouch = Offset(normX, normY)

                                        // Find closest corner pin
                                        val dTL = (tlNorm - normTouch).getDistanceSquared()
                                        val dTR = (trNorm - normTouch).getDistanceSquared()
                                        val dBR = (brNorm - normTouch).getDistanceSquared()
                                        val dBL = (blNorm - normTouch).getDistanceSquared()

                                        val minD = minOf(dTL, dTR, dBR, dBL)
                                        when (minD) {
                                            dTL -> tlNorm = normTouch
                                            dTR -> trNorm = normTouch
                                            dBR -> brNorm = normTouch
                                            dBL -> blNorm = normTouch
                                        }
                                    }
                                }
                        ) {
                            val pTL = Offset(offsetX + tlNorm.x * imgW, offsetY + tlNorm.y * imgH)
                            val pTR = Offset(offsetX + trNorm.x * imgW, offsetY + trNorm.y * imgH)
                            val pBR = Offset(offsetX + brNorm.x * imgW, offsetY + brNorm.y * imgH)
                            val pBL = Offset(offsetX + blNorm.x * imgW, offsetY + blNorm.y * imgH)

                            // Quad outline
                            val path = Path().apply {
                                moveTo(pTL.x, pTL.y)
                                lineTo(pTR.x, pTR.y)
                                lineTo(pBR.x, pBR.y)
                                lineTo(pBL.x, pBL.y)
                                close()
                            }
                            drawPath(path, color = Color(0x334F46E5))
                            drawPath(path, color = Color(0xFF6366F1), style = Stroke(width = 4f))

                            // Draw draggable corner circles
                            val pinRadius = 24f
                            listOf(pTL, pTR, pBR, pBL).forEach { pt ->
                                drawCircle(Color.White, radius = pinRadius, center = pt)
                                drawCircle(Color(0xFF4F46E5), radius = pinRadius - 6f, center = pt)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val bmp = sourceBitmap
            if (flattenedBitmap != null) {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val uri = DocumentScannerUtil.saveDocument(context, flattenedBitmap!!)
                            isProcessing = false
                            if (uri != null) {
                                HapticUtil.performSuccess(context)
                                Toast.makeText(context, "Saved document to Pictures/Documents", Toast.LENGTH_LONG).show()
                                onDocumentSaved()
                                onDismissRequest()
                            } else {
                                HapticUtil.performError(context)
                                Toast.makeText(context, "Could not save document", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Document")
                }
            } else if (bmp != null) {
                Button(
                    onClick = {
                        isProcessing = true
                        updateFlattenedPreview(bmp, tlNorm, trNorm, brNorm, blNorm, selectedFilter) {
                            flattenedBitmap = it
                            isProcessing = false
                            HapticUtil.performSuccess(context)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flatten & Straighten")
                }
            }
        },
        dismissButton = {
            if (flattenedBitmap != null) {
                OutlinedButton(
                    onClick = { flattenedBitmap = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adjust Pins")
                }
            } else {
                OutlinedButton(onClick = onDismissRequest, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun updateFlattenedPreview(
    bmp: Bitmap,
    tl: Offset,
    tr: Offset,
    br: Offset,
    bl: Offset,
    filter: DocumentScannerUtil.DocumentFilter,
    onReady: (Bitmap) -> Unit
) {
    val w = bmp.width.toFloat()
    val h = bmp.height.toFloat()

    val ptTL = PointF(tl.x * w, tl.y * h)
    val ptTR = PointF(tr.x * w, tr.y * h)
    val ptBR = PointF(br.x * w, br.y * h)
    val ptBL = PointF(bl.x * w, bl.y * h)

    val warped = DocumentScannerUtil.warpPerspective(bmp, ptTL, ptTR, ptBR, ptBL)
    val filtered = DocumentScannerUtil.applyDocumentFilter(warped, filter)
    onReady(filtered)
}
