package com.aware.plugin.ambient_noise;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.Aware;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;

import java.util.Calendar;

public class ContextCard implements IContextCard {

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {
            if( card != null ) {
                Cursor latest = sContext.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, null, null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
                if( latest != null && latest.moveToFirst() ) {
                    frequency.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.FREQUENCY))) + " Hz");
                    decibels.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.DECIBELS))) + " dB");
                    ambient_noise.setText(latest.getInt(latest.getColumnIndex(AmbientNoise_Data.IS_SILENT))==0?"Noisy":"Silent");
                    rms.setText("RMS: "+String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.RMS))));
                    rms_threshold.setText("Threshold: "+String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.SILENCE_THRESHOLD))));
                }
                if( latest != null && ! latest.isClosed() ) latest.close();
                ambient_plot = (LinearLayout) card.findViewById(R.id.ambient_plot);
                ambient_plot.removeAllViews();
                ambient_plot.addView(drawGraph(sContext));
            }
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

    /**
     * Constructor for Stream reflection
     */
    public ContextCard(){}

    private Context sContext;
    private int refresh_interval = 5 * 1000;
    private View card;
    private TextView frequency, decibels, ambient_noise, rms, rms_threshold;
    private LinearLayout ambient_plot;

    private LayoutInflater sInflater;
	
	public View getContextCard(Context context) {

        sContext = context;

        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

		sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		card = sInflater.inflate(R.layout.ambient_layout, null);
		
		frequency = (TextView) card.findViewById(R.id.frequency);
		decibels = (TextView) card.findViewById(R.id.decibels);
		ambient_noise = (TextView) card.findViewById(R.id.ambient_noise);
		rms = (TextView) card.findViewById(R.id.rms);
		rms_threshold = (TextView) card.findViewById(R.id.rms_threshold);

        //Begin refresh cycle
        uiRefresher.post(uiChanger);

		return card;
	}
	
	private GraphicalView drawGraph( Context context ) {

        String[] x_hours = new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23"};

        //Get today's time from the beginning in milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        GraphicalView mChart;

        XYSeries audio_signal = new XYSeries("Daily Noise Exposure");

        double[] averages = new double[24];
        Cursor latest = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, new String[]{ "AVG(" + AmbientNoise_Data.DECIBELS +") as average", "strftime('%H'," + AmbientNoise_Data.TIMESTAMP +"/1000, 'unixepoch','localtime')+0 as time_of_day" }, AmbientNoise_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ", null, AmbientNoise_Data.TIMESTAMP + " ASC");
        if( latest != null && latest.moveToFirst() ) {
            do {
                averages[latest.getInt(1)] = Math.round(latest.getDouble(0));
            } while(latest.moveToNext());
        }
        if( latest != null && ! latest.isClosed() ) latest.close();

        for( int i = 0; i< averages.length; i++ ){
            audio_signal.add(i, averages[i]);
        }

		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(audio_signal);

		//setup frequency
		XYSeriesRenderer frequency_renderer = new XYSeriesRenderer();
		frequency_renderer.setColor(Color.parseColor("#33B5E5"));
        frequency_renderer.setDisplayChartValues(false);
		
		//Setup graph
		XYMultipleSeriesRenderer dataset_renderer = new XYMultipleSeriesRenderer();
        dataset_renderer.setChartTitle("Daily Noise Exposure");
        dataset_renderer.setLabelsColor(Color.BLACK);
        dataset_renderer.setXLabelsColor(Color.BLACK);
        dataset_renderer.setYLabelsColor(0, Color.BLACK);
        dataset_renderer.setShowLegend(false);
        dataset_renderer.setYTitle("Average noise (dB)");
        dataset_renderer.setXTitle("Time of day");
        dataset_renderer.setAntialiasing(true);
        dataset_renderer.setShowAxes(false);
        dataset_renderer.setApplyBackgroundColor(true);
        dataset_renderer.setBackgroundColor(Color.WHITE);
        dataset_renderer.setMarginsColor(Color.WHITE);

        for(int i=0; i< x_hours.length; i++) {
            dataset_renderer.addXTextLabel(i, x_hours[i]);
        }
		
		//add plot renderers to main renderer
		dataset_renderer.addSeriesRenderer(frequency_renderer);
		
		//put everything together
		mChart = ChartFactory.getBarChartView(context, dataset, dataset_renderer, BarChart.Type.DEFAULT);
		
		return mChart;
	}

    //This is a BroadcastReceiver that keeps track of stream status. Used to stop the refresh when user leaves the stream and restart again otherwise
    private StreamObs streamObs = new StreamObs();
    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
                //start refreshing when user enters the stream
                uiRefresher.postDelayed(uiChanger, refresh_interval);
            }
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }
}
