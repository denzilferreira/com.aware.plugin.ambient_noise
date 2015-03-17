package com.aware.plugin.ambient_noise;

import android.content.Context;
import android.database.Cursor;

import com.aware.Aware;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


public class AudioAnalysis {
	
	private Context context;
	private double[] magnitudes;
	private short[] audio_data;
	private int buffer_size;
	
	public AudioAnalysis(Context c, short[] audio, int buffer) {
		context = c;
		audio_data = audio;
		buffer_size = buffer;
	}
	
	/**
	 * Get sample Root-mean-squares value. Used to detect silence.
	 * @return
	 */
    public double getRMS() {
        if( audio_data.length == 0) return 0;

        double sum = 0d;
        for(int i=0; i<audio_data.length; i++ ) {
            sum += audio_data[i];
        }

        double average = sum/audio_data.length;

        double sumMeanSquare = 0d;
        for(int i=0; i< audio_data[i]; i++) {
            sumMeanSquare += Math.pow(audio_data[i]-average, 2d);
        }
        double averageMeanSquare = sumMeanSquare/audio_data.length;
        return Math.sqrt(averageMeanSquare);
    }
	
	private void adaptiveThreshold() {
		String[] projection = {
			"MIN(" + AmbientNoise_Data.RMS + ") as minimum_rms",
			"strftime('%d/%m/%Y'," + AmbientNoise_Data.TIMESTAMP + "/1000,'unixepoch', 'localtime') as sample_day"
		};
		
		Cursor daily_minimum = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, projection, AmbientNoise_Data.RMS + " > 0 ) GROUP BY ( sample_day ", null, "timestamp ASC");
		if( daily_minimum != null ) {
			double minimums[] = new double[daily_minimum.getCount()];
			if( daily_minimum.moveToFirst() ) {
				do{
					minimums[daily_minimum.getPosition()] = daily_minimum.getDouble(0);
				} while( daily_minimum.moveToNext() );
				
				double sum = 0;
				for( double min_day : minimums ) {
					sum+=min_day;
				}
				double daily_average = sum/minimums.length;
				Aware.setSetting(context, Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE, String.format("%.1f", daily_average+(.25*daily_average)) );
			}
		}
        if( daily_minimum!= null && ! daily_minimum.isClosed()) daily_minimum.close();
	}
	
	//RMS to check if we are in silence
	public boolean isSilent(double rms) {
        adaptiveThreshold();
		double threshold = Double.valueOf(Aware.getSetting(context, Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE));
		return (rms <= threshold);
	}
	
	/**
	 * Get sound frequency in Hz
	 * @return
	 */
	public double getFrequency() {
        if( audio_data.length == 0 ) return 0;

		//Create an FFT buffer
		double[] fft_buffer = new double[ buffer_size * 2 ];
		for( int i = 0; i< audio_data.length; i++ ) {
			fft_buffer[2*i] = (double) audio_data[i];
			fft_buffer[2*i+1] = 0;
		}
		
		//apply FFT to fill imaginary buffers
		DoubleFFT_1D fft = new DoubleFFT_1D(buffer_size);
		fft.realForward(fft_buffer);
		
		//Fetch power spectrum (magnitudes)
		//and normalize them
		magnitudes = new double[buffer_size/2];
		for(int i = 1; i< buffer_size/2-1; i++ ) {
			double real = fft_buffer[2*i];
			double imaginary = fft_buffer[2*i+1];
			magnitudes[i] = Math.sqrt((real*real)+(imaginary*imaginary));
		}
		
		//find largest peak in power spectrum (magnitudes)
		double max = -1;
		int max_index = -1;
		for( int i=0; i<buffer_size/2-1; i++ ) {
			if( magnitudes[i] > max ) {
				max = magnitudes[i];
				max_index = i;
			}
		}
		return 2*(max_index*8000/buffer_size);
	}
	
	/**
	 * Relative ambient noise in dB
	 */
	public double getdB() {
		if( audio_data.length == 0 ) return 0;

        double amplitude = -1;
		for( int i=0; i<audio_data.length; i++ ) {
			if( amplitude < audio_data[i] ) {
				amplitude = audio_data[i];
			}
		}
		return Math.abs(20*Math.log10(amplitude/32768.0));
	}
}
