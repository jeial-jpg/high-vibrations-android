package ru.highvibrations.browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * Разрешение на уведомления.
 *
 * Android 13+ : системный запрос POST_NOTIFICATIONS при первом запуске.
 * Android 8-12: разрешение выдаётся при установке, мы создаём канал;
 *               если пользователь выключил уведомления - предлагаем настройки.
 * Android 7-  : разрешение выдаётся при установке.
 *
 * Факт запроса хранится в приватных настройках, поэтому повторно
 * приложение не спрашивает и выбор пользователя не сбрасывается.
 */
public final class NotificationPermission {

    public static final String CHANNEL_ID = "web_notifications";
    public static final int REQUEST_CODE = 2001;
    public static final String GATE_KEY = "notifications";

    private NotificationPermission() {
    }

    /** Разрешены ли уведомления на уровне системы. */
    public static boolean isGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager == null || manager.areNotificationsEnabled();
    }

    /** Создаёт канал уведомлений (нужен начиная с Android 8). */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Уведомления сайта", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Сообщения от сайта high-vibrations.ru");
        manager.createNotificationChannel(channel);
    }

    /** Спрашивает по правилу: сейчас, при отказе - через 24 часа, после второго отказа - никогда. */
    public static void askOnce(final Activity activity) {
        if (activity == null) {
            return;
        }
        ensureChannel(activity);
        if (isGranted(activity)) {
            PermissionGate.markGranted(activity, GATE_KEY);
            return;
        }
        if (!PermissionGate.shouldAsk(activity, GATE_KEY)) {
            return; // ещё рано или пользователь уже ответил дважды
        }
        PermissionGate.markAsked(activity, GATE_KEY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
        } else {
            offerSettings(activity);
        }
    }

    private static void offerSettings(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Уведомления выключены")
                .setMessage("Чтобы получать уведомления от сайта, разрешите их в настройках.")
                .setPositiveButton("Открыть настройки", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                        try {
                            activity.startActivity(intent);
                        } catch (Exception exception) {
                            openAppSettings(activity);
                        }
                    }
                })
                .setNegativeButton("Позже", null)
                .show();
    }

    private static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null));
        try {
            activity.startActivity(intent);
        } catch (Exception ignored) {
            // настроек нет - ничего не делаем
        }
    }
}
