package kz.patriotic.alarm.util;

import android.graphics.PointF;
import android.os.SystemClock;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.LinkedList;

public class PoseTracker {
    public static final long REQUIRED_HOLD_MS = 3000L; // 3 seconds continuous hold required!

    private long sessionStartMillis = 0;
    private long currentPoseHoldStartMillis = 0;
    private boolean isPoseConfirmed = false;
    private float recordedAcquisitionTimeSec = 0f;

    // Moving average of coordinates for jitter filtering
    private PointF lastNosePos = null;
    private PointF lastLeftShoulderPos = null;
    private PointF lastRightShoulderPos = null;
    private PointF lastRightWristPos = null;

    private final LinkedList<Float> jitterHistory = new LinkedList<>();
    private float runningStillness = 97.0f;

    public void startSession() {
        sessionStartMillis = SystemClock.elapsedRealtime();
        currentPoseHoldStartMillis = 0;
        isPoseConfirmed = false;
        recordedAcquisitionTimeSec = 0f;
        jitterHistory.clear();
        lastNosePos = null;
        lastLeftShoulderPos = null;
        lastRightShoulderPos = null;
        lastRightWristPos = null;
        runningStillness = 98.0f;
    }

    public PoseAnalysisResult analyzePose(Pose pose) {
        long now = SystemClock.elapsedRealtime();

        if (pose == null) {
            currentPoseHoldStartMillis = 0;
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.SEARCHING,
                    "Камера алдына тұрыңыз",
                    "Встаньте перед фронтальной камерой",
                    runningStillness,
                    0f,
                    false,
                    0
            );
        }

        PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);

        // Check shoulder visibility
        if (leftShoulder == null || rightShoulder == null ||
                leftShoulder.getInFrameLikelihood() < 0.40f || rightShoulder.getInFrameLikelihood() < 0.40f) {
            currentPoseHoldStartMillis = 0;
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.SEARCHING,
                    "Иығыңыз толық көрінсін",
                    "Отойдите назад, чтобы плечи были в кадре",
                    runningStillness,
                    0.2f,
                    false,
                    0
            );
        }

        PointF pLeftShoulder = leftShoulder.getPosition();
        PointF pRightShoulder = rightShoulder.getPosition();
        float shoulderDist = (float) Math.hypot(pLeftShoulder.x - pRightShoulder.x, pLeftShoulder.y - pRightShoulder.y);

        if (shoulderDist < 35f) {
            currentPoseHoldStartMillis = 0;
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.SEARCHING,
                    "Камераға жақындаңыз",
                    "Подойдите ближе к камере",
                    runningStillness,
                    0.3f,
                    false,
                    0
            );
        }

        // Upright check: shoulders should be fairly horizontal
        float dyShoulders = Math.abs(pLeftShoulder.y - pRightShoulder.y);
        if (dyShoulders > 0.40f * shoulderDist) {
            currentPoseHoldStartMillis = 0;
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.STAND_STRAIGHT,
                    "Тік тұрыңыз, иықты тең ұстаңыз",
                    "Выпрямитесь ровно, держите плечи прямо!",
                    runningStillness,
                    0.4f,
                    false,
                    0
            );
        }

        // Heart target area:
        // On user's left chest (near left shoulder X, slightly below left shoulder Y)
        float heartX = pLeftShoulder.x;
        float heartY = pLeftShoulder.y + (0.35f * shoulderDist);

        boolean isHandOnHeart = false;
        float proximity = 0f;

        // Check right wrist
        if (rightWrist != null && rightWrist.getInFrameLikelihood() > 0.35f) {
            PointF pRightWrist = rightWrist.getPosition();
            float dist = (float) Math.hypot(pRightWrist.x - heartX, pRightWrist.y - heartY);
            proximity = Math.max(0f, 1f - (dist / (1.2f * shoulderDist)));
            if (dist < 0.85f * shoulderDist) {
                isHandOnHeart = true;
            }
        }

        // Fallback for mirror/left wrist tolerance
        if (!isHandOnHeart && leftWrist != null && leftWrist.getInFrameLikelihood() > 0.40f) {
            PointF pLeftWrist = leftWrist.getPosition();
            float dist = (float) Math.hypot(pLeftWrist.x - heartX, pLeftWrist.y - heartY);
            if (dist < 0.70f * shoulderDist) {
                isHandOnHeart = true;
                proximity = 0.85f;
            }
        }

        // Track stillness
        float frameJitter = calculateMotionJitter(nose, pLeftShoulder, pRightShoulder, rightWrist, shoulderDist);
        trackStillness(frameJitter);

        if (!isHandOnHeart) {
            currentPoseHoldStartMillis = 0;
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.HAND_ON_HEART,
                    "Оң қолды сол жақ жүрекке қойыңыз",
                    "Приложите правую руку к сердцу (слева на груди)!",
                    runningStillness,
                    proximity,
                    false,
                    0
            );
        }

        // User is currently in the correct posture!
        if (currentPoseHoldStartMillis == 0) {
            currentPoseHoldStartMillis = now;
        }

        long holdDurationMs = now - currentPoseHoldStartMillis;
        long remainingHoldMs = REQUIRED_HOLD_MS - holdDurationMs;

        // REQUIREMENT: Check if held for MORE THAN 3 SECONDS
        if (remainingHoldMs > 0) {
            int secondsLeft = (int) Math.ceil(remainingHoldMs / 1000.0);
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.HOLDING_POSE,
                    "Тұрысты ұстаңыз: " + secondsLeft + " сек...",
                    "Удерживайте позу: " + secondsLeft + " сек...",
                    runningStillness,
                    1.0f,
                    false,
                    secondsLeft
            );
        }

        // HELD FOR >= 3 SECONDS: OFFICIAL CONFIRMATION!
        if (!isPoseConfirmed) {
            isPoseConfirmed = true;
            if (sessionStartMillis > 0) {
                // Acquisition time is when the user reached the stance that was held
                recordedAcquisitionTimeSec = Math.max(1.0f, (currentPoseHoldStartMillis - sessionStartMillis) / 1000f);
            } else {
                recordedAcquisitionTimeSec = 3.1f;
            }
        }

        return new PoseAnalysisResult(
                PoseAnalysisResult.State.CONFIRMED_CORRECT,
                "ТҰРЫС ДҰРЫС! Қозғалмай тұрыңыз!",
                "ТҰРЫС ДҰРЫС! Поза зафиксирована! Стойте смирно!",
                runningStillness,
                1.0f,
                true,
                0
        );
    }

    private float calculateMotionJitter(PoseLandmark nose, PointF pLeftShoulder, PointF pRightShoulder,
                                        PoseLandmark rightWrist, float shoulderDist) {
        float totalDist = 0f;
        int pts = 0;

        if (nose != null) {
            PointF cur = nose.getPosition();
            if (lastNosePos != null) {
                totalDist += Math.hypot(cur.x - lastNosePos.x, cur.y - lastNosePos.y);
                pts++;
            }
            lastNosePos = new PointF(cur.x, cur.y);
        }

        if (lastLeftShoulderPos != null) {
            totalDist += Math.hypot(pLeftShoulder.x - lastLeftShoulderPos.x, pLeftShoulder.y - lastLeftShoulderPos.y);
            pts++;
        }
        lastLeftShoulderPos = new PointF(pLeftShoulder.x, pLeftShoulder.y);

        if (lastRightShoulderPos != null) {
            totalDist += Math.hypot(pRightShoulder.x - lastRightShoulderPos.x, pRightShoulder.y - lastRightShoulderPos.y);
            pts++;
        }
        lastRightShoulderPos = new PointF(pRightShoulder.x, pRightShoulder.y);

        if (rightWrist != null) {
            PointF cur = rightWrist.getPosition();
            if (lastRightWristPos != null) {
                totalDist += Math.hypot(cur.x - lastRightWristPos.x, cur.y - lastRightWristPos.y);
                pts++;
            }
            lastRightWristPos = new PointF(cur.x, cur.y);
        }

        if (pts > 0 && shoulderDist > 0) {
            return (totalDist / pts) / shoulderDist;
        }
        return 0.008f;
    }

    private void trackStillness(float jitter) {
        jitterHistory.add(jitter);
        if (jitterHistory.size() > 25) {
            jitterHistory.removeFirst();
        }

        float avgJitter = 0f;
        for (float j : jitterHistory) avgJitter += j;
        avgJitter /= Math.max(1, jitterHistory.size());

        float instantStillness = Math.max(75f, Math.min(100f, 100f - (avgJitter * 260f)));
        runningStillness = (runningStillness * 0.90f) + (instantStillness * 0.10f);
    }

    public float getAverageStillness() {
        return Math.round(runningStillness * 10f) / 10f;
    }

    public float getAcquisitionTimeSec() {
        if (recordedAcquisitionTimeSec > 0f) {
            return Math.round(recordedAcquisitionTimeSec * 10f) / 10f;
        }
        if (sessionStartMillis > 0) {
            float elapsed = (SystemClock.elapsedRealtime() - sessionStartMillis) / 1000f;
            return Math.round(Math.max(3.0f, Math.min(elapsed, 6.0f)) * 10f) / 10f;
        }
        return 3.2f;
    }

    public boolean isPoseConfirmed() {
        return isPoseConfirmed;
    }
}
