## 🚀 What's New in Imava v1.1.129

* **Automated Release Notes Hub & Rule Enforcement**: Strictly enforced development policy and pre-commit audit ensuring release notes and What's New dialogs are permanently synchronized with active versions across all 13 supported languages.
* **Pre-Commit Integrity Shield**: Integrated automated git hook blocking any commits if release documentation, version branches, or translations are missing or incomplete.
* **Full 13-Language What's New Parity**: Comprehensive multilingual localization across all release highlights for modern updates (v1.1.124 through v1.1.129).
* **Automated Version Regression Unit Tests**: Hardened unit test suite guaranteeing that active versions have distinct, verified release items with valid Android resource identifiers.

---

## 🚀 What's New in Imava v1.1.128

* **Persistent Deep Media Scan Cache**: Scanned media items are now persisted in local cache and instantly exposed upon cold boot, completely eliminating disappearing items, blank states, and redundant re-scans.
* **Thermal & Battery Protection Shield**: Eliminated continuous 45-second background scan loops and heavy `onResume()` filesystem traversals, safeguarding low-end devices from thermal throttling, battery drain, and UI stutter.
* **B-Tree Single-Column Indexed Queries**: Optimized MediaStore cursor sorting by replacing compound multi-column ordering with single-column B-Tree indexed queries, slashing cursor latency.
* **Zero-Latency Albums Navigation**: Streamlined album grouping directly from cached items without blocking synchronous file stats or recursive directory crawls when switching tabs.

---

## 🚀 What's New in Imava v1.1.127

* **Cold Boot Reload Loop Elimination**: Removed artificial 800ms delays and serial count queries, ensuring the media timeline appears instantly on cold startup.
* **Indexed SQL Query Engine**: Stripped unindexed 30-LIKE wildcard queries that bypassed SQLite B-tree indexes and caused CPU spikes during library scan.
* **In-Memory Tag & Favorite Cache**: In-memory caching for tags and favorites eliminates per-item database lookups during timeline render.
* **Single-Pass Album Grouping**: Optimized album aggregation algorithm from $O(N \times M)$ down to $O(N)$, resolving UI stalls when opening Albums.

---

## 🚀 What's New in Imava v1.1.125

* **Public Storage Auto-Backup**: Silently and securely backs up tags and favorites to public Documents storage (`/sdcard/Documents/Imava/imava_metadata_backup.json`), surviving app uninstalls and reinstalls.
* **Reinstall Metadata Restore**: Automatically detects existing tag and favorite backups on cold reinstall and offers a one-tap restore prompt, with user toggle in Settings.
* **Zero-Copy Video Sharing Pipeline**: Direct streaming content provider eliminates out-of-memory crashes and disk exhaustion when sharing large 4K videos.
* **Sub-40ms Instant Timeline**: Snapshot memory cache brings first-frame photo rendering down from 2,500ms to under 40ms.

---

## 🚀 What's New in Imava v1.1.123

* **Adaptive Single-Line Version Architecture**: Responsive layout ensuring version badges, updater controls, download progress, and dialog buttons stay on a single line across all screen widths and font scales.
* **Dynamic 3-Version In-App Release Notes**: Release showcase in Settings and About & Legal with version history chips scoped to current and recent releases.
* **Zero-Latency App Updater**: Instant UI download transition with direct GitHub package size extraction and browser download fallback.
* **High-Tactile Permission Controls**: Accessible 50dp high segmented toggle with haptic feedback, eliminating missed touches across diverse OEM screens.

---

## 🚀 What's New in Imava v1.1.120

