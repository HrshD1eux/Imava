# Imava Development Rules & Workflow Conventions

These rules are strictly enforced across all modifications in this repository:

## 1. Permission Additions & Auditing Rule
Whenever any Android permission is added, modified, or removed:
- **`app/src/main/AndroidManifest.xml`**: Document the exact rationale in XML comments.
- **`PermissionsTransparencyScreen.kt`**: Add the permission to `detailedKnownPermissions` with user-friendly description, technical explanation, and introduced API level.
- **`README.md`**: Update the Permission Transparency Matrix table.

## 2. Third-Party Library & Dependency Rule
Whenever any third-party library or dependency is introduced or updated:
- **`AboutLegalScreen.kt`**: Add the audited component, its exact license (Apache 2.0 / MIT / GPL-3.0), and component purpose.
- **`README.md`**: Update the Tech Stack table.
- **`app/build.gradle.kts`**: Ensure dependencies are strictly non-bloat, FOSS-friendly, and include zero tracking/adware SDKs.

## 3. New Feature Workflow Rule
Whenever any new feature is implemented:
- **UI Exposure**: Must be accessible in the UI (dedicated screen, dialog, action button, or settings tile) with tactile feedback and error handling.
- **Localization**: All user-facing strings must use `stringResource(R.string.<key>)` and be translated across all 11+ supported language resource folders (`values`, `values-es`, `values-pt`, `values-pt-rBR`, `values-de`, `values-ru`, `values-fr`, `values-hi`, `values-bn`, `values-mr`, `values-te`, `values-ta`, `values-gu`).
- **Documentation**: If the feature is a unique or advanced killer feature, document it in `README.md`. Standard generic gallery features belong in `FEATURES.md`.

## 4. README Curation Rule
- Keep `README.md` strictly focused on **unique, advanced, killer capabilities** that differentiate Imava from standard galleries.
- Standard generic gallery features (e.g. basic grids, cropping, sliders, slideshows) must NOT clutter `README.md`—they are cataloged in `FEATURES.md`.
- Maintain up-to-date **Contribution Guidelines** in `README.md`.

## 5. Verification Rule
Before committing any changes:
- Run `./gradlew compileDebugKotlin`
- Run `./gradlew testDebugUnitTest`
- Ensure zero broken tests and zero compilation regressions.
