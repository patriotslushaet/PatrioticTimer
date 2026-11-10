package kz.patriotic.alarm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.service.AlarmService;
import kz.patriotic.alarm.util.PatrioticMusicManager;

public class TokayevVideoActivity extends AppCompatActivity {
    private static final String TAG = "TokayevVideoActivity";

    private VideoView videoView;
    private float stillnessScore = 98f;
    private float reactionTime = 3.2f;
    private String anthemDuration = "01:50";
    private boolean isTransitioning = false;

    private int playbackIteration = 0;
    private static final int MAX_ITERATIONS = 2; // Plays through once, repeats ONCE (total 2 times)

    private final Handler autoFinishHandler = new Handler(Looper.getMainLooper());
    private Runnable autoFinishRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokayev_video);

        AlarmService.stopAlarm(this);

        Intent intent = getIntent();
        if (intent != null) {
            stillnessScore = intent.getFloatExtra("stillness_score", 98f);
            reactionTime = intent.getFloatExtra("reaction_time", 3.2f);
            if (intent.hasExtra("anthem_duration")) {
                anthemDuration = intent.getStringExtra("anthem_duration");
            }
        }

        videoView = findViewById(R.id.video_tokayev);

        // Hide skip/finish button - automatic transition after the video repeats once
        View btnFinish = findViewById(R.id.btn_finish_video);
        if (btnFinish != null) {
            btnFinish.setVisibility(View.GONE);
        }

        setupVideoAndMusic();
    }

    private void setupVideoAndMusic() {
        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.tokayev_video);
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(false); // We control the repeat count manually
                mp.setVolume(0f, 0f); // Mute short video audio so music takes focus

                playbackIteration = 1;
                videoView.start();

                // Start patriotic music simultaneously from 0:00!
                // It will seamlessly continue through to the completed statistics screen.
                PatrioticMusicManager.startTokayevMusic(this);

                // Safety fallback timer (~3.6 seconds = 1.67s * 2 + buffer)
                int videoDuration = videoView.getDuration();
                long totalPlayMs = (videoDuration > 0) ? (long) videoDuration * MAX_ITERATIONS + 200L : 3600L;

                autoFinishRunnable = this::proceedToCompletedScreen;
                autoFinishHandler.postDelayed(autoFinishRunnable, totalPlayMs);
            });

            videoView.setOnCompletionListener(mp -> {
                playbackIteration++;
                if (playbackIteration < MAX_ITERATIONS) {
                    // Repeat once!
                    videoView.seekTo(0);
                    videoView.start();
                } else {
                    // Completed exactly 1 repeat! Proceed to statistics
                    proceedToCompletedScreen();
                }
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Video error: " + what + ", " + extra);
                autoFinishHandler.postDelayed(this::proceedToCompletedScreen, 2500);
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed setting up video or music: " + e.getMessage());
            proceedToCompletedScreen();
        }
    }

    private synchronized void proceedToCompletedScreen() {
        if (isTransitioning) return;
        isTransitioning = true;

        if (autoFinishRunnable != null) {
            autoFinishHandler.removeCallbacks(autoFinishRunnable);
        }

        // Keep music playing! Do NOT stop music here, it overlays onto the statistics screen.
        if (videoView != null) {
            try { videoView.stopPlayback(); } catch (Exception ignored) {}
        }

        Intent intent = new Intent(this, CompletedActivity.class);
        intent.putExtra("stillness_score", stillnessScore);
        intent.putExtra("reaction_time", reactionTime);
        intent.putExtra("anthem_duration", anthemDuration);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoFinishRunnable != null) {
            autoFinishHandler.removeCallbacks(autoFinishRunnable);
        }
        if (videoView != null) {
            try { videoView.stopPlayback(); } catch (Exception ignored) {}
        }
    }
}
