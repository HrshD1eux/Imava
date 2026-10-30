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
import com.hrshd1eux.imava.core.util.UpdateInfo
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
    }

    private var hasPermissionsState = mutableStateOf(false)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            window.colorMode = ActivityInfo.COLOR_MODE_HDR
        }

        checkPermissions()
        handleIncomingIntent(intent)

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
            title = "Imava is Locked",
            subtitle = "Authenticate to access gallery",
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
            viewModel.openExternalMedia(this, intent.data!!, intent.type)
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
                            android.widget.Toast.makeText(context, "Emergency Shake: Vault Locked 🔒", android.widget.Toast.LENGTH_SHORT).show()
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
            } else {
                viewModel.currentCategoryName = null
                viewModel.currentScreen = Screen.Albums
            }
        } else if (viewModel.currentBucketId != null) {
            viewModel.selectBucket(null, null)
            viewModel.currentScreen = Screen.Albums
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
                                text = "${selectionState.selectedIds.size} selected",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectionState.clear() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit selection"
                                )
                            }
                        },
                        actions = {
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
                                    text = if (isAllSelected) "Deselect All" else "Select All",
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
                    Text(
                        text = if (viewModel.currentCategoryName != null && currentScreen == Screen.Photos) {
                            viewModel.currentCategoryName!!
                        } else if (viewModel.currentBucketName != null && currentScreen == Screen.Photos) {
                            viewModel.currentBucketName!!
                        } else {
                            currentScreen.name
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
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
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All Media")
                        }
                    } else if (viewModel.currentBucketId != null && currentScreen == Screen.Photos) {
                        IconButton(onClick = { 
                            viewModel.selectBucket(null, null) 
                            viewModel.currentScreen = Screen.Albums
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All Media")
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.Photos) {
                        DescriptiveIconButton(
                            onClick = { viewModel.toggleGridStyle() },
                            title = "Grid Layout",
                            description = if (viewModel.gridStyle == GridStyle.SQUARE) "Current: Square grid. Tap to switch to adaptive ratio." else "Current: Adaptive aspect ratio. Tap to switch to square."
                        ) {
                            Icon(
                                imageVector = if (viewModel.gridStyle == GridStyle.SQUARE) Icons.Default.CropSquare else Icons.Default.GridView,
                                contentDescription = "Toggle Grid Style"
                            )
                        }

                        DescriptiveIconButton(
                            onClick = { viewModel.toggleSortOrder() },
                            title = "Sort Order",
                            description = if (viewModel.sortOrder == SortOrder.NEWEST_FIRST) "Current: Newest first. Tap to show oldest first." else "Current: Oldest first. Tap to show newest first."
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.SwapVert,
                                contentDescription = if (viewModel.sortOrder == SortOrder.NEWEST_FIRST) "Newest First" else "Oldest First",
                                tint = if (viewModel.sortOrder == SortOrder.OLDEST_FIRST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DescriptiveIconButton(
                            onClick = { viewModel.toggleSortMode() },
                            title = "View Mode",
                            description = if (viewModel.sortMode == TimelineSortMode.DATE_GROUPED) "Current: Grouped by date. Tap for continuous stream." else "Current: Continuous stream. Tap to group by date."
                        ) {
                            Icon(
                                imageVector = if (viewModel.sortMode == TimelineSortMode.DATE_GROUPED) Icons.Default.DateRange else Icons.Default.ViewStream,
                                contentDescription = "Toggle Sort Mode"
                            )
                        }

                        DescriptiveIconButton(
                            onClick = {
                                if (selectionState.selectedIds.isEmpty()) {
                                    Toast.makeText(context, "Select the images you want to share", Toast.LENGTH_SHORT).show()
                                } else {
                                    showImavaDropDialog = true
                                }
                            },
                            title = "Wi-Fi Drop ⚡",
                            description = "Transfer media directly to PC, Mac, or phone over local Wi-Fi without cloud"
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = "Wi-Fi Drop"
                            )
                        }

                        val categoryName by viewModel.currentCategoryNameFlow.collectAsState()
                        if (categoryName == "Trash") {
                            var showEmptyTrashConfirm by remember { mutableStateOf(false) }

                            IconButton(onClick = { showEmptyTrashConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Empty Trash",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            if (showEmptyTrashConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showEmptyTrashConfirm = false },
                                    icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    title = { Text("Empty Trash?") },
                                    text = { Text("All items in Trash will be permanently deleted from your device storage. This action cannot be undone.") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showEmptyTrashConfirm = false
                                                viewModel.emptyTrash(context)
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Empty Trash")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEmptyTrashConfirm = false }) {
                                            Text("Cancel")
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
                                    contentDescription = "Vault Security Options"
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
                                            Text("Lock Vault Now", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("Immediately lock and hide private media", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            Text("Vault Security Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("Master password, decoy PIN, backup & delete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            val currentBucketName = viewModel.currentBucketName ?: "Album"
                            val isAutoScrub = viewModel.isAutoScrubEnabled(currentBucketId)

                            IconButton(onClick = { showAlbumMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Album Options"
                                )
                            }

                            DropdownMenu(
                                expanded = showAlbumMenu,
                                onDismissRequest = { showAlbumMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Scrub Album Metadata 🛡️") },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                    onClick = {
                                        showAlbumMenu = false
                                        showScrubCurrentAlbumDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isAutoScrub) "Auto-Scrub: Enabled ✓" else "Auto-Scrub: Disabled 🔄") },
                                    leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                    onClick = {
                                        showAlbumMenu = false
                                        val newState = viewModel.toggleAutoScrubBucket(currentBucketId)
                                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                        android.widget.Toast.makeText(
                                            context,
                                            if (newState) "Auto-Scrub enabled for $currentBucketName 🔄" else "Auto-Scrub disabled for $currentBucketName",
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
                        val trashedList by viewModel.trashed.collectAsState()
                        val selectedIds = selectionState.selectedIds
                        val isViewingTrash = categoryName == "Trash" || (selectedIds.isNotEmpty() && trashedList.any { selectedIds.contains(it.id) })

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
                                    label = "Restore",
                                    onClick = { showSelectionRestoreConfirmDialog = true }
                                )
                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.DeleteForever,
                                    label = "Delete Forever",
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
                                    label = "Unhide",
                                    onClick = {
                                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                        viewModel.hideSelectedMedia(context)
                                    }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Share,
                                    label = "Share",
                                    onClick = { showSelectionShareDialog = true }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.DeleteForever,
                                    label = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { showSelectionDeletePermanentlyConfirmDialog = true }
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    SelectionActionButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        icon = Icons.Default.MoreVert,
                                        label = "More",
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
                                                Text("Favorite", style = MaterialTheme.typography.bodyMedium)
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
                                                    Text("Compress Images", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Bulk compress photos to target size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                    Text("Manage Tags", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Add or remove tags for selected items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    label = "Share",
                                    onClick = { showSelectionShareDialog = true }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.WifiTethering,
                                    label = "Drop",
                                    contentDescription = "Wi-Fi Drop",
                                    onClick = { showImavaDropDialog = true }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Edit,
                                    label = "Edit",
                                    enabled = selectedIds.size == 1,
                                    onClick = {
                                        if (selectedIds.size == 1) {
                                            val singleId = selectedIds.first()
                                            val item = allVisibleItems.firstOrNull { it.id == singleId }
                                            if (item is com.hrshd1eux.imava.data.model.MediaItem.Photo) {
                                                selectionState.clear()
                                                viewModel.editingMediaItem = item
                                            } else if (item is com.hrshd1eux.imava.data.model.MediaItem.Video) {
                                                android.widget.Toast.makeText(context, "Editing is only supported for photos", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Item not found", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "Select 1 photo to edit", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                SelectionActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Delete,
                                    label = "Delete",
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
                                        label = "More",
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
                                                Column {
                                                    Text("Wi-Fi Drop ⚡", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Direct P2P share to PC or phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                showImavaDropDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text("Favorite", style = MaterialTheme.typography.bodyMedium)
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
                                                    Text("Compress Images", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Bulk compress photos to target size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                    Text("Manage Tags", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Add or remove tags for selected items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        Text("Move / Copy", style = MaterialTheme.typography.bodyMedium)
                                                        Text("Move or copy to an album", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        Text(if (isViewingVault) "Unhide" else "Move to Vault", style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (isViewingVault) "Restore to gallery" else "Protect with PIN/Password", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        Text(if (selectedIds.size == 1) "Rename" else "Batch Rename", style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (selectedIds.size == 1) "Rename item" else "Rename ${selectedIds.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Wi-Fi Drop ⚡", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Transfer uncompressed to PC or phone over Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            onClick = {
                                                showSelectionMorePopup = false
                                                showImavaDropDialog = true
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Create PDF", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Convert photos into PDF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        android.widget.Toast.makeText(context, "Generating PDF...", android.widget.Toast.LENGTH_SHORT).show()
                                                        val pdfUri = com.hrshd1eux.imava.core.util.PdfConverter.createPdfFromImages(context, itemsToExport)
                                                        if (pdfUri != null) {
                                                            com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                                            com.hrshd1eux.imava.core.util.PdfConverter.sharePdf(context, pdfUri)
                                                        } else {
                                                            com.hrshd1eux.imava.core.util.HapticUtil.performError(context)
                                                            android.widget.Toast.makeText(context, "Could not create PDF from selected items", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        android.widget.Toast.makeText(context, "No photos selected for PDF", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Clean Copy (Strip EXIF)", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Export copies without metadata", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        android.widget.Toast.makeText(context, "No photos selected to export", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Lossless Rotate", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Rotate photos 90° clockwise", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        android.widget.Toast.makeText(context, "Select photos to rotate", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )

                                        if (!isViewingVault) {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text("Collage", style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (selectedIds.size in 2..9) "Create collage" else "Requires 2 to 9 photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                            android.widget.Toast.makeText(context, "Select 2 to 9 photos for collage", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text("Compare", style = MaterialTheme.typography.bodyMedium)
                                                        Text(if (selectedIds.size == 2) "Compare 2 items side-by-side" else "Requires exactly 2 items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        Text("Slideshow", style = MaterialTheme.typography.bodyMedium)
                                                        Text("Play animated slideshow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        Text("Print", style = MaterialTheme.typography.bodyMedium)
                                                        Text("Send photo to printer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                            android.widget.Toast.makeText(context, "Select a photo to print", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text("Shift Timestamp", style = MaterialTheme.typography.bodyMedium)
                                                        Text("Adjust date and time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                } else if (viewModel.currentCategoryName != "Hidden Vault") {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Photos,
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.currentScreen = Screen.Photos
                            },
                            icon = { Icon(Icons.Default.Photo, contentDescription = "Photos") },
                            label = { Text("Photos") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Albums,
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.currentScreen = Screen.Albums
                                viewModel.loadBuckets()
                            },
                            icon = { Icon(Icons.Default.Album, contentDescription = "Albums") },
                            label = { Text("Albums") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Settings,
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.currentScreen = Screen.Settings
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
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

                HorizontalPager(
                    state = mainPagerState,
                    beyondBoundsPageCount = 0,
                    modifier = Modifier.fillMaxSize(),
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
                            android.widget.Toast.makeText(context, "Items safely moved to your Private Vault!", android.widget.Toast.LENGTH_SHORT).show()
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
            title = { Text("Share Selected Files 🛡️") },
            text = {
                Column {
                    Text(
                        text = "Choose metadata & privacy level before sharing:",
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
                            Text("Strip All Metadata 🛡️", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Remove all GPS coordinates, timestamps, & camera serials", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Fuzz GPS Location (~2 km) 🌐", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Obscure exact street with coarse ~2 km radius; keep city area", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Full Metadata 📷", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Preserve original EXIF markers and exact GPS position", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    text = "Send via Imava Wi-Fi Drop ⚡",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Direct high-speed P2P transfer to PC, Mac or phone (zero cloud)",
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
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionShareDialog = false }) {
                    Text("Cancel")
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
            title = { Text("Hide in Private Vault") },
            text = {
                Column {
                    Text("The selected items will be safely locked in your private vault so only you can see them.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Next, your phone will ask to remove the copies from your main gallery. Please tap Allow on the next screen.",
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
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultPreConfirmDialog = false }) {
                    Text("Cancel")
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
            title = { Text("Vault Setup Required") },
            text = {
                Text("Private Vault has not been set up yet. Please create your master password, PIN, or pattern first so your selected items can be securely encrypted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVaultSetupRequiredDialog = false
                        viewModel.currentScreen = Screen.VaultSetup
                    }
                ) {
                    Text("Set Up Vault")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showVaultSetupRequiredDialog = false
                        pendingMoveToVaultIds = emptySet()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSelectionRestoreConfirmDialog) {
        val count = selectionState.selectedIds.size
        AlertDialog(
            onDismissRequest = { showSelectionRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.RestoreFromTrash, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore items?") },
            text = { Text("Are you sure you want to restore $count ${if (count == 1) "item" else "items"} from Trash back to your gallery?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectionRestoreConfirmDialog = false
                        com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                        viewModel.restoreSelectedMedia(context)
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSelectionDeleteConfirmDialog) {
        val count = selectionState.selectedIds.size
        AlertDialog(
            onDismissRequest = { showSelectionDeleteConfirmDialog = false },
            title = { Text("Move to Trash?") },
            text = { Text("Are you sure you want to move $count ${if (count == 1) "item" else "items"} to Trash?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectionDeleteConfirmDialog = false
                        viewModel.deleteSelectedMedia(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSelectionDeletePermanentlyConfirmDialog) {
        val count = selectionState.selectedIds.size
        AlertDialog(
            onDismissRequest = { showSelectionDeletePermanentlyConfirmDialog = false },
            title = { Text("Delete Permanently?") },
            text = { Text("Are you sure you want to permanently delete $count ${if (count == 1) "item" else "items"}? This action is irreversible and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectionDeletePermanentlyConfirmDialog = false
                        viewModel.deleteSelectedMediaPermanently(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionDeletePermanentlyConfirmDialog = false }) {
                    Text("Cancel")
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
                    "Updated tags on ${selectedItems.size} items",
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

    if (showScrubCurrentAlbumDialog && viewModel.currentBucketId != null) {
        val currentBucketId = viewModel.currentBucketId!!
        val currentBucketName = viewModel.currentBucketName ?: "Album"
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
                            android.widget.Toast.makeText(context, "Sanitized $count photos in $currentBucketName", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            android.widget.Toast.makeText(context, "No photos required sanitization in $currentBucketName", android.widget.Toast.LENGTH_SHORT).show()
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
                text = "Privacy First Media Access",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To list your items local-first, the app queries your device's media storage database directly. No internet telemetry is requested.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRequestPermissions) {
                Text("Allow Local Access")
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
                Text("App Settings")
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
