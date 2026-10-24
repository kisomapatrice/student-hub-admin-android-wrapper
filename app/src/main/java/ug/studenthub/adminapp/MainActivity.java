package ug.studenthub.adminapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String ADMIN_HOME = "https://student-hub.great-site.net/admin/CONSOLE/index.php?module=dashboard";
    private static final String ALLOWED_HOST = "student-hub.great-site.net";
    private static final int FILE_PICKER_REQUEST = 7301;

    private FrameLayout root;
    private WebView webView;
    private View startupOverlay;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button retryButton;
    private boolean loadFailed = false;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16, 42, 67));
        getWindow().setNavigationBarColor(Color.rgb(16, 42, 67));
        buildLayout();
        configureWebView();
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            if (webView.getUrl() == null) webView.loadUrl(ADMIN_HOME);
        } else {
            webView.loadUrl(ADMIN_HOME);
        }
    }

    private void buildLayout() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        startupOverlay = buildStartupOverlay();
        root.addView(startupOverlay, new FrameLayout.LayoutParams(-1, -1));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(60, 145, 220)));
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.rgb(60, 145, 220)));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP);
        root.addView(progressBar, progressParams);
        setContentView(root);
    }

    private View buildStartupOverlay() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(28), dp(28), dp(28), dp(28));
        panel.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(16, 42, 67), Color.rgb(23, 77, 124)}));

        TextView logo = new TextView(this);
        logo.setText("SH");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(30);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoShape = new GradientDrawable();
        logoShape.setColor(Color.rgb(23, 105, 170));
        logoShape.setCornerRadius(dp(22));
        logoShape.setStroke(dp(1), Color.rgb(158, 208, 255));
        logo.setBackground(logoShape);
        panel.addView(logo, new LinearLayout.LayoutParams(dp(84), dp(84)));

        TextView title = splashText("Student Hub Admin", 25, Color.WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-2, -2);
        titleParams.topMargin = dp(22);
        panel.addView(title, titleParams);

        TextView subtitle = splashText("UNITE KALIRO CAMPUS", 12, Color.rgb(217, 234, 251), true);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-2, -2);
        subParams.topMargin = dp(8);
        panel.addView(subtitle, subParams);

        ProgressBar spinner = new ProgressBar(this);
        spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        spinnerParams.topMargin = dp(30);
        panel.addView(spinner, spinnerParams);

        statusText = splashText("Connecting securely…", 14, Color.WHITE, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.topMargin = dp(18);
        panel.addView(statusText, statusParams);

        retryButton = new Button(this);
        retryButton.setText("Try again");
        retryButton.setTextColor(Color.rgb(16, 42, 67));
        retryButton.setAllCaps(false);
        retryButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable buttonShape = new GradientDrawable();
        buttonShape.setColor(Color.WHITE);
        buttonShape.setCornerRadius(dp(14));
        retryButton.setBackground(buttonShape);
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> retryLoading());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(160), dp(48));
        retryParams.topMargin = dp(18);
        panel.addView(retryButton, retryParams);

        TextView version = splashText("SECURE ADMIN ACCESS  •  V1.2.0", 11, Color.rgb(217, 234, 251), false);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(-2, -2);
        versionParams.topMargin = dp(38);
        panel.addView(version, versionParams);
        return panel;
    }

    private TextView splashText(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return routeUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUrl(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadFailed = false;
                startupOverlay.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
                statusText.setText("Connecting securely…");
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(5);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setProgress(100);
                if (!loadFailed) {
                    startupOverlay.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showLoadError();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                showLoadError();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent;
                try {
                    intent = params.createIntent();
                    startActivityForResult(intent, FILE_PICKER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker is available on this device.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
    }

    private boolean routeUrl(String rawUrl) {
        Uri uri = Uri.parse(rawUrl);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if ("https".equals(scheme) && isAllowedHost(uri)) {
            return false;
        }
        if ("mailto".equals(scheme) || "tel".equals(scheme)) {
            openExternal(uri);
            return true;
        }
        if ("https".equals(scheme) || "http".equals(scheme)) {
            openExternal(uri);
            return true;
        }
        return true;
    }

    private boolean isAllowedHost(Uri uri) {
        String host = uri.getHost();
        return host != null && (ALLOWED_HOST.equalsIgnoreCase(host) || host.toLowerCase().endsWith("." + ALLOWED_HOST));
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "No app is available to open this link.", Toast.LENGTH_LONG).show();
        }
    }

    private void showLoadError() {
        loadFailed = true;
        webView.setVisibility(View.GONE);
        startupOverlay.setVisibility(View.VISIBLE);
        statusText.setText("Unable to connect. Check your internet and try again.");
        retryButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void retryLoading() {
        loadFailed = false;
        webView.setVisibility(View.VISIBLE);
        startupOverlay.setVisibility(View.VISIBLE);
        statusText.setText("Connecting securely…");
        retryButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(5);
        webView.loadUrl(ADMIN_HOME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST && filePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
