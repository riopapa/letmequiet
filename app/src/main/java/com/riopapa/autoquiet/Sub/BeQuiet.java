package com.riopapa.autoquiet.Sub;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

public class BeQuiet {

    public BeQuiet(Context context, boolean OnOff) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        SharedPreferences sharedPref = context.getSharedPreferences("saved", Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        if (OnOff) {
            int rVol = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            int mVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int nVol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            int sVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            int aVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            if (mVol > 5 && nVol > 5) {
                sharedEditor.putInt("ring", rVol);
                sharedEditor.putInt("music", mVol);
                sharedEditor.putInt("notify", nVol);
                sharedEditor.putInt("system", sVol);
                sharedEditor.putInt("alarm", aVol);
                sharedEditor.apply();
                sharedEditor.commit();
            }
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 2, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 2, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 2, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 2, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0);

        } else {
            int vol;
            vol = sharedPref.getInt("ring", 5);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, vol, 0);

            vol = sharedPref.getInt("music", 5);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);

            vol = sharedPref.getInt("notify", 5);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, vol, 0);

            vol = sharedPref.getInt("system", 5);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, vol, 0);

            vol = sharedPref.getInt("alarm", 5);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0);

        }
    }
}
