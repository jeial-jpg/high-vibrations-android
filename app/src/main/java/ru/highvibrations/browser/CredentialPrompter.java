package ru.highvibrations.browser;

import android.app.Activity;

/**
 * Диалоги как в обычных браузерах, но в стиле сайта:
 *  - "Сохранить пароль?"  - если для сайта ещё ничего не сохранено;
 *  - "Обновить пароль?"   - если пароль изменился.
 * Повторные одинаковые данные повторно не спрашиваются.
 */
public final class CredentialPrompter {

    private static final String SAVE_TITLE = "Сохранить пароль?";
    private static final String UPDATE_TITLE = "Обновить пароль?";
    private static final String STORE_HINT = "Пароль хранится только на этом устройстве";
    private static final String NO_TEXT = "Нет";

    private final Activity activity;
    private final PasswordVault vault;

    public CredentialPrompter(Activity activity, PasswordVault vault) {
        this.activity = activity;
        this.vault = vault;
    }

    /** Вызывать из UI-потока. */
    public void handle(final String host, final String username, final String password) {
        if (host == null || host.isEmpty() || password == null || password.isEmpty()) {
            return;
        }
        PasswordVault.Credentials saved = vault.get(host);
        if (saved != null && password.equals(saved.password)) {
            return; // эти данные уже сохранены - не пристаём
        }
        if (saved == null) {
            askSave(host, username, password);
        } else {
            askUpdate(host, username, password);
        }
    }

    private void askSave(final String host, final String username, final String password) {
        StringBuilder body = new StringBuilder();
        if (username != null && !username.isEmpty()) {
            body.append("Логин: ").append(username).append('\n');
        }
        body.append(STORE_HINT);

        PasswordDialog.show(activity, SAVE_TITLE, siteName(), body.toString(),
                "Сохранить", NO_TEXT, new PasswordDialog.OnAccept() {
                    @Override
                    public void onAccept() {
                        vault.save(host, username, password);
                    }
                });
    }

    private void askUpdate(final String host, final String username, final String password) {
        String body = "Пароль для этого сайта изменился.\nОбновить сохранённый?";

        PasswordDialog.show(activity, UPDATE_TITLE, siteName(), body,
                "Обновить", NO_TEXT, new PasswordDialog.OnAccept() {
                    @Override
                    public void onAccept() {
                        vault.save(host, username, password);
                    }
                });
    }

    /** В диалоге показываем название приложения, а не домен. */
    private String siteName() {
        return activity.getString(R.string.app_name);
    }
}
