package kz.patriotic.alarm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.StreakTier;
import kz.patriotic.alarm.util.StreakManager;

public class CompletedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);

        float stillness = getIntent().getFloatExtra("stillness_score", 98f);
        float reaction = getIntent().getFloatExtra("reaction_time", 2.8f);
        String anthemDuration = getIntent().getStringExtra("anthem_duration");
        if (anthemDuration == null) anthemDuration = "01:43";

        // Increment streak & update widgets
        StreakManager.recordCompletion(this, true);

        int currentStreak = StreakManager.getCurrentStreak(this);
        StreakTier tier = StreakTier.getTierForDays(currentStreak);
        int missesRemaining = StreakManager.getMonthlyMissesRemaining(this);

        TextView tvStreakResult = findViewById(R.id.tv_streak_result);
        TextView tvTierResult = findViewById(R.id.tv_tier_result);
        TextView tvStatStillnessVal = findViewById(R.id.tv_stat_stillness_val);
        TextView tvStatStillnessDesc = findViewById(R.id.tv_stat_stillness_desc);
        TextView tvStatReactionVal = findViewById(R.id.tv_stat_reaction_val);
        TextView tvStatReactionDesc = findViewById(R.id.tv_stat_reaction_desc);
        TextView tvStatAnthemVal = findViewById(R.id.tv_stat_anthem_val);

        tvStreakResult.setText("Серия продлена: " + currentStreak + " дней подряд!");
        tvTierResult.setText("Ранг: " + tier.getKzTitle() + " (" + tier.getRuTitle() + ") • " + missesRemaining + "/2 пропусков доступно");

        tvStatStillnessVal.setText(String.format(Locale.US, "%.0f%%", stillness));
        if (stillness >= 95f) {
            tvStatStillnessDesc.setText("Идеальная дисциплина («Сап түзеу»): вы стояли как почетный караул — абсолютно смирно без лишних движений!");
        } else if (stillness >= 85f) {
            tvStatStillnessDesc.setText("Отличная патриотическая стойка: высокая неподвижность, рука уверенно держалась на сердце.");
        } else {
            tvStatStillnessDesc.setText("Хорошая стойка: завтра постарайтесь стоять еще более неподвижно до финальной ноты гимна!");
        }

        tvStatReactionVal.setText(String.format(Locale.US, "%.1f сек", reaction));
        if (reaction <= 3.5f) {
            tvStatReactionDesc.setText("Молниеносная реакция патриота: вы встали прямо и приложили руку к сердцу за считанные секунды!");
        } else if (reaction <= 6.0f) {
            tvStatReactionDesc.setText("Хорошая реакция: подъем выполнен вовремя, стойка принята оперативно.");
        } else {
            tvStatReactionDesc.setText("Стойка принята: старайтесь вставать в позу с первыми аккордами гимна!");
        }

        tvStatAnthemVal.setText(anthemDuration);

        MaterialButton btnReturn = findViewById(R.id.btn_return_main);
        btnReturn.setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
        finish();
    }
}
