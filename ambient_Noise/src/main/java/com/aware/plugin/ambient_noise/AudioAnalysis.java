package com.aware.plugin.ambient_noise;

import android.content.Context;
import android.database.Cursor;

import com.aware.Aware;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


public class AudioAnalysis {
	
	private static Context context;
	private static double[] magnitudes;
	private static short[] audio_data;
	private static int buffer_size;
	
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
		double[] sqr_samples = new double[buffer_size];
		for(int i=0; i<audio_data.length; i++) {
			sqr_samples[i] = audio_data[i] * audio_data[i];
		}
		
		double sum_sqr=0;
		for(int i=0; i<sqr_samples.length; i++) {
			sum_sqr+=sqr_samples[i];
		}
		
		//Calculate RMS from provided sample
		double current_rms = Math.sqrt(sum_sqr/sqr_samples.length);
		adaptiveThreshold(current_rms);
		return current_rms;
	}
	
	private void adaptiveThreshold(double current_rms) {
		String[] projection = {
			"AVG(" + AmbientNoise_Data.RMS + ") as avg_rms",
			"strftime('%d/%m/%Y'," + AmbientNoise_Data.TIMESTAMP + "/1000,'unixepoch') as sample_day"
		};
		
		Cursor days_average = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, projection, AmbientNoise_Data.RMS + " > 0 ) GROUP BY ( sample_day ", null, null);
		if( days_average != null ) {
			double minimums[] = new double[days_average.getCount()];
			if( days_average.moveToFirst() ) {
				do{
					minimums[days_average.getPosition()] = days_average.getDouble(0);
				} while( days_average.moveToNext() );
				
				double daily_average = 0;
				for( double min_day : minimums ) {
					daily_average+=min_day;
				}
				daily_average = (daily_average/minimums.length) + (.25*daily_average);
				
				Aware.setSetting(context, Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE, String.format("%.0f", daily_average));
				
				days_average.close();
			}
		}
	}
	
	//RMS to check if we are in silence
	public boolean isSilent(double rms) {
		double threshold = Double.valueOf(Aware.getSetting(context, Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE));
		return (rms <= threshold);
	}
	
	/**
	 * Get sound frequency in Hz
	 * @return
	 */
	public double getFrequency() {
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
		return max_index*8000/buffer_size;
	}
	
	/**
	 * Relative ambient noise in dB
	 */
	public double getdB() {
		double amplitude = -1;
		for( int i=0; i<audio_data.length; i++ ) {
			if( amplitude < audio_data[i] ) {
				amplitude = audio_data[i];
			}
		}
		return Math.abs(20*Math.log10(amplitude/32768.0));
	}
}
