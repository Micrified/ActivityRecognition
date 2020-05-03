package com.example.activityrecognition;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Timer;


public class ActivityTraining {

    ArrayList<Float> current_training_set = new ArrayList<Float>();
    ArrayList<Float> current_set_variances = new ArrayList<Float>();
    int window_size;//in amount of samples
    int amount_windows;//in windows
    int sample_period;//in microseconds
    int sampling_time;//in seconds

    public boolean isRecording = false;

    public ActivityTraining(int sample_period)
    {
        window_size = 10;
        amount_windows = 40;
        sampling_time = (sample_period*window_size*amount_windows);
        System.out.println("Accelerometer sampling time: " + sample_period + "ms");
        System.out.println("Window time: " + (sample_period*window_size) + "ms");
        System.out.println("Total training time: " + (sample_period*window_size*amount_windows) + "ms");
    }

    //chops raw data into windows
    public ArrayList<ArrayList<Float>> windowFormatter(ArrayList<Float> training_set)
    {
        ArrayList<ArrayList<Float>> windowSets = new ArrayList<ArrayList<Float>>();
        ArrayList<Float> currentSet = new ArrayList<Float>();

        int sample = 0;
        int window = 0;
        System.out.println("Training set data length: " + training_set.size());
        for (Float f:training_set
             ) {
            currentSet.add(f);
            System.out.println("Sample: " + sample + "/" + (window_size - 1) + " for Window: " + window);

            if(sample == window_size - 1)
            {
                sample = 0;
                windowSets.add(currentSet);
                currentSet = new ArrayList<Float>();
                window++;
            }
            else
            {
                sample++;
            }
        }

        System.out.println("Total windows: " + window);
        return windowSets;
    }

    public Float variance(ArrayList<Float> list) {
        Double asq = 0.0;
        for(Float i:list){
            asq = asq + Math.pow((i-average(list)),2);
        }

        return (float) (asq / list.size());
    }

    Float sum(ArrayList<Float> list) {
        Float sum = 0f;
        for(Float i:list){
            sum +=i;
        }
        return sum;
    }

    Float average(ArrayList<Float> list) {
        int size = list.size();
        return (Float) (1.0f*sum(list)/size);
    }

    public void addDatapoint(float point)
    {
        if(isRecording)
        {
            current_training_set.add(point);
        }
    }

    public void startTraining(final Context context, final Activity activity_type, int sampling_period){
        //recalculate sampling time
        sample_period = sampling_period;
        sampling_time = (sample_period*window_size*amount_windows);
        System.out.println("Will train for " + sampling_time/1000 + "seconds");

        //start recording at this index
        isRecording = true;
        current_training_set.clear();

        new Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        current_set_variances.clear();
                        for (ArrayList<Float> af : windowFormatter((ArrayList<Float>) current_training_set.clone())
                             ) {
                            current_set_variances.add(variance(af));//one per window
                        }
                        //export
                        export(context, activity_type);
                        isRecording = false;
                    }
                },
                sampling_time//in seconds
        );
    }

    //formatting for csv so that shit's on one column
    String formatData(ArrayList<Float> data)
    {
        String outputString = "";
        for (Float f: data
             ) {
            outputString += f.toString() + System.lineSeparator();
        }
        return outputString;
    }

    // Writes the data for each cell
    public void export (Context context, Activity activity_type) {
        String type = "error";
        switch (activity_type)
        {
            case Jumping:
                type = "Jumping";
                break;
            case Walking:
                type = "Walking";
                break;
            case Standing:
                type = "Standing";
                break;
        }

        File file = null;
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;

        // Ensure that we can export
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == false) {
            Log.e("OS", "Environment can't use external storage!");
            return;
        }

        String filename = type + "_" + System.currentTimeMillis() + "_raw" + ".csv";

        file = new File(context.getExternalFilesDir(null), filename);

        try {
            fileOutputStream = new FileOutputStream(file, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            String s = String.format("Exporting data %s", type);
            System.out.println(s);

            String data_string = formatData(current_training_set);
            outputStreamWriter.write(data_string, 0,  data_string.length());

            outputStreamWriter.close();
            fileOutputStream.close();

            System.out.println("Finished writing");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //processed variances
        filename = type + "_" + System.currentTimeMillis() + "_processed" + ".csv";

        file = new File(context.getExternalFilesDir(null), filename);

        try {
            fileOutputStream = new FileOutputStream(file, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            String s = String.format("Exporting processed data %s", type);
            System.out.println(s);

            String data_string = formatData(current_set_variances);
            outputStreamWriter.write(data_string, 0,  data_string.length());

            outputStreamWriter.close();
            fileOutputStream.close();

            System.out.println("Finished writing");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

enum Activity {
    Walking,
    Standing,
    Jumping
}
