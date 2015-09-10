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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
                    frequency.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.FREQUENCY))) + " Hz");
                    decibels.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.DECIBELS))) + " dB");
                    ambient_noise.setText(latest.getInt(latest.getColumnIndex(AmbientNoise_Data.IS_SILENT))==0?"Noisy":"Silent");
                    rms.setText("RMS: "+String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.RMS))));
                    rms_threshold.setText("Threshold: "+String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.SILENCE_THRESHOLD))));
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
    private int refresh_interval = 5 * 1000;
    private View card;
    private TextView frequency, decibels, ambient_noise, rms, rms_threshold;
    private LinearLayout ambient_plot;
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

        ambient_plot = (LinearLayout) card.findViewById(R.id.ambient_plot);
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

        String[] x_hours = new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23"};

        //Get today's time from the beginning in milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        ArrayList<BarEntry> y_data = new ArrayList<>();
        Cursor latest = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, new String[]{ "AVG(" + AmbientNoise_Data.DECIBELS +") as average", "strftime('%H'," + AmbientNoise_Data.TIMESTAMP +"/1000, 'unixepoch','localtime')+0 as time_of_day" }, AmbientNoise_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ", null, AmbientNoise_Data.TIMESTAMP + " ASC");

        Log.d(Plugin.TAG, DatabaseUtils.dumpCursorToString(latest));

        if( latest != null && latest.moveToFirst() ) {
            do {
                if( latest.getFloat(0) instanceof  ) continue;

                y_data.add(new BarEntry(latest.getFloat(0), Integer.parseInt(latest.getString(1))));
            } while(latest.moveToNext());
        }
        if( latest != null && ! latest.isClosed() ) latest.close();

        BarDataSet dataSet = new BarDataSet(y_data, "Ambient noise (dB)");
        BarData data = new BarData(x_hours, dataSet);

        mChart.setDescription("Daily Noise Exposure");
        mChart.setMinimumHeight(200);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBorders(false);

        XAxis xs = mChart.getXAxis();
        xs.setPosition(XAxis.XAxisPosition.BOTTOM);

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