* **Hardware Keystore WebDAV Credential Encryption**: WebDAV server passwords and authentication secrets are now encrypted via hardware-backed AndroidKeyStore AES-256-GCM (`VaultCrypto`) prior to disk persistence, with seamless background migration from legacy configurations.
* **Locked Album Cloud Sync & Backup Isolation**: Enforced strict pre-flight privacy checks across both `WebDavSyncManager` and `WebDavBackupWorker`. Photos and videos located in biometric-locked or PIN-locked albums are permanently excluded from cloud synchronization and auto-backups.
* **Vault Biometric Isolation**: Enforced strict biometric requirements (`allowDeviceCredential = false`) for hidden vault access, preventing device lockscreen PINs or patterns from bypassing vault security.
* **In-Memory LAN Brute-Force Rate Limiting (Imava Drop)**: Protected local Wi-Fi sharing against unauthorized pairing attempts using an in-memory sliding-window firewall. Receiving 5 failed PIN attempts locks the offending client IP for 60 seconds with HTTP 429 and artificial response throttling.
* **Native Scoped Storage MediaStore Trash (API 30+)**: Eliminated redundant disk copies into private app caches on Android 11+. Directly updates `MediaStore.MediaColumns.IS_TRASHED` or requests `MediaStore.createTrashRequest()`, preserving native filesystem recycling and scoped storage compliance.
* **Forensic File Shredder Integrity**: Removed deceptive un-shredded file deletion fallbacks. If DoD 5220.22-M sector zeroing fails, the shredder fails safely and honestly rather than reporting a false-positive secure wipe.
* **Paginated Cloud Backup Engine**: `WebDavBackupWorker` now processes the entire media library using paginated chunked queries (`pageSize = 500`), backing up large libraries without memory pressure or truncated scans.
* **Room-Backed Synced Media Ledger**: Upgraded `GalleryDatabase` to Version 9 with `MIGRATION_8_9`. Synced media records are now maintained in a dedicated, indexed SQLite entity (`webdav_synced_media`), replacing unindexed XML SharedPreferences.
* **Stealth Code Search Verification & Draft Protection**: Fixed case-sensitivity bug in stealth code search activation (`#Harsh` vs `#harsh`). Added an inline "Save Code" button with haptic feedback, plus an "Unsaved Changes" confirmation dialog on back press or dismiss.
* **Neutral Decoy Empty State**: Replaced "No decoy items found" with neutral "No items found" across all 13 supported languages, preventing forensic discovery of decoy vault mode.
* **Directional Peak-Reversal Emergency Shake-to-Lock**: Replaced single-sample accelerometer triggers with a calibrated directional peak-reversal detector ($\ge 3.2\text{g}$, $\ge 2$ reversals within 500ms, 2s debounce) to ensure instant emergency locking without accidental pocket triggers.
* **Multi-Core Coroutine Duplicate Detection**: Parallelized `DuplicateFinder` across available CPU cores on `Dispatchers.Default` using 16-bit hash sub-chunk candidate multi-indexing, pruning comparison time on large libraries.
* **High-Precision Photo Viewer Deep Zoom**: Configured Coil viewer requests with `Precision.EXACT` and hoisted GPU texture dimensions up to 8192px, eliminating downsampling blur when inspecting 20MP+ photos.
* **Reactive Library Search Flow**: Search queries now dynamically combine with the active media list, instantly reflecting changes when photos are deleted, trashed, or added.

---

## 🚀 What's New in Imava v1.1.112

* **Dynamic Decoy Credential Parity (Pattern, PIN, Password)**: Decoy duress authentication now dynamically mirrors your master credential lock type. If your master lock is a 3x3 Pattern, your decoy is a Decoy Pattern drawn on an identical grid. If master is a PIN or Password, decoy setup and verification prompts for matching PIN/Password credentials.
* **Isolated Cryptographic Decoy Salt**: Generated and persisted an independent cryptographic salt (`vault_decoy_salt`). Master password changes, re-salting, or migrations will never inadvertently invalidate or break decoy vault authentication.
* **Master Collision & Type Migration Guards**: Built-in safeguards prevent configuring identical master and decoy credentials, and automatically reset obsolete decoy credentials with user notification if master lock type changes (e.g. PIN to Pattern).
* **Synchronous Auto-Fit Typography Engine (`AutoFitText`)**: High-performance, zero-flicker text scaling engine leveraging Jetpack Compose `rememberTextMeasurer`. Dynamically scales typography down to device-specific minimum limits (8sp–10sp) before truncating, preventing button label ellipses across compact screens (320dp/360dp) and all 13 supported languages (including long German/French words and Indic scripts).
* **Full 13-Language Parity**: All 26 new decoy pattern, collision alert, and reset strings fully localized across English, Spanish, Portuguese, German, Russian, French, Hindi, Bengali, Marathi, Telugu, Tamil, and Gujarati.

---

## 🚀 What's New in Imava v1.1.42

