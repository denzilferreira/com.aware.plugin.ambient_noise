package com.aware.plugin.ambient_noise;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class ContextCard implements IContextCard {

    final Handler uiUpdater = new Handler();

    LineChart ambient_chart;
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

        ambient_chart = (LineChart) card.findViewById(R.id.ambient_chart);
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
                    decibels.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.DECIBELS))) + " dB");
                    ambient_noise.setText(latest.getInt(latest.getColumnIndex(AmbientNoise_Data.IS_SILENT)) == 0?"Noisy":"Silent");
                }
                if (latest != null && !latest.isClosed()) latest.close();

                drawGraph(context, ambient_chart);
            }
        });
    }

    private LineChart drawGraph(Context context, LineChart lineChart) {

        ArrayList<String> audioTimeline = new ArrayList<>();
        ArrayList<Entry> audioSamples = new ArrayList<>();

        Cursor latest = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, null, null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
        if (latest != null && latest.moveToFirst()) {
            byte[] raw_audio = latest.getBlob(latest.getColumnIndex(AmbientNoise_Data.RAW));
            for (int i = 0; i< raw_audio.length; i++) {
                audioTimeline.add(String.valueOf(i));
                audioSamples.add(new Entry(Float.parseFloat(""+ raw_audio[i]), i ));
            }
        }
        if (latest != null && !latest.isClosed()) latest.close();

        LineDataSet audioDataSet = new LineDataSet(audioSamples, "Audio sample");
        audioDataSet.setColor(Color.parseColor("#33B5E5"));
        audioDataSet.setDrawValues(false);
        audioDataSet.setDrawCircles(false);

        ArrayList<ILineDataSet> plotData = new ArrayList<>();
        plotData.add(audioDataSet);

        LineData audioData = new LineData(audioTimeline, plotData);

        lineChart.setNoDataText("Waiting for recording data...");
        lineChart.setDescription("");

        ViewGroup.LayoutParams params = lineChart.getLayoutParams();
        params.height = 300;
        lineChart.setLayoutParams(params);
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);

        YAxis left = lineChart.getAxisLeft();
        left.setDrawLabels(true);
        left.setDrawGridLines(true);
        left.setDrawAxisLine(true);
        left.setGranularity(50);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.getLegend().setForm(Legend.LegendForm.LINE);

        lineChart.setData(audioData);

        return lineChart;
    }
}
