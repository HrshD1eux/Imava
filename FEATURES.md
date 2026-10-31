# Imava — Complete Feature Inventory & Technical Matrix

> **Status:** Current Production Architecture (`v1.1.77`)  
> **Philosophy:** Offline-First, Zero Telemetry, Zero Proprietary Bloat, Hardware-Accelerated Performance, Forensics-Grade Privacy.

---

## 1. Core Architecture & High-Performance Engine

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Hardware OS Thumbnail Fetcher** | `MediaStoreThumbnailFetcher.kt` | Hooks directly into Android OS `ContentResolver.loadThumbnail()` (API 29+). Loads pre-rendered hardware thumbnails in **1–2 ms**, completely bypassing software CPU decoding of 50MP original files during grid flings. |
| **Zero-FUSE Ingestion** | `CursorExtensions.kt`, `MediaStoreDataSource.kt` | Ingests media attributes strictly via SQLite cursor columns (`DATE_MODIFIED`, `SIZE`, `DATE_TAKEN`). Completely eliminates blocking POSIX `stat()` calls across the Android user-space FUSE filesystem layer. |
| **Room Single-Source-of-Truth Cache** | `GalleryDatabase.kt`, `MetadataDao.kt` | Local Room database indexes custom user tags, favorite status, trashed timestamps, hidden flags, and OCR index. Synchronized in batched memory operations (`getAllMetadata()`). |
| **Paging 3 Virtualized Stream** | `MediaPagingSource.kt`, `MainViewModel.kt` | Dynamically streams thousands of items with `pageSize = 100` and `prefetchDistance = 120`. Session-caches total counts to prevent redundant `COUNT(*)` SQLite calls during 120Hz flings. |
| **Zero-Telemetry Sandbox** | `AndroidManifest.xml` | Completely stripped of Google Play Services, Firebase, crashlytics, trackers, advertising IDs, and external analytics. 100% network-isolated except for user-initiated WebDAV or P2P Wi-Fi transfers. |

---

## 2. Timeline & Media Grid Experience

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Dynamic Multi-Span Grid** | `LazyVerticalStaggeredGrid`, `TimelineScreen.kt` | Supports custom column densities from **1 to 6 columns** with smooth pinch-to-resize gesture transitions. |
| **Square vs Natural Aspect Ratio** | `GridStyle.SQUARE` vs `GridStyle.NATURAL` | Toggle between a uniform square grid and dynamic masonry/staggered layout preserving original image proportions. |
| **Sticky Date Headers** | `TimelineItem.Header`, `TimelineScreen.kt` | Groups photos chronologically (`Today`, `Yesterday`, `Mon, 19 Oct`, or full date for prior years) with full-line span separator injection. |
| **Date-Range Multi-Select** | `TimelineHeader.kt` | Tap any date header's "Select" / "Deselect" button to instantaneously toggle all photos captured on that day. |
| **Drag-to-Select Gesture** | `detectDragGestures`, `dragSelectionModifier` | Long-press and drag your finger across the grid to fluidly select dozens of media items in a single uninterrupted swipe. |
| **Top-Center Floating Date Badge** | `TimelineFloatingDateBadge.kt` | Zero right-edge obstruction. A sleek elevated capsule (`CircleShape`, Material 3) pops up at top-center during scroll to display active month/year (`October 2026`), smoothly fading out when idle. Zero gesture collisions with Android OS back swipe. |
| **Memories & Stories Carousel** | `MemoryStoryCalculator.kt`, `MemoriesStoriesHeader.kt` | Algorithmic retrospective stories: *"1 Year Ago Today"*, *"X Years Ago"*, and same-month memories. Includes an Instagram-style fullscreen Story Player with auto-advance and tap-to-pause. |
| **Quick Filter Chips** | `TimelineFilterChips.kt` | Instant horizontal chip row to filter timeline view: **All**, **Photos**, **Videos**, **Custom Hashtags (`#tag`)**, with a tag manager sheet. |

---

