package kz.patriotic.alarm.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

import kz.patriotic.alarm.R;

public class LegalAgreementsDialog {

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(R.layout.dialog_legal_agreements);

        TextView btnAccept = dialog.findViewById(R.id.btn_accept_legal);
        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.findViewById(R.id.btn_close_legal).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
