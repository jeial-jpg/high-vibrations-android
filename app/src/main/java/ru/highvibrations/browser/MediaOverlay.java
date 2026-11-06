package ru.highvibrations.browser;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;

/**
 * Кнопки управления медиа: переключение камеры и микрофон.
 *
 * Показываются только пока страница реально использует камеру
 * (об этом сообщает JS-хук через MediaBridge) и прячутся, когда поток
 * остановлен, страница сменилась или приложение ушло в фон.
 */
public final class MediaOverlay extends FrameLayout {

    private static final int BUTTON_DP = 44;
    private static final int MARGIN_DP = 16;
    private static final int GAP_DP = 10;

    private final WebView webView;
    private final ImageButton cameraButton;
    private final ImageButton micButton;
    private boolean micMuted;

    public MediaOverlay(Activity activity, WebView webView) {
        super(activity);
        this.webView = webView;

        cameraButton = createButton(activity, R.drawable.ic_camera_switch,
                "Переключить камеру");
        micButton = createButton(activity, R.drawable.ic_mic_on, "Выключить микрофон");

        addView(micButton, layoutParams(0));
        addView(cameraButton, layoutParams(BUTTON_DP + GAP_DP));

        cameraButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                run("window.__hvSwitchCamera&&window.__hvSwitchCamera()");
            }
        });
        micButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                run("window.__hvToggleMic&&window.__hvToggleMic()");
            }
        });

        setVisibility(GONE);
    }

    /** Пришло от страницы: идёт ли поток с камеры. */
    public void handleCameraActive(final boolean active) {
        post(new Runnable() {
            @Override
            public void run() {
                setVisibility(active ? VISIBLE : GONE);
            }
        });
    }

    /** Пришло от страницы: состояние микрофона. */
    public void handleMicMuted(final boolean muted) {
        post(new Runnable() {
            @Override
            public void run() {
                micMuted = muted;
                micButton.setImageResource(muted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
                micButton.setContentDescription(muted ? "Включить микрофон" : "Выключить микрофон");
            }
        });
    }

    /** Пришло от страницы: активная камера (не используется визуально, задел на будущее). */
    public void handleFacing(String facing) {
        // сейчас не требуется визуально различать фронтальную и тыловую
    }

    /** Скрыть кнопки (смена страницы, пауза приложения, диалог пароля). */
    public void hideButtons() {
        post(new Runnable() {
            @Override
            public void run() {
                setVisibility(GONE);
            }
        });
    }

    /** Спросить у страницы актуальное состояние (после возврата из фона). */
    public void refresh() {
        run("window.__hvMediaReport&&window.__hvMediaReport()");
    }

    public boolean isMicMuted() {
        return micMuted;
    }

    private void run(final String js) {
        if (webView != null) {
            webView.evaluateJavascript(js, null);
        }
    }

    private ImageButton createButton(Context context, int iconRes, String description) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setBackgroundResource(R.drawable.media_button_bg);
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setContentDescription(description);
        return button;
    }

    private LayoutParams layoutParams(int bottomOffsetDp) {
        LayoutParams params = new LayoutParams(dp(BUTTON_DP), dp(BUTTON_DP));
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.rightMargin = dp(MARGIN_DP);
        params.bottomMargin = dp(MARGIN_DP + bottomOffsetDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
