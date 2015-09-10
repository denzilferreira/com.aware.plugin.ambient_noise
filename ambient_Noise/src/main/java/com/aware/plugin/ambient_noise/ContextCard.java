package com.aware.plugin.ambient_noise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.math.RoundingMode;
import java.sql.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ContextCard implements IContextCard {

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {
            if( card != null ) {
                Cursor latest = sContext.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, null, null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
                if( latest != null && latest.moveToFirst() ) {
                    if( ! latest.isNull(latest.getColumnIndex(AmbientNoise_Data.FREQUENCY)) && ! Double.isInfinite(latest.getColumnIndex(AmbientNoise_Data.FREQUENCY))) {
                        frequency.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.FREQUENCY))) + " Hz");
                    } else {
                        frequency.setText("NA Hz");
                    }
                    if( ! Double.isInfinite(latest.getColumnIndex(AmbientNoise_Data.DECIBELS)) ) {
                        decibels.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.DECIBELS))) + " dB");
                    } else {
                        decibels.setText("NA dB");
                    }
                    if( ! latest.isNull(latest.getColumnIndex(AmbientNoise_Data.IS_SILENT)) ) {
                        ambient_noise.setText(latest.getInt(latest.getColumnIndex(AmbientNoise_Data.IS_SILENT)) == 0 ? "Noisy" : "Silent");
                    }
                    if( ! latest.isNull(latest.getColumnIndex(AmbientNoise_Data.RMS)) && ! Double.isInfinite(latest.getColumnIndex(AmbientNoise_Data.RMS))) {
                        rms.setText("RMS: " + String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.RMS))));
                    }
                    if( ! latest.isNull(latest.getColumnIndex(AmbientNoise_Data.SILENCE_THRESHOLD)) && ! Double.isInfinite(latest.getColumnIndex(AmbientNoise_Data.SILENCE_THRESHOLD))) {
                        rms_threshold.setText("Threshold: "+String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.SILENCE_THRESHOLD))));
                    }
                }
                if( latest != null && ! latest.isClosed() ) latest.close();

                //Refresh BarChart
                mChart = drawGraph(sContext);
            }
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

    /**
     * Constructor for Stream reflection
     */
    public ContextCard(){}

    private Context sContext;
    private int refresh_interval = 60000; //every minute
    private View card;
    private TextView frequency, decibels, ambient_noise, rms, rms_threshold;
    private BarChart mChart;

    public View getContextCard(Context context) {

        sContext = context;

        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

		LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		card = sInflater.inflate(R.layout.ambient_layout, null);
        mChart = (BarChart) card.findViewById(R.id.bar_chart);
        mChart = drawGraph(context);
		
		frequency = (TextView) card.findViewById(R.id.frequency);
		decibels = (TextView) card.findViewById(R.id.decibels);
		ambient_noise = (TextView) card.findViewById(R.id.ambient_noise);
		rms = (TextView) card.findViewById(R.id.rms);
		rms_threshold = (TextView) card.findViewById(R.id.rms_threshold);

        //Begin refresh cycle
        uiRefresher.post(uiChanger);

		return card;
	}
	
	private BarChart drawGraph( Context context ) {

        ArrayList<String> x_hours = new ArrayList<>();
        for(int i=0; i<24; i++) {
            x_hours.add(String.valueOf(i));
        }

        //Get today's time from the beginning in milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        Cursor latest = context.getContentResolver().query(
                AmbientNoise_Data.CONTENT_URI,
                new String[]{ "AVG(" + AmbientNoise_Data.DECIBELS +") as average",
                              "strftime('%H'," + AmbientNoise_Data.TIMESTAMP +"/1000, 'unixepoch','localtime')+0 as time_of_day" },
                AmbientNoise_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ",
                null,
                "time_of_day ASC");

        if( latest != null && latest.moveToFirst() ) {
            do {
                if( ! Float.isInfinite(latest.getFloat(0)) ) {
                    barEntries.add(new BarEntry(latest.getFloat(0), latest.getInt(1)));
                }
            } while(latest.moveToNext());
        }
        if( latest != null && ! latest.isClosed() ) latest.close();

        BarDataSet dataSet = new BarDataSet(barEntries, "Average noise (dB)");
        dataSet.setColor(Color.parseColor("#33B5E5"));

        BarData data = new BarData(x_hours, dataSet);
        mChart.setContentDescription("Daily Noise Exposure");
        mChart.setDescription("");
        mChart.setMinimumHeight(200);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBorders(false);

        YAxis left = mChart.getAxisLeft();
        left.setDrawLabels(false);
        left.setDrawGridLines(false);
        left.setDrawAxisLine(false);
        YAxis right = mChart.getAxisRight();
        right.setDrawAxisLine(false);
        right.setDrawLabels(false);
        right.setDrawGridLines(false);

        XAxis bottom = mChart.getXAxis();
        bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        bottom.setSpaceBetweenLabels(0);
        bottom.setDrawGridLines(false);

        mChart.setData(data);
        mChart.invalidate();

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
