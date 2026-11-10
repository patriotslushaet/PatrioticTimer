package kz.patriotic.alarm.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.StreakTier;
import kz.patriotic.alarm.util.StreakManager;

public class CompletedActivity extends AppCompatActivity {

    private ImageView ivFlagEmblem;
    private TextView tvTitleCompleted, tvSubtitleKz;
    private CardView cardStreakResult, cardStatStillness, cardStatReaction, cardStatAnthem;
    private TextView tvStreakResult, tvTierResult;
    private TextView tvStatStillnessVal, tvStatStillnessDesc;
    private TextView tvStatReactionVal, tvStatReactionDesc;
    private TextView tvStatAnthemVal;
    private MaterialButton btnReturnMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);

        final float targetStillness = getIntent().getFloatExtra("stillness_score", 98f);
        final float targetReaction = getIntent().getFloatExtra("reaction_time", 2.8f);
        String anthemDuration = getIntent().getStringExtra("anthem_duration");
        if (anthemDuration == null) anthemDuration = "01:50";

        // Increment streak & update widgets
        StreakManager.recordCompletion(this, true);

        final int currentStreak = StreakManager.getCurrentStreak(this);
        StreakTier tier = StreakTier.getTierForDays(currentStreak);
        int missesRemaining = StreakManager.getMonthlyMissesRemaining(this);

        initViews();

        tvTierResult.setText("Ранг: " + tier.getKzTitle() + " (" + tier.getRuTitle() + ") • Доступно пропусков: " + missesRemaining + "/2");
        tvStatAnthemVal.setText(anthemDuration);

        setupTextDescriptions(targetStillness, targetReaction);

        // Run animations!
        startCelebrationAnimations(targetStillness, targetReaction, currentStreak);

        btnReturnMain.setOnClickListener(v -> returnToMain());
    }

    private void initViews() {
        ivFlagEmblem = findViewById(R.id.iv_flag_emblem);
        tvTitleCompleted = findViewById(R.id.tv_completed_title);
        tvSubtitleKz = findViewById(R.id.tv_completed_kz);
        cardStreakResult = findViewById(R.id.card_streak_result);
        cardStatStillness = findViewById(R.id.card_stat_stillness);
        cardStatReaction = findViewById(R.id.card_stat_reaction);
        cardStatAnthem = findViewById(R.id.card_stat_anthem);

        tvStreakResult = findViewById(R.id.tv_streak_result);
        tvTierResult = findViewById(R.id.tv_tier_result);
        tvStatStillnessVal = findViewById(R.id.tv_stat_stillness_val);
        tvStatStillnessDesc = findViewById(R.id.tv_stat_stillness_desc);
        tvStatReactionVal = findViewById(R.id.tv_stat_reaction_val);
        tvStatReactionDesc = findViewById(R.id.tv_stat_reaction_desc);
        tvStatAnthemVal = findViewById(R.id.tv_stat_anthem_val);
        btnReturnMain = findViewById(R.id.btn_return_main);
    }

    private void setupTextDescriptions(float stillness, float reaction) {
        if (stillness >= 95f) {
            tvStatStillnessDesc.setText("Идеальная выправка («Сап түзеу»): вы стояли как почетный караул — абсолютно смирно без лишних движений!");
        } else if (stillness >= 85f) {
            tvStatStillnessDesc.setText("Отличная патриотическая стойка: высокая неподвижность, рука уверенно лежала на сердце.");
        } else {
            tvStatStillnessDesc.setText("Хорошая стойка: завтра постарайтесь стоять еще более неподвижно до конца гимна!");
        }

        if (reaction <= 3.5f) {
            tvStatReactionDesc.setText("Молниеносная реакция патриота: вы приняли ровную стойку с рукой на сердце за считанные секунды!");
        } else if (reaction <= 6.0f) {
            tvStatReactionDesc.setText("Надежная реакция: оперативный подъем под первые аккорды государственного гимна.");
        } else {
            tvStatReactionDesc.setText("Стойка зафиксирована: стремитесь принимать позу сразу с первыми звуками гимна!");
        }
    }

    private void startCelebrationAnimations(float targetStillness, float targetReaction, int currentStreak) {
        // Initial hidden state for cards
        View[] animatedViews = new View[]{cardStreakResult, cardStatStillness, cardStatReaction, cardStatAnthem, btnReturnMain};
        for (View v : animatedViews) {
            v.setAlpha(0f);
            v.setTranslationY(80f);
        }

        // 1. Emblem bounce animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivFlagEmblem, View.SCALE_X, 0.4f, 1.15f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivFlagEmblem, View.SCALE_Y, 0.4f, 1.15f, 1.0f);
        ObjectAnimator alphaEmblem = ObjectAnimator.ofFloat(ivFlagEmblem, View.ALPHA, 0f, 1f);
        AnimatorSet emblemSet = new AnimatorSet();
        emblemSet.playTogether(scaleX, scaleY, alphaEmblem);
        emblemSet.setDuration(700);
        emblemSet.setInterpolator(new OvershootInterpolator(1.8f));
        emblemSet.start();

        // 2. Stillness count-up animation
        ValueAnimator stillnessAnim = ValueAnimator.ofFloat(0f, targetStillness);
        stillnessAnim.setDuration(1200);
        stillnessAnim.setInterpolator(new DecelerateInterpolator());
        stillnessAnim.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            tvStatStillnessVal.setText(String.format(Locale.US, "%.0f%%", val));
        });
        stillnessAnim.setStartDelay(300);
        stillnessAnim.start();

        // 3. Reaction time count-up animation
        ValueAnimator reactionAnim = ValueAnimator.ofFloat(0.0f, targetReaction);
        reactionAnim.setDuration(1000);
        reactionAnim.setInterpolator(new DecelerateInterpolator());
        reactionAnim.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            tvStatReactionVal.setText(String.format(Locale.US, "%.1f сек", val));
        });
        reactionAnim.setStartDelay(400);
        reactionAnim.start();

        // 4. Streak count-up animation
        ValueAnimator streakAnim = ValueAnimator.ofInt(Math.max(0, currentStreak - 1), currentStreak);
        streakAnim.setDuration(800);
        streakAnim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            tvStreakResult.setText("Серия: " + val + " дней подряд! (+1)");
        });
        streakAnim.setStartDelay(200);
        streakAnim.start();

        // 5. Staggered card slides
        long delay = 350;
        for (View v : animatedViews) {
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(550)
                    .setStartDelay(delay)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            delay += 120;
        }

        // Haptic feedback
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(120);
                }
            }
        } catch (Exception ignored) {}
    }

    private void returnToMain() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        returnToMain();
    }
}
