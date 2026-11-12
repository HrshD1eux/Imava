package com.hrshd1eux.imava.ui

import android.Manifest
import com.hrshd1eux.imava.core.util.findActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Compress
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.widthIn
import com.hrshd1eux.imava.ui.common.AppUpdateDialog
import com.hrshd1eux.imava.core.util.AppUpdateManager
import com.hrshd1eux.imava.core.util.AppUpdateDownloadState
import com.hrshd1eux.imava.core.util.UpdateInfo
import androidx.compose.material.icons.filled.SystemUpdate
import com.hrshd1eux.imava.ui.common.BulkCompressDialog
import com.hrshd1eux.imava.ui.common.BulkTagDialog
import com.hrshd1eux.imava.ui.common.DescriptiveIconButton
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import com.hrshd1eux.imava.ui.common.ScrubAlbumDialog
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.tween
import android.content.pm.ActivityInfo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.ui.albums.AlbumsScreen
import com.hrshd1eux.imava.ui.search.SearchScreen
import com.hrshd1eux.imava.ui.theme.ImavaTheme
import com.hrshd1eux.imava.ui.timeline.TimelineScreen
import com.hrshd1eux.imava.ui.viewer.PhotoViewerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
    }

    private var hasPermissionsState = mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // OS records notification permission grant or denial
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val readGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasFullImages = results[Manifest.permission.READ_MEDIA_IMAGES] == true
            val hasFullVideos = results[Manifest.permission.READ_MEDIA_VIDEO] == true
            val hasPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                results[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
            } else false
            (hasFullImages && hasFullVideos) || hasPartial
        } else {
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        hasPermissionsState.value = readGranted
        if (readGranted) {
            viewModel.loadMediaStream()
            viewModel.loadBuckets()
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.hrshd1eux.imava.core.util.LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            window.colorMode = ActivityInfo.COLOR_MODE_HDR
        }

        checkPermissions()
        handleIncomingIntent(intent)
        viewModel.syncLocaleState()

        setContent {
            ImavaTheme(appTheme = viewModel.appTheme) {
                val hasPermissions by hasPermissionsState

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAppLockedState.value) {
                        com.hrshd1eux.imava.ui.common.AppLockScreen(onUnlockClick = { promptAppUnlock() })
                    } else if (hasPermissions || viewModel.isExternalViewIntent) {
                        MainScreenLayout(viewModel)
                        
                        val editingItem = viewModel.editingMediaItem
                        if (editingItem != null) {
                            com.hrshd1eux.imava.ui.editor.PhotoEditorScreen(
                                viewModel = viewModel,
                                mediaItem = editingItem,
                                onDismiss = { viewModel.editingMediaItem = null }
                            )
                        }
                    } else {
                        PermissionFallbackScreen(
                            onRequestPermissions = {
                                requestPermissionLauncher.launch(requiredPermissions)
                            }
                        )
                    }
                }
            }
        }
    }

    private var isAppLockedState = mutableStateOf(false)

    private fun promptAppUnlock() {
        com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
            activity = this,
            title = getString(R.string.app_locked_title),
            subtitle = getString(R.string.app_lock_biometric_subtitle),
            onSuccess = {
                com.hrshd1eux.imava.core.util.AppLockManager.markAppUnlocked()
                isAppLockedState.value = false
            },
            onError = {
                // Keep locked
            }
        )
    }

    private fun updatePrivacyShield(isLeaving: Boolean = false) {
        val galleryPrefs = getSharedPreferences("gallery_prefs", MODE_PRIVATE)
        val shieldEnabled = galleryPrefs.getBoolean("recents_privacy_shield_enabled", true)
        val isVaultActive = (viewModel.isVaultUnlocked.value && viewModel.currentCategoryName == "Hidden Vault") ||
                viewModel.activeMediaItem?.isHidden == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!shieldEnabled && !isVaultActive)
        }

        if (isVaultActive || (shieldEnabled && isLeaving)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        updatePrivacyShield(isLeaving = !hasFocus)
    }

    override fun onResume() {
        super.onResume()
        try {
            com.hrshd1eux.imava.core.util.AppLockManager.onAppResume()
            if (com.hrshd1eux.imava.core.util.AppLockManager.isAppLockEnabled(this) &&
                !com.hrshd1eux.imava.core.util.AppLockManager.isAppUnlockedForSession()
            ) {
                isAppLockedState.value = true
                promptAppUnlock()
            }
            if (viewModel.currentCategoryName == "Trash" && com.hrshd1eux.imava.core.util.AppLockManager.isTrashLocked(this)) {
                viewModel.currentCategoryName = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModel.onAppForegrounded(this)
        checkPermissions()
        if (hasPermissionsState.value) {
            viewModel.refreshAll()
            viewModel.triggerAutoDeepScan()
        }

        updatePrivacyShield(isLeaving = false)
    }

    override fun onPause() {
        super.onPause()
        com.hrshd1eux.imava.core.util.AppLockManager.onAppPause()
        updatePrivacyShield(isLeaving = true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        updatePrivacyShield(isLeaving = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activeItem = viewModel.activeMediaItem
            if (activeItem is com.hrshd1eux.imava.data.model.MediaItem.Video) {
                try {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (_: Exception) {}
            }
        }
    }

    private fun checkPermissions() {
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasFullImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasFullVideos = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            } else false
            (hasFullImages && hasFullVideos) || hasPartial
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        hasPermissionsState.value = isGranted
        if (isGranted) {
            viewModel.refreshAll()

            // Request WRITE_EXTERNAL_STORAGE on Android 10 and lower if not yet granted
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                val hasWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (!hasWrite) {
                    requestPermissionLauncher.launch(requiredPermissions)
                }
            }

            // Request POST_NOTIFICATIONS on Android 13+ (API 33+) once if not yet granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    val prefs = getSharedPreferences("gallery_prefs", MODE_PRIVATE)
                    val askedAlready = prefs.getBoolean("has_prompted_post_notifications", false)
                    if (!askedAlready) {
                        prefs.edit().putBoolean("has_prompted_post_notifications", true).apply()
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleActivityResult(requestCode, resultCode, this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            viewModel.openExternalMedia(intent.data!!, intent.type)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreenLayout(viewModel: MainViewModel) {
    val selectionState = viewModel.selectionState
    val currentScreen = viewModel.currentScreen
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSelectionShareDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showImavaDropDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectionSharePrivacyMode by remember { androidx.compose.runtime.mutableStateOf(com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL) }
    var showSelectionDeleteConfirmDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSelectionDeletePermanentlyConfirmDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSelectionRestoreConfirmDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showMoveToAlbumDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showTimeShiftDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showBatchRenameDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSingleRenameDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var compareItems by remember { androidx.compose.runtime.mutableStateOf<Pair<com.hrshd1eux.imava.data.model.MediaItem, com.hrshd1eux.imava.data.model.MediaItem>?>(null) }
    var collageImageUris by remember { androidx.compose.runtime.mutableStateOf<List<Uri>?>(null) }
    var showStorageDoctor by remember { androidx.compose.runtime.mutableStateOf(false) }
    var slideshowItems by remember { androidx.compose.runtime.mutableStateOf<List<com.hrshd1eux.imava.data.model.MediaItem>?>(null) }
    var showVaultPreConfirmDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showVaultSetupRequiredDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pendingMoveToVaultIds by remember { androidx.compose.runtime.mutableStateOf(emptySet<Long>()) }
    var showSelectionMorePopup by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showBulkCompressDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    DisposableEffect(Unit) {
        com.hrshd1eux.imava.core.p2p.ImavaDropServer.onFileReceived = {
            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                viewModel.refreshAll()
            }
        }
        onDispose {
            com.hrshd1eux.imava.core.p2p.ImavaDropServer.onFileReceived = null
        }
    }
    var showBulkTagDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showScrubCurrentAlbumDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isScrubbingCurrentAlbum by remember { androidx.compose.runtime.mutableStateOf(false) }
    var currentAlbumScrubProgress by remember { androidx.compose.runtime.mutableStateOf(0) }
    var currentAlbumScrubTotal by remember { androidx.compose.runtime.mutableStateOf(0) }
    var showSecureShredderNotice by remember {
        androidx.compose.runtime.mutableStateOf(com.hrshd1eux.imava.ui.common.SecureShredderNoticeManager.shouldShowNotice(context))
    }
    var appUpdateInfoToPrompt by remember { androidx.compose.runtime.mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val settingsPrefs = context.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
        val autoCheck = settingsPrefs.getBoolean("auto_check_updates", true)
        if (autoCheck) {
            try {
                val info = AppUpdateManager.checkForUpdates(context)
                if (info.hasUpdate && AppUpdateManager.shouldPromptUpdate(context, info.latestVersion)) {
                    appUpdateInfoToPrompt = info
                }
            } catch (_: Exception) {}
        }
    }
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val isVaultActive = (isVaultUnlocked && viewModel.currentCategoryName == "Hidden Vault") ||
            viewModel.activeMediaItem?.isHidden == true
    val activity = context.findActivity()

    DisposableEffect(isVaultActive) {
        val prefs = activity?.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE)
        val shieldEnabled = prefs?.getBoolean("recents_privacy_shield_enabled", true) == true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            activity?.setRecentsScreenshotEnabled(!shieldEnabled && !isVaultActive)
        }
        if (isVaultActive) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (!isVaultActive) {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    val galleryPrefs = remember(context) { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    val shakeLockEnabled = remember(galleryPrefs) { galleryPrefs.getBoolean("vault_shake_lock_enabled", true) }

    DisposableEffect(isVaultActive, shakeLockEnabled) {
        if (isVaultActive && shakeLockEnabled) {
            val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            val accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            var lastShakeTimestamp = 0L

            val listener = object : android.hardware.SensorEventListener {
                override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                    if (event == null) return
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gForce = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() / android.hardware.SensorManager.GRAVITY_EARTH
                    if (gForce > 2.5f) {
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTimestamp > 1500L) {
                            lastShakeTimestamp = now
                            viewModel.lockVault(context)
                            viewModel.activeMediaItem = null
                            com.hrshd1eux.imava.core.util.HapticUtil.performError(context)
                            android.widget.Toast.makeText(context, context.getString(R.string.emergency_shake_vault_locked), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
            }

            if (sensorManager != null && accelerometer != null) {
                sensorManager.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
            }

            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
                viewModel.lockVault(context)
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.triggerAutoScrub()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val screens = remember { listOf(Screen.Photos, Screen.Albums, Screen.Settings) }
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    
    val backEnabled = selectionState.inSelectionMode ||
            collageImageUris != null ||
            showStorageDoctor ||
            slideshowItems != null ||
            viewModel.activeMemoryStory != null ||
            viewModel.activeMediaItem != null ||
            viewModel.isExternalViewIntent ||
            viewModel.currentCategoryName != null ||
            viewModel.currentBucketId != null ||
            currentScreen != Screen.Photos

    BackHandler(enabled = backEnabled) {
        if (selectionState.inSelectionMode) {
            selectionState.clear()
        } else if (collageImageUris != null) {
            collageImageUris = null
        } else if (showStorageDoctor) {
            showStorageDoctor = false
        } else if (slideshowItems != null) {
            slideshowItems = null
        } else if (viewModel.activeMemoryStory != null) {
            viewModel.activeMemoryStory = null
        } else if (viewModel.activeMediaItem != null) {
            viewModel.activeMediaItem = null
            if (viewModel.isExternalViewIntent) {
                activity?.finish()
            }
        } else if (viewModel.isExternalViewIntent) {
            activity?.finish()
        } else if (currentScreen == Screen.PermissionsTransparency || currentScreen == Screen.OpenSourceLicenses) {
            viewModel.currentScreen = Screen.AboutLegal
        } else if (currentScreen == Screen.AboutLegal) {
            viewModel.currentScreen = Screen.Settings
        } else if (currentScreen == Screen.VaultSetup) {
            viewModel.currentScreen = Screen.Albums
        } else if (currentScreen == Screen.VaultSettings) {
            if (viewModel.currentCategoryName == "Hidden Vault") {
                viewModel.currentScreen = Screen.Photos
            } else {
                viewModel.currentScreen = Screen.Settings
            }
        } else if (viewModel.currentCategoryName != null) {
            if (viewModel.currentCategoryName == "Hidden Vault") {
                viewModel.lockVault(context)
            } else if (!isWideScreen) {
                viewModel.currentCategoryName = null
                viewModel.currentScreen = Screen.Albums
            } else {
                viewModel.currentCategoryName = null
                viewModel.currentScreen = Screen.Photos
            }
        } else if (viewModel.currentBucketId != null) {
            if (!isWideScreen) {
                viewModel.selectBucket(null, null)
                viewModel.currentScreen = Screen.Albums
            } else {
                viewModel.selectBucket(null, null)
                viewModel.currentScreen = Screen.Photos
            }
        } else if (currentScreen != Screen.Photos) {
            viewModel.currentScreen = Screen.Photos
        }
    }
    val initialPageIndex = remember {
        val idx = screens.indexOf(viewModel.currentScreen)
        if (idx >= 0) idx else 0
    }
    val mainPagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { screens.size }
    )


    LaunchedEffect(currentScreen) {
        if (currentScreen != Screen.Search) {
            val page = screens.indexOf(currentScreen)
            if (page >= 0 && mainPagerState.currentPage != page) {
                mainPagerState.scrollToPage(page)
            }
        }
    }


    LaunchedEffect(mainPagerState.settledPage) {
        if (currentScreen != Screen.Search) {
            val targetScreen = screens.getOrNull(mainPagerState.settledPage)
            if (targetScreen != null && viewModel.currentScreen != targetScreen) {
                if (viewModel.currentCategoryName == "Hidden Vault" && targetScreen != Screen.Photos) {
                    viewModel.lockVault(context)
                }
                viewModel.currentScreen = targetScreen
            }
        }
    }

    val isFullScreenOverlayActive = viewModel.activeMemoryStory != null ||
            viewModel.activeMediaItem != null ||
            compareItems != null ||
            collageImageUris != null ||
            slideshowItems != null ||
            showStorageDoctor ||
            currentScreen == Screen.DuplicateFinder ||
            currentScreen == Screen.Search ||
            currentScreen == Screen.AboutLegal ||
            currentScreen == Screen.PermissionsTransparency ||
            currentScreen == Screen.OpenSourceLicenses ||
            currentScreen == Screen.VaultSetup ||
            currentScreen == Screen.VaultSettings

    Scaffold(
        topBar = {
            if (!isFullScreenOverlayActive) {
                if (selectionState.inSelectionMode) {
                    val allVisibleItems by viewModel.visibleMediaItems.collectAsState()
                    val allIds = remember(allVisibleItems) { allVisibleItems.map { it.id }.toSet() }
                    val isAllSelected = allIds.isNotEmpty() && selectionState.selectedIds.containsAll(allIds)

                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.items_selected_format, selectionState.selectedIds.size),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectionState.clear() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_exit_selection)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showImavaDropDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiTethering,
                                    contentDescription = stringResource(R.string.settings_wifi_drop),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (isAllSelected) {
                                        selectionState.clear()
                                    } else {
                                        selectionState.selectAll(allIds)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAllSelected) stringResource(R.string.action_deselect_all) else stringResource(R.string.action_select_all),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                } else {
                TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentScreen == Screen.Photos) {
                                    viewModel.triggerScrollToTop()
                                    com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                }
                            }
                    ) {
                        Text(
                            text = if (viewModel.currentCategoryName != null && currentScreen == Screen.Photos) {
                                when (viewModel.currentCategoryName) {
                                    "Favorites" -> stringResource(R.string.category_favorites)
                                    "Trash" -> stringResource(R.string.category_trash)
                                    "Hidden Vault" -> stringResource(R.string.category_vault)
                                    "Videos" -> stringResource(R.string.category_videos)
                                    "Smart:HugeVideos" -> stringResource(R.string.smart_album_huge_videos)
                                    "Smart:BurstShots" -> stringResource(R.string.smart_album_burst_shots)
                                    "Smart:ThisYear" -> stringResource(R.string.smart_album_this_year)
                                    "Smart:Screenshots" -> stringResource(R.string.smart_album_screenshots)
                                    "Smart:Tagged" -> stringResource(R.string.smart_album_tagged)
                                    else -> viewModel.currentCategoryName!!
                                }
                            } else if (viewModel.currentBucketName != null && currentScreen == Screen.Photos) {
                                viewModel.currentBucketName!!
                            } else {
                                when (currentScreen) {
                                    Screen.Photos -> stringResource(R.string.nav_photos)
                                    Screen.Albums -> stringResource(R.string.nav_albums)
                                    Screen.Settings -> stringResource(R.string.nav_settings)
                                    else -> currentScreen.name
                                }
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    if (viewModel.currentCategoryName != null && currentScreen == Screen.Photos) {
                        IconButton(onClick = { 
                            if (viewModel.currentCategoryName == "Hidden Vault") {
                                viewModel.lockVault(context)
                            } else {
                                viewModel.currentCategoryName = null 
                                viewModel.currentScreen = Screen.Albums
                            }
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_all_media))
                        }
                    } else if (viewModel.currentBucketId != null && currentScreen == Screen.Photos) {
                        IconButton(onClick = { 
                            viewModel.selectBucket(null, null) 
                            viewModel.currentScreen = Screen.Albums
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_all_media))
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.Photos && viewModel.currentCategoryName != "Trash") {
                        DescriptiveIconButton(
                            onClick = { viewModel.toggleGridStyle() },
                            title = stringResource(R.string.toolbar_grid_layout),
                            description = if (viewModel.gridStyle == GridStyle.SQUARE) stringResource(R.string.toolbar_grid_square_desc) else stringResource(R.string.toolbar_grid_adaptive_desc)
                        ) {
                            Icon(
                                imageVector = if (viewModel.gridStyle == GridStyle.SQUARE) Icons.Default.CropSquare else Icons.Default.GridView,
                                contentDescription = stringResource(R.string.toolbar_toggle_grid_style)
                            )
                        }

                        DescriptiveIconButton(
                            onClick = { viewModel.toggleSortOrder() },
                            title = stringResource(R.string.toolbar_sort_order),
                            description = if (viewModel.sortOrder == SortOrder.NEWEST_FIRST) stringResource(R.string.toolbar_sort_newest_desc) else stringResource(R.string.toolbar_sort_oldest_desc)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.SwapVert,
                                contentDescription = if (viewModel.sortOrder == SortOrder.NEWEST_FIRST) stringResource(R.string.sort_newest_first) else stringResource(R.string.sort_oldest_first),
                                tint = if (viewModel.sortOrder == SortOrder.OLDEST_FIRST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DescriptiveIconButton(
                            onClick = { viewModel.toggleSortMode() },
                            title = stringResource(R.string.toolbar_view_mode),
                            description = if (viewModel.sortMode == TimelineSortMode.DATE_GROUPED) stringResource(R.string.toolbar_view_grouped_desc) else stringResource(R.string.toolbar_view_stream_desc)
                        ) {
                            Icon(
                                imageVector = if (viewModel.sortMode == TimelineSortMode.DATE_GROUPED) Icons.Default.DateRange else Icons.Default.ViewStream,
                                contentDescription = stringResource(R.string.toolbar_toggle_sort_mode)
                            )
                        }

                        DescriptiveIconButton(
                            onClick = {
                                if (selectionState.selectedIds.isEmpty()) {
                                    Toast.makeText(context, context.getString(R.string.wifi_drop_select_prompt), Toast.LENGTH_SHORT).show()
                                } else {
                                    showImavaDropDialog = true
                                }
                            },
                            title = stringResource(R.string.settings_wifi_drop),
                            description = stringResource(R.string.wifi_drop_toolbar_desc)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = stringResource(R.string.settings_wifi_drop)
                            )
                        }

                        val categoryName by viewModel.currentCategoryNameFlow.collectAsState()
                        if (categoryName == "Trash") {
                            var showEmptyTrashConfirm by remember { mutableStateOf(false) }

                            IconButton(onClick = { showEmptyTrashConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = stringResource(R.string.empty_trash),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            if (showEmptyTrashConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showEmptyTrashConfirm = false },
                                    icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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
                                        TextButton(onClick = { showEmptyTrashConfirm = false }) {
                                            Text(stringResource(R.string.action_cancel))
                                        }
                                    }
                                )
                            }
                        }

                        if (categoryName == "Hidden Vault") {
                            var showVaultMenu by remember { mutableStateOf(false) }

                            IconButton(onClick = { showVaultMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.vault_security_options)
                                )
                            }

                            DropdownMenu(
                                expanded = showVaultMenu,
                                onDismissRequest = { showVaultMenu = false },
                                modifier = Modifier
                                    .widthIn(min = 250.dp, max = 290.dp)
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(stringResource(R.string.lock_vault_now), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(stringResource(R.string.lock_vault_now_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showVaultMenu = false
                                        viewModel.lockVault(context)
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(stringResource(R.string.vault_security_settings), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(stringResource(R.string.vault_security_settings_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showVaultMenu = false
                                        viewModel.currentScreen = Screen.VaultSettings
                                    }
                                )
                            }
                        }

                        if (viewModel.currentBucketId != null && categoryName == null) {
                            var showAlbumMenu by remember { mutableStateOf(false) }
                            val currentBucketId = viewModel.currentBucketId!!
                            val currentBucketName = viewModel.currentBucketName ?: stringResource(R.string.album_default_label)
                            val isAutoScrub = viewModel.isAutoScrubEnabled(currentBucketId)

                            IconButton(onClick = { showAlbumMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.album_options)
                                )
                            }

                            DropdownMenu(
                                expanded = showAlbumMenu,
                                onDismissRequest = { showAlbumMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.scrub_album_title)) },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                    onClick = {
                                        showAlbumMenu = false
                                        showScrubCurrentAlbumDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isAutoScrub) stringResource(R.string.auto_scrub_enabled_menu) else stringResource(R.string.auto_scrub_disabled_menu)) },
                                    leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                    onClick = {
                                        showAlbumMenu = false
                                        val newState = viewModel.toggleAutoScrubBucket(currentBucketId)
                                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                        android.widget.Toast.makeText(
                                            context,
                                            if (newState) context.getString(R.string.auto_scrub_enabled_toast, currentBucketName) else context.getString(R.string.auto_scrub_disabled_toast, currentBucketName),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            }
        }
    },
        bottomBar = {
            if (!isFullScreenOverlayActive) {
                AnimatedContent(
                    targetState = selectionState.inSelectionMode,
                    transitionSpec = {
                        slideInVertically { it } togetherWith slideOutVertically { it }
                    },
                    label = "BottomBarTransition"
                ) { inSelection ->
                    if (inSelection) {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        val categoryName by viewModel.currentCategoryNameFlow.collectAsState()
                        val selectedIds = selectionState.selectedIds
                        val isViewingTrash = categoryName == "Trash"

                        val isViewingVault = categoryName == "Hidden Vault"

                        if (isViewingTrash) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.RestoreFromTrash,
                                    label = stringResource(R.string.action_restore),
                                    onClick = { showSelectionRestoreConfirmDialog = true }
                                )
                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.DeleteForever,
                                    label = stringResource(R.string.delete_forever),
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { showSelectionDeletePermanentlyConfirmDialog = true }
                                )
                            }
                        } else if (isViewingVault) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Visibility,
                                    label = stringResource(R.string.action_unhide),
                                    onClick = {
                                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                        viewModel.hideSelectedMedia(context)
                                    }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Share,
                                    label = stringResource(R.string.action_share),
                                    onClick = { showSelectionShareDialog = true }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.DeleteForever,
                                    label = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { showSelectionDeletePermanentlyConfirmDialog = true }
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    SelectionActionButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        icon = Icons.Default.MoreVert,
                                        label = stringResource(R.string.action_more),
                                        onClick = { showSelectionMorePopup = true }
                                    )

                                    DropdownMenu(
                                        expanded = showSelectionMorePopup,
                                        onDismissRequest = { showSelectionMorePopup = false },
                                        modifier = Modifier
                                            .widthIn(min = 250.dp, max = 290.dp)
                                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.action_favorite), style = MaterialTheme.typography.bodyMedium)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                                viewModel.toggleFavoriteSelectedMedia(selectedIds.toSet())
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(R.string.menu_compress_images), style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.menu_compress_images_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Compress, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                showBulkCompressDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(R.string.menu_manage_tags), style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.menu_manage_tags_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                showBulkTagDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            val allVisibleItems by viewModel.visibleMediaItems.collectAsState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Share,
                                    label = stringResource(R.string.action_share),
                                    onClick = { showSelectionShareDialog = true }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Edit,
                                    label = stringResource(R.string.action_edit),
                                    enabled = selectedIds.size == 1,
                                    onClick = {
                                        if (selectedIds.size == 1) {
                                            val singleId = selectedIds.first()
                                            val item = allVisibleItems.firstOrNull { it.id == singleId }
                                            if (item is com.hrshd1eux.imava.data.model.MediaItem.Photo) {
                                                selectionState.clear()
                                                viewModel.editingMediaItem = item
                                            } else if (item is com.hrshd1eux.imava.data.model.MediaItem.Video) {
                                                android.widget.Toast.makeText(context, context.getString(R.string.edit_photos_only_toast), android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, context.getString(R.string.item_not_found_toast), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, context.getString(R.string.select_one_photo_to_edit_toast), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Delete,
                                    label = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { showSelectionDeleteConfirmDialog = true }
                                )

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SelectionActionButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        icon = Icons.Default.MoreVert,
                                        label = stringResource(R.string.action_more),
                                        onClick = { showSelectionMorePopup = true }
                                    )

                                    DropdownMenu(
                                        expanded = showSelectionMorePopup,
                                        onDismissRequest = { showSelectionMorePopup = false },
                                        modifier = Modifier
                                            .widthIn(min = 250.dp, max = 290.dp)
                                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.action_favorite), style = MaterialTheme.typography.bodyMedium)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                                viewModel.toggleFavoriteSelectedMedia(selectedIds.toSet())
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(R.string.menu_compress_images), style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.menu_compress_images_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Compress, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                showBulkCompressDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(R.string.menu_manage_tags), style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.menu_manage_tags_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                showBulkTagDialog = true
                                            }
                                        )

                                        if (!isViewingVault) {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(stringResource(R.string.menu_move_copy), style = MaterialTheme.typography.bodyMedium)
                                                        Text(stringResource(R.string.menu_move_copy_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    showMoveToAlbumDialog = true
                                                }
                                            )
                                        }

                                        if (!viewModel.isVaultDisabled || isViewingVault) {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(if (isViewingVault) stringResource(R.string.action_unhide) else stringResource(R.string.menu_move_to_vault), style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (isViewingVault) stringResource(R.string.menu_unhide_desc) else stringResource(R.string.menu_move_to_vault_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(if (isViewingVault) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    if (isViewingVault) {
                                                        viewModel.hideSelectedMedia(context)
                                                    } else {
                                                        showVaultPreConfirmDialog = true
                                                    }
                                                }
                                            )
                                        }

                                        if (!isViewingVault) {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(if (selectedIds.size == 1) stringResource(R.string.action_rename) else stringResource(R.string.rename_file), style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (selectedIds.size == 1) stringResource(R.string.menu_rename_item_desc) else stringResource(R.string.items_selected_format, selectedIds.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    if (selectedIds.size == 1) {
                                                        showSingleRenameDialog = true
                                                    } else {
                                                        showBatchRenameDialog = true
                                                    }
                                                }
                                            )
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(R.string.menu_create_pdf), style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.menu_create_pdf_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                scope.launch {
                                                    val itemsToExport = viewModel.getSelectedMediaItems(selectedIds.toSet())
                                                    if (itemsToExport.isNotEmpty()) {
                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_generating_pdf), android.widget.Toast.LENGTH_SHORT).show()
                                                        val pdfUri = com.hrshd1eux.imava.core.util.PdfConverter.createPdfFromImages(context, itemsToExport)
                                                        if (pdfUri != null) {
                                                            com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                                            com.hrshd1eux.imava.core.util.PdfConverter.sharePdf(context, pdfUri)
                                                        } else {
                                                            com.hrshd1eux.imava.core.util.HapticUtil.performError(context)
                                                            android.widget.Toast.makeText(context, context.getString(R.string.toast_pdf_failed), android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_pdf_no_photos), android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(R.string.menu_clean_copy), style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.menu_clean_copy_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                scope.launch {
                                                    val items = viewModel.getSelectedMediaItems(selectedIds.toSet())
                                                    if (items.isNotEmpty()) {
                                                        viewModel.exportCleanCopies(context, items)
                                                    } else {
                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_no_photos_export), android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(R.string.menu_lossless_rotate), style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.menu_lossless_rotate_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                scope.launch {
                                                    val items = viewModel.getSelectedMediaItems(selectedIds.toSet())
                                                    val photos = items.filterIsInstance<com.hrshd1eux.imava.data.model.MediaItem.Photo>()
                                                    if (photos.isNotEmpty()) {
                                                        photos.forEach { p ->
                                                            viewModel.rotateMediaLosslessly(context, p, clockwise = true)
                                                        }
                                                        selectionState.clear()
                                                    } else {
                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_select_photos_rotate), android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )

                                        if (!isViewingVault) {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(stringResource(R.string.menu_collage), style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (selectedIds.size in 2..9) stringResource(R.string.menu_collage_ready_desc) else stringResource(R.string.menu_collage_req_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                enabled = selectedIds.size in 2..9,
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    scope.launch {
                                                        val items = viewModel.getSelectedMediaItems(selectedIds.toSet())
                                                        val photoUris = items.filterIsInstance<com.hrshd1eux.imava.data.model.MediaItem.Photo>().map { it.uri }
                                                        if (photoUris.size in 2..9) {
                                                            selectionState.clear()
                                                            collageImageUris = photoUris
                                                        } else {
                                                            android.widget.Toast.makeText(context, context.getString(R.string.toast_select_collage_photos), android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(stringResource(R.string.menu_compare), style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (selectedIds.size == 2) stringResource(R.string.menu_compare_desc) else stringResource(R.string.menu_compare_req_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Compare, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                enabled = selectedIds.size == 2,
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    scope.launch {
                                                        val items = viewModel.getSelectedMediaItems(selectedIds.toSet())
                                                        if (items.size == 2) {
                                                            compareItems = Pair(items[0], items[1])
                                                        }
                                                    }
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(stringResource(R.string.menu_slideshow), style = MaterialTheme.typography.bodyMedium)
                                                        Text(stringResource(R.string.menu_slideshow_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Slideshow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    scope.launch {
                                                        val items = viewModel.getSelectedMediaItems(selectedIds.toSet())
                                                        if (items.isNotEmpty()) {
                                                            selectionState.clear()
                                                            slideshowItems = items
                                                        }
                                                    }
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(stringResource(R.string.menu_print), style = MaterialTheme.typography.bodyMedium)
                                                        Text(stringResource(R.string.menu_print_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    scope.launch {
                                                        val items = viewModel.getSelectedMediaItems(selectedIds.toSet())
                                                        val firstPhoto = items.filterIsInstance<com.hrshd1eux.imava.data.model.MediaItem.Photo>().firstOrNull()
                                                        if (firstPhoto != null) {
                                                            com.hrshd1eux.imava.core.util.PrintUtil.printPhoto(context, firstPhoto)
                                                        } else {
                                                            android.widget.Toast.makeText(context, context.getString(R.string.toast_select_photo_print), android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(stringResource(R.string.menu_shift_timestamp), style = MaterialTheme.typography.bodyMedium)
                                                        Text(stringResource(R.string.menu_shift_timestamp_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                onClick = {
                                                    showSelectionMorePopup = false
                                                    showTimeShiftDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!isWideScreen && viewModel.currentCategoryName != "Hidden Vault") {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Photos && viewModel.currentBucketId == null && viewModel.currentCategoryName == null,
                            onClick = {
                                if (currentScreen == Screen.Photos && viewModel.currentBucketId == null && viewModel.currentCategoryName == null) {
                                    viewModel.triggerScrollToTop()
                                    com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                } else {
                                    viewModel.selectBucket(null, null)
                                    viewModel.currentCategoryName = null
                                    viewModel.setSearchQuery("")
                                    viewModel.currentScreen = Screen.Photos
                                }
                            },
                            icon = { Icon(Icons.Default.Photo, contentDescription = stringResource(R.string.nav_photos)) },
                            label = { Text(stringResource(R.string.nav_photos)) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Albums,
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.currentScreen = Screen.Albums
                                viewModel.loadBuckets()
                            },
                            icon = { Icon(Icons.Default.Album, contentDescription = stringResource(R.string.nav_albums)) },
                            label = { Text(stringResource(R.string.nav_albums)) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Settings,
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.currentScreen = Screen.Settings
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                            label = { Text(stringResource(R.string.nav_settings)) }
                        )
                    }
                }
            }
        }
    }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val batchProgress by viewModel.batchOperationProgress.collectAsState()
            AnimatedVisibility(
                visible = batchProgress != null,
                enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                    animationSpec = tween(250),
                    initialOffsetY = { -it }
                ),
                exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { -it }
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(150f)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                batchProgress?.let { progress ->
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        shadowElevation = 8.dp,
                        tonalElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (progress.isIndeterminate || progress.total <= 0) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                val frac = (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
                                CircularProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = progress.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!progress.isIndeterminate && progress.total > 0) {
                                Text(
                                    text = "${progress.current} / ${progress.total}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val currentFilterType by viewModel.selectedFilter.collectAsState()
                val currentCustomTagVal by viewModel.selectedCustomTag.collectAsState()
                val isInsideSubView = viewModel.currentCategoryName != null ||
                        viewModel.currentBucketId != null ||
                        currentFilterType != MediaFilterType.ALL ||
                        currentCustomTagVal != null ||
                        selectionState.inSelectionMode ||
                        viewModel.activeMediaItem != null ||
                        isFullScreenOverlayActive

                val pagerContent: @Composable (Modifier) -> Unit = { pagerModifier ->
                    HorizontalPager(
                        state = mainPagerState,
                        beyondBoundsPageCount = 0,
                        modifier = pagerModifier,
                        userScrollEnabled = !isInsideSubView
                    ) { page ->
                        when (screens[page]) {
                            Screen.Photos -> TimelineScreen(viewModel = viewModel)
                            Screen.Albums -> AlbumsScreen(
                                viewModel = viewModel,
                                onStorageDoctorClick = { showStorageDoctor = true },
                            )
                            Screen.Settings -> com.hrshd1eux.imava.ui.settings.SettingsScreen(viewModel = viewModel)
                            else -> TimelineScreen(viewModel = viewModel)
                        }
                    }
                }

                if (isWideScreen && !isFullScreenOverlayActive && !selectionState.inSelectionMode && viewModel.currentCategoryName != "Hidden Vault") {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            NavigationRailItem(
                                selected = currentScreen == Screen.Photos && viewModel.currentBucketId == null && viewModel.currentCategoryName == null,
                                onClick = {
                                    if (currentScreen == Screen.Photos && viewModel.currentBucketId == null && viewModel.currentCategoryName == null) {
                                        viewModel.triggerScrollToTop()
                                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                    } else {
                                        viewModel.selectBucket(null, null)
                                        viewModel.currentCategoryName = null
                                        viewModel.setSearchQuery("")
                                        viewModel.currentScreen = Screen.Photos
                                    }
                                },
                                icon = { Icon(Icons.Default.Photo, contentDescription = stringResource(R.string.nav_photos)) },
                                label = { Text(stringResource(R.string.nav_photos)) }
                            )
                            NavigationRailItem(
                                selected = currentScreen == Screen.Albums,
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    viewModel.currentScreen = Screen.Albums
                                    viewModel.loadBuckets()
                                },
                                icon = { Icon(Icons.Default.Album, contentDescription = stringResource(R.string.nav_albums)) },
                                label = { Text(stringResource(R.string.nav_albums)) }
                            )
                            NavigationRailItem(
                                selected = currentScreen == Screen.Settings,
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    viewModel.currentScreen = Screen.Settings
                                },
                                icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                                label = { Text(stringResource(R.string.nav_settings)) }
                            )
                        }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            pagerContent(Modifier.fillMaxSize())
                        }
                    }
                } else {
                    pagerContent(Modifier.fillMaxSize())
                }
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.Search,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                SearchScreen(viewModel = viewModel)
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.DuplicateFinder,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                com.hrshd1eux.imava.ui.search.DuplicateFinderScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.currentScreen = Screen.Albums }
                )
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.AboutLegal,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                com.hrshd1eux.imava.ui.settings.AboutLegalScreen(
                    onBackClick = { viewModel.currentScreen = Screen.Settings },
                    onOpenPermissions = { viewModel.currentScreen = Screen.PermissionsTransparency },
                    onOpenLicenses = { viewModel.currentScreen = Screen.OpenSourceLicenses }
                )
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.PermissionsTransparency,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                com.hrshd1eux.imava.ui.settings.PermissionsTransparencyScreen(
                    onBackClick = { viewModel.currentScreen = Screen.AboutLegal }
                )
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.OpenSourceLicenses,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                com.hrshd1eux.imava.ui.settings.OpenSourceLicensesScreen(
                    onBackClick = { viewModel.currentScreen = Screen.AboutLegal }
                )
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.VaultSetup,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                com.hrshd1eux.imava.ui.vault.VaultSetupScreen(
                    onBackClick = { viewModel.currentScreen = Screen.Albums },
                    onSetupComplete = {
                        if (pendingMoveToVaultIds.isNotEmpty()) {
                            viewModel.hideMediaByIds(context, pendingMoveToVaultIds)
                            pendingMoveToVaultIds = emptySet()
                            android.widget.Toast.makeText(context, context.getString(R.string.toast_moved_to_vault), android.widget.Toast.LENGTH_SHORT).show()
                        }
                        viewModel.currentBucketId = null
                        viewModel.currentBucketName = null
                        viewModel.currentCategoryName = "Hidden Vault"
                        viewModel.currentScreen = Screen.Photos
                        viewModel.unlockVault()
                    }
                )
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.VaultSettings,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                com.hrshd1eux.imava.ui.vault.VaultSettingsScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        if (viewModel.currentCategoryName == "Hidden Vault") {
                            viewModel.currentScreen = Screen.Photos
                        } else {
                            viewModel.currentScreen = Screen.Settings
                        }
                    },
                    onVaultDeleted = {
                        viewModel.currentCategoryName = null
                        viewModel.currentScreen = Screen.Albums
                    }
                )
            }


            AnimatedVisibility(
                visible = viewModel.activeMemoryStory != null,
                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                exit = fadeOut() + scaleOut(targetScale = 0.94f)
            ) {
                val story = viewModel.activeMemoryStory
                if (story != null) {
                    com.hrshd1eux.imava.ui.timeline.MemoryStoryPlayerScreen(
                        story = story,
                        onDismiss = { viewModel.activeMemoryStory = null },
                        onOpenMediaInViewer = { item ->
                            viewModel.setCustomViewerMediaItems(story.items)
                            viewModel.activeMemoryStory = null
                            viewModel.activeMediaItem = item
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = viewModel.activeMediaItem != null,
                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                exit = fadeOut() + scaleOut(targetScale = 0.94f)
            ) {
                PhotoViewerScreen(viewModel = viewModel)
            }

            AnimatedVisibility(
                visible = compareItems != null,
                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                exit = fadeOut() + scaleOut(targetScale = 0.94f)
            ) {
                compareItems?.let { (item1, item2) ->
                    com.hrshd1eux.imava.ui.compare.PhotoCompareScreen(
                        item1 = item1,
                        item2 = item2,
                        onBack = { compareItems = null }
                    )
                }
            }


            val collageUris = collageImageUris
            if (collageUris != null) {
                com.hrshd1eux.imava.ui.collage.CollageMakerScreen(
                    imageUris = collageUris,
                    onDismiss = { collageImageUris = null },
                    onCollageSaved = {
                        collageImageUris = null
                        selectionState.clear()
                        viewModel.refreshAll()
                    }
                )
            }


            if (showStorageDoctor) {
                com.hrshd1eux.imava.ui.storage.StorageDoctorScreen(
                    viewModel = viewModel,
                    onDismiss = { showStorageDoctor = false },
                    onMediaClick = { item ->
                        showStorageDoctor = false
                        viewModel.activeMediaItem = item
                    }
                )
            }


            val showItems = slideshowItems
            if (showItems != null) {
                com.hrshd1eux.imava.ui.slideshow.SlideshowPlayerScreen(
                    mediaItems = showItems,
                    onDismiss = { slideshowItems = null }
                )
            }
        }
    }

    if (showSelectionShareDialog) {
        AlertDialog(
            onDismissRequest = { showSelectionShareDialog = false },
            title = { Text(stringResource(R.string.share_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.share_choose_privacy),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectionSharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectionSharePrivacyMode == com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL,
                            onClick = { selectionSharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.share_strip_all), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.share_strip_all_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectionSharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.FUZZ_LOCATION }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectionSharePrivacyMode == com.hrshd1eux.imava.core.util.SharePrivacyMode.FUZZ_LOCATION,
                            onClick = { selectionSharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.FUZZ_LOCATION }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.share_fuzz_gps), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.share_fuzz_gps_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectionSharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.FULL_METADATA }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectionSharePrivacyMode == com.hrshd1eux.imava.core.util.SharePrivacyMode.FULL_METADATA,
                            onClick = { selectionSharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.FULL_METADATA }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.share_full_metadata), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.share_full_metadata_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSelectionShareDialog = false
                                showImavaDropDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.share_via_wifi_drop_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.share_via_wifi_drop_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectionShareDialog = false
                        viewModel.shareSelectedMedia(context, selectionSharePrivacyMode)
                    }
                ) {
                    Text(stringResource(R.string.action_share))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionShareDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showImavaDropDialog) {
        val allVisible by viewModel.visibleMediaItems.collectAsState()
        val selectedIds = selectionState.selectedIds
        val itemsToDrop = remember(selectedIds, allVisible) {
            if (selectedIds.isNotEmpty()) {
                allVisible.filter { it.id in selectedIds }
            } else {
                emptyList()
            }
        }
        com.hrshd1eux.imava.ui.p2p.ImavaDropDialog(
            itemsToShare = itemsToDrop,
            onDismissRequest = { showImavaDropDialog = false }
        )
    }

    if (showVaultPreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showVaultPreConfirmDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.vault_hide_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.vault_hide_dialog_message))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.vault_hide_allow_next_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVaultPreConfirmDialog = false
                        val vaultPrefs = context.getSharedPreferences("vault_prefs", android.content.Context.MODE_PRIVATE)
                        val storedPinHash = vaultPrefs.getString("vault_pin_hash", null)
                        val legacyPin = vaultPrefs.getString("vault_pin", null)
                        val isVaultDisabled = vaultPrefs.getBoolean("vault_disabled", false)
                        val isConfigured = !isVaultDisabled && (storedPinHash != null || legacyPin != null)

                        if (!isConfigured) {
                            pendingMoveToVaultIds = selectionState.selectedIds.toSet()
                            showVaultSetupRequiredDialog = true
                        } else {
                            viewModel.hideSelectedMedia(context)
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultPreConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showVaultSetupRequiredDialog) {
        AlertDialog(
            onDismissRequest = {
                showVaultSetupRequiredDialog = false
                pendingMoveToVaultIds = emptySet()
            },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.vault_setup_required_title)) },
            text = {
                Text(stringResource(R.string.vault_setup_required_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVaultSetupRequiredDialog = false
                        viewModel.currentScreen = Screen.VaultSetup
                    }
                ) {
                    Text(stringResource(R.string.vault_setup_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showVaultSetupRequiredDialog = false
                        pendingMoveToVaultIds = emptySet()
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSelectionRestoreConfirmDialog) {
        val count = selectionState.selectedIds.size
        AlertDialog(
            onDismissRequest = { showSelectionRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.RestoreFromTrash, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.restore_items_confirm_title)) },
            text = { Text(stringResource(R.string.restore_items_confirm_message, count)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectionRestoreConfirmDialog = false
                        com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                        viewModel.restoreSelectedMedia(context)
                    }
                ) {
                    Text(stringResource(R.string.action_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionRestoreConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }


    if (showSelectionDeleteConfirmDialog) {
        val count = selectionState.selectedIds.size
        AlertDialog(
            onDismissRequest = { showSelectionDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.trash_items_confirm_title)) },
            text = { Text(stringResource(R.string.trash_items_confirm_message, count)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectionDeleteConfirmDialog = false
                        viewModel.deleteSelectedMedia(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.move_to_trash))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSelectionDeletePermanentlyConfirmDialog) {
        val count = selectionState.selectedIds.size
        AlertDialog(
            onDismissRequest = { showSelectionDeletePermanentlyConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_permanently_confirm_title)) },
            text = { Text(stringResource(R.string.delete_permanently_confirm_message, count)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectionDeletePermanentlyConfirmDialog = false
                        viewModel.deleteSelectedMediaPermanently(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_permanently))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionDeletePermanentlyConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showMoveToAlbumDialog) {
        val bucketList by viewModel.buckets.collectAsState()
        com.hrshd1eux.imava.ui.common.MoveCopyAlbumDialog(
            buckets = bucketList,
            isCopy = false,
            onDismiss = { showMoveToAlbumDialog = false },
            onConfirm = { targetDir, isCopy ->
                showMoveToAlbumDialog = false
                viewModel.moveOrCopySelectedMedia(context, targetDir, isCopy) {}
            }
        )
    }

    if (showTimeShiftDialog) {
        com.hrshd1eux.imava.ui.common.ExifTimeShiftDialog(
            selectedCount = selectionState.selectedIds.size,
            onDismiss = { showTimeShiftDialog = false },
            onConfirm = { offsetMs, exactTimestamp ->
                showTimeShiftDialog = false
                viewModel.shiftSelectedMediaTimestamp(context, offsetMs, exactTimestamp) {}
            }
        )
    }

    if (showBatchRenameDialog) {
        val selectedIdsSet = selectionState.selectedIds.toSet()
        val allVisible by viewModel.visibleMediaItems.collectAsState()
        val selectedItems = remember(selectedIdsSet, allVisible) {
            allVisible.filter { selectedIdsSet.contains(it.id) }
        }
        com.hrshd1eux.imava.ui.common.BatchRenameDialog(
            selectedItems = selectedItems,
            onDismiss = { showBatchRenameDialog = false },
            onConfirmRename = { renames ->
                viewModel.batchRenameSelectedMedia(context, renames)
                showBatchRenameDialog = false
            }
        )
    }

    if (showSingleRenameDialog) {
        val selectedIdsSet = selectionState.selectedIds.toSet()
        val allVisible by viewModel.visibleMediaItems.collectAsState()
        val singleItem = remember(selectedIdsSet, allVisible) {
            allVisible.firstOrNull { selectedIdsSet.contains(it.id) }
        }
        if (singleItem != null) {
            com.hrshd1eux.imava.ui.common.SingleRenameDialog(
                item = singleItem,
                onDismiss = { showSingleRenameDialog = false },
                onConfirmRename = { newNameWithExt ->
                    showSingleRenameDialog = false
                    viewModel.renameMedia(context, singleItem, newNameWithExt)
                    selectionState.clear()
                }
            )
        } else {
            showSingleRenameDialog = false
        }
    }

    if (showBulkCompressDialog) {
        val selectedIds = selectionState.selectedIds
        val selectedIdsSet = remember(selectedIds) { selectedIds.toSet() }
        val allVisible by viewModel.visibleMediaItems.collectAsState()
        val selectedItems = remember(selectedIdsSet, allVisible) {
            allVisible.filter { selectedIdsSet.contains(it.id) }
        }

        BulkCompressDialog(
            selectedItems = selectedItems,
            onDismiss = { showBulkCompressDialog = false },
            onComplete = {
                showBulkCompressDialog = false
                selectionState.clear()
                viewModel.refreshAll()
            }
        )
    }

    if (showBulkTagDialog) {
        val selectedIds = selectionState.selectedIds
        val selectedIdsSet = remember(selectedIds) { selectedIds.toSet() }
        val allVisible by viewModel.visibleMediaItems.collectAsState()
        val selectedItems = remember(selectedIdsSet, allVisible) {
            allVisible.filter { selectedIdsSet.contains(it.id) }
        }
        val distinctTags by viewModel.allDistinctTags.collectAsState()

        BulkTagDialog(
            selectedItems = selectedItems,
            allExistingTags = distinctTags,
            onDismiss = { showBulkTagDialog = false },
            onConfirm = { tagsToAdd, tagsToRemove ->
                showBulkTagDialog = false
                viewModel.bulkUpdateMediaTags(selectedIdsSet, tagsToAdd, tagsToRemove)
                com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.toast_updated_tags_count, selectedItems.size),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    val currentAppUpdate = appUpdateInfoToPrompt
    if (currentAppUpdate != null) {
        AppUpdateDialog(
            updateInfo = currentAppUpdate,
            onDismiss = { appUpdateInfoToPrompt = null }
        )
    }

    val appUpdateDownloadState by AppUpdateManager.downloadState.collectAsState()
    var installPromptDismissedVersion by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val completedUpdateState = appUpdateDownloadState as? AppUpdateDownloadState.Completed

    if (completedUpdateState != null && currentAppUpdate == null && installPromptDismissedVersion != completedUpdateState.version) {
        AlertDialog(
            onDismissRequest = { installPromptDismissedVersion = completedUpdateState.version },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.update_ready_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.update_ready_desc, completedUpdateState.version),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.update_tap_install_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        installPromptDismissedVersion = completedUpdateState.version
                        AppUpdateManager.installApk(context, completedUpdateState.apkFile)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.update_install_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { installPromptDismissedVersion = completedUpdateState.version }
                ) {
                    Text(stringResource(R.string.update_later))
                }
            }
        )
    }

    if (showScrubCurrentAlbumDialog && viewModel.currentBucketId != null) {
        val currentBucketId = viewModel.currentBucketId!!
        val currentBucketName = viewModel.currentBucketName ?: stringResource(R.string.album_default_label)
        val allVisibleItems by viewModel.visibleMediaItems.collectAsState()
        val albumItemCount = remember(allVisibleItems, currentBucketId) {
            allVisibleItems.count { it.bucketId == currentBucketId }
        }

        ScrubAlbumDialog(
            albumName = currentBucketName,
            itemCount = albumItemCount,
            isScrubbing = isScrubbingCurrentAlbum,
            progressCurrent = currentAlbumScrubProgress,
            progressTotal = currentAlbumScrubTotal,
            onDismiss = {
                if (!isScrubbingCurrentAlbum) {
                    showScrubCurrentAlbumDialog = false
                }
            },
            onConfirmScrub = { options ->
                isScrubbingCurrentAlbum = true
                currentAlbumScrubProgress = 0
                currentAlbumScrubTotal = albumItemCount
                viewModel.scrubAlbumMetadata(
                    context = context,
                    bucketId = currentBucketId,
                    options = options,
                    onProgress = { current, total ->
                        currentAlbumScrubProgress = current
                        currentAlbumScrubTotal = total
                    },
                    onComplete = { count ->
                        isScrubbingCurrentAlbum = false
                        showScrubCurrentAlbumDialog = false
                        if (count > 0) {
                            com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                            android.widget.Toast.makeText(context, context.getString(R.string.toast_sanitized_count_album, count, currentBucketName), android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            android.widget.Toast.makeText(context, context.getString(R.string.toast_no_sanitization_needed, currentBucketName), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        )
    }

    if (showSecureShredderNotice) {
        com.hrshd1eux.imava.ui.common.SecureShredderNoticeDialog(
            context = context,
            onDismiss = { showSecureShredderNotice = false }
        )
    }
}

@Composable
fun PermissionFallbackScreen(onRequestPermissions: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.request_permissions_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.request_permissions_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRequestPermissions) {
                Text(stringResource(R.string.grant_permission))
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            ) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

@Composable
fun SelectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    contentDescription: String = label,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
