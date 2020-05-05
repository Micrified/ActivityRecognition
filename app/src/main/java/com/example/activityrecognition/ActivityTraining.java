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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;


public class ActivityTraining {

    ArrayList<ArrayList<Float>> trained_variances_set = new ArrayList<ArrayList<Float>>();

    ArrayList<Float> current_training_set = new ArrayList<Float>();
    ArrayList<Float> current_set_variances = new ArrayList<Float>();

    LinkedList<Float> current_buffer = current_buffer = new LinkedList<Float>();

    int window_size;//in amount of samples
    int amount_windows;//in windows
    int sample_period;//in microseconds
    int sampling_time;//in seconds

    public boolean isRecording = false;
    public boolean isComparing = false;

    public Activity current_activity = null;

    public ActivityTraining(int sample_period)
    {
        //init trained set
        //Activity.values().length
        for (int i = 0; i < Activity.values().length; i++) {
            trained_variances_set.add(new ArrayList<Float>());
        }
        window_size = 10;
        amount_windows = 20;
        sampling_time = (sample_period*window_size*amount_windows);
        MainActivity.Log("Accelerometer sampling time: " + sample_period + "ms");
        MainActivity.Log("Window time: " + (sample_period*window_size) + "ms");
        MainActivity.Log("Total training time: " + (sample_period*window_size*amount_windows) + "ms");
        getCurrentActivity();
    }

    //chops raw data into windows
    public ArrayList<ArrayList<Float>> windowFormatter(ArrayList<Float> training_set)
    {
        ArrayList<ArrayList<Float>> windowSets = new ArrayList<ArrayList<Float>>();
        ArrayList<Float> currentSet = new ArrayList<Float>();

        int sample = 0;
        int window = 0;
        MainActivity.Log("Training set data length: " + training_set.size());
        for (Float f:training_set
             ) {
            currentSet.add(f);
            MainActivity.Log("Sample: " + sample + "/" + (window_size - 1) + " for Window: " + window);

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

        MainActivity.Log("Total windows: " + window);
        return windowSets;
    }

    public Float variance(List<Float> list) {
        Double asq = 0.0;
        for(Float i:list){
            asq = asq + Math.pow((i-average(list)),2);
        }

        return (float) (asq / list.size());
    }

    Float sum(List<Float> list) {
        Float sum = 0f;
        for(Float i:list){
            sum +=i;
        }
        return sum;
    }

    Float average(List<Float> list) {
        int size = list.size();
        return (Float) (1.0f*sum(list)/size);
    }

    public void addDatapoint(float point)
    {
        if(isRecording)
        {
            current_training_set.add(point);
        }

        //rolling buffer with a "queue"
        if(!isComparing) {
            if (current_buffer.size() == window_size)
                current_buffer.removeFirst();

            current_buffer.addLast(point);
        }
    }

    public void getCurrentActivity()
    {
        //sample for one window

        new Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        //calculate variance
                        //compare to the trained sets
                        if(!isRecording)
                        {
                            isComparing = true;
                            int activity = compareCurrentTrained();
                            if(activity != -1)
                            {
                                MainActivity.LogActivity("Current Activity: " + Activity.values()[activity].toString());
                                current_activity = Activity.values()[activity];
                            }
                            isComparing = false;
                        }
                        getCurrentActivity();
                    }
                },
                (sampling_time/amount_windows) //one window
        );

    }

    public int compareCurrentTrained()
    {
        //get variance of window
        Float currentVariance = variance(cloneArrayListFloat(current_buffer));

        Float[] distances = new Float[trained_variances_set.size()];
        for (ArrayList<Float> f: trained_variances_set
             ) {if(f.size() == 0)
                 return -1;
        }
        int i = 0;
        //for each float in these sets get a knnsample object and add to giant list
        ArrayList<knnSample> samples = new ArrayList<knnSample>();
        for (ArrayList<Float> af:trained_variances_set
             ) {
            for (Float f:af
                 ) {
                samples.add(new knnSample(f, Math.abs(f - currentVariance), Activity.values()[i]));
            }
            i++;
        }
        int k = (int) Math.floor(Math.sqrt(samples.size()));
        Collections.sort(samples);//sort big to small
        i = 0;

        //sort giant list and see what the majority is labeled as
        int[] activities_count = new int[Activity.values().length];
        MainActivity.Log("Total size of samples: " + samples.size());
        if(samples.size() == 0)
        {
            return -1;
        }


        while(i < k)
        {
            activities_count[samples.get(i).activity.ordinal()]++;
            i++;
        }

        MainActivity.LogActivity("knn samples per activity: " + activities_count[0] + ", " + activities_count[1] + ", " + activities_count[2]);

        i = 0;
        //see what the majority is labeled as
        float currHighest = -1; int highestIndex = -1;
        for (int j:activities_count
        ) {
            if(j > currHighest )
            {
                currHighest = j;
                highestIndex = i;
            }
            i++;
        }

        return highestIndex;
    }

    public void startTraining(final Context context, final Activity activity_type, int sampling_period){
        //recalculate sampling time
        sample_period = sampling_period;
        sampling_time = (sample_period*window_size*amount_windows);
        MainActivity.Log("Will train for " + sampling_time/1000 + " seconds");

        //start recording at this index
        isRecording = true;
        current_training_set.clear();

        new Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        isRecording = false;
                        current_set_variances.clear();
                        for (ArrayList<Float> af : windowFormatter((ArrayList<Float>) cloneArrayListFloat(current_training_set))
                             ) {
                            current_set_variances.add(variance(af));//one per window
                        }
                        //export
                        export(context, activity_type);

                        //add to known dataset
                        MainActivity.Log("Size of trained sets: " + trained_variances_set.size() + "Size of expected trained sets: " + Activity.values().length);
                        trained_variances_set.set(activity_type.ordinal(), cloneArrayListFloat(current_set_variances));
                        MainActivity.Log("Added variances to set " + activity_type.ordinal() + " as " + activity_type.toString());

                    }
                },
                sampling_time//in milliseconds
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

    // Writes the data for trained activity
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
            MainActivity.Log(s);

            String data_string = formatData(current_training_set);
            outputStreamWriter.write(data_string, 0,  data_string.length());

            outputStreamWriter.close();
            fileOutputStream.close();

            MainActivity.Log("Finished writing");
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
            MainActivity.Log(s);

            String data_string = formatData(current_set_variances);
            outputStreamWriter.write(data_string, 0,  data_string.length());

            outputStreamWriter.close();
            fileOutputStream.close();

            MainActivity.Log("Finished writing");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ArrayList<Float> cloneArrayListFloat(List<Float> toClone) {
        ArrayList<Float> newList = new ArrayList<Float>();
        for (Float f : toClone) {
            newList.add(f.floatValue());
        }

        return newList;
    }
}

class knnSample implements Comparable
{
    public Float variance, distance;
    public Activity activity;
    public knnSample(Float variance, Float distance, Activity activity)
    {
        this.variance = variance;
        this.distance = distance;
        this.activity = activity;
    }

    @Override
    public int compareTo(Object o) {
        return this.distance.compareTo(((knnSample) o).distance);
    }
}


enum Activity {
    Walking,
    Standing,
    Jumping
}
