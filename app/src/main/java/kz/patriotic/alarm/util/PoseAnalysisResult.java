package kz.patriotic.alarm.util;

public class PoseAnalysisResult {
    public enum State {
        SEARCHING,          // No clear upper body detected
        STAND_STRAIGHT,     // Body detected, but shoulders tilted / not upright
        HAND_ON_HEART,      // Upright, but hand not yet placed on left chest
        HOLDING_POSE,       // Pose assumed! Holding countdown: 3.. 2.. 1..
        CONFIRMED_CORRECT   // Held >= 3 seconds! Confirmed correct posture!
    }

    private final State state;
    private final String statusMessageKz;
    private final String statusMessageRu;
    private final float stillnessScore;
    private final float handHeartProximity;
    private final boolean isPostureCorrect;
    private final int holdSecondsRemaining;

    public PoseAnalysisResult(State state, String statusMessageKz, String statusMessageRu,
                              float stillnessScore, float handHeartProximity,
                              boolean isPostureCorrect, int holdSecondsRemaining) {
        this.state = state;
        this.statusMessageKz = statusMessageKz;
        this.statusMessageRu = statusMessageRu;
        this.stillnessScore = stillnessScore;
        this.handHeartProximity = handHeartProximity;
        this.isPostureCorrect = isPostureCorrect;
        this.holdSecondsRemaining = holdSecondsRemaining;
    }

    public State getState() { return state; }
    public String getStatusMessageKz() { return statusMessageKz; }
    public String getStatusMessageRu() { return statusMessageRu; }
    public float getStillnessScore() { return stillnessScore; }
    public float getHandHeartProximity() { return handHeartProximity; }
    public boolean isPostureCorrect() { return isPostureCorrect; }
    public int getHoldSecondsRemaining() { return holdSecondsRemaining; }
}
