package ru.highvibrations.browser;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Навигационный сторож: пускает только разрешённые хосты.
 * Срабатывает и на обычные переходы, и на редиректы/скрипты (onPageStarted).
 */
public final class KioskWebViewClient extends WebViewClient {

    private static final String TAG = "KioskBrowser";

    /** Колбэк для Activity: страница догрузилась (можно вставлять скрипты). */
    public interface PageListener {
        void onPageLoaded(String url);
    }

    private final SitePolicy policy;
    private final String startUrl;
    private final PageListener pageListener;

    public KioskWebViewClient(SitePolicy policy, String startUrl, PageListener pageListener) {
        this.policy = policy;
        this.startUrl = startUrl;
        this.pageListener = pageListener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // true = "обработано нами", значит переход отменяется
        return !policy.isAllowed(request.getUrl().toString());
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return !policy.isAllowed(url);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        Log.i(TAG, "start " + url);
        if (!policy.isAllowed(url)) {
            // серверный 302 или location.href обходят проверку выше
            Log.w(TAG, "blocked, back to start: " + url);
            view.stopLoading();
            view.loadUrl(startUrl);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.i(TAG, "done " + url);
        if (pageListener != null) {
            pageListener.onPageLoaded(url);
        }
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request,
                                WebResourceError error) {
        if (request.isForMainFrame()) {
            Log.e(TAG, "error " + error.getErrorCode() + " " + error.getDescription()
                    + " " + request.getUrl());
        }
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        // проверку сертификата не подменяем никогда
        handler.cancel();
    }
}
