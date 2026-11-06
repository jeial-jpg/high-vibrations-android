package ru.highvibrations.browser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Показ системного уведомления от имени сайта.
 * Используется, когда страница вызывает Notification API.
 */
public final class WebNotifications {

    private WebNotifications() {
    }

    public static void show(Context context, String title, String body) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !manager.areNotificationsEnabled()) {
            return; // пользователь запретил уведомления - молчим
        }
        NotificationPermission.ensureChannel(context);

        String safeTitle = (title == null || title.isEmpty()) ? "На высоких вибрациях" : title;
        String safeBody = body == null ? "" : body;

        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(context, NotificationPermission.CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_emblem)
                .setContentTitle(safeTitle)
                .setContentText(safeBody)
                .setStyle(new Notification.BigTextStyle().bigText(safeBody))
                .setAutoCancel(true)
                .setContentIntent(openApp(context));

        manager.notify((int) (System.currentTimeMillis() % 100000), builder.build());
    }

    private static PendingIntent openApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 0, intent, flags);
    }
}