## 3. Albums & Folder Management

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Multi-Layout Album Switcher** | `AlbumLayoutMode`, `AlbumsScreen.kt` | View albums in 4 distinct layout modes: **List View**, **Large Grid (2 Cols)**, **Medium Grid (3 Cols)**, and **Small Grid (4 Cols)**. |
| **Multi-Criteria Album Sorting** | `AlbumSortOrder`, `AlbumsScreen.kt` | Sort folders by **Name (A to Z)**, **Item Count (Largest First)**, or **Recently Updated**. |
| **Pinned Albums** | `pinnedBucketIds`, `AlbumsScreen.kt` | Pin frequently accessed folders (e.g. Camera, Favorites, Work) to permanently stay at the top of the albums screen. |
| **Album Creation & File Relocation** | `MoveCopyAlbumDialog.kt` | Create brand new physical albums and move or copy single or batch media items across directories with automatic MediaScanner re-indexing. |
| **Excluded Folders Manager** | `_excludedBucketIds`, `SettingsScreen.kt` | Blacklist specific directories (e.g. cache folders, app assets, sticker packs) from appearing anywhere in the gallery. Restore them at any time. |
| **Album Auto-Scrubbing** | `AutoScrubManager.kt`, `AutoScrubWorker.kt` | Designate specific folders for continuous privacy sanitization. Strips all GPS coordinates, camera serial numbers, and EXIF metadata from incoming photos automatically. |
| **Individual Album Biometric Lock** | `AppLockManager.kt` | Lock sensitive albums behind biometrics or device PIN. Locked albums are completely removed from the timeline and require authentication to inspect. |
| **Secondary Storage Deep Scan** | `MediaStoreDataSource.kt` | Deep-scans device storage trees to unearth hidden third-party media (Telegram images, WhatsApp status caches, DCIM sub-folders) ignored by the standard MediaStore index. |

---

## 4. Fullscreen Photo & Video Viewer

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **120Hz Fullscreen Pager** | `HorizontalPager`, `PhotoViewerScreen.kt` | Hardware-accelerated image viewer with swipe navigation, high-resolution original rendering, and uncompressed zoom inspection. |
| **Sub-Pixel Pinch & Pan Engine** | `Zoomable.kt` | Smooth multi-touch zoom up to 8x magnification, double-tap zoom toggle, and bounded pan constraints. |
| **Guest Hand-Off (Pin Photo)** | `activity.startLockTask()`, `PhotoViewerScreen.kt` | Locks the phone screen to the current active photo. Friends or colleagues cannot swipe left/right, access other photos, exit to the gallery, or view other apps without biometric unlock. |
| **Forensic EXIF Info Sheet** | `InfoBottomSheet.kt` | Comprehensive metadata breakdown: Camera Manufacturer, Model, Aperture ($f$-stop), Exposure Time (shutter speed), ISO, Focal Length, File Path, File Size, Exact Resolution, and clickable GPS Map coordinates. |
| **On-Device Offline OCR** | `TesseractOcrEngine.kt`, `OcrScanBottomSheet.kt` | Extracts printed and handwritten text directly from photos completely offline without sending any data to the cloud. Allows 1-tap text copy and clipboard export. |
| **Lossless EXIF Rotation** | `LosslessRotationUtil.kt` | Updates image orientation tags clockwise or counterclockwise directly in JPEG headers without decompressing or re-encoding pixels (0% quality loss, 0ms latency). |
| **Target File-Size Compressor** | `ImageCompressor.kt`, `PhotoViewerScreen.kt` | Re-compresses photos to exact target sizes (e.g. <200 KB for government portals, or custom percentage) with resolution preview. |
| **Fullscreen Slideshow Player** | `SlideshowPlayerScreen.kt` | Plays hands-free animated slideshow of albums or selected photos with configurable slide durations. |
| **Wallpaper & Lockscreen Setter** | `WallpaperUtil.kt` | Sets photo as Home Screen, Lock Screen, or Both with aspect-ratio crop presets. |
| **Custom Album Covers** | `PhotoViewerScreen.kt` | Assign any photo as the persistent representative thumbnail for its parent album. |
| **Direct Print Service** | `PrintUtil.kt` | Connects directly to local Wi-Fi and Bluetooth printers via Android `PrintHelper` without third-party printer drivers. |
| **ExoPlayer Video Engine** | `VideoPlayer.kt` | Hardware-accelerated video player with audio track switching, loop playback toggle, timeline scrubber, and aspect ratio modes (**Fit**, **Fill**, **Zoom**). |
| **Lossless Video Muter** | `VideoMuterUtil.kt` | Strips audio channels losslessly using `MediaExtractor` + `MediaMuxer` without re-encoding video frames, outputting a silent copy in seconds. |
| **Hardware Audio Extractor** | `AudioExtractor.kt` | Demuxes audio streams from videos and saves them as standalone `.m4a` music files in `Music/Imava_Audio`. |
| **Lossless Video Trimmer & GIF Maker** | `VideoTrimDialog.kt`, `GifEncoder.kt` | Frame-accurate video trimming with start/end slider controls, and 1-tap conversion of video clips into animated GIFs. |
| **In-Place Motion Photo Player** | `PhotoViewerScreen.kt` | Automatically identifies embedded Google/Samsung micro-videos in JPEGs. Renders an interactive floating `[ Motion 🎞️ ]` pill; tapping it instantly transitions the photo into a looping video playback (`Playing ⏸️`) directly inside the viewer. |
| **Motion Photo Toolset (Scrub/MP4/GIF)** | `MotionPhotoUtil.kt`, `MotionPhotoExportDialog.kt` | Frame-by-frame scrub slider over the embedded micro-video, with 1-tap exports as a standalone `.mp4` video or animated `.gif`. |