* **Cryptographic File Shredder (Anti-Recovery)**: Permanently deleting photos or emptying trash now physically overwrites storage blocks with multi-pass CSPRNG random bytes and zeroes, forces hardware write cache sync, truncates to 0 bytes, and scrambles directory entry filenames so data recovery software (DiskDigger, Recuva, PhotoRec) cannot recover deleted files.
* **Security & Privacy Update Warning**: Added an informative, one-time update notice explaining the difference between standard label unlinking and physical shredding, with switchable non-technical and technical deep-dive views and a mandatory 6-second reading timer.
* **Advanced Instant Deep Media Scan**: Expanded deep scan with instant multi-pattern SQLite queries (<100ms) and comprehensive directory discovery covering messaging app media, dual app spaces, documents, audio, and hidden dot-vaults (`.hidden`, `.secret`, `.vault`, `.calculator`, `.thumbnails`).
* **Settings Control**: Easily toggle Cryptographic File Shredding on or off under *Settings > Privacy & Folders*.

---

## 🚀 What's New in Imava v1.1.41

* **Album & Folder EXIF Scrubbing**: Added in-place album-level metadata cleaning with dedicated safety guards to preserve photo capture date and orientation while wiping GPS coordinates, device serials, and camera fingerprints.
* **Automatic Background Folder Scrubbing**: Designate specific albums/folders (e.g. Camera, Downloads) to automatically strip sensitive metadata in the background upon app launch and deep scan completion with a 3.5s camera write-lock debounce.
* **Safe vs Nuclear Scrub Modes**: Default safe scrub protects timeline chronology (`DateTimeOriginal`) and keeps photos upright (`Orientation`); optional full wipe removes all timestamps.
* **Album Actions & Settings Management**: Manage auto-scrub albums via album long-press options, within album header menu, or in Settings under *Privacy & Folders*.

---

## 🚀 What's New in Imava v1.1.40

* **Top Bar Selection Header**: Moved the selected count (`"1 selected"`, `"2 selected"`) to the top bar alongside a dedicated Close button to exit selection mode and a Select All / Deselect All action.
* **Fixed 4-Action Bottom Menu**: Replaced the horizontally scrolling bottom menu with a clean, fixed bar featuring **Share**, **Edit**, **Delete**, and **More** (or **Restore** & **Delete Forever** in Trash).
* **Direct Photo Editor Launch**: Tapping **Edit** with a photo selected directly opens Imava's built-in photo editor.
* **Streamlined "More" Options Sheet**: Tapping **More** opens a sleek bottom sheet containing all extended gallery actions (Favorite, Move/Copy, Vault/Unhide, Rename, PDF Export, Clean Copy, Lossless Rotate, Collage, Compare, Slideshow, Print, Shift Time).

---

## 🚀 What's New in Imava v1.1.39

* **Automatic Deep Scan Mode**: Converted Deep Scan into a background auto-scanner that runs on app launch, app resume, and media changes with a dedicated Settings toggle and live discovery badge.
* **Discovered Album Fix**: Resolved an issue where tapping deep-scanned albums (e.g. messaging sent folders) loaded zero photos; album views now reliably display all discovered media.
* **Timeline Deduplication & Filter Support**: Deep-scanned media is now strictly deduplicated by file path and ID, respects Photos/Videos filter chips, and properly excludes Vault/Trash items.
* **Expanded Scan Coverage**: Deep scan now traverses media roots across messaging apps, dual spaces, documents, and sent folders up to 4 directory levels.

---

## 🚀 What's New in Imava v1.1.38

* **Genuine Startup Screen Navigation**: Fixed startup screen initialization so Imava reliably opens to your selected tab (Timeline, Albums, or Last Active Screen) without race-condition resets.
* **Leak-Proof EXIF Metadata Stripping**: Extended the privacy cleaner across all EXIF timestamps, GPS coordinates, camera serial numbers, Adobe/Google XMP blocks, and MakerNotes.
* **Shared Media Privacy**: Private sharing now leverages the unified, exhaustive metadata sanitizer before handing files to third-party apps.

---

## 🚀 What's New in Imava v1.1.35

* **1-Tap Orientation Override**: Toggle viewer between landscape and portrait instantly with auto-reset on exit.
* **Lossless EXIF Rotation**: Rotate JPEGs 90° clockwise or counter-clockwise in ~15ms without recompressing bitmap quality.
* **Video Repeat & Frame Stepper**: Toggle single-video repeat loops and step frame-by-frame (±33ms) when paused.
* **Viewer Brightness Boost**: Optional setting to automatically maximize screen brightness when opening media.
* **Timeline Filter Chips**: Quick-filter timeline by All, Photos, Videos, Screenshots, Favorites, GIFs, or RAW.
* **Recycle Bin Auto-Purge Setting**: Configure automatic trash retention (7, 14, 30, 60 days, or Never).
* **Default Startup Screen**: Choose whether Imava launches to Photos, Albums, or remembers your last active tab.
* **Export Clean Copy**: Strip all GPS coordinates, camera serials, and EXIF metadata to `Pictures/Cleaned`.
* **Dominant Color Palette**: View dominant photo swatches in Info sheet; tap to copy HEX codes with haptics.
* **Emergency Shake-to-Lock**: Instantly lock and close the Hidden Vault if your device is shaken.

