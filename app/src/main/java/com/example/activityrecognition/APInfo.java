package com.example.activityrecognition;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class APInfo {

    // Human readable access-point name (not unique)
    private String ssid;

    // Access-point identifier (unique)
    private String bssid;

    // RSS values of this captured access-point
    private ArrayList<Double> samples;

    // RSS value mean
    private double mean;

    // RSS value variance
    private double variance;

    public APInfo (String ssid, String bssid)
    {
        mean = variance = Double.NaN;
        this.ssid = ssid;
        this.bssid = bssid;
        this.samples = new ArrayList<Double>();
    }

    // Adder: Sample
    public void addSample (Double sample)
    {
        this.samples.add(sample);
        mean = variance = Double.NaN;
    }

    // Adder: Samples
    public void addSamples (ArrayList<Double> samples)
    {
        this.samples.addAll(samples);
        mean = variance = Double.NaN;
    }

    // Setter: Samples
    public void setSamples (ArrayList<Double> samples)
    {
        this.samples = samples;
        mean = variance = Double.NaN;
    }

    // Getter: Samples
    public ArrayList<Double> getSamples ()
    {
        return this.samples;
    }

    // Getter: ssid
    public String getSSID ()
    {
        return this.ssid;
    }

    // Getter: bssid
    public String getBSSID ()
    {
        return this.bssid;
    }

    // Getter: mean
    public double getMean ()
    {
        if (Double.isNaN(mean)) {
            this.recompute();
        }
        return this.mean;
    }

    // Getter: variance
    public double getVariance ()
    {
        if (Double.isNaN(variance)) {
            this.recompute();
        }
        return this.variance;
    }

    // Recomputes mean and variance
    private void recompute ()
    {
        this.variance = variance((this.mean = mean()));
    }

    private double mean () {
        double acc, n = (double)samples.size();

        if (samples.size() == 0) {
            return Double.NaN;
        } else {
            acc = samples.get(0);
        }

        for (int i = 1; i < samples.size(); ++i) {
            acc += this.samples.get(i);
        }

        return (acc / n);
    }

    private double variance (double mean)
    {
        double acc, n = (double)samples.size();

        if (samples.size() == 1) {
            return 0;
        }

        if (samples.size() == 0) {
            return Double.NaN;
        } else {
            acc = (samples.get(0) - mean) * (samples.get(0) - mean);
        }

        for (int i = 1; i < samples.size(); ++i) {
            acc += (samples.get(i) - mean) * (samples.get(i) - mean);
        }

        return (acc / (n - 1.0));
    }

    public void serialize (OutputStreamWriter w) {
        String fmt = bssid + " " + ssid;
        for (Double d : samples) {
            fmt = fmt + String.format(" %f", d);
        }
        fmt += "\n";
        try {
            w.write(fmt);
        } catch (IOException e) {
            Log.e("I/O", "APInfo (" + bssid +") failed: " + e.toString());
        }
    }
}
