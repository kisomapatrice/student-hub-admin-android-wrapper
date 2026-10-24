# Student Hub Admin Android Wrapper — V1.2.0

A native Android WebView wrapper for the Student Hub V1 Admin Console. It does not modify or duplicate the website, its authentication, or its database.

## Build

The `Build Student Hub Admin Android v1.2.0` GitHub Actions workflow builds a debug APK for testing. In GitHub, open Actions, choose the workflow, and run it on `main`. Download the `Student-Hub-Admin-v1.2.0-APK` artifact from the successful run.

## Included

- Branded startup overlay and launcher icon.
- Retry experience when the site cannot be reached.
- Secure HTTPS-only WebView settings and host-aware external link handling.
- Persistent WebView cookies and storage for the existing website login.
- Android Back navigation and file chooser support for website upload controls.

## Production boundary

The wrapper opens `https://student-hub.great-site.net/admin/CONSOLE/index.php?module=dashboard`. It does not include website source code, database credentials, or the AI Worker secrets.
