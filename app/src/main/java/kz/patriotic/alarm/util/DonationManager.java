package kz.patriotic.alarm.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import kz.patriotic.alarm.R;

public class DonationManager {

    public static void showDonationDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(R.layout.dialog_donation);

        boolean isKz = LanguageProgressionManager.isKazakh(activity);

        TextView tvTitle = dialog.findViewById(R.id.tv_donation_title);
        TextView tvSubtitle = dialog.findViewById(R.id.tv_donation_subtitle);
        TextView btnClose = dialog.findViewById(R.id.btn_close_donation);

        if (tvTitle != null) {
            tvTitle.setText(isKz ? "Әзірлеушіні қолдау" : "Поддержать разработчика");
        }
        if (tvSubtitle != null) {
            tvSubtitle.setText(isKz ?
                    "Қазақстанның отандық патриоттық оятарын дамытуға үлес қосыңыз!" :
                    "Поддержите развитие казахстанского патриотического будильника!");
        }
        if (btnClose != null) {
            btnClose.setText(isKz ? "Жабу" : "Закрыть");
        }

        int[] buttonIds = new int[]{
                R.id.btn_donate_100,
                R.id.btn_donate_200,
                R.id.btn_donate_500,
                R.id.btn_donate_1000,
                R.id.btn_donate_2000,
                R.id.btn_donate_5000,
                R.id.btn_donate_10000
        };

        int[] amounts = new int[]{100, 200, 500, 1000, 2000, 5000, 10000};

        for (int i = 0; i < buttonIds.length; i++) {
            final int amount = amounts[i];
            TextView btn = dialog.findViewById(buttonIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    String msg = isKz ?
                            ("Үлкен рақмет! Сіз отандық жобаны " + amount + " ₸ көлемінде қолдадыңыз! 🇰🇿") :
                            ("Огромное спасибо! Вы поддержали проект на " + amount + " ₸! 🇰🇿");
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
            }
        }

        dialog.findViewById(R.id.btn_close_donation).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
