package com.aware.plugin.ambient_noise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	/**
	 * Activate/deactivate plugin
	 */
	public static final String STATUS_PLUGIN_AMBIENT_NOISE = "status_plugin_ambient_noise";
	
	/**
	 * How frequently do we sample the microphone
	 */
	public static final String FREQUENCY_PLUGIN_AMBIENT_NOISE = "frequency_plugin_ambient_noise";
	
	/**
	 * Adaptive silence threshold (device dependent)
	 */
	protected static final String THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE = "threshold_silence_plugin_ambient_noise";
	
	/**
	 * Time window to visualize
	 */
	public static final String TIME_WINDOW_PLUGIN_AMBIENT_NOISE = "time_window_plugin_ambient_noise";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.plugin_settings);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		syncSettings();
	}
	
	private void syncSettings() {
		CheckBoxPreference active = (CheckBoxPreference) findPreference(STATUS_PLUGIN_AMBIENT_NOISE);
		active.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_AMBIENT_NOISE).equals("true"));
		
		EditTextPreference frequency = (EditTextPreference) findPreference(FREQUENCY_PLUGIN_AMBIENT_NOISE);
		if( Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_AMBIENT_NOISE).length() == 0 ) {
			Aware.setSetting(getApplicationContext(), FREQUENCY_PLUGIN_AMBIENT_NOISE, 5);
		}
		frequency.setSummary(Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_AMBIENT_NOISE) + " seconds");
		
		if( Aware.getSetting(getApplicationContext(), THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE).length() == 0 ) {
			Aware.setSetting(getApplicationContext(), THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE, 100);
		}
		
		EditTextPreference time_window = (EditTextPreference) findPreference(TIME_WINDOW_PLUGIN_AMBIENT_NOISE);
		if( Aware.getSetting(getApplicationContext(), TIME_WINDOW_PLUGIN_AMBIENT_NOISE).length() == 0) {
			Aware.setSetting(getApplicationContext(), TIME_WINDOW_PLUGIN_AMBIENT_NOISE, 5);
		}
		time_window.setSummary(Aware.getSetting(getApplicationContext(), TIME_WINDOW_PLUGIN_AMBIENT_NOISE) + " minutes");
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference preference = findPreference(key);
		
		if( preference.getKey().equals(STATUS_PLUGIN_AMBIENT_NOISE)) {
			boolean is_active = sharedPreferences.getBoolean(key, false);
			Aware.setSetting(getApplicationContext(), key, is_active);
			if( is_active ) {
				Aware.startPlugin(getApplicationContext(), getPackageName());
			} else {
				Aware.stopPlugin(getApplicationContext(), getPackageName());
			}	
		}
		if( preference.getKey().equals(FREQUENCY_PLUGIN_AMBIENT_NOISE)) {
			preference.setSummary(sharedPreferences.getString(key, "5") + " seconds");
			Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "5"));
		}
		if( preference.getKey().equals(TIME_WINDOW_PLUGIN_AMBIENT_NOISE)) {
			preference.setSummary(sharedPreferences.getString(key, "5") + " minutes");
			Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "5"));
		}
		
		Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
		sendBroadcast(apply);
	}
}
