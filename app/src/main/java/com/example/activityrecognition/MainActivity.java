package com.example.activityrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
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
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    // ImageView for the stationary symbol
    private ImageView image_view_activity;

    // Graph for displaying live accelerometer information
    private GraphView graph_view;

    // Sensor manager instance
    private SensorManager sensorManager;

    // Accelerometer sampling period (in microseconds)
    private int accelerometer_sampling_period = SensorManager.SENSOR_DELAY_UI;

    // localization
    private Button button_localization;

    // Button (train standing)
    private Button button_train_standing;

    // Button (train walking)
    private Button button_train_walking;

    // Button (train jumping)
    private Button button_train_jumping;

    // Buffer length
    private final static int data_cap = 100;

    // Line graph data
    private float[] data_dx = new float[data_cap];
    private float[] data_dy = new float[data_cap];
    private float[] data_dz = new float[data_cap];

    //sampling rate
    private int numSamples;
    private long startTime;
    private double sampling_period = 10;

    //ActivityTraining recording object
    private ActivityTraining activity_trainer = new ActivityTraining((int) sampling_period);
    boolean isTraining = false;

    public static TextView textview_logger = null;
    public static TextView textview_activity = null;

    // Simple integer tracking offset
    private int data_offset = 0;

    public  static void Log(final String message)
    {
        if(textview_logger != null)
        {
            new Handler(Looper.getMainLooper()).post(new Runnable ()
            {
                @Override
                public void run ()
                {
                    System.out.println(message);
                    textview_logger.setText(message);
                }
            });
        }
        else
            return;
    }

    public  static void LogActivity(final String message)
    {
        if(textview_activity != null)
        {
            new Handler(Looper.getMainLooper()).post(new Runnable ()
            {
                @Override
                public void run ()
                {
                    System.out.println(message);
                    textview_activity.setText(message);
                }
            });
        }
        else
            return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Sensor accelerometer;

        // Text view for logs
        this.textview_logger = findViewById(R.id.textview_log);

        // Text view for activities
        this.textview_activity = findViewById(R.id.textview_activity);

        // Configure stationary imageview
        this.image_view_activity = findViewById(R.id.image_view_activity);



        // Configure the graph display
        this.graph_view = findViewById(R.id.graph_view);

        // Configure the training button
        this.button_localization = findViewById(R.id.localization);

        // Configure the buttons
        this.button_train_standing = findViewById(R.id.button_train_standing);
        this.button_train_walking = findViewById(R.id.button_train_walking);
        this.button_train_jumping = findViewById(R.id.button_train_jumping);

        // Connect the buttons
        this.button_train_walking.setOnClickListener(this);
        this.button_train_standing.setOnClickListener(this);
        this.button_train_jumping.setOnClickListener(this);
        this.button_localization.setOnClickListener(this);

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
                if(activity_trainer.isRecording == false)
                    activity_trainer.startTraining(this.getApplicationContext(), Activity.Standing, (int) sampling_period);
            }
            break;
            case R.id.button_train_walking: {
                Log.i("Button", "Pressed to train (walking)");;
                if(activity_trainer.isRecording == false)
                activity_trainer.startTraining(this.getApplicationContext(), Activity.Walking, (int) sampling_period);
            }
            break;
            case R.id.button_train_jumping: {
                Log.i("Button", "Pressed to train (jumping)");;
                if(activity_trainer.isRecording == false)
                    activity_trainer.startTraining(this.getApplicationContext(), Activity.Jumping, (int) sampling_period);
            }
            break;
            case R.id.localization: {
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

                //check sampling rate
                activity_trainer.addDatapoint(sensorEvent.values[2]);

                numSamples++;
                long now = System.currentTimeMillis();
                if (now >= startTime + 1000) {
                    sampling_period = 1000f / numSamples;
                    //System.out.println("Current sampling rate: " + sampling_period + "ms");
                    //System.out.println(numSamples + " per second");
                    startTime = now;
                    numSamples = 0;
                }

                // Update the offset
                data_offset = (data_offset + 1) % data_cap;

                // Post results
                new Handler(Looper.getMainLooper()).post(new Runnable ()
                {
                    @Override
                    public void run ()
                    {
                        if(activity_trainer.current_activity == Activity.Jumping)
                        {
                            image_view_activity.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.svg_drawable_jumping));
                        }
                        else if(activity_trainer.current_activity == Activity.Walking)
                        {
                            image_view_activity.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.svg_drawable_walking));
                        }
                        else if (activity_trainer.current_activity == Activity.Standing)
                        {
                            image_view_activity.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.svg_drawable_standing));
                        }
                        else
                        {
                            image_view_activity.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.svg_drawable_questionmark));
                        }
                    }
                });

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
