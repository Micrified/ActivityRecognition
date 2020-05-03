package com.example.activityrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
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
    private int accelerometer_sampling_period = SensorManager.SENSOR_DELAY_UI;

    // Switch
    private Switch switch_train;

    // Button (train standing)
    private Button button_train_standing;

    // Button (train walking)
    private Button button_train_walking;

    // Button (locate)
    private Button button_locate;

    // Button (train location)
    private Button button_train_location;

    // Spinner (train location)
    private Spinner spinner_location;

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

        // Configure the training switch
        this.switch_train = findViewById(R.id.switch_train);

        // Connect the switch


        // Connect the location spinner
        this.spinner_location = findViewById(R.id.spinner_location);

        // Set the spinner values
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.spinner_locations, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner_location.setAdapter(adapter);

        // Configure the buttons
        this.button_train_standing = findViewById(R.id.button_train_standing);
        this.button_train_walking = findViewById(R.id.button_train_walking);
        this.button_locate = findViewById(R.id.button_locate);
        this.button_train_location = findViewById(R.id.button_train_location);

        // Connect the buttons
        this.button_train_walking.setOnClickListener(this);
        this.button_train_standing.setOnClickListener(this);
        this.button_locate.setOnClickListener(this);
        this.button_train_location.setOnClickListener(this);

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
            case R.id.button_train_location: {
                Log.i("Button", "Pressed to train (" +
                        this.spinner_location.getSelectedItem().toString() + ")");
            }
            break;
            case R.id.button_locate: {
                Log.i("Button", "Pressed to locate");
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
