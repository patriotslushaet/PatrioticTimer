package kz.patriotic.alarm.util;

import android.graphics.PointF;
import android.os.SystemClock;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.LinkedList;

public class PoseTracker {
    private long startTimeMillis = 0;
    private long firstCorrectPoseMillis = 0;
    private float recordedAcquisitionTimeSec = 0f;

    // Stillness metrics tracking
    private final LinkedList<Float> recentJitters = new LinkedList<>();
    private PointF lastNosePos = null;
    private PointF lastLeftShoulderPos = null;
    private PointF lastRightShoulderPos = null;
    private PointF lastRightWristPos = null;

    private float cumulativeStillness = 95f;
    private int framesInCorrectPose = 0;
    private int totalFramesAnalyzed = 0;

    public void startSession() {
        startTimeMillis = SystemClock.elapsedRealtime();
        firstCorrectPoseMillis = 0;
        recordedAcquisitionTimeSec = 0f;
        recentJitters.clear();
        lastNosePos = null;
        lastLeftShoulderPos = null;
        lastRightShoulderPos = null;
        lastRightWristPos = null;
        cumulativeStillness = 95f;
        framesInCorrectPose = 0;
        totalFramesAnalyzed = 0;
    }

    public PoseAnalysisResult analyzePose(Pose pose) {
        totalFramesAnalyzed++;

        if (pose == null) {
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.SEARCHING,
                    "Камера алдына тұрыңыз",
                    "Встаньте перед камерой во весь рост или по пояс",
                    getAverageStillness(),
                    0f,
                    false
            );
        }

        PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);

        // Check if upper body is reasonably visible
        if (leftShoulder == null || rightShoulder == null ||
                leftShoulder.getInFrameLikelihood() < 0.45f || rightShoulder.getInFrameLikelihood() < 0.45f) {
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.SEARCHING,
                    "Иығыңыз анық көрінуі керек",
                    "Отойдите немного назад, чтобы плечи были в кадре",
                    getAverageStillness(),
                    0.2f,
                    false
            );
        }

        PointF pLeftShoulder = leftShoulder.getPosition();
        PointF pRightShoulder = rightShoulder.getPosition();
        float shoulderDist = (float) Math.hypot(pLeftShoulder.x - pRightShoulder.x, pLeftShoulder.y - pRightShoulder.y);

        if (shoulderDist < 40f) {
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.SEARCHING,
                    "Камераға жақындаңыз",
                    "Подойдите ближе к камере",
                    getAverageStillness(),
                    0.3f,
                    false
            );
        }

        // Check upright posture: shoulder tilt shouldn't be extreme
        float dyShoulders = Math.abs(pLeftShoulder.y - pRightShoulder.y);
        if (dyShoulders > 0.45f * shoulderDist) {
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.STAND_STRAIGHT,
                    "Тік тұрыңыз, иығыңызды теңестіріңіз",
                    "Выпрямитесь ровно, держите плечи прямо!",
                    getAverageStillness(),
                    0.5f,
                    false
            );
        }

        // Heart target point:
        // Anatomic heart is on the left side of chest (around left shoulder x, slightly lower than shoulder)
        float heartX = pLeftShoulder.x;
        float heartY = pLeftShoulder.y + (0.35f * shoulderDist);

        // Right hand on heart check:
        // In selfie mode / anatomical landmarks, either rightWrist or user's hand crosses over towards left chest
        boolean handOnHeart = false;
        float handProximity = 0f;

        if (rightWrist != null && rightWrist.getInFrameLikelihood() > 0.35f) {
            PointF pRightWrist = rightWrist.getPosition();
            float distToHeart = (float) Math.hypot(pRightWrist.x - heartX, pRightWrist.y - heartY);
            handProximity = Math.max(0f, 1f - (distToHeart / (1.1f * shoulderDist)));

            // If right wrist is within 0.7 * shoulderDist from heart
            if (distToHeart < 0.75f * shoulderDist) {
                handOnHeart = true;
            }
        }

        // Also check if left wrist was placed on heart (for ambidextrous tolerance, but guide right hand)
        if (!handOnHeart && leftWrist != null && leftWrist.getInFrameLikelihood() > 0.4f) {
            PointF pLeftWrist = leftWrist.getPosition();
            float distToHeart = (float) Math.hypot(pLeftWrist.x - heartX, pLeftWrist.y - heartY);
            if (distToHeart < 0.65f * shoulderDist) {
                handOnHeart = true;
                handProximity = 0.85f;
            }
        }

        // Calculate motion jitter for stillness
        float frameJitter = calculateMotionJitter(nose, pLeftShoulder, pRightShoulder, rightWrist, shoulderDist);
        trackStillness(frameJitter);

        if (!handOnHeart) {
            return new PoseAnalysisResult(
                    PoseAnalysisResult.State.HAND_ON_HEART,
                    "Оң қолыңызды сол жақ жүрекке қойыңыз!",
                    "Приложите правую руку к сердцу (слева на груди)!",
                    getAverageStillness(),
                    handProximity,
                    false
            );
        }

        // Correct Posture Achieved!
        framesInCorrectPose++;
        if (firstCorrectPoseMillis == 0) {
            firstCorrectPoseMillis = SystemClock.elapsedRealtime();
            if (startTimeMillis > 0) {
                recordedAcquisitionTimeSec = Math.max(0.8f, (firstCorrectPoseMillis - startTimeMillis) / 1000f);
            } else {
                recordedAcquisitionTimeSec = 2.4f;
            }
        }

        return new PoseAnalysisResult(
                PoseAnalysisResult.State.CORRECT_POSTURE,
                "ТҰРЫС ДҰРЫС! Қозғалмай тұрыңыз!",
                "ПОЗА ПРИНЯТА! Стойте смирно под звуки гимна!",
                getAverageStillness(),
                1.0f,
                true
        );
    }

    private float calculateMotionJitter(PoseLandmark nose, PointF leftShoulder, PointF rightShoulder,
                                        PoseLandmark rightWrist, float shoulderDist) {
        float jitterSum = 0f;
        int points = 0;

        if (nose != null) {
            PointF cur = nose.getPosition();
            if (lastNosePos != null) {
                jitterSum += Math.hypot(cur.x - lastNosePos.x, cur.y - lastNosePos.y);
                points++;
            }
            lastNosePos = new PointF(cur.x, cur.y);
        }

        if (lastLeftShoulderPos != null) {
            jitterSum += Math.hypot(leftShoulder.x - lastLeftShoulderPos.x, leftShoulder.y - lastLeftShoulderPos.y);
            points++;
        }
        lastLeftShoulderPos = new PointF(leftShoulder.x, leftShoulder.y);

        if (lastRightShoulderPos != null) {
            jitterSum += Math.hypot(rightShoulder.x - lastRightShoulderPos.x, rightShoulder.y - lastRightShoulderPos.y);
            points++;
        }
        lastRightShoulderPos = new PointF(rightShoulder.x, rightShoulder.y);

        if (rightWrist != null) {
            PointF cur = rightWrist.getPosition();
            if (lastRightWristPos != null) {
                jitterSum += Math.hypot(cur.x - lastRightWristPos.x, cur.y - lastRightWristPos.y);
                points++;
            }
            lastRightWristPos = new PointF(cur.x, cur.y);
        }

        if (points > 0 && shoulderDist > 0) {
            return (jitterSum / points) / shoulderDist;
        }
        return 0.01f;
    }

    private void trackStillness(float jitter) {
        recentJitters.add(jitter);
        if (recentJitters.size() > 30) {
            recentJitters.removeFirst();
        }

        // Low jitter (< 0.02) yields 98%-100% stillness
        float avgJitter = 0f;
        for (float j : recentJitters) avgJitter += j;
        avgJitter /= Math.max(1, recentJitters.size());

        float frameStillness = Math.max(70f, Math.min(100f, 100f - (avgJitter * 280f)));
        cumulativeStillness = (cumulativeStillness * 0.92f) + (frameStillness * 0.08f);
    }

    public float getAverageStillness() {
        return Math.round(cumulativeStillness * 10f) / 10f;
    }

    public float getAcquisitionTimeSec() {
        if (recordedAcquisitionTimeSec > 0f) {
            return Math.round(recordedAcquisitionTimeSec * 10f) / 10f;
        }
        if (startTimeMillis > 0) {
            float elapsed = (SystemClock.elapsedRealtime() - startTimeMillis) / 1000f;
            return Math.round(Math.min(elapsed, 4.5f) * 10f) / 10f;
        }
        return 3.2f;
    }

    public int getFramesInCorrectPose() {
        return framesInCorrectPose;
    }
}
