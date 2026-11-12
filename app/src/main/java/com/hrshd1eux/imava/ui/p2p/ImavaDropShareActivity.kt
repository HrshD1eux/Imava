package com.hrshd1eux.imava.ui.p2p

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.p2p.ImavaDropServer
import com.hrshd1eux.imava.core.p2p.ImavaDropShareHelper
import com.hrshd1eux.imava.core.util.LocaleHelper
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.ui.theme.ImavaTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Lightweight System Share Sheet target (`ACTION_SEND` / `ACTION_SEND_MULTIPLE` for image and
 * video MIME types) that stages shared media and opens [ImavaDropDialog] directly without unlocking
 * or loading the main gallery UI.
 */
class ImavaDropShareActivity : ComponentActivity() {

    private var stagingJob: Job? = null
    private var isPreparing by mutableStateOf(true)
    private var completedCount by mutableIntStateOf(0)
    private var totalCount by mutableIntStateOf(0)
    private var stagedItems by mutableStateOf<List<MediaItem>>(emptyList())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appTheme = getSharedPreferences("gallery_prefs", MODE_PRIVATE)
            .getString("app_theme", "system") ?: "system"

        handleIncomingShareIntent(intent)

        setContent {
            ImavaTheme(appTheme = appTheme) {
                when {
                    isPreparing -> {
                        Dialog(onDismissRequest = { cancelAndClose() }) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.imava_drop_share_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (totalCount > 1) {
                                            stringResource(
                                                R.string.drop_share_preparing_count_fmt,
                                                completedCount.coerceAtMost(totalCount),
                                                totalCount
                                            )
                                        } else {
                                            stringResource(R.string.drop_share_preparing)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(onClick = { cancelAndClose() }) {
                                        Text(stringResource(R.string.action_cancel))
                                    }
                                }
                            }
                        }
                    }

                    stagedItems.isNotEmpty() -> {
                        ImavaDropDialog(
                            itemsToShare = stagedItems,
                            fromExternalShare = true,
                            onDismissRequest = {
                                ImavaDropServer.stop()
                                finishAndRemoveTaskSafely()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.action
        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            handleIncomingShareIntent(intent)
        }
    }

    private fun handleIncomingShareIntent(incomingIntent: Intent?) {
        stagingJob?.cancel()
        val uris = ImavaDropShareHelper.extractShareUris(incomingIntent)
        if (uris.isEmpty()) {
            Toast.makeText(this, getString(R.string.drop_share_no_supported_media), Toast.LENGTH_SHORT).show()
            finishAndRemoveTaskSafely()
            return
        }

        isPreparing = true
        completedCount = 0
        totalCount = uris.size

        stagingJob = lifecycleScope.launch {
            try {
                val prepared = ImavaDropShareHelper.stageSharedMedia(
                    context = applicationContext,
                    intent = incomingIntent,
                    onProgress = { done, total ->
                        completedCount = done
                        totalCount = total
                    }
                )
                if (prepared.isEmpty()) {
                    Toast.makeText(
                        this@ImavaDropShareActivity,
                        getString(R.string.drop_share_no_supported_media),
                        Toast.LENGTH_SHORT
                    ).show()
                    finishAndRemoveTaskSafely()
                } else {
                    stagedItems = prepared
                    isPreparing = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(
                    this@ImavaDropShareActivity,
                    getString(R.string.drop_share_failed),
                    Toast.LENGTH_SHORT
                ).show()
                finishAndRemoveTaskSafely()
            }
        }
    }

    private fun cancelAndClose() {
        stagingJob?.cancel()
        if (!ImavaDropServer.isRunning()) {
            ImavaDropShareHelper.clearStagedFiles(applicationContext)
        }
        finishAndRemoveTaskSafely()
    }

    private fun finishAndRemoveTaskSafely() {
        try {
            finishAndRemoveTask()
        } catch (_: Exception) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && ImavaDropServer.launchedFromExternalShare) {
            ImavaDropServer.stop()
        }
    }
}