---

## 5. Selection & Batch Operations

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Balanced Action Bottom Bar** | `MainActivity.kt` | Balanced 4-pillar selection bar: **Share**, **Edit**, **Delete**, and **More** (with quick Wi-Fi Drop action). |
| **Instant Optimistic Deletion** | `MainViewModel.kt` | Removes selected items from UI state in **0 ms** with haptic feedback, while resolving Room metadata and MediaStore file trash/deletion in background coroutines. |
| **Bulk Image Compressor** | `BulkCompressDialog.kt` | Batch compresses dozens of selected photos to target file size thresholds simultaneously. |
| **Bulk Tagging System** | `BulkTagDialog.kt` | Adds or removes custom organizational tags (`#family`, `#receipt`, `#vacation`) across multiple selected items at once. |
| **Batch Move & Copy** | `MoveCopyAlbumDialog.kt` | Batch relocates or duplicates selected items across device directories. |
| **EXIF Timestamp Shifter** | `ExifTimeShiftDialog.kt` | Shifts capture dates forward or backward by custom hours, days, or months across selected items (corrects camera clock misalignments). |
| **Single & Batch Pattern Renamer** | `BatchRenameDialog.kt`, `SingleRenameDialog.kt` | Renames multiple files simultaneously using automated numbering sequences (e.g. `Vacation_01`, `Vacation_02`). |
| **Multi-Image PDF Converter** | `PdfConverter.kt` | Bundles 1 to 50+ selected photos into a single multi-page PDF document using native Android graphics (0 external libraries). |
| **Clean Copy Exporter** | `MainViewModel.exportCleanCopies()` | Generates copies of selected photos with all EXIF markers and GPS metadata stripped clean for safe sharing. |
| **Side-by-Side Photo Comparison** | `PhotoCompareScreen.kt` | Dual-view split comparison slider with synchronized pinch-to-zoom for comparing focus, sharpness, and color grading between two photos. |
| **Native Canvas Collage Maker** | `CollageMakerScreen.kt`, `CollageMakerUtil.kt` | Builds creative collages from 2 to 9 photos with customizable grid layouts, border widths, colors, and shape masks (Rectangle, Rounded, Circle, Heart, Star). |

---

## 6. Privacy, Security & Hidden Vault

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Hardware AES-256 GCM Vault** | `VaultCrypto.kt`, `VaultSetupScreen.kt` | Private storage backed by Android Keystore. Encrypts files with AES/GCM/NoPadding (256-bit key) and PBKDF2WithHmacSHA256 (100,000 iterations) salt hashing. Encrypted files cannot be read by other apps or file managers. |
| **Plausible Deniability Decoy PIN** | `VaultStateHolder.kt`, `VaultUnlockDialog.kt` | Configure a secondary Decoy PIN. Entering the decoy PIN under duress unlocks an empty, harmless decoy vault while keeping your real private files hidden and protected. |
| **Biometric Authentication** | `BiometricAuthHelper.kt` | Supports Fingerprint, Face Unlock, and Class 3 Biometrics with automatic fallback to device PIN/Pattern. |
| **Biometric Trash Protection** | `AppLockManager.isTrashLocked()` | Hides deleted media behind biometric authentication. Deleted photos cannot be previewed or restored without identity verification. |
| **Forensic File Shredder** | `FileShredderUtil.kt` | CSPRNG 3-pass file shredder (DoD 5220.22-M / NIST 800-88 compliant): overwrites byte sectors with `0x00`, `0xFF`, and random cryptographic noise before unlinking. Prevents data recovery from flash memory. |
| **Recents Screenshot Shield** | `FLAG_SECURE`, `MainActivity.kt` | Blurs or blanks the app in the Android Recent Apps switcher and blocks unauthorized OS screenshots while the gallery or vault is open. |
| **Multi-Tier Privacy Sharing** | `SharePrivacyMode`, `MainActivity.kt` | Choose privacy level whenever sharing: **Strip All Metadata** (No GPS/Camera info), **Fuzz Location (~2 km radius)** (keeps city/region, hides street/home address), or **Full Metadata**. |
| **Root & Jailbreak Detection** | `RootDetectionUtil.kt` | Offline heuristic check detecting root binaries (`su`), test-keys, and Magisk paths, warning users of potential OS security compromise. |

