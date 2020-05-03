package com.example.activityrecognition;

import android.net.wifi.ScanResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class Cell {

    // The cell identifier
    private int id;

    // Scan Information
    private ArrayList<ArrayList<ScanResult>> scans;

    // The training set
    private ArrayList<ArrayList<Integer>> training_set;

    public Cell(int id) {
        this.id = id;
    }

    // Inserts a new scan set
    public void append (ArrayList<ScanResult> scan_results)
    {
        // Base Case: No data
        if (scans == null) {
            scans = new ArrayList<ArrayList<ScanResult>>();
        }

        // Append the scan results
        scans.add(scan_results);
    }

    // Getter: id
    public int getId () { return this.id; }

    // Getter: scans
    public ArrayList<ArrayList<ScanResult>> getScans ()
    {
        return scans;
    }

    // Getter: training set
    public ArrayList<ArrayList<Integer>> get_training_set () {
        return training_set;
    }

    // Clear scans
    public void clear ()
    {
        this.scans = new ArrayList<ArrayList<ScanResult>>();
    }

    // Returns the training set. Requires the entire set of all BSSIDs detected
    public void make_training_set (ArrayList<String> all_bssids)
    {
        ArrayList<ArrayList<Integer>> training_set = new ArrayList<ArrayList<Integer>>();

        // For each scan we have: Build a binary string
        for (ArrayList<ScanResult> scan : scans) {
            training_set.add(makeCode(scan, all_bssids));
        }

        // Assign the training set
        this.training_set = training_set;
    }

    // Creates a hamming code for the given ScanResult array and BSSID set
    public static ArrayList<Integer> makeCode (ArrayList<ScanResult> scan,
                                                ArrayList<String> all_bssids)
    {
        ArrayList<Integer> binary_string = new ArrayList<Integer>();
        for (String bssid : all_bssids) {
            if (Cell.contains(scan, bssid)) {
                binary_string.add(1);
            } else {
                binary_string.add(0);
            }
        }
        return binary_string;
    }

    // Returns boolean true if the given set of ScanResults contains the BSSID
    private static boolean contains (ArrayList<ScanResult> list, String bssid)
    {
        for (ScanResult r : list) {
            if (r.BSSID.equals(bssid)) {
                return true;
            }
        }
        return false;
    }
}
