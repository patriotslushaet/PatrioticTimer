package kz.patriotic.alarm.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

import kz.patriotic.alarm.R;

/**
 * Manages background playback of the Tokayev music (you-cant-rush-perfection slowed)
 * continuously transitioning from TokayevVideoActivity directly into CompletedActivity
 * (statistics screen) without interruption or cut-off.
 */
public class PatrioticMusicManager {
    private static final String TAG = "PatrioticMusicManager";
    private static MediaPlayer musicPlayer;

    public static synchronized void startTokayevMusic(Context context) {
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) {
                    return; // Already playing continuously!
                }
            } catch (Exception ignored) {}
        }

        try {
            stopMusic();
            musicPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.tokayev_music);
            if (musicPlayer != null) {
                musicPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                musicPlayer.setLooping(true); // Loops so it gracefully continues through stats screen
                musicPlayer.start();
                Log.d(TAG, "Tokayev music started successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Tokayev music: " + e.getMessage());
        }
    }

    public static synchronized boolean isPlaying() {
        try {
            return musicPlayer != null && musicPlayer.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized void stopMusic() {
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                }
                musicPlayer.release();
            } catch (Exception ignored) {}
            musicPlayer = null;
            Log.d(TAG, "Tokayev music stopped");
        }
    }
}
