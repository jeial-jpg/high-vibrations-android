package ru.highvibrations.browser;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Правило повторного запроса разрешения.
 *
 * Схема: спросили -> отказ -> следующий вопрос не раньше чем через 24 часа ->
 * второй отказ -> больше не спрашиваем никогда.
 * Если разрешение выдано, счётчик сбрасывается: при будущем отключении
 * в настройках снова спросим, но опять не чаще раза в сутки и не больше двух раз.
 */
public final class PermissionGate {

    /** Пауза перед повторным вопросом. */
    public static final long RETRY_DELAY_MS = 24L * 60L * 60L * 1000L;

    private static final String PREFS = "permission_gate";
    private static final int MAX_ASKS = 2;

    private PermissionGate() {
    }

    /** Пора ли спрашивать снова. */
    public static boolean shouldAsk(Context context, String key) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getBoolean(key + "_final", false)) {
            return false; // уже отказали дважды
        }
        int asks = prefs.getInt(key + "_asks", 0);
        if (asks == 0) {
            return true; // ещё не спрашивали
        }
        if (asks >= MAX_ASKS) {
            return false;
        }
        long last = prefs.getLong(key + "_last", 0L);
        return System.currentTimeMillis() - last >= RETRY_DELAY_MS;
    }

    /** Вызывать перед показом системного диалога. */
    public static void markAsked(Context context, String key) {
        SharedPreferences prefs = prefs(context);
        int asks = prefs.getInt(key + "_asks", 0) + 1;
        prefs.edit()
                .putInt(key + "_asks", asks)
                .putLong(key + "_last", System.currentTimeMillis())
                .putBoolean(key + "_final", asks >= MAX_ASKS)
                .apply();
    }

    /** Разрешение получено: следующий возможный вопрос - не раньше чем через сутки. */
    public static void markGranted(Context context, String key) {
        prefs(context).edit()
                .putInt(key + "_asks", 1)
                .putLong(key + "_last", System.currentTimeMillis())
                .putBoolean(key + "_final", false)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
