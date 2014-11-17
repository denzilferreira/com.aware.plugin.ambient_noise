package com.aware.plugin.ambient_noise;

import android.content.ContentValues;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.utils.Aware_Plugin;

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
	
	/**
	 * Current sound frequency
	 */
	private double sound_frequency;
	private double sound_db;
	private boolean is_silent;
	private double sound_rms;
	
	//AWARE context producer
	public static ContextProducer context_producer;
	
	public Thread audio_thread = new Thread(){
		public void run() {
			//Get minimum size of the buffer for pre-determined audio setup
			int buffer_size = AudioRecord.getMinBufferSize( 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 3;
			
			//Initialize audio recorder. Use MediaRecorder.AudioSource.VOICE_RECOGNITION to disable Automated Voice Gain from microphone and use DSP if available
			AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
			
			//Audio data buffer
			short[] audio_data = new short[buffer_size];
			
			while( true ) {
				//Quit if the user disables the plugin
				if( Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIENT_NOISE).equals("false") ) {
					break;
				}
				
				if( recorder.getState() == AudioRecord.STATE_INITIALIZED ) {
					
					if( recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED ) {
						recorder.startRecording();
					}
					
					recorder.read(audio_data, 0, buffer_size);
					
					AudioAnalysis audio_analysis = new AudioAnalysis(getApplicationContext(), audio_data, buffer_size);
					
					sound_rms = audio_analysis.getRMS();
					is_silent = audio_analysis.isSilent(sound_rms);
					sound_frequency = audio_analysis.getFrequency();
					sound_db = audio_analysis.getdB();
					
					ContentValues data = new ContentValues();
					data.put(AmbientNoise_Data.TIMESTAMP, System.currentTimeMillis());
					data.put(AmbientNoise_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
					data.put(AmbientNoise_Data.FREQUENCY, sound_frequency);
					data.put(AmbientNoise_Data.DECIBELS, sound_db);
					data.put(AmbientNoise_Data.RMS, sound_rms);
					data.put(AmbientNoise_Data.IS_SILENT, is_silent);
					data.put(AmbientNoise_Data.SILENCE_THRESHOLD, Aware.getSetting(getApplicationContext(), Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE));
					
					getContentResolver().insert(AmbientNoise_Data.CONTENT_URI, data);
					
					//Share context
					context_producer.onContext();
					
					try {
						Thread.sleep( Long.parseLong( Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE)) * 1000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				} else { //recorder is busy right now, let's wait 30 seconds before we try again
					try {
						Thread.sleep( 30 * 1000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			//Clean-up
			recorder.stop();
			recorder.release();
		};
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		TAG = "AWARE::Ambient Noise";
		DEBUG = true;
		
		Intent aware = new Intent(this, Aware.class);
		startService(aware);
		
		Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIENT_NOISE, true);
		if( Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE).length() == 0 ) {
			Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE, 5);
		}
		if( Aware.getSetting(getApplicationContext(), Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE).length() == 0 ) {
			Aware.setSetting(getApplicationContext(), Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE, 100);
		}
		
		CONTEXT_PRODUCER = new ContextProducer() {
			@Override
			public void onContext() {
				Intent context_ambient_noise = new Intent();
				context_ambient_noise.setAction(ACTION_AWARE_PLUGIN_AMBIENT_NOISE);
				context_ambient_noise.putExtra(EXTRA_SOUND_FREQUENCY, sound_frequency);
				context_ambient_noise.putExtra(EXTRA_SOUND_DB, sound_db);
				context_ambient_noise.putExtra(EXTRA_SOUND_RMS, sound_rms);
				context_ambient_noise.putExtra(EXTRA_IS_SILENT, is_silent);
				sendBroadcast(context_ambient_noise);
			}
		};
		context_producer = CONTEXT_PRODUCER;
		audio_thread.start();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIENT_NOISE, false);
	}
}
