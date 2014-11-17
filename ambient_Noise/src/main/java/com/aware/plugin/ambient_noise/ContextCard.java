package com.aware.plugin.ambient_noise;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.Aware;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.utils.IContextCard;

public class ContextCard implements IContextCard {
	
	private static TextView frequency, decibels, ambient_noise, rms, rms_threshold;
	private static LinearLayout ambient_plot;
	
	private static final CardUpdater card_updater = new CardUpdater();
	private static boolean is_registered = false;

    /**
     * Constructor for Stream reflection
     */
    public ContextCard(){}
	
	public View getContextCard(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View card = inflater.inflate(R.layout.ambient_layout, null);
		
		frequency = (TextView) card.findViewById(R.id.frequency);
		decibels = (TextView) card.findViewById(R.id.decibels);
		ambient_noise = (TextView) card.findViewById(R.id.ambient_noise);
		rms = (TextView) card.findViewById(R.id.rms);
		rms_threshold = (TextView) card.findViewById(R.id.rms_threshold);
		
		ambient_plot = (LinearLayout) card.findViewById(R.id.ambient_plot);
		ambient_plot.removeAllViews();
		ambient_plot.addView(drawGraph(context));
		
		if( ! is_registered ) {
			context.registerReceiver(card_updater, new IntentFilter(Plugin.ACTION_AWARE_PLUGIN_AMBIENT_NOISE));
			is_registered = true;
		}
		
		return card;
	}
	
	private static GraphicalView drawGraph( Context context ) {
		GraphicalView mChart = null;
		
		//Apply user-defined time window
		if( Aware.getSetting(context, Settings.TIME_WINDOW_PLUGIN_AMBIENT_NOISE).length() == 0 ) {
			Aware.setSetting(context, Settings.TIME_WINDOW_PLUGIN_AMBIENT_NOISE, 5);
		}
		long delta_time = System.currentTimeMillis()-(Integer.valueOf(Aware.getSetting(context, Settings.TIME_WINDOW_PLUGIN_AMBIENT_NOISE)) * 60 * 1000);
		
		XYSeries frequency_series = new XYSeries("Frequency (Hz)");
		Cursor audio_frequency = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, AmbientNoise_Data.TIMESTAMP + " > " + delta_time, null, AmbientNoise_Data.TIMESTAMP + " ASC");
		if( audio_frequency != null && audio_frequency.moveToFirst() ) {
			do {
				frequency_series.add(audio_frequency.getPosition(), audio_frequency.getDouble(audio_frequency.getColumnIndex(AmbientNoise_Data.FREQUENCY)));
			} while( audio_frequency.moveToNext() );
		}
        if( audio_frequency != null && ! audio_frequency.isClosed()) audio_frequency.close();
		
		XYSeries decibels_series = new XYSeries("Decibels (dB)");
		Cursor audio_decibels = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, AmbientNoise_Data.TIMESTAMP + " > " + delta_time, null, AmbientNoise_Data.TIMESTAMP + " ASC");
		if( audio_decibels != null && audio_decibels.moveToFirst() ) {
			do {
				decibels_series.add(audio_decibels.getPosition(), audio_decibels.getDouble(audio_decibels.getColumnIndex(AmbientNoise_Data.DECIBELS)));
			} while( audio_decibels.moveToNext() );
		}
        if( audio_decibels != null && ! audio_decibels.isClosed() ) audio_decibels.close();
		
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(frequency_series);
		dataset.addSeries(decibels_series);
		
		//setup frequency
		XYSeriesRenderer frequency_renderer = new XYSeriesRenderer();
		frequency_renderer.setColor(Color.BLUE);
		frequency_renderer.setPointStyle(PointStyle.POINT);
		frequency_renderer.setDisplayChartValues(false);
		frequency_renderer.setLineWidth(1);
		frequency_renderer.setFillPoints(true);
		
		XYSeriesRenderer decibels_renderer = new XYSeriesRenderer();
		decibels_renderer.setColor(Color.RED);
		decibels_renderer.setPointStyle(PointStyle.POINT);
		decibels_renderer.setDisplayChartValues(false);
		decibels_renderer.setLineWidth(1);
		decibels_renderer.setFillPoints(true);
		
		//Setup graph
		XYMultipleSeriesRenderer dataset_renderer = new XYMultipleSeriesRenderer();
		dataset_renderer.setChartTitle("Ambient noise");
		dataset_renderer.setApplyBackgroundColor(true);
		dataset_renderer.setBackgroundColor(Color.WHITE);
		dataset_renderer.setMarginsColor(Color.WHITE);
		dataset_renderer.setAxesColor(Color.BLACK); //used in titles
		dataset_renderer.setXTitle("Time");
		dataset_renderer.setFitLegend(true);
		dataset_renderer.setXLabels(0);
		dataset_renderer.setYLabels(0);
		dataset_renderer.setDisplayValues(false);
		dataset_renderer.setPanEnabled(false);
		dataset_renderer.setClickEnabled(false);
		dataset_renderer.setShowAxes(true);
		dataset_renderer.setShowGrid(true);
		dataset_renderer.setShowLabels(true);
		dataset_renderer.setAntialiasing(true);
		
		//add plot renderers to main renderer
		dataset_renderer.addSeriesRenderer(frequency_renderer);
		dataset_renderer.addSeriesRenderer(decibels_renderer);
		
		//put everything together
		mChart = ChartFactory.getLineChartView(context, dataset, dataset_renderer);
		
		return mChart;
	}
	
	public static class CardUpdater extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			double extra_frequency = intent.getDoubleExtra(Plugin.EXTRA_SOUND_FREQUENCY, 0);
			double extra_decibels = intent.getDoubleExtra(Plugin.EXTRA_SOUND_DB, 0);
			boolean extra_is_silent = intent.getBooleanExtra(Plugin.EXTRA_IS_SILENT, false); 
			
			frequency.setText(String.format("%.1f", extra_frequency) + " Hz");
			decibels.setText(String.format("%.1f", extra_decibels) + " dB");
			ambient_noise.setText(extra_is_silent?"Silent":"Noisy");
			rms.setText("RMS: " + String.format("%.0f", intent.getDoubleExtra(Plugin.EXTRA_SOUND_RMS, 100)));
			rms_threshold.setText("Threshold: " + Aware.getSetting(context, Settings.THRESHOLD_SILENCE_PLUGIN_AMBIENT_NOISE));
			
			ambient_plot.removeAllViews();
			ambient_plot.addView(drawGraph(context));
		}
	}
}
