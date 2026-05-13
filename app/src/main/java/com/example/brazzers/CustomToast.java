package com.example.brazzers;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Premium styled toast replacement.
 * Slides up from bottom with emoji + styled background.
 */
public class CustomToast {

    public static final int INFO = 0;
    public static final int SUCCESS = 1;
    public static final int ERROR = 2;

    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void show(Activity activity, String message, int type) {
        if (activity == null || activity.isFinishing()) return;

        handler.post(() -> {
            try {
                FrameLayout root = activity.findViewById(android.R.id.content);
                if (root == null) return;

                String emoji;
                int bgRes;
                int textColor;

                switch (type) {
                    case SUCCESS:
                        emoji = "✅";
                        bgRes = R.drawable.bg_toast_success;
                        textColor = Color.parseColor("#00D9A6");
                        break;
                    case ERROR:
                        emoji = "⚠️";
                        bgRes = R.drawable.bg_toast_error;
                        textColor = Color.parseColor("#FF5252");
                        break;
                    default:
                        emoji = "💡";
                        bgRes = R.drawable.bg_toast_info;
                        textColor = Color.parseColor("#B0B0B0");
                        break;
                }

                TextView tv = new TextView(activity);
                tv.setText(emoji + "  " + message);
                tv.setTextColor(textColor);
                tv.setTextSize(14);
                tv.setPadding(52, 28, 52, 28);
                tv.setGravity(Gravity.CENTER);
                tv.setBackgroundResource(bgRes);
                tv.setElevation(24);
                tv.setAlpha(0f);
                tv.setTranslationY(80);
                tv.setTag("custom_toast");

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.bottomMargin = 140;
                params.leftMargin = 40;
                params.rightMargin = 40;

                // Remove any existing custom toast
                for (int i = root.getChildCount() - 1; i >= 0; i--) {
                    if ("custom_toast".equals(root.getChildAt(i).getTag())) {
                        root.removeViewAt(i);
                    }
                }

                root.addView(tv, params);

                // Animate in
                tv.animate()
                        .alpha(1f)
                        .translationY(0)
                        .setDuration(250)
                        .start();

                // Auto-dismiss after 2.5s
                handler.postDelayed(() -> {
                    if (tv.getParent() != null) {
                        tv.animate()
                                .alpha(0f)
                                .translationY(40)
                                .setDuration(200)
                                .withEndAction(() -> {
                                    if (tv.getParent() != null) {
                                        ((FrameLayout) tv.getParent()).removeView(tv);
                                    }
                                })
                                .start();
                    }
                }, 2500);

            } catch (Exception ignored) {
                // Fallback silently if view attachment fails
            }
        });
    }
}
