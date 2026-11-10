package kz.patriotic.alarm.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import kz.patriotic.alarm.R;

public class AdManager {
    private static final String PREF_NAME = "patriotic_ad_prefs";
    private static final String KEY_AD_FREE = "is_ad_free_forever";
    private static int actionCounter = 0;

    public static boolean isAdFree(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_AD_FREE, false);
    }

    public static void setAdFree(Context context, boolean adFree) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_AD_FREE, adFree).apply();
    }

    /**
     * Shows an interstitial ad frequently (every 2-3 actions) unless user paid 2000 KZT to remove ads.
     */
    public static void showInterstitialAd(Activity activity, Runnable onDismiss) {
        if (activity == null || activity.isFinishing()) {
            if (onDismiss != null) onDismiss.run();
            return;
        }

        if (isAdFree(activity)) {
            if (onDismiss != null) onDismiss.run();
            return;
        }

        actionCounter++;
        // Frequent ad appearance
        Dialog adDialog = new Dialog(activity);
        adDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        adDialog.setCancelable(false);

        if (adDialog.getWindow() != null) {
            adDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Custom styled Ad Dialog
        adDialog.setContentView(R.layout.dialog_ad_interstitial);

        TextView tvTimer = adDialog.findViewById(R.id.tv_ad_timer);
        MaterialButton btnClose = adDialog.findViewById(R.id.btn_close_ad);
        MaterialButton btnRemoveAds = adDialog.findViewById(R.id.btn_remove_ads_ad);

        if (btnClose != null) {
            btnClose.setEnabled(false);
            new CountDownTimer(3000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (tvTimer != null) {
                        tvTimer.setText("Реклама (" + ((millisUntilFinished / 1000) + 1) + "с)");
                    }
                }

                @Override
                public void onFinish() {
                    if (tvTimer != null) tvTimer.setText("Реклама завершена");
                    if (btnClose != null) {
                        btnClose.setEnabled(true);
                        btnClose.setText("Жабу (Пропустить)");
                    }
                }
            }.start();

            btnClose.setOnClickListener(v -> {
                adDialog.dismiss();
                if (onDismiss != null) onDismiss.run();
            });
        }

        if (btnRemoveAds != null) {
            btnRemoveAds.setOnClickListener(v -> {
                adDialog.dismiss();
                showPurchaseRemoveAdsDialog(activity, onDismiss);
            });
        }

        adDialog.show();
    }

    /**
     * Modal to purchase ad removal for 2,000 KZT forever.
     */
    public static void showPurchaseRemoveAdsDialog(Activity activity, Runnable onPurchased) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(R.layout.dialog_remove_ads);

        dialog.findViewById(R.id.btn_confirm_remove_ads).setOnClickListener(v -> {
            setAdFree(activity, true);
            Toast.makeText(activity, "Құттықтаймыз! Реклама мәңгіге өшірілді (2 000 ₸)", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            if (onPurchased != null) onPurchased.run();
        });

        dialog.findViewById(R.id.btn_close_remove_ads).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
