AWARE Plugin: Ambient Noise
===========================

This plugin measures the ambient noise (Hz, dB) as noisy or silent moments. It adds the daily noise exposure on the stream, showing the average dB and Hz per hour throughout the day.

[ ![Download](https://api.bintray.com/packages/denzilferreira/com.awareframework/com.aware.plugin.ambient_noise/images/download.svg) ](https://bintray.com/denzilferreira/com.awareframework/com.aware.plugin.ambient_noise/_latestVersion)

# Settings
* status_plugin_ambient_noise: (boolean) activate/deactivate ambient noise plugin
* frequency_plugin_ambient_noise: (integer) interval between audio data snippets, in minutes
* plugin_ambient_noise_sample_size: (integer) For how long we collect data, in seconds
* plugin_ambient_noise_silence_threshold: (integer) Above which is no longer silent, in dB

# Broadcasts
**ACTION_AWARE_PLUGIN_AMBIENT_NOISE**
Broadcasted when we classify the ambient noise, with the following extras:
- **sound_frequency**: (double) sound frequency in Hz
- **sound_db**: (double) sound decibels in dB
- **sound_rms**: (double) sound RMS (used to classify silent/not silent)
- **is_silent**: (boolean) true or false if it is silent
    
# Providers
##  Ambient Noise Data
> content://com.aware.plugin.ambient_noise.provider.ambient_noise/plugin_ambient_noise

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
double_frequency | REAL | sound frequency in Hz
double_decibels	| REAL | sound decibels in dB
double_RMS | REAL |	sound RMS
is_silent |	INTEGER | 0 = not silent 1 = is silent
raw | BLOB | the audio snippet raw data collected
double_silent_threshold | REAL | the defined threshold value when classifying
