package kz.patriotic.alarm.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import kz.patriotic.alarm.R;

public class TokayevVideoActivity extends AppCompatActivity {
    private static final String TAG = "TokayevVideoActivity";

    private VideoView videoView;
    private MediaPlayer musicPlayer;
    private float stillnessScore = 98f;
    private float reactionTime = 3.2f;
    private String anthemDuration = "01:43";
    private boolean isTransitioning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokayev_video);

        Intent intent = getIntent();
        if (intent != null) {
            stillnessScore = intent.getFloatExtra("stillness_score", 98f);
            reactionTime = intent.getFloatExtra("reaction_time", 3.2f);
            if (intent.hasExtra("anthem_duration")) {
                anthemDuration = intent.getStringExtra("anthem_duration");
            }
        }

        videoView = findViewById(R.id.video_tokayev);
        MaterialButton btnFinish = findViewById(R.id.btn_finish_video);

        btnFinish.setOnClickListener(v -> proceedToCompletedScreen());

        setupVideoAndMusic();
    }

    private void setupVideoAndMusic() {
        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.tokayev_video);
            videoView.setVideoURI(videoUri);

            // Music from project: you-cant-rush-perfection
            musicPlayer = MediaPlayer.create(this, R.raw.tokayev_music);
            if (musicPlayer != null) {
                musicPlayer.setOnCompletionListener(mp -> proceedToCompletedScreen());
            }

            // Loop the short Tokayev video clip smoothly while the music plays
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                // Mute the video's original brief track so the official project music takes full focus
                mp.setVolume(0f, 0f);
                videoView.start();

                // Start music simultaneously from 0:00!
                if (musicPlayer != null && !musicPlayer.isPlaying()) {
                    musicPlayer.start();
                }
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Video error: " + what + ", " + extra);
                if (musicPlayer != null && !musicPlayer.isPlaying()) {
                    musicPlayer.start();
                }
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed setting up video or music: " + e.getMessage());
        }
    }

    private synchronized void proceedToCompletedScreen() {
        if (isTransitioning) return;
        isTransitioning = true;

        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) musicPlayer.stop();
                musicPlayer.release();
            } catch (Exception ignored) {}
            musicPlayer = null;
        }

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
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) musicPlayer.stop();
                musicPlayer.release();
            } catch (Exception ignored) {}
        }
        if (videoView != null) {
            try { videoView.stopPlayback(); } catch (Exception ignored) {}
        }
    }
}
