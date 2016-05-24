package com.aware.plugin.ambient_noise;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.aware.Aware;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class AudioAnalysis {

    private static Context context;
    private static short[] audio_data;
    private static double audio_elapsed;

    public AudioAnalysis(Context c, short[] audio, double elapsed) {
        context = c;
        audio_data = audio;
        audio_elapsed = elapsed;
    }

    /**
     * Get sample Root Mean Squares value. Used to detect speech.
     *
     * @return RMS value
     */
    public double getRMS() {
        double sum = 0d;
        for (short data : audio_data) {
            sum += data;
        }
        double average = sum / audio_data.length;
        double sumMeanSquare = 0d;
        for (short data : audio_data) {
            sumMeanSquare += Math.pow(data - average, 2d);
        }
        double averageMeanSquare = sumMeanSquare / audio_data.length;
        return Math.sqrt(averageMeanSquare);
    }

    public boolean isSilent(double db) {
        double threshold = Double.valueOf(Aware.getSetting(context, Settings.PLUGIN_AMBIENT_NOISE_SILENCE_THRESHOLD));
        return (db <= threshold);
    }

    public float getFrequency() {
        int numSamples = audio_data.length;
        int numCrossing = 0;
        for (int p = 0; p < numSamples - 1; p++) {
            if ((audio_data[p] > 0 && audio_data[p + 1] <= 0) || (audio_data[p] < 0 && audio_data[p + 1] >= 0)) {
                numCrossing++;
            }
        }
        float numSecondsRecorded = (float)numSamples/(float)44100;
        float numCycles = numCrossing / 2;
        return numCycles / (float)numSecondsRecorded;
    }

    /**
     * Relative ambient noise in dB
     *
     * @return dB level
     */
    public double getdB() {
        if (audio_data.length == 0) return 0;
        double amplitude = -1;
        for (short data : audio_data) {
            if (amplitude < data) {
                amplitude = data;
            }
        }
        return Math.abs(20 * Math.log10(amplitude / 32768.0));
    }
}
