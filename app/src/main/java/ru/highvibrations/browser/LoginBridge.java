package ru.highvibrations.browser;

import android.webkit.JavascriptInterface;

/**
 * Мост страница -> приложение: страница сообщает, что пользователь
 * ввёл логин и пароль. Только запись: прочитать сохранённый пароль
 * из JavaScript нельзя.
 */
public final class LoginBridge {

    /** Принимает данные формы (вызывается в фоновом потоке WebView). */
    public interface CredentialSink {
        void onCredentials(String host, String username, String password);
    }

    private final CredentialSink sink;

    public LoginBridge(CredentialSink sink) {
        this.sink = sink;
    }

    @JavascriptInterface
    public void onCredentials(String host, String username, String password) {
        if (sink == null || host == null || host.isEmpty()) {
            return;
        }
        if (password == null || password.isEmpty()) {
            return;
        }
        sink.onCredentials(host, username == null ? "" : username, password);
    }
}
