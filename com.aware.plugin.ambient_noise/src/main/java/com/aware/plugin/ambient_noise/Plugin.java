package com.aware.plugin.ambient_noise;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

public class Plugin extends Aware_Plugin {

    /**
     * Broadcasted with sound frequency value <br/>
     * Extra: sound_frequency, in Hz
     */
    public static final String ACTION_AWARE_PLUGIN_AMBIENT_NOISE = "ACTION_AWARE_PLUGIN_AMBIENT_NOISE";

    /**
     * Extra for ACTION_AWARE_PLUGIN_AMBIENT_NOISE
     * (double) sound frequency in Hz
     */
    public static final String EXTRA_SOUND_FREQUENCY = "sound_frequency";

    /**
     * Extra for ACTION_AWARE_PLUGIN_AMBIENT_NOISE
     * (boolean) true/false if silent
     */
    public static final String EXTRA_IS_SILENT = "is_silent";

    /**
     * Extra for ACTION_AWARE_PLUGIN_AMBIENT_NOISE
     * (double) sound noise in dB
     */
    public static final String EXTRA_SOUND_DB = "sound_db";

    /**
     * Extra for ACTION_AWARE_PLUGIN_AMBIENT_NOISE
     * (double) sound RMS
     */
    public static final String EXTRA_SOUND_RMS = "sound_rms";

    public static final String SCHEDULER_PLUGIN_AMBIENT_NOISE = "SCHEDULER_PLUGIN_AMBIENT_NOISE";

    //AWARE context producer
    public static ContextProducer context_producer;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::Ambient Noise";

        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Intent context_ambient_noise = new Intent();
                context_ambient_noise.setAction(ACTION_AWARE_PLUGIN_AMBIENT_NOISE);
                context_ambient_noise.putExtra(EXTRA_SOUND_FREQUENCY, AudioAnalyser.sound_frequency);
                context_ambient_noise.putExtra(EXTRA_SOUND_DB, AudioAnalyser.sound_db);
                context_ambient_noise.putExtra(EXTRA_SOUND_RMS, AudioAnalyser.sound_rms);
                context_ambient_noise.putExtra(EXTRA_IS_SILENT, AudioAnalyser.is_silent);
                sendBroadcast(context_ambient_noise);
            }
        };
        context_producer = CONTEXT_PRODUCER;

        REQUIRED_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{AmbientNoise_Data.CONTENT_URI};
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean permissions_ok = true;
        for(String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIENT_NOISE, true);

            if (Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE, 5);
            }
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SAMPLE_SIZE).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SAMPLE_SIZE, 30);
            }
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SILENCE_THRESHOLD).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SILENCE_THRESHOLD, 50);
            }

            try {
                Scheduler.Schedule audioSampler = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_AMBIENT_NOISE);
                if (audioSampler == null) {
                    audioSampler = new Scheduler.Schedule(SCHEDULER_PLUGIN_AMBIENT_NOISE)
                            .setInterval(Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE)))
                            .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                            .setActionClass(getPackageName() + "/" + AudioAnalyser.class.getName());
                    Scheduler.saveSchedule(this, audioSampler);
                } else {
                    //Check if there is a change on the sampling interval
                    if (audioSampler.getInterval() != Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE))) {
                        audioSampler = new Scheduler.Schedule(SCHEDULER_PLUGIN_AMBIENT_NOISE)
                                .setInterval(Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE)))
                                .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                                .setActionClass(getPackageName() + "/" + AudioAnalyser.class.getName());
                        Scheduler.saveSchedule(this, audioSampler);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Scheduler.removeSchedule(this, SCHEDULER_PLUGIN_AMBIENT_NOISE);

        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIENT_NOISE, false);
        Aware.stopAWARE(this);
    }
}