---

## 7. P2P Zero-Cloud Sharing ("Imava Drop")

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Direct Wi-Fi HTTP Server** | `ImavaDropServer.kt` | Embedded lightweight HTTP server. Shares uncompressed photos/videos directly to any PC, Mac, iPhone, or Android device on the local network at 50–100 MB/s without cloud uploads. |
| **Pure Kotlin QR Code Generator** | `PureQrCodeEncoder.kt` | Generates standard QR codes natively using pure Kotlin Reed-Solomon math (zero ZXing or external dependencies). |
| **Glassmorphism Web Client** | `ImavaDropServer.serveHtmlClient()` | Serves a responsive web application to connecting browsers, complete with album thumbnails and live activity logs. |
| **Fullscreen In-Browser Lightbox** | Embedded Web Script | Connecting browsers can preview full-resolution images and videos directly in a modal lightbox before downloading. |
| **Bidirectional File Drag & Drop** | HTTP Multipart Upload Handler | Drag and drop photos or documents in the PC/Mac browser to upload them directly back into the phone's gallery. |
| **Batch ZIP Archive Download** | `serveZipArchive()` | Compresses all shared media on-the-fly into a single `.zip` file for 1-click download on PC. |
| **Background Service & Wi-Fi Lock** | `ImavaDropService.kt`, `WifiManager.WifiLock` | Runs as a foreground service with high-performance Wi-Fi lock. Minimizing the app or turning off the screen does **not** terminate transfers. |

---

## 8. Storage Doctor & Deduplication

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Storage Health Audit** | `StorageDoctorScreen.kt`, `StorageDoctorUtils.kt` | Analyzes device storage and calculates total potential reclaimable disk space across multiple cleanup categories. |
| **Large Video Hunter** | `StorageReport.largeVideos` | Identifies and sorts videos larger than 50 MB to quickly free up gigabytes of storage. |
| **Stale Screenshot Cleaner** | `StorageReport.staleScreenshots` | Discovers screenshots older than 30 days that are cluttering device memory. |
| **Rapid Burst Take Grouping** | `StorageReport.burstGroups` | Groups photos taken within 2.5 seconds of each other in the same bucket, allowing 1-tap pruning of duplicate takes. |
| **Exact Byte-for-Byte Duplicate Finder** | `DuplicateFinder.kt` | Scans files matching exact size, file length, and content hashes to eliminate identical duplicate files. |
| **Perceptual Image Hash (Visual Duplicates)**| `DuplicateFinder.computeDHash()`, `computeAHash()` | Computes 64-bit difference hashes (`dHash`) and average hashes (`aHash`) to catch visually identical photos even if re-compressed, re-saved, or slightly resized (Hamming distance $\le 4$). |

---

## 9. Search & Discovery Engine

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Multi-Index Instant Search** | `SearchScreen.kt` | Search across filenames, album names, custom tags, and OCR-extracted text with instantaneous filtering. |
| **Hardware Attribute Filter Dialog** | `AdvancedFilterDialog.kt` | Filter gallery by hardware parameters: **Media Type** (Photos, Videos, GIFs, RAW, Panoramas, Motion Photos), **File Size** (>100MB, >500MB, >1GB, <1MB), **Resolution** (4K UHD, 1080p FHD, Panorama, Portrait, Landscape, Square), **Date Presets** (Past 7/30 days, year), and **Camera Model** (e.g. Sony, Pixel, Galaxy). |

---

## 10. Built-in Photo Editor

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **Non-Destructive Transform Pipeline** | `PhotoEditorScreen.kt`, `PhotoEditPipeline.kt` | Full editing suite operating on high-resolution sub-sampled memory bitmaps with non-destructive state tracking. |
| **Precision Rotation & Flipping** | `EditorTab.ROTATE`, `EditorTab.FLIP` | 90° clockwise/counterclockwise stepping, arbitrary angle rotation slider, and horizontal/vertical mirroring. |
| **Aspect Ratio & Free Crop** | `EditorTab.CROP`, `CropGridOverlay` | Interactive 8-handle touch crop box with rule-of-thirds grid. Presets: **Freeform**, **Original**, **1:1 Square**, **4:3**, **16:9**, **3:2**. |
| **Color & Light Tuning** | `EditorTab.TUNING` | Real-time adjustment sliders for **Brightness**, **Contrast**, **Saturation**, and **Warmth** (color temperature). |
| **Creative Color Presets** | `PhotoEditPipeline.kt` | Instant tone grading presets: **Vivid**, **Dramatic**, **Warm**, **Cool**, **Vintage**, and **Monochrome (B&W)**. |
| **Privacy Redaction: Pixelate Mosaic** | `PhotoMarkupUtils.MarkupTool.PIXELATE_MOSAIC` | Redaction brush that renders a pixelated mosaic over sensitive regions (credit card numbers, faces, license plates, home addresses). |
| **Solid Redaction Mask** | `PhotoMarkupUtils.MarkupTool.SOLID_MASK` | Blackout marker for completely obscuring confidential text. |
| **Markup & Annotation Tools** | `PhotoMarkupUtils.MarkupTool` | Freehand drawing pen, directional arrow tool, and rectangle bounding-box highlighter with customizable stroke widths and color picker. Includes undo/redo history. |

