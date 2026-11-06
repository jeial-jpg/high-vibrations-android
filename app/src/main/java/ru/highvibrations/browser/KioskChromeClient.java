package ru.highvibrations.browser;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Возможности WebView: видео на весь экран, камера/микрофон для сайта,
 * выбор файла в формах.
 */
public final class KioskChromeClient extends WebChromeClient {

    /** Выбор файла обрабатывает Activity (у неё есть startActivityForResult). */
    public interface FileChooserDelegate {
        boolean openFileChooser(ValueCallback<Uri[]> callback, FileChooserParams params);
    }

    private final Activity activity;
    private final ViewGroup container;
    private final WebView webView;
    private final FileChooserDelegate fileChooserDelegate;
    private final MediaPermissionController media;

    private View customView;
    private CustomViewCallback customViewCallback;

    public KioskChromeClient(Activity activity, ViewGroup container, WebView webView,
                             FileChooserDelegate fileChooserDelegate,
                             MediaPermissionController media) {
        this.activity = activity;
        this.container = container;
        this.webView = webView;
        this.fileChooserDelegate = fileChooserDelegate;
        this.media = media;
    }

    /** Сейчас открыто полноэкранное видео? */
    public boolean isFullscreenVideo() {
        return customView != null;
    }

    /** Выйти из полноэкранного видео (вызывается по кнопке «назад»). */
    public void leaveFullscreenVideo() {
        if (customView == null) {
            return;
        }
        container.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        FullscreenController.apply(activity);
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }
        customView = view;
        customViewCallback = callback;
        container.addView(view);
        webView.setVisibility(View.GONE);
        FullscreenController.apply(activity);
    }

    @Override
    public void onHideCustomView() {
        leaveFullscreenVideo();
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage message) {
        // видно в logcat: ошибки наших вставленных скриптов тоже
        Log.d("KioskBrowser", "console: " + message.message()
                + " (" + message.lineNumber() + ")");
        return true;
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        // камера/микрофон: только свой сайт, только video/audio, только с согласия
        media.handle(request);
    }

    @Override
    public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                     FileChooserParams params) {
        return fileChooserDelegate != null && fileChooserDelegate.openFileChooser(callback, params);
    }
}
