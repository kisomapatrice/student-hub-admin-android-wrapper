package ug.studenthub.adminapp;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Thin, full-screen browser shell for the existing Student Hub V1 Admin Console.
 * It does not contain backend credentials and does not bypass the site's login or permissions.
 */
public final class MainActivity extends Activity {
    private static final String ADMIN_HOME =
            "https://student-hub.great-site.net/admin/CONSOLE/index.php?module=dashboard";
    private static final String ALLOWED_HOST = "student-hub.great-site.net";
    private static final int FILE_CHOOSER_REQUEST = 4107;

    private WebView webView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> pendingFileCallback;
    private FrameLayout startupOverlay;
    private TextView startupMessage;
    private ProgressBar startupSpinner;
    private Button retryButton;
    private boolean startupFinished = false;
    private boolean startupLoadFailed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        buildInterface();
        configureWebView();

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // WebView restored its existing navigation state.
        } else {
            webView.loadUrl(ADMIN_HOME);
        }
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(16, 42, 67));
        window.setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }
    }

    private void buildInterface() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(247, 249, 252));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP);
        root.addView(progressBar, progressParams);

        // Branded startup overlay remains visible until the first page loads.
        startupOverlay = buildStartupOverlay();
        root.addView(startupOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private FrameLayout buildStartupOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        GradientDrawable backdrop = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(16, 42, 67), Color.rgb(29, 78, 216)});
        overlay.setBackground(backdrop);
        overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(28), dp(24), dp(28), dp(24));
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        overlay.addView(content, contentParams);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setContentDescription("Student Hub app icon");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(94), dp(94));
        logoParams.bottomMargin = dp(18);
        content.addView(logo, logoParams);

        TextView title = startupText("Student Hub", 29, Color.WHITE, true);
        content.addView(title, matchWrap());

        TextView section = startupText("ADMIN CONSOLE", 13, Color.rgb(219, 234, 254), true);
        section.setLetterSpacing(0.18f);
        LinearLayout.LayoutParams sectionParams = matchWrap();
        sectionParams.topMargin = dp(7);
        content.addView(section, sectionParams);

        TextView campus = startupText("UNITE KALIRO CAMPUS", 13, Color.WHITE, false);
        LinearLayout.LayoutParams campusParams = matchWrap();
        campusParams.topMargin = dp(5);
        campusParams.bottomMargin = dp(28);
        content.addView(campus, campusParams);

        startupSpinner = new ProgressBar(this);
        startupSpinner.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startupSpinner.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        }
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        spinnerParams.bottomMargin = dp(14);
        content.addView(startupSpinner, spinnerParams);

        startupMessage = startupText("Connecting to Student Hub…", 14, Color.WHITE, false);
        LinearLayout.LayoutParams messageParams = matchWrap();
        messageParams.bottomMargin = dp(12);
        content.addView(startupMessage, messageParams);

        retryButton = new Button(this);
        retryButton.setText("Try again");
        retryButton.setTextColor(Color.rgb(16, 42, 67));
        retryButton.setAllCaps(false);
        retryButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.WHITE);
        buttonBackground.setCornerRadius(dp(12));
        retryButton.setBackground(buttonBackground);
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> {
            startupLoadFailed = false;
            startupSpinner.setVisibility(View.VISIBLE);
            startupMessage.setText("Connecting to Student Hub…");
            retryButton.setVisibility(View.GONE);
            startupOverlay.setAlpha(1f);
            startupOverlay.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            webView.loadUrl(ADMIN_HOME);
        });
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(150), dp(48));
        retryParams.topMargin = dp(4);
        content.addView(retryButton, retryParams);

        TextView footer = startupText("STUDENT HUB  ·  V1", 11, Color.rgb(219, 234, 254), false);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(34);
        content.addView(footer, footerParams);
        return overlay;
    }

    private TextView startupText(String text, float sizeSp, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        label.setGravity(Gravity.CENTER);
        label.setFontFeatureSettings("kern");
        label.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        label.setIncludeFontPadding(true);
        return label;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showStartupError(String message) {
        if (startupFinished || startupOverlay == null) {
            showMessage(message);
            return;
        }
        startupLoadFailed = true;
        startupSpinner.setVisibility(View.GONE);
        startupMessage.setText(message);
        retryButton.setVisibility(View.VISIBLE);
        startupOverlay.setAlpha(1f);
        startupOverlay.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void finishStartup() {
        if (startupFinished) return;
        startupFinished = true;
        if (startupOverlay != null) {
            startupOverlay.animate().alpha(0f).setDuration(220).withEndAction(() -> {
                startupOverlay.setVisibility(View.GONE);
                startupOverlay.setAlpha(1f);
            }).start();
        }
    }

    private void configureWebView() {
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookies.setAcceptThirdPartyCookies(webView, false);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // The site's authenticated console relies on JavaScript.
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true); // Needed for user-selected upload files.
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return routeUri(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUri(Uri.parse(url));
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Never bypass certificate errors.
                handler.cancel();
                showStartupError("Secure connection failed. Check your connection and try again.");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    showStartupError("Student Hub could not load. Check your internet connection and try again.");
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!startupLoadFailed) finishStartup();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (pendingFileCallback != null) {
                    pendingFileCallback.onReceiveValue(null);
                    pendingFileCallback = null;
                }
                pendingFileCallback = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    pendingFileCallback = null;
                    filePathCallback.onReceiveValue(null);
                    showMessage("No file picker is available on this device.");
                    return false;
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // Open target=_blank links safely: the Student Hub host stays in-app;
                // unrelated web sites open in the Android browser.
                WebView popup = new WebView(MainActivity.this);
                popup.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView popupView, WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        if (isAllowedHost(uri)) {
                            webView.loadUrl(uri.toString());
                        } else {
                            openExternal(uri);
                        }
                        popupView.post(popupView::destroy);
                        return true;
                    }

                    @Override
                    @SuppressWarnings("deprecation")
                    public boolean shouldOverrideUrlLoading(WebView popupView, String url) {
                        Uri uri = Uri.parse(url);
                        if (isAllowedHost(uri)) webView.loadUrl(url); else openExternal(uri);
                        popupView.post(popupView::destroy);
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                Uri uri = Uri.parse(url);
                // Do not forward authenticated cookies to a different host.
                if (!isAllowedHost(uri) || !"https".equalsIgnoreCase(uri.getScheme())) {
                    openExternal(uri);
                    return;
                }
                try {
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    String cookie = CookieManager.getInstance().getCookie(url);
                    if (cookie != null && !cookie.isEmpty()) request.addRequestHeader("Cookie", cookie);
                    request.addRequestHeader("User-Agent", webView.getSettings().getUserAgentString());
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                    if (mimetype != null && !mimetype.isEmpty()) request.setMimeType(mimetype);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(url, contentDisposition, mimetype));
                    DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (manager != null) {
                        manager.enqueue(request);
                        showMessage("Download started. Check your Downloads folder.");
                    } else {
                        showMessage("Downloads are unavailable on this device.");
                    }
                } catch (RuntimeException ex) {
                    showMessage("Could not start this download. Try opening it in your browser.");
                }
            }
        });
    }

    private boolean routeUri(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (("https".equals(scheme) || "http".equals(scheme)) && isAllowedHost(uri)) {
            return false; // Continue normal navigation only to the V1 host.
        }
        if ("https".equals(scheme) || "http".equals(scheme)
                || "mailto".equals(scheme) || "tel".equals(scheme) || "sms".equals(scheme)) {
            openExternal(uri);
        }
        // Block unknown schemes such as javascript:, file:, intent:, or content:.
        return true;
    }

    private boolean isAllowedHost(Uri uri) {
        return uri != null
                && "https".equalsIgnoreCase(uri.getScheme())
                && ALLOWED_HOST.equalsIgnoreCase(uri.getHost());
    }

    private void openExternal(Uri uri) {
        if (uri == null) return;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!("https".equals(scheme) || "http".equals(scheme)
                || "mailto".equals(scheme) || "tel".equals(scheme) || "sms".equals(scheme))) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ex) {
            showMessage("No app is available to open this link.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (pendingFileCallback != null) {
                Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                pendingFileCallback.onReceiveValue(results);
                pendingFileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
