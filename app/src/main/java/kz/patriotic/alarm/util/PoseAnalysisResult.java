package kz.patriotic.alarm.util;

public class PoseAnalysisResult {
    public enum State {
        SEARCHING,          // No clear person detected
        STAND_STRAIGHT,     // Person detected, but not standing upright / centered
        HAND_ON_HEART,      // Person upright, but right hand not on heart
        CORRECT_POSTURE     // Perfect! Standing straight with right hand on left chest
    }

    private final State state;
    private final String statusMessageKz;
    private final String statusMessageRu;
    private final float stillnessScore; // 0..100%
    private final float handHeartProximity; // 0..1
    private final boolean isPostureCorrect;

    public PoseAnalysisResult(State state, String statusMessageKz, String statusMessageRu,
                              float stillnessScore, float handHeartProximity, boolean isPostureCorrect) {
        this.state = state;
        this.statusMessageKz = statusMessageKz;
        this.statusMessageRu = statusMessageRu;
        this.stillnessScore = stillnessScore;
        this.handHeartProximity = handHeartProximity;
        this.isPostureCorrect = isPostureCorrect;
    }

    public State getState() { return state; }
    public String getStatusMessageKz() { return statusMessageKz; }
    public String getStatusMessageRu() { return statusMessageRu; }
    public float getStillnessScore() { return stillnessScore; }
    public float getHandHeartProximity() { return handHeartProximity; }
    public boolean isPostureCorrect() { return isPostureCorrect; }
}
