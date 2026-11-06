package ru.highvibrations.browser;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Красивый диалог в стиле сайта: белая карточка, золотая рамка,
 * лепестки, замок, две кнопки. Заменяет системный AlertDialog.
 */
public final class PasswordDialog {

    /** Нажали основную кнопку (Сохранить / Обновить). */
    public interface OnAccept {
        void onAccept();
    }

    private PasswordDialog() {
    }

    public static void show(Activity activity, String title, String site, String body,
                            String okText, String cancelText, final OnAccept onAccept) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_password);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        TextView titleView = (TextView) dialog.findViewById(R.id.dialog_title);
        TextView siteView = (TextView) dialog.findViewById(R.id.dialog_site);
        TextView bodyView = (TextView) dialog.findViewById(R.id.dialog_body);
        TextView okView = (TextView) dialog.findViewById(R.id.dialog_ok);
        TextView cancelView = (TextView) dialog.findViewById(R.id.dialog_cancel);

        titleView.setText(title);
        siteView.setText(site);
        bodyView.setText(body);
        okView.setText(okText);
        cancelView.setText(cancelText);

        okView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (onAccept != null) {
                    onAccept.onAccept();
                }
            }
        });
        cancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f);
            params.dimAmount = 0.65f;
            window.setAttributes(params);
        }
        dialog.show();
    }
}
