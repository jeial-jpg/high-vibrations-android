package ru.highvibrations.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * Точка входа. Связывает модули:
 * KioskConfig (настройки) -> SitePolicy (правила) -> клиенты WebView.
 * Здесь только сборка UI и жизненный цикл, никакой бизнес-логики.
 */
public class MainActivity extends Activity implements KioskChromeClient.FileChooserDelegate {

    private static final int FILE_CHOOSER_REQUEST = 1001;

    private KioskConfig config;
    private SitePolicy policy;
    private WebView webView;
    private FrameLayout rootView;
    private KioskChromeClient chromeClient;
    private ValueCallback<Uri[]> filePathCallback;
    private PasswordVault vault;
    private CredentialPrompter prompter;
    private MediaPermissionController mediaController;
    private MediaOverlay mediaOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = KioskConfig.from(this);
        policy = new SitePolicy(config);
        vault = new PasswordVault(this);
        prompter = new CredentialPrompter(this, vault);
        mediaController = new MediaPermissionController(this, policy);
        buildLayout();
        buildWebView();
        FullscreenController.apply(this);
        // пока смотрим видео - экран не гасим
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Всегда открываем сайт при создании Activity.
        // Пока процесс жив (приложение свёрнуто, но в памяти) Activity не
        // пересоздаётся, страница и история сохраняются - повторной загрузки нет.
        // Если Android выгрузил приложение из кеша, оно откроет сайт заново.
        webView.loadUrl(config.getStartUrl());
    }

    private void buildLayout() {
        rootView = new FrameLayout(this);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootView.setBackgroundColor(0xFF000000);
        setContentView(rootView);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void buildWebView() {
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(0xFF000000);
        WebViewSettings.apply(webView.getSettings());

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(
                new KioskWebViewClient(policy, config.getStartUrl(), this::onPageLoaded));
        webView.addJavascriptInterface(new LoginBridge(this::onCredentialsFromPage), "HVLogin");
        webView.addJavascriptInterface(new WebNotificationBridge(
                (title, body) -> WebNotifications.show(this, title, body)), "HVNotify");
        chromeClient = new KioskChromeClient(this, rootView, webView, this, mediaController);
        webView.setWebChromeClient(chromeClient);
        rootView.addView(webView);

        mediaOverlay = new MediaOverlay(this, webView);
        rootView.addView(mediaOverlay);
        webView.addJavascriptInterface(new MediaBridge(mediaOverlay), "HVMedia");
    }
    /** Страница загрузилась: ставим наблюдатель за формой и подставляем пароль. */
    private void onPageLoaded(String url) {
        android.util.Log.i("KioskBrowser", "page loaded -> inject " + url);
        String host = hostOf(url);
        if (webView == null || host == null) {
            return;
        }
        webView.evaluateJavascript(LoginScripts.captureWatcher(), null);
        webView.evaluateJavascript(MediaScripts.inject(), null);
        if (mediaOverlay != null) {
            mediaOverlay.hideButtons();
        }
        webView.evaluateJavascript(
                NotificationScripts.polyfill(NotificationPermission.isGranted(this)), null);
        if (!policy.isAllowedHost(host)) {
            return;
        }
        PasswordVault.Credentials saved = vault.get(host);
        if (saved != null && !saved.password.isEmpty()) {
            webView.evaluateJavascript(LoginScripts.autofill(saved.username, saved.password), null);
        }
    }

    /** Пришло от страницы (фоновый поток WebView) - спрашиваем пользователя. */
    private void onCredentialsFromPage(String host, String username, String password) {
        if (!policy.isAllowedHost(host)) {
            return;
        }
        runOnUiThread(() -> prompter.handle(host, username, password));
    }

    private static String hostOf(String url) {
        if (url == null) {
            return null;
        }
        try {
            return Uri.parse(url).getHost();
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public boolean openFileChooser(ValueCallback<Uri[]> callback,
                                   WebChromeClient.FileChooserParams params) {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
        }
        filePathCallback = callback;
        try {
            startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
        } catch (ActivityNotFoundException exception) {
            filePathCallback = null;
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MediaPermissionController.REQUEST_CODE) {
            mediaController.onRequestPermissionsResult(requestCode, grantResults);
        } else if (requestCode == NotificationPermission.REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PermissionGate.markGranted(this, NotificationPermission.GATE_KEY);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (chromeClient != null && chromeClient.isFullscreenVideo()) {
            chromeClient.leaveFullscreenVideo();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        // иначе ничего не делаем: выйти из приложения кнопкой «назад» нельзя
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            FullscreenController.apply(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FullscreenController.apply(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // сохраняем cookies на диск: вход на сайт не потеряется после перезапуска
        CookieManager.getInstance().flush();
        if (webView != null) {
            webView.onPause();
        }
        if (mediaOverlay != null) {
            mediaOverlay.hideButtons();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
        FullscreenController.apply(this);
        if (mediaOverlay != null) {
            mediaOverlay.refresh();
        }
        // один раз за установку: системный запрос "разрешить уведомления?"
        NotificationPermission.askOnce(this);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            rootView.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
