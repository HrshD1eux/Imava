# 📋 Imava Pre-Release & Store Publishing Checklist

> [!WARNING]
> **CRITICAL STORE POLICY VIOLATION NOTICE**
> 
> Before publishing **Imava** to **Google Play Store**, **F-Droid**, or any commercial app marketplace, you **MUST** remove the in-app APK downloader and the `REQUEST_INSTALL_PACKAGES` permission. 
> 
> Failure to do so will result in an **instant rejection**, **app suspension**, or **account policy strike** from Google Play.

---

## 🚨 Why This Must Be Removed Before Store Publishing

1. **Google Play Developer Policy (Malware / Device Abuse)**
   * Google Play strictly restricts `android.permission.REQUEST_INSTALL_PACKAGES`. Only dedicated app stores, browser apps, or enterprise device management tools are permitted to request this permission. A gallery app requesting package installation triggers an immediate automated policy ban.
2. **Google Play Developer Distribution Agreement (Section 4.5)**
   * Apps distributed via Google Play are strictly prohibited from downloading, installing, or updating executable binary (`.apk`) files outside of Google Play's official billing and update pipeline.
3. **Google Play Protect Heuristic Flagging**
   * Downloading an APK over an HTTP connection and prompting system installation without Play Store signature verification triggers anti-malware heuristic flags (e.g., classifying the app as a "Dropper" or "Trojan Downloader").
4. **F-Droid Anti-Feature Rules**
   * F-Droid flags in-app binary downloaders as an anti-feature (`NonFreeNet` / `UpstreamNonFree`) and requires self-updating mechanisms to be disabled for official repo inclusion.

---

## 🛠️ Step-by-Step Removal Guide (When Ready to Publish)

### Step 1: Remove Permission from Manifest
In [`app/src/main/AndroidManifest.xml`](file:///h:/Programming/Main%20Projects/Android/Gallery/app/src/main/AndroidManifest.xml):
* Remove line:
  ```xml
  <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
  ```
* *(Optional for 100% offline Play Store release)*: If you want zero network permissions on Google Play, also remove:
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  ```

---

### Step 2: Remove Binary Installer in `AppUpdateManager.kt`
In [`app/src/main/java/com/hrshd1eux/imava/core/util/AppUpdateManager.kt`](file:///h:/Programming/Main%20Projects/Android/Gallery/app/src/main/java/com/hrshd1eux/imava/core/util/AppUpdateManager.kt):
* Delete or disable `downloadAndInstallApk()` and `installApk()`.
* Replace with a clean browser intent:
  ```kotlin
  fun openReleasePage(context: Context, releaseUrl: String = "https://github.com/HrshD1eux/Imava/releases/latest") {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
  }
  ```

---

### Step 3: Update UI in `SettingsScreen.kt`
In [`app/src/main/java/com/hrshd1eux/imava/ui/settings/SettingsScreen.kt`](file:///h:/Programming/Main%20Projects/Android/Gallery/app/src/main/java/com/hrshd1eux/imava/ui/settings/SettingsScreen.kt):
* In the update dialog, replace the `"Download & Install"` button action:
  * Instead of calling `AppUpdateManager.downloadAndInstallApk(...)`, trigger an external browser intent to `https://github.com/HrshD1eux/Imava/releases/latest` or the app's Google Play Store listing.
  * Change button label to: `"View Latest Release ↗"`.

---

### Step 4: Update Background Notification in `AppUpdateCheckWorker.kt`
In [`app/src/main/java/com/hrshd1eux/imava/core/worker/AppUpdateCheckWorker.kt`](file:///h:/Programming/Main%20Projects/Android/Gallery/app/src/main/java/com/hrshd1eux/imava/core/worker/AppUpdateCheckWorker.kt):
* Change notification pending intent so tapping the update notification opens the GitHub release webpage or Google Play Store instead of attempting an in-app download.

---

## 💡 Recommended Architecture: Product Flavors

Instead of manually editing files each time you release, configure two build flavors in `app/build.gradle.kts`:

```kotlin
android {
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            // Retains in-app updater and REQUEST_INSTALL_PACKAGES for standalone GitHub APKs
        }
        create("playstore") {
            dimension = "distribution"
            // Strips REQUEST_INSTALL_PACKAGES, redirects updates to browser or Google Play
        }
        create("fdroid") {
            dimension = "distribution"
            // Disables in-app update checks completely; updates managed via F-Droid client
        }
    }
}
```

---

## 📋 General Pre-Flight Store Checklist
- [ ] `REQUEST_INSTALL_PACKAGES` permission stripped
- [ ] No direct executable binary `.apk` downloading in code
- [ ] Cleaned Fastlane metadata in `fastlane/metadata/android/en-US/`
- [ ] `MANAGE_EXTERNAL_STORAGE` policy declaration submitted (required on Google Play for All Files Access)
- [ ] Keystore and release signing keys configured in `keystore.properties`
- [ ] Tested release build via `./gradlew assembleRelease`
