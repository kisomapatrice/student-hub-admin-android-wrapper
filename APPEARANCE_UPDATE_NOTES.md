# Student Hub Admin Android — Appearance Update V1.1

## What changed

- Replaced the generic launcher mark with a navy, white, and blue open-book icon.
- Added a branded startup screen with Student Hub / Admin Console / Unite Kaliro Campus labeling.
- Added a visible retry button when the first connection fails, without weakening HTTPS or certificate checks.
- Bumped Android app version to 1.0.1 (versionCode 2).

## Scope

This update changes only the separate Android wrapper project. It does not change the live Student Hub V1 website, PHP files, database, roles, passwords, or hosting configuration. The app still requires an internet connection and uses the existing server-side login and permissions.

## Build and install

Build with the provided GitHub Actions update workflow. Download the `Student-Hub-Admin-APK` artifact and extract `app-debug.apk`.

A GitHub-hosted debug build may have a different debug signing key from the previous build. If Android refuses to install it over the existing debug app, uninstall the old `Student Hub Admin` app and then install the new APK. Uninstalling removes the app's local WebView session, so sign in again afterward. This does not delete any Student Hub server records.