---

## 11. Sync, Backup & Integrity

| Feature | Technical Implementation | Practical Capability |
| :--- | :--- | :--- |
| **WebDAV / Local NAS Auto-Backup** | `WebDavSyncManager.kt`, `WebDavClient.kt` | Private synchronization with Synology NAS, TrueNAS, Nextcloud, QNAP, or personal WebDAV instances over local Wi-Fi. Includes periodic background WorkManager backups. |
| **Direct GitHub Release Updater** | `AppUpdateManager.kt`, `AppUpdateCheckWorker.kt` | Checks the official GitHub repository releases API for version updates and allows direct APK downloads without relying on the Google Play Store. |
| **Permissions Transparency & Licenses** | `PermissionsTransparencyScreen.kt`, `OpenSourceLicensesScreen.kt` | Explicit breakdown of every Android permission requested, why it is needed, and full open-source license attribution. |

---

## 12. Micro-Features with Massive Impact ("The Secret Weapons")

Subtle, high-leverage features built natively into Imava that solve daily mobile headaches:

1. **In-Viewer Motion Photo Live Playback**: Automatically identifies Google & Samsung embedded micro-videos in JPEGs. Tapping the `[ Motion 🎞️ ]` badge transforms the still photo into a live looping video directly inside the viewer.
2. **Guest Hand-Off (Screen Pinning)**: Uses Android's native `startLockTask()` to pin the screen to the currently viewed photo. When handing your phone to a friend or coworker, they cannot swipe left or right to snoop through other photos, nor can they exit to your home screen without biometric unlock.
3. **Lossless Video Muter (Instant Container Remux)**: Strips audio tracks directly at the MP4 container level using `MediaExtractor` + `MediaMuxer` in <1 second with 0% video re-compression loss.
4. **Hardware Audio Extractor**: Extracts audio tracks from video files and saves them as standalone `.m4a` music files in `Music/Imava_Audio`.
5. **Lossless EXIF Rotation**: Rotates photos 90° clockwise in <5 milliseconds by rewriting the 2-byte EXIF orientation flag without decoding or re-compressing pixels (zero quality degradation).
6. **Location Fuzzing (~2 km Radius) on Share**: Don't want to share your exact home coordinates? The sharing privacy dialog can apply a ~2 km Gaussian offset to GPS coordinates so recipients see the general city or district while keeping your exact house address secret.
7. **Perceptual Image Hash Deduplication (`dHash` + `aHash`)**: 64-bit difference hashing with Hamming distance matching catches visually identical photos even if they were re-saved, resized on WhatsApp/Telegram, or re-compressed.
8. **CSPRNG 3-Pass Forensic File Shredder**: Overwrites storage sectors with `0x00`, `0xFF`, and random cryptographic noise before unlinking, defeating flash memory forensic recovery.
9. **Dual Zoom-Synced Photo Compare Slider**: Split-screen comparison slider where zooming and panning one photo synchronously moves the other photo for pixel-peeping sharpness comparison.
10. **Native Multi-Image PDF Converter**: Converts 1 to 50+ photos into a single PDF document in seconds with zero third-party PDF SDKs.
11. **Recents Screenshot Shield (`FLAG_SECURE`)**: Blurs app previews in the Android app switcher and blocks unauthorized screenshots while viewing private media.
12. **Plausible Deniability Decoy PIN**: Secondary PIN unlocks a harmless, empty decoy vault under duress.
13. **Pure Kotlin QR Code Generator**: Reed-Solomon QR encoder running in pure Kotlin without ZXing or external dependencies.
14. **Auto-Purge Expired Trash Worker**: Background WorkManager cleans up trashed files older than 30 days automatically.
15. **Database Integrity & Orphan Cleaner Worker**: Cleans up Room database records for files deleted externally by third-party file managers.
