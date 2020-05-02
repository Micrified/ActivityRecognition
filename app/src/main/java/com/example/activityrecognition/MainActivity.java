package com.example.activityrecognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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
    private int accelerometer_sampling_period = SensorManager.SENSOR_DELAY_NORMAL;

    // Some simple buffers and their length
    private final static int data_cap = 100;
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

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onSensorChanged (SensorEvent sensorEvent) {
        LineGraphSeries<DataPoint> series_x, series_y, series_z;
        DataPoint[] xs = new DataPoint[data_cap];
        DataPoint[] ys = new DataPoint[data_cap];
        DataPoint[] zs = new DataPoint[data_cap];

        switch(sensorEvent.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER: {
                System.out.printf("%f %f %f\n", sensorEvent.values[0], sensorEvent.values[1],
                        sensorEvent.values[2]);

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
                series_x = new LineGraphSeries<>(xs);
                series_y = new LineGraphSeries<>(ys);
                series_z = new LineGraphSeries<>(zs);

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
