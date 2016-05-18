package com.aware.plugin.ambient_noise;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class ContextCard implements IContextCard {

    final Handler uiUpdater = new Handler();

    LinearLayout plotContainer;
    LineChart ambient_chart;
    TextView frequency;
    TextView decibels;
    TextView ambient_noise;

    Context context;

    /**
     * Constructor for Stream reflection
     */
    public ContextCard() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateUI();
            }
        }, 0, 1000);
    }

    public View getContextCard(Context context) {

        this.context = context;

        LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View card = sInflater.inflate(R.layout.ambient_layout, null);

        plotContainer = (LinearLayout) card.findViewById(R.id.ambient_chart_container);
        ambient_chart = (LineChart) card.findViewById(R.id.ambient_chart);
        frequency = (TextView) card.findViewById(R.id.frequency);
        decibels = (TextView) card.findViewById(R.id.decibels);
        ambient_noise = (TextView) card.findViewById(R.id.ambient_noise);

        return card;
    }

    private void updateUI() {

        uiUpdater.post(new Runnable() {
            @Override
            public void run() {

                Cursor latest = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, null, null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
                if (latest != null && latest.moveToFirst()) {
                    frequency.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.FREQUENCY))) + " Hz");
                    decibels.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.DECIBELS))) + " dB");
                    ambient_noise.setText(latest.getInt(latest.getColumnIndex(AmbientNoise_Data.IS_SILENT)) == 0?"Noisy":"Silent");
                }
                if (latest != null && !latest.isClosed()) latest.close();

                plotContainer.removeAllViews();
//                plotContainer.addView(drawGraph(context, ambient_chart));
                plotContainer.invalidate();

            }
        });
    }

//    private LineChart drawGraph(Context context, LineChart lineChart) {
//
//        ArrayList<String> xValues = new ArrayList<>();
//        ArrayList<Entry> dbEntries = new ArrayList<>();
//
//        Cursor latest = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, new String[]{AmbientNoise_Data.RAW}, null, null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
//        if (latest != null && latest.moveToFirst()) {
//
//            byte[] raw = latest.getBlob(latest.getColumnIndex(AmbientNoise_Data.RAW));
//
//            do {
//                dbEntries.add(new Entry(latest.getFloat(0), latest.getInt(2)));
////                hzEntries.add(new BarEntry(latest.getFloat(1), latest.getInt(2)));
//            } while (latest.moveToNext());
//        }
//        if (latest != null && !latest.isClosed()) latest.close();
//
//        LineDataSet dbData = new LineDataSet(dbEntries, "");
//        dbData.setColor(Color.parseColor("#33B5E5"));
//        dbData.setDrawValues(false);
//
//        ArrayList<ILineDataSet> datasets = new ArrayList<>();
//        datasets.add(dbData);
//
//        LineData data = new LineData(xValues, datasets);
//
//        lineChart.setContentDescription("");
//        lineChart.setDescription("");
//        lineChart.setMinimumHeight(200);
//        lineChart.setBackgroundColor(Color.WHITE);
//        lineChart.setDrawGridBackground(false);
//        lineChart.setDrawBorders(false);
//
//        lineChart.getLegend().setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
//
//        YAxis left = lineChart.getAxisLeft();
//        left.setDrawLabels(true);
//        left.setDrawGridLines(true);
//        left.setDrawAxisLine(true);
//        left.setGranularity(50);
//
//        YAxis right = lineChart.getAxisRight();
//        right.setDrawAxisLine(false);
//        right.setDrawLabels(false);
//        right.setDrawGridLines(false);
//
//        XAxis bottom = lineChart.getXAxis();
//        bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
//        bottom.setSpaceBetweenLabels(0);
//        bottom.setDrawGridLines(false);
//
//        lineChart.setData(data);
//        lineChart.invalidate();
//        lineChart.animateX(1000);
//
//        return lineChart;
//    }
}
