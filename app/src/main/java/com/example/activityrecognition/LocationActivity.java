package com.example.activityrecognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class LocationActivity extends AppCompatActivity implements View.OnClickListener {

    // Graph for displaying training information
    private GraphView graph_view;

    // Button (train)
    private Button button_train_location;

    // Button (locate)
    private Button button_locate;

    // TextView (location)
    private TextView text_view_location;

    // Spinner (cell selection)
    private Spinner spinner_cell_selection;

    // Progress dialog
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Connect graph
        graph_view = findViewById(R.id.graph_view_location);

        // Configure graph
        this.graph_view.getGridLabelRenderer().setGridColor(Color.WHITE);
        this.graph_view.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);
        this.graph_view.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);

        // Connect buttons
        this.button_train_location = findViewById(R.id.button_train_location);
        this.button_locate = findViewById(R.id.button_locate);

        // Link buttons
        this.button_train_location.setOnClickListener(this);
        this.button_locate.setOnClickListener(this);

        // Link textview
        this.text_view_location = findViewById(R.id.text_view_location);

        // Link spinner
        this.spinner_cell_selection = findViewById(R.id.spinner_cell_selection);

        // Configure spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_locations, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_cell_selection.setAdapter(adapter);

        // Initialize the loading dialog popup class
        loadingDialog = new LoadingDialog(this);

        // Initialize and set the Location Manager and WiFi Manager
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        LocationManager.getInstance().setWifiManager(wifiManager);
        wifiManager.setWifiEnabled(true);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
            case R.id.button_train_location: {

                int cell = this.spinner_cell_selection.getSelectedItemPosition();
                Log.i("onClick", "Training " + cell);
                this.scanWiFiForCell(cell);
                LocationActivity.this.loadingDialog.startLoadingDialog();
            }
            break;

            case R.id.button_locate: {
                Log.i("onClick", "Classifying ...");
                this.scanWiFiForClassification();
                LocationActivity.this.loadingDialog.startLoadingDialog();
            }
            break;
        }
    }

    // Converts the given ordered result data into series
    private void graph_result_data (ArrayList<LocationManager.ResultSample> result_data)
    {
        Log.i("graph_result_data", "graphing " + result_data.size() + " results...");

        // The color set for the series
        int[] color_ids = new int[]{R.color.color1, R.color.color2, R.color.color3, R.color.color4};

        // Initialize array holding all series
        ArrayList<LineGraphSeries<DataPoint>> cell_series =
                new ArrayList<LineGraphSeries<DataPoint>>();

        // Initialize a new series for all cells
        for (int i = 0; i < 4; ++i) {
            cell_series.add(new LineGraphSeries<DataPoint>());
            cell_series.get(i).setColor(getResources().getColor(R.color.colorX));
        }

        // Sort the points into their respective labels
        LineGraphSeries<DataPoint> set = null;
        for (LocationManager.ResultSample r : result_data) {
            cell_series.get(r.cell_id).appendData(new DataPoint(r.hamming, r.cell_id),
                    true, result_data.size());
        }

        // Add all series to the graph
        for (LineGraphSeries<DataPoint> series : cell_series) {
            graph_view.addSeries(series);
        }

        // Plot it in the graph
        graph_view.onDataChanged(true, true);
    }

    // Performs a WiFi scan and classifies the sample
    private void scanWiFiForClassification ()
    {
        final WifiManager m = LocationManager.getInstance().getWifiManager();

        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<ScanResult> scan = m.getScanResults();

                if (intent.getBooleanExtra(m.EXTRA_RESULTS_UPDATED, false) == false) {
                    Log.e("scanWiFiForClassification", "Failed scan!");
                    return;
                } else {
                    Log.i("scanWiFiForClassification", "Scan success!");
                }

                // Convert stupid list to ArrayList
                ArrayList<ScanResult> out = new ArrayList<>();
                for (ScanResult res : scan) {
                    out.add(res);
                }

                // Classify the sample
                final int cell_classification = LocationManager.getInstance().classify(out);

                // Post results
                new Handler(Looper.getMainLooper()).post(new Runnable ()
                {
                    @Override
                    public void run ()
                    {
                        LocationActivity.this.text_view_location.setText(
                                String.format("Cell %d", cell_classification + 1)
                        );
                        LocationActivity.this.graph_result_data(LocationManager.getInstance().get_result_set());
                        LocationActivity.this.loadingDialog.dismissDialog();
                    }
                });
            }
        };

        // Register receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(r, intentFilter);

        // Start WiFi scan
        m.startScan();
    }

    // Performs a WiFi scan and stores it as training data for the given cell
    private void scanWiFiForCell (final int cell)
    {
        final WifiManager m = LocationManager.getInstance().getWifiManager();
        Log.i("scanWiFiForCell", "Scanning for cell " + cell);

        BroadcastReceiver r = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                context.unregisterReceiver(this);
                List<ScanResult> results = m.getScanResults();
                if (intent.getBooleanExtra(m.EXTRA_RESULTS_UPDATED, false) == false) {
                    Log.e("scanWiFiForCell", "Failed scan!");
                    return;
                } else {
                    Log.i("scanWiFiForCell", "Success (" + results.size() + ")");
                }

                // Collect results
                final ArrayList<ScanResult> filtered = new ArrayList<ScanResult>();
                for (ScanResult result : results) {
                    System.out.printf("[%s|%s|%s]\n", result.BSSID, result.SSID, result.level);
                    filtered.add(result);
                }

                // Post results
                new Handler(Looper.getMainLooper()).post(new Runnable ()
                {
                    @Override
                    public void run ()
                    {
                        LocationManager.getInstance().addScan(cell, filtered);
                        LocationActivity.this.loadingDialog.dismissDialog();
                    }
                });
            }
        };

        // Register receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(r, intentFilter);

        // Start WiFi scan
        m.startScan();
    }
}
