package ru.highvibrations.browser;

import android.webkit.JavascriptInterface;

/**
 * Мост страница -> приложение для состояния камеры и микрофона.
 * Страница сообщает: идёт ли поток с камеры, выключен ли микрофон,
 * какая камера активна.
 */
public final class MediaBridge {

    private final MediaOverlay overlay;

    public MediaBridge(MediaOverlay overlay) {
        this.overlay = overlay;
    }

    @JavascriptInterface
    public void onCameraActive(boolean active) {
        android.util.Log.i("KioskBridge", "onCameraActive " + active);
        overlay.handleCameraActive(active);
    }

    @JavascriptInterface
    public void onMicMuted(boolean muted) {
        android.util.Log.i("KioskBridge", "onMicMuted " + muted);
        overlay.handleMicMuted(muted);
    }

    @JavascriptInterface
    public void onCameraFacing(String facing) {
        android.util.Log.i("KioskBridge", "onCameraFacing " + facing);
        overlay.handleFacing(facing);
    }
}
