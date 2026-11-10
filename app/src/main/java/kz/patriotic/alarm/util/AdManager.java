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
     * Triggers frequent ads on key actions (e.g. every 2-3 tab switches, edits, actions)
     */
    public static void triggerFrequentAd(Activity activity, Runnable onDismiss) {
        if (activity == null || activity.isFinishing()) {
            if (onDismiss != null) onDismiss.run();
            return;
        }

        if (isAdFree(activity)) {
            if (onDismiss != null) onDismiss.run();
            return;
        }

        actionCounter++;
        if (actionCounter % 2 == 0 || actionCounter % 3 == 0) {
            showInterstitialAd(activity, onDismiss);
        } else {
            if (onDismiss != null) onDismiss.run();
        }
    }

    /**
     * Shows an interstitial ad frequently with a countdown timer.
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

        Dialog adDialog = new Dialog(activity);
        adDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        adDialog.setCancelable(false);

        if (adDialog.getWindow() != null) {
            adDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        adDialog.setContentView(R.layout.dialog_ad_interstitial);

        boolean isKz = LanguageProgressionManager.isKazakh(activity);

        TextView tvTimer = adDialog.findViewById(R.id.tv_ad_timer);
        MaterialButton btnClose = adDialog.findViewById(R.id.btn_close_ad);
        MaterialButton btnRemoveAds = adDialog.findViewById(R.id.btn_remove_ads_ad);

        if (btnClose != null) {
            btnClose.setEnabled(false);
            new CountDownTimer(4000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int sec = (int) ((millisUntilFinished / 1000) + 1);
                    if (tvTimer != null) {
                        tvTimer.setText(isKz ? ("Жарнама (" + sec + "с)") : ("Реклама (" + sec + "с)"));
                    }
                }

                @Override
                public void onFinish() {
                    if (tvTimer != null) {
                        tvTimer.setText(isKz ? "Жарнама аяқталды" : "Реклама завершена");
                    }
                    if (btnClose != null) {
                        btnClose.setEnabled(true);
                        btnClose.setText(isKz ? "Жабу (Өткізіп жіберу)" : "Закрыть (Пропустить)");
                    }
                }
            }.start();

            btnClose.setOnClickListener(v -> {
                adDialog.dismiss();
                if (onDismiss != null) onDismiss.run();
            });
        }

        if (btnRemoveAds != null) {
            btnRemoveAds.setText(isKz ? "Жарнаманы өшіру (2 000 ₸)" : "Отключить рекламу (2 000 ₸)");
            btnRemoveAds.setOnClickListener(v -> {
                adDialog.dismiss();
                showPurchaseRemoveAdsDialog(activity, onDismiss);
            });
        }

        adDialog.show();
    }

    /**
     * Requirement: Restoration of misses 2/2 or 1/2 costs 2 mandatory ad views!
     */
    public static void showMandatoryDoubleAdForMissRestore(Activity activity, Runnable onComplete) {
        if (activity == null || activity.isFinishing()) return;

        boolean isKz = LanguageProgressionManager.isKazakh(activity);

        // First mandatory ad (1 of 2)
        playMandatoryAd(activity, 1, 2, () -> {
            // Second mandatory ad (2 of 2)
            playMandatoryAd(activity, 2, 2, () -> {
                // Success: restore monthly misses to 2/2
                StreakManager.restoreMonthlyMisses(activity);
                String msg = isKz ?
                        "Құттықтаймыз! 2 міндетті жарнама қаралды. Серия өткізіп алу запасы 2/2 толық қалпына келтірілді! 🇰🇿" :
                        "Поздравляем! Просмотрено 2 обязательных рекламы. Запас пропусков 2/2 успешно восстановлен! 🇰🇿";
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                if (onComplete != null) onComplete.run();
            });
        });
    }

    private static void playMandatoryAd(Activity activity, int currentNum, int totalNum, Runnable onDone) {
        Dialog adDialog = new Dialog(activity);
        adDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        adDialog.setCancelable(false);

        if (adDialog.getWindow() != null) {
            adDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        adDialog.setContentView(R.layout.dialog_ad_interstitial);

        boolean isKz = LanguageProgressionManager.isKazakh(activity);

        TextView tvTimer = adDialog.findViewById(R.id.tv_ad_timer);
        MaterialButton btnClose = adDialog.findViewById(R.id.btn_close_ad);

        if (btnClose != null) {
            btnClose.setEnabled(false);
            new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int sec = (int) ((millisUntilFinished / 1000) + 1);
                    if (tvTimer != null) {
                        String title = isKz ?
                                ("Міндетті жарнама " + currentNum + "/" + totalNum + " (" + sec + "с)") :
                                ("Обязательная реклама " + currentNum + "/" + totalNum + " (" + sec + "с)");
                        tvTimer.setText(title);
                    }
                }

                @Override
                public void onFinish() {
                    if (tvTimer != null) {
                        tvTimer.setText(isKz ? ("Жарнама " + currentNum + "/" + totalNum + " аяқталды ✓") : ("Реклама " + currentNum + "/" + totalNum + " завершена ✓"));
                    }
                    if (btnClose != null) {
                        btnClose.setEnabled(true);
                        btnClose.setText(isKz ? "Жалғастыру →" : "Продолжить →");
                    }
                }
            }.start();

            btnClose.setOnClickListener(v -> {
                adDialog.dismiss();
                if (onDone != null) onDone.run();
            });
        }

        adDialog.show();
    }

    /**
     * Requirement: Restoration of streak costs 500 KZT!
     */
    public static void showPurchaseStreakRestoreDialog(Activity activity, Runnable onPurchased) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(R.layout.dialog_streak_restore_paid);

        boolean isKz = LanguageProgressionManager.isKazakh(activity);

        TextView tvTitle = dialog.findViewById(R.id.tv_restore_title);
        TextView tvDesc = dialog.findViewById(R.id.tv_restore_desc);
        TextView tvPerk = dialog.findViewById(R.id.tv_restore_perk);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm_restore_paid);
        MaterialButton btnClose = dialog.findViewById(R.id.btn_close_restore_paid);

        if (tvTitle != null) {
            tvTitle.setText(isKz ? "Серияны қалпына келтіру" : "Восстановление серии");
        }
        if (tvDesc != null) {
            tvDesc.setText(isKz ?
                    "500 ₸ төлем арқылы сіздің патриоттық серияңыз толық сақталып, айлық 2/2 өткізіп алу запасы толық қайтарылады!" :
                    "За 500 ₸ ваша патриотическая серия будет сохранена, а запас пропусков в этом месяце вернется на 2/2!");
        }
        if (tvPerk != null) {
            tvPerk.setText(isKz ?
                    "Серия жоғалмайды • 2/2 запас беріледі" :
                    "Серия сохраняется • Дается запас 2/2");
        }
        if (btnConfirm != null) {
            btnConfirm.setText(isKz ?
                    "500 ₸ төлеу және қалпына келтіру" :
                    "Оплатить 500 ₸ и восстановить");
            btnConfirm.setOnClickListener(v -> {
                StreakManager.restoreStreakPaid(activity);
                String toastMsg = isKz ?
                        "Құттықтаймыз! 500 ₸ төленді, серия және 2/2 запас қалпына келтірілді! 🇰🇿" :
                        "Поздравляем! Оплачено 500 ₸, серия и запас 2/2 успешно восстановлены! 🇰🇿";
                Toast.makeText(activity, toastMsg, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                if (onPurchased != null) onPurchased.run();
            });
        }

        if (btnClose != null) {
            btnClose.setText(isKz ? "Жабу (Бас тарту)" : "Закрыть (Отмена)");
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
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

        boolean isKz = LanguageProgressionManager.isKazakh(activity);

        TextView tvTitle = dialog.findViewById(R.id.tv_remove_ads_title);
        TextView tvDesc = dialog.findViewById(R.id.tv_remove_ads_desc);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm_remove_ads);
        MaterialButton btnClose = dialog.findViewById(R.id.btn_close_remove_ads);

        if (tvTitle != null) {
            tvTitle.setText(isKz ? "Жарнаманы мәңгіге өшіру (2 000 ₸)" : "Отключить рекламу навсегда (2 000 ₸)");
        }
        if (tvDesc != null) {
            tvDesc.setText(isKz ?
                    "Барлық жарнамаларды өшіріп, патриоттық оятарды таза әрі ыңғайлы пайдаланыңыз!" :
                    "Отключите всю рекламу и пользуйтесь патриотическим будильником с максимальным комфортом!");
        }
        if (btnConfirm != null) {
            btnConfirm.setText(isKz ? "Өшіру — 2 000 ₸" : "Отключить навсегда — 2 000 ₸");
            btnConfirm.setOnClickListener(v -> {
                setAdFree(activity, true);
                String toastMsg = isKz ?
                        "Құттықтаймыз! Жарнама мәңгіге өшірілді (2 000 ₸) ✓" :
                        "Поздравляем! Реклама отключена навсегда (2 000 ₸) ✓";
                Toast.makeText(activity, toastMsg, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                if (onPurchased != null) onPurchased.run();
            });
        }
        if (btnClose != null) {
            btnClose.setText(isKz ? "Жабу" : "Закрыть");
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
