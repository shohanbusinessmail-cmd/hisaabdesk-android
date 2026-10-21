# Release Process

1. Bump `versionCode`/`versionName` in `app/build.gradle.kts`; update CHANGELOG in README if needed.
2. `./gradlew testDebugUnitTest lintDebug assembleDebug` locally or via Actions (Actions tab → Android CI).
3. Tag: `git tag v1.0.0 && git push origin v1.0.0` → `release.yml` runs tests, builds release APK, computes SHA-256, creates GitHub Release with APK + checksum.
4. Signing: if `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` secrets are set, the release APK is signed with that key; otherwise the workflow builds an **unsigned** release APK and the release notes state it cannot be installed as an update-track build — use the CI debug APK (debug-signed) for testing installs. Never commit keystores/passwords. Losing the release key prevents in-place updates — back it up securely offline.
5. Verify: Actions run green, artifact `app-debug.apk` downloadable, package `com.hisaabdesk.app`, version matches tag; install on a device, complete onboarding smoke test.
6. Download from Releases page → install → Android may warn about unknown sources for sideloading; enable per-app install permission.
