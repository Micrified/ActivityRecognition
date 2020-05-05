package com.example.activityrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    // ImageView for the walking symbol
    private ImageView image_view_walking;

    // ImageView for the stationary symbol
    private ImageView image_view_standing;

    // Graph for displaying live accelerometer information
    private GraphView graph_view;

    // Sensor manager instance
    private SensorManager sensorManager;

    // Accelerometer sampling period (in microseconds)
    private int accelerometer_sampling_period = SensorManager.SENSOR_DELAY_UI;

    // Button (train standing)
    private Button button_train_standing;

    // Button (train walking)
    private Button button_train_walking;


    // Button (go to location activity)
    private Button button_location_activity;

    // Buffer length
    private final static int data_cap = 100;

    // Line graph data
    private float[] data_dx = new float[data_cap];
    private float[] data_dy = new float[data_cap];
    private float[] data_dz = new float[data_cap];


    // Simple integer tracking offset
    private int data_offset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Sensor accelerometer;

        // Configure walking imageview
        this.image_view_walking = findViewById(R.id.image_view_walking);

        // Configure stationary imageview
        this.image_view_standing = findViewById(R.id.image_view_standing);

        // Configure the graph display
        this.graph_view = findViewById(R.id.graph_view);

        // Configure the buttons
        this.button_train_standing = findViewById(R.id.button_train_standing);
        this.button_train_walking = findViewById(R.id.button_train_walking);
        this.button_location_activity = findViewById(R.id.button_location_activity);

        // Connect the buttons
        this.button_train_walking.setOnClickListener(this);
        this.button_train_standing.setOnClickListener(this);
        this.button_location_activity.setOnClickListener(this);

        // Configure the graph
        this.graph_view.getGridLabelRenderer().setGridColor(Color.WHITE);
        this.graph_view.getGridLabelRenderer().setVerticalLabelsVisible(false);
        this.graph_view.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);

        // Get the sensor manager, and then configure the accelerometer
        SensorManager m = (SensorManager)(this.getSystemService(Context.SENSOR_SERVICE));
        if ((accelerometer = m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) == null) {
            Log.e("INIT", "This device does not have an accelerometer!");
        } else {
            m.registerListener(this, accelerometer, accelerometer_sampling_period);
        }

        // Assign the sensor manager
        this.sensorManager = m;

    }

    public void animateView (View view) {
        ImageView v = (ImageView)view;

        Drawable d = v.getDrawable();

        if (d instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avd = (AnimatedVectorDrawableCompat)d;
            avd.start();
        } else {
            if (d instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable avd = (AnimatedVectorDrawable)d;
                avd.start();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_train_standing: {
                Log.i("Button", "Pressed to train (standing)");
            }
            break;
            case R.id.button_train_walking: {
                Log.i("Button", "Pressed to train (walking)");
            }
            break;
            case R.id.button_location_activity: {
                Log.i("Button", "Pressed to go to location activity");
                Intent intent = new Intent(this, LocationActivity.class);
                startActivity(intent);
            }
            break;
        }
    }

    @Override
    public void onSensorChanged (SensorEvent sensorEvent) {
        LineGraphSeries<DataPoint> series_x, series_y, series_z;
        DataPoint[] xs = new DataPoint[data_cap];
        DataPoint[] ys = new DataPoint[data_cap];
        DataPoint[] zs = new DataPoint[data_cap];

        switch(sensorEvent.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER: {

                // Remove all series from the graph
                graph_view.removeAllSeries();

                // Save the values in the rolling buffers
                data_dx[data_offset] = sensorEvent.values[0];
                data_dy[data_offset] = sensorEvent.values[1];
                data_dz[data_offset] = sensorEvent.values[2];

                // Update the offset
                data_offset = (data_offset + 1) % data_cap;

                // Create the new set of data points
                for (int i = 0; i < data_cap; ++i) {
                    int j = (data_offset + i) % data_cap;
                    xs[i] = new DataPoint(i * accelerometer_sampling_period, data_dx[j]);
                    ys[i] = new DataPoint(i * accelerometer_sampling_period, data_dy[j]);
                    zs[i] = new DataPoint(i * accelerometer_sampling_period, data_dz[j]);
                }

                // Create the new data series
                series_x = new LineGraphSeries<>(xs); series_x.setColor(getResources().getColor(R.color.colorX));
                series_y = new LineGraphSeries<>(ys); series_y.setColor(getResources().getColor(R.color.colorY));
                series_z = new LineGraphSeries<>(zs); series_z.setColor(getResources().getColor(R.color.colorZ));

                // Plot the new series
                graph_view.addSeries(series_x);
                graph_view.addSeries(series_y);
                graph_view.addSeries(series_z);


                // Plot it in the graph
                graph_view.onDataChanged(true, true);

                break;
            }

        }
    }


    // Handler for change in sensor accuracy
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Handler for resuming application context
    @Override
    public void onResume() {
        super.onResume();

        // Obtain a new reference to accelerometer
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Register it if non-null
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    this.accelerometer_sampling_period);
        }
    }

    // Handler for losing application context
    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

}
