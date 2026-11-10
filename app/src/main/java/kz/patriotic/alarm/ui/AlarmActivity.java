package kz.patriotic.alarm.ui;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.util.PoseAnalysisResult;
import kz.patriotic.alarm.util.PoseTracker;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "AlarmActivity";

    private PreviewView cameraPreviewView;
    private TextView tvPoseStatus, tvLiveStillness, tvLiveReaction, tvAnthemTimer;
    private ProgressBar progressAnthem;
    private ImageView ivHeartTarget, ivSilhouette;

    private MediaPlayer anthemPlayer;
    private CountDownTimer anthemTimer;
    private PoseDetector poseDetector;
    private ExecutorService cameraExecutor;
    private final PoseTracker poseTracker = new PoseTracker();

    private boolean isFinished = false;
    private int anthemDurationTotalMs = 103000; // ~1:43

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn screen on and show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_alarm);

        initViews();
        initPoseDetector();
        startCamera();
        startAnthemPlayback();
    }

    private void initViews() {
        cameraPreviewView = findViewById(R.id.camera_preview);
        tvPoseStatus = findViewById(R.id.tv_pose_status);
        tvLiveStillness = findViewById(R.id.tv_live_stillness);
        tvLiveReaction = findViewById(R.id.tv_live_reaction);
        tvAnthemTimer = findViewById(R.id.tv_anthem_timer);
        progressAnthem = findViewById(R.id.progress_anthem);
        ivHeartTarget = findViewById(R.id.iv_heart_target);
        ivSilhouette = findViewById(R.id.iv_silhouette);

        findViewById(R.id.btn_skip_test).setOnClickListener(v -> {
            Toast.makeText(this, "Переход к видео Токаева...", Toast.LENGTH_SHORT).show();
            proceedToTokayevVideo();
        });

        poseTracker.startSession();
    }

    private void initPoseDetector() {
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImageFrame);

                // Use Front-Facing Selfie Camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Failed to bind camera: " + e.getMessage());
                // Fallback simulation in case front camera isn't accessible on emulator
                startSimulationFallback();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImageFrame(@NonNull ImageProxy imageProxy) {
        if (isFinished) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        PoseAnalysisResult result = poseTracker.analyzePose(pose);
                        runOnUiThread(() -> updatePoseFeedback(result));
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Pose detection error: " + e.getMessage()))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void updatePoseFeedback(PoseAnalysisResult result) {
        if (isFinished) return;

        tvPoseStatus.setText(result.getStatusMessageRu());

        switch (result.getState()) {
            case SEARCHING:
                tvPoseStatus.setBackgroundResource(R.drawable.bg_btn_kazakh_blue);
                ivHeartTarget.setVisibility(View.GONE);
                break;
            case STAND_STRAIGHT:
                tvPoseStatus.setBackgroundResource(R.drawable.bg_pill_ineligible);
                ivHeartTarget.setVisibility(View.GONE);
                break;
            case HAND_ON_HEART:
                tvPoseStatus.setBackgroundResource(R.drawable.bg_pill_ineligible);
                ivHeartTarget.setVisibility(View.VISIBLE);
                break;
            case CORRECT_POSTURE:
                tvPoseStatus.setBackgroundResource(R.drawable.bg_btn_kazakh_gold);
                tvPoseStatus.setTextColor(0xFF071A2C);
                ivHeartTarget.setVisibility(View.VISIBLE);
                break;
        }

        tvLiveStillness.setText(String.format(Locale.US, "%.0f%%", result.getStillnessScore()));
        tvLiveReaction.setText(String.format(Locale.US, "%.1f сек", poseTracker.getAcquisitionTimeSec()));
    }

    private void startAnthemPlayback() {
        try {
            anthemPlayer = MediaPlayer.create(this, R.raw.kazakhstan_anthem);
            if (anthemPlayer != null) {
                anthemPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                anthemDurationTotalMs = Math.max(10000, anthemPlayer.getDuration());
                anthemPlayer.setOnCompletionListener(mp -> proceedToTokayevVideo());
                anthemPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Anthem playback failed: " + e.getMessage());
        }

        // Anthem timer & progress countdown
        anthemTimer = new CountDownTimer(anthemDurationTotalMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedMs = anthemDurationTotalMs - millisUntilFinished;
                int progress = (int) ((elapsedMs * 100) / anthemDurationTotalMs);
                progressAnthem.setProgress(progress);

                int secondsLeft = (int) (millisUntilFinished / 1000);
                int mins = secondsLeft / 60;
                int secs = secondsLeft % 60;
                tvAnthemTimer.setText(String.format(Locale.US, "Гимн Казахстана (%02d:%02d)", mins, secs));
            }

            @Override
            public void onFinish() {
                proceedToTokayevVideo();
            }
        }.start();
    }

    private void startSimulationFallback() {
        // Fallback simulation timer if camera hardware is unavailable
        new CountDownTimer(10000, 1000) {
            int step = 0;
            @Override
            public void onTick(long l) {
                step++;
                if (step == 2) {
                    updatePoseFeedback(new PoseAnalysisResult(
                            PoseAnalysisResult.State.HAND_ON_HEART,
                            "Қолыңызды жүрекке қойыңыз",
                            "Приложите правую руку к сердцу!",
                            96f, 0.7f, false
                    ));
                } else if (step >= 4) {
                    updatePoseFeedback(new PoseAnalysisResult(
                            PoseAnalysisResult.State.CORRECT_POSTURE,
                            "ТҰРЫС ДҰРЫС!",
                            "ПОЗА ПРИНЯТА! Стойте смирно под звуки гимна!",
                            98f, 1f, true
                    ));
                }
            }
            @Override
            public void onFinish() {}
        }.start();
    }

    private synchronized void proceedToTokayevVideo() {
        if (isFinished) return;
        isFinished = true;

        if (anthemTimer != null) anthemTimer.cancel();
        if (anthemPlayer != null) {
            try {
                if (anthemPlayer.isPlaying()) anthemPlayer.stop();
                anthemPlayer.release();
            } catch (Exception ignored) {}
            anthemPlayer = null;
        }

        float finalStillness = poseTracker.getAverageStillness();
        float finalReaction = poseTracker.getAcquisitionTimeSec();

        Intent intent = new Intent(this, TokayevVideoActivity.class);
        intent.putExtra("stillness_score", finalStillness);
        intent.putExtra("reaction_time", finalReaction);
        intent.putExtra("anthem_duration", "01:43");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (anthemTimer != null) anthemTimer.cancel();
        if (anthemPlayer != null) {
            try {
                if (anthemPlayer.isPlaying()) anthemPlayer.stop();
                anthemPlayer.release();
            } catch (Exception ignored) {}
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (poseDetector != null) {
            poseDetector.close();
        }
    }
}
