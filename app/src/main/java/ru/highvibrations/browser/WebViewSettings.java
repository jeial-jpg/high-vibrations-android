package ru.highvibrations.browser;

import android.annotation.SuppressLint;
import android.webkit.WebSettings;

/**
 * Настройки WebView: JavaScript, хранилища, медиа, кэш, User-Agent.
 * Вынесено из Activity, чтобы она оставалась тонкой.
 */
public final class WebViewSettings {

    private WebViewSettings() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void apply(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        String userAgent = settings.getUserAgentString();
        if (userAgent != null) {
            // метка "wv" ломает плееры на части сайтов
            settings.setUserAgentString(userAgent.replace("; wv", ""));
        }
    }
}