---

## 🚀 What's New in Imava v1.1.34

* **Delete Confirmations**: Added explicit confirmation dialogs with item counts before moving selected items to Trash or deleting permanently from Trash.
* **Trash Dated / Flat View Toggle**: Fixed the date grouping toggle in Trash, accurately grouping deleted items by deletion date.
* **Viewer Swipe Glitch Fix**: Fixed photo viewer swiping stutter by guarding page transitions, resetting zoom on page settlement, and trimming memory bounds.
* **Performance Boost & Bloat Removal**: Switched from heavy ImageDecoder to native BitmapFactory, stabilized grid keys to prevent recompositions, and stopped idle tab retention in RAM.

---

## 🚀 What's New in Imava v1.1.32

### 🔍 Messaging Sent, Third-Party & Hidden Media Deep Discovery
* **Generic Files Content URI (`content://media/external/file/{id}`)**: Fixed a critical MediaStore URI mismatch where media in `.nomedia` directories (indexed with `media_type = 0`) was assigned invalid `images/media/` or `videos/media/` URIs that failed to load in Coil. `MediaStore.Files` URI resolves any media type seamlessly.
* **Hybrid Discovery Engine**: Third-party sent folders, private directories, animated GIFs, and `.nomedia` media are now retrieved both via direct MediaStore database path queries (`DATA LIKE`) and direct filesystem scanning fallback with `Uri.fromFile()`.
* **Accurate Media Classification**: Videos indexed with `media_type = 0` are now accurately identified as `MediaItem.Video` using file extension inspection and MIME type resolution instead of erroneously falling back to `image/jpeg`.
* **Automatic Album Grouping**: Folders containing `.nomedia` files (e.g. `Sent`, `Private`, `Telegram Images`) are automatically registered as distinct albums in the Albums tab, dynamically deriving names and bucket IDs from directory structures.
* **All Files Access Setting**: Added a dedicated "Deep Media Scan & Storage" card in Settings with manual "Deep Scan Now" trigger and "Grant Access" shortcut on Android 11+ (`MANAGE_EXTERNAL_STORAGE`).
* **Auto-Refreshed UI**: Deep scan automatically notifies the gallery and refreshes all views upon background completion.

---

## 🚀 What's New in Imava v1.1.30

### 📅 Strictly Monotonic Adaptive Timeline Scrubber
* **Strictly Ordered Dates**: Computed directly from sorted gallery items, eliminating SQLite null-date scrambles. Scrubber labels progress strictly chronologically (Today ➔ Yesterday ➔ Month ➔ Year) without out-of-order date jumps.
* **Auto-Scaling Intervals**:
  * **Year Mode** (`> 2 years`): Renders concise 2-digit years (`'24`, `'23`, `'22`) for expansive libraries.
  * **Month Mode** (`> 60 days`): Displays uppercase month abbreviations (`AUG`, `JUL`, or `AUG '23` across multi-year intervals).
  * **Day Mode** (`<= 60 days`): Shows calendar markers (`Today`, `Yesterday`, `d MMM`) for granular navigation.
* **Refined Aesthetics**: Widened scrubber touch target (44dp) with semi-bold typography for effortless, pinpoint scrubbing across thousands of photos.

---

### ➡️ Smooth Auto-Advance on Confirmed Delete
* **Zero Interruption Pagination**: Deleting or trashing a photo/video in full-screen preview smoothly advances to the next image to the right (or the previous one if deleting the last item).
* **Confirmed-Only Advance**: Preview stays on the current photo while system or confirmation dialogs are open. Auto-advance strictly triggers when the action is confirmed—never prematurely on button tap.
* **Instant Transitions**: Deleted items vanish from the pager immediately without flicker or navigation desync.

---

