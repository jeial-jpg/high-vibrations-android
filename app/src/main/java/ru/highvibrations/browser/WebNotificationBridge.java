package ru.highvibrations.browser;

import android.webkit.JavascriptInterface;

/**
 * Мост страница -> приложение для уведомлений:
 * JavaScript вызывает HVNotify.show(title, body), приложение показывает
 * обычное системное уведомление.
 */
public final class WebNotificationBridge {

    /** Куда отдавать уведомление. */
    public interface Sink {
        void onWebNotification(String title, String body);
    }

    private final Sink sink;

    public WebNotificationBridge(Sink sink) {
        this.sink = sink;
    }

    @JavascriptInterface
    public void show(String title, String body) {
        if (sink == null) {
            return;
        }
        sink.onWebNotification(title, body);
    }
}
