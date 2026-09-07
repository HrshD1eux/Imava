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

### 🔍 WhatsApp Sent & Hidden Media Deep Scan
* **Zero-Type MediaStore Fix**: Added full file extension pattern matching (`DISPLAY_NAME LIKE '%.jpg' ...`) so media in `.nomedia` directories (WhatsApp Sent, Private, Telegram) with `MEDIA_TYPE == 0` is recognized and displayed.
* **Explicit MIME Type Registration**: Media scanning now resolves and registers explicit MIME types (`image/jpeg`, `video/mp4`) during scans, preventing `.nomedia` media from being marked as unreadable binary octet-streams.
* **Deep Directory Paths**: Explicitly scans WhatsApp Sent, Private, Animated GIFs, WhatsApp Business, Telegram, and standard media paths up to depth 6.

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
* Zero cloud dependencies, 100% offline, zero analytics.

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
* **No More "Today" Timestamp Resets**: When moving or copying photos/videos to other albums, their original capture dates are 100% preserved.
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
* Zero cloud dependencies, 100% offline, zero analytics.