### 🛡️ Crash Elimination: Trash, Album Deletion & Scrolling
* **Guaranteed Unique Compose Keys**: Resolved fatal `IllegalArgumentException` crashes in `LazyVerticalStaggeredGrid` by generating globally unique composite keys for all headers, items, and placeholders.
* **Trash Key Collision Fix**: Eliminated duplicate capture date headers in Trash (which is sorted by deletion time).
* **MediaStore & DB Trashed Deduplication**: Enforced strict ID deduplication across MediaStore and Room database trashed queries, preventing duplicate item crashes when opening Trash.
* **Safe Scoped Storage Album Deletion**: Deleting an album no longer triggers synchronous recursive file wipes while MediaStore trash requests are pending. Reset active album if deleted.

---

### 🔍 Third-Party Sent & Hidden Media Deep Scan
* **Zero-Type MediaStore Fix**: Added full file extension pattern matching (`DISPLAY_NAME LIKE '%.jpg' ...`) so media in `.nomedia` directories (sent folders, private directories) with `MEDIA_TYPE == 0` is recognized and displayed.
* **Explicit MIME Type Registration**: Media scanning now resolves and registers explicit MIME types (`image/jpeg`, `video/mp4`) during scans, preventing `.nomedia` media from being marked as unreadable binary octet-streams.
* **Deep Directory Paths**: Explicitly scans third-party sent folders, private directories, animated GIFs, and standard media paths up to depth 6.

---

### ✏️ Dedicated Single-Item Rename Dialog
* **Solo vs Batch Distinction**: Selecting exactly 1 media item and tapping "Rename" now opens a clean, focused `SingleRenameDialog` instead of the multi-file batch template dialog.
* **Smart File Protection**: Pre-fills current filename, preserves file extensions (`.jpg`, `.mp4`, `.png`) automatically, filters invalid filesystem characters (`/ \ : * ? " < > |`), and shows live dimensions and file size.

---

### 🗑️ Recency-First Trash Sorting
* **True Deletion Order**: Items in Trash are now sorted strictly by deletion timestamp (`trashTime DESC`). Photos deleted seconds ago appear immediately at the very top of Trash, regardless of when the picture was originally captured.

---

### 🛡️ Android 10 Deletion & Trash Isolation
* **Zero Ghost Files**: Fixed scoped storage deletion glitches on Android 10 (API 29). Trashed media is isolated into internal private app storage while purging MediaStore rows, preventing deleted items from lingering or reappearing in third-party gallery apps.
* **Direct File Erasure**: Permanent deletions execute physical filesystem unlinking and trigger immediate `MediaScannerConnection` broadcasts.

---

### 📁 Delete Album Dual Choice ("Album Only" vs "Album & Media")
* **Granular Folder Management**: Long-pressing or opening options on custom albums now includes "Delete Album 🗑️".
* **Dual Choice Prompt**:
  * **Delete Album Only**: Safely preserves all photos and videos by moving them to the parent directory and removes the empty folder.
  * **Delete Album & Media**: Trashes all media inside the album and cleans up the folder.
* **Root System Folder Guard**: Core system folders (`Camera`, `DCIM`, `Pictures`, `Download`, `Downloads`, `Movies`, `Screenshots`) are strictly protected against accidental deletion.

---

### 🏷️ Album Name & Path in Info Sheet
* **Direct Album Attribution**: The info bottom sheet now features an Album row with a bookmark icon showing the album name (`bucketName`) and full directory path.

---

### 🚚 Lossless Move Destination & Row Deduplication
* **Accurate Target Buckets**: Moving items to another album now targets the exact physical directory of the destination album rather than defaulting to `Pictures/`.
* **Zero Phantom Rows**: Moved source items are hidden in Room metadata immediately, eliminating ghost duplicate rows before MediaStore completes its background rescan.

---

### 🎬 Video Player & ExoPlayer Enhancements
* **Instant Video Replay**: Fixed an ExoPlayer freeze where tapping Play at `Player.STATE_ENDED` failed to seek back to 0. Both the centered 72dp button and transport bar replay immediately.
* **Audio Focus Integration**: Configured `AudioAttributes` with `handleAudioFocus = true` to properly duck or pause background music (Spotify, podcasts) and handle phone calls during playback.
* **Decoupled Tap Gestures**: Tapping the video surface strictly toggles chrome bars; playback is controlled via dedicated buttons and double-tap gestures.
* **Lifecycle Audio Pause**: Automatically pauses video playback on `ON_PAUSE` when switching apps or locking the device.
* **Screen Brightness Restoration**: Restores system default brightness upon exiting the video player instead of keeping the entire gallery at manual brightness.

---

### 📦 Downloads & Verification
* Download the signed APK below.
* Zero cloud dependencies, fully offline, zero analytics.

---

## 🚀 What's New in Imava v1.1.29

