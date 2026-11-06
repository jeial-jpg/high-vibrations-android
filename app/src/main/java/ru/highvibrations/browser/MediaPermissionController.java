package ru.highvibrations.browser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.webkit.PermissionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Камера и микрофон для страницы.
 *
 * Разрешено только своему сайту и его поддоменам, только ресурсы
 * VIDEO_CAPTURE и AUDIO_CAPTURE, и только после системного разрешения
 * пользователя. Всё остальное (MIDI, защищённый контент и пр.) - отказ.
 */
public final class MediaPermissionController {

    private static final String TAG = "KioskMedia";
    private static final String GATE_KEY = "media";

    public static final int REQUEST_CODE = 2002;

    private final Activity activity;
    private final SitePolicy policy;

    private PermissionRequest pendingRequest;
    private String[] pendingResources = new String[0];

    public MediaPermissionController(Activity activity, SitePolicy policy) {
        this.activity = activity;
        this.policy = policy;
    }

    /** Вызывается из WebChromeClient.onPermissionRequest (UI-поток). */
    public void handle(PermissionRequest request) {
        String host = hostOf(request.getOrigin());
        if (host == null || !policy.isAllowedHost(host)) {
            Log.w(TAG, "deny: чужой источник " + request.getOrigin());
            request.deny();
            return;
        }

        List<String> allowed = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                    || PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                allowed.add(resource);
            }
        }
        if (allowed.isEmpty()) {
            Log.w(TAG, "deny: нет подходящих ресурсов");
            request.deny();
            return;
        }

        List<String> needed = new ArrayList<>();
        if (allowed.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                && !granted(Manifest.permission.CAMERA)) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (allowed.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                && !granted(Manifest.permission.RECORD_AUDIO)) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }

        if (needed.isEmpty()) {
            Log.i(TAG, "grant: системные разрешения уже есть");
            request.grant(allowed.toArray(new String[0]));
            return;
        }

        if (!PermissionGate.shouldAsk(activity, GATE_KEY)) {
            Log.i(TAG, "deny: повторный вопрос не положен");
            request.deny();
            return;
        }
        if (pendingRequest != null) {
            pendingRequest.deny(); // не держим два запроса одновременно
        }
        pendingRequest = request;
        pendingResources = allowed.toArray(new String[0]);
        PermissionGate.markAsked(activity, GATE_KEY);
        Log.i(TAG, "спрашиваем системные разрешения: " + needed);
        activity.requestPermissions(needed.toArray(new String[0]), REQUEST_CODE);
    }

    /** Вызывается из Activity.onRequestPermissionsResult (UI-поток). */
    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode != REQUEST_CODE || pendingRequest == null) {
            return;
        }
        boolean allGranted = grantResults.length > 0;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
            }
        }
        if (!allGranted) {
            Log.w(TAG, "deny: пользователь отказал");
            pendingRequest.deny();
            pendingRequest = null;
            return;
        }

        List<String> toGrant = new ArrayList<>();
        for (String resource : pendingResources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                    && granted(Manifest.permission.CAMERA)) {
                toGrant.add(resource);
            }
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                    && granted(Manifest.permission.RECORD_AUDIO)) {
                toGrant.add(resource);
            }
        }
        if (toGrant.isEmpty()) {
            Log.w(TAG, "deny: разрешение не выдано");
            pendingRequest.deny();
        } else {
            Log.i(TAG, "grant: " + toGrant);
            pendingRequest.grant(toGrant.toArray(new String[0]));
            PermissionGate.markGranted(activity, GATE_KEY);
        }
        pendingRequest = null;
    }

    private boolean granted(String permission) {
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private static String hostOf(Uri origin) {
        if (origin == null) {
            return null;
        }
        return origin.getHost();
    }
}