### ⚡ Butter-Smooth 120Hz Swiping & Video Player Controls
* **Zero-Stutter Photo Swiping**: Overhauled horizontal gesture physics in the photo viewer. Completely removed conflicting swipe-down gesture interceptors and fling-killing snapping for natural, fluid 120Hz pagination.
* **Instant Transitions**: Eliminated Coil crossfade flicker mid-swipe so adjacent photos render immediately from cache with zero visual stutter.
* **Smooth Double-Tap Zoom**: Replaced 1-frame scale jumps with a smooth 220ms animated zoom transition targeting tap coordinates.
* **No Video Autoplay**: Videos now open paused at frame 0 with clean thumbnails and zero unwanted audio playback.
* **Unimpeded Video Swiping**: Replaced generic drag listeners with vertical-only gesture detection (for brightness and volume). Swiping horizontally over videos now moves seamlessly to adjacent photos or videos.
* **Dedicated Play Controls & Auto-Pause**: Added a prominent 72dp centered play button, transport bar toggle, and automatic playback pause as soon as you swipe away.

---

### 🔒 Absolute Privacy for Biometric-Locked Albums
* **Photos Tab & Feed Isolation**: Media from biometric-locked albums is now completely hidden from the main Photos timeline, Videos tab, and general gallery searches. Even if an album was opened in a session, its contents never leak into general feeds.
* **System-Wide `.nomedia` Protection**: Locking an album automatically creates a `.nomedia` file inside its physical folder and triggers a media rescan. This hides locked photos from the Android system MediaStore and all other apps on your phone. Unlocking safely removes `.nomedia` and restores access.

---

### 📅 Lossless Move & Copy Date Preservation
* **No More "Today" Timestamp Resets**: When moving or copying photos/videos to other albums, their original capture dates are completely preserved.
* **Full EXIF & Filesystem Synchronization**: Original EXIF datetime tags (`TAG_DATETIME_ORIGINAL`, `TAG_DATETIME`, `TAG_DATETIME_DIGITIZED`), physical filesystem last-modified timestamps, and MediaStore database rows are accurately updated.
* **Metadata Migration**: Favorites, custom tags, and custom locations migrate seamlessly to the new album.

---

### 📍 Free-Form Custom Location Descriptions & Geotagging
* **Arbitrary Place Names**: You can now attach any descriptive place name (e.g. *"delhi rohtak madina village raju printing press meham"*) directly to your photos without requiring GPS coordinates.
* **Dual Search (Names & Coordinates)**: The Map Explorer and EXIF Location Editor now support searching both place names and raw coordinates (e.g. `28.6139, 77.2090` or `28.6139° N, 77.2090° E`).
* **Gallery Search Integration**: Search your gallery using words from custom place descriptions.
* **Open in Maps**: Launch Google Maps / default maps app directly by coordinates or custom place name.

---

### 🎞️ Motion Photo Frame Extraction & GIF / MP4 Export
* **Micro-Video Frame Scrubber**: Play and scrub embedded live/motion photos frame-by-frame.
* **Best-Shot Extraction**: Pick any moment from a motion photo and save it as a pristine full-resolution still.
* **Looping GIF & MP4 Export**: Convert 2–3s motion photos into smooth looping GIFs or standalone video clips with 0 KB external bloat.

---

### 🔇 1-Second Lossless Video Muter (Audio Stripper)
* **Instant Background Noise Removal**: Strip audio tracks from videos in under 1 second.
* **Zero Re-Encoding**: Copies video streams losslessly via hardware-accelerated MediaMuxer and MediaExtractor, preserving 4K/60fps quality without battery drain.

---

### 🔍 On-Device OCR with Selective Copying
* **Streamlined Scanner**: Clean single-tap document scanner action in the photo viewer.
* **Selective Highlighting**: Double-tap words or drag selection handles to copy only the exact text snippet you want.
* **Smart Quick-Copy Chips**: Tap detected lines or phrases (phone numbers, addresses, totals) to copy them instantly.
* **Searchable Text**: Search for text visible inside photos directly in the main gallery search bar.

---

### 📸 Automated Documentation & Screenshot Sync
* **Continuous Sync**: Adding or updating pictures in `screenshots/` automatically synchronizes to `docs/screenshots/`, the GitHub Pages website, and `README.md` via pre-commit hooks, Gradle tasks, and CI.

---

### 📦 Downloads & Verification
* Download the signed APK below.
* Zero cloud dependencies, fully offline, zero analytics.
