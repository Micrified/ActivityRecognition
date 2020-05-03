package com.example.activityrecognition;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class LocationManager implements Serializable {

    // Singleton instance (one one instance of this class may exist)
    transient private static LocationManager singleton = null;

    // The number of cells
    private static final int cell_count = 4;

    // Cells
    private List<Cell> cells;

    // K-NN value (choose approx sqrt(N))
    private static int k = 3;

    // A boolean indicating if training is required
    private boolean training_required = true;

    // Inner class for the training-set
    class TrainingSample {
        public int cell_id;
        public ArrayList<Integer> code;
        public TrainingSample (int cell_id, ArrayList<Integer> code) {
            this.cell_id = cell_id; this.code = code;
        }
    }

    // Training set
    private ArrayList<TrainingSample> training_set;

    // Set of unique BSSIDs
    private ArrayList<String> unique_bssids;


    // Singleton constructor
    private LocationManager ()
    {
        // Initialize cells array
        this.cells = new ArrayList<Cell>();

        // Populate the cells array
        for (int id = 0; id < cell_count; ++id) {
            cells.add(new Cell(id));
        }
    }

    // Singleton fetcher
    public static LocationManager getInstance()
    {
        if (singleton == null) {
            singleton = new LocationManager();
        }
        return singleton;
    }

    // Adds scan results to a cell
    public void addScan (int cell, ArrayList<ScanResult> scan)
    {
        cells.get(cell).append(scan);
        this.training_required = true;
    }

    // Clears scan results from a cell
    public void clearScans (int cell)
    {
        cells.get(cell).clear();
        this.training_required = true;
    }

    // Classifier
    public int classify (ArrayList<ScanResult> test_sample)
    {
        int[] cell_count = {0,0,0,0};

        // Train if needed (new data may have been added to some cells)
        if (training_required) {
            train();
        }

        // Convert the test-sample to a binary hamming code
        final ArrayList<Integer> test_code = Cell.makeCode(test_sample, unique_bssids);

        // Sort by smallest hamming distance
        Collections.sort(training_set, new Comparator() {
                    public int compare(Object a, Object b) {
           int h_a = LocationManager.hamming(test_code, (ArrayList<Integer>)a);
           int h_b = LocationManager.hamming(test_code, (ArrayList<Integer>)b);
           if (h_a < h_b) {
               return -1;
           } else {
               return 1;
           }
        }});

        // Classify using K-NN (increment cell label if sample belonged to it)
        for (int i = 0; i < k; ++i) {
            cell_count[training_set.get(i).cell_id]++;
        }

        // Return the majority vote
        if (cell_count[0] > cell_count[1]) {
            if (cell_count[2] > cell_count[3]) {
                return (cell_count[0] > cell_count[2]) ? 0 : 2;
            } else {
                return (cell_count[0] > cell_count[3]) ? 0 : 3;
            }
        } else {
            if (cell_count[2] > cell_count[3]) {
                return (cell_count[1] > cell_count[2]) ? 1 : 2;
            } else {
                return (cell_count[1] > cell_count[3]) ? 1 : 3;
            }
        }
    }

    // Training (post-processing)
    private void train ()
    {
        // Reset the training set
        training_set = new ArrayList<TrainingSample>();

        // Generate the list of unique BSSIDs
        unique_bssids = get_unique_bssids();

        // For all cells: generate the training set
        for (Cell c : cells) {
            c.make_training_set(unique_bssids);
        }

        // Building the overall training set
        for (Cell c : cells) {
            ArrayList<ArrayList<Integer>> training_sets = c.get_training_set();
            for (ArrayList<Integer> code : training_sets) {
                training_set.add(new TrainingSample(c.getId(), code));
            }
        }

        // Mark complete
        training_required = false;
    }

    // Creates set of unique access points from the set of cells
    private ArrayList<String> get_unique_bssids ()
    {
        ArrayList<String> unique_bssids = new ArrayList<String>();
        HashSet<String> set = new HashSet<String>();

        // Insert all BSSIDs for all scans across all cells into a hashmap
        for (Cell c : cells)
        for (ArrayList<ScanResult> scan : c.getScans())
        for (ScanResult result : scan)
        set.add(result.BSSID);

        // Create an iterator over the hashmap to get the values
        Iterator iterator = set.iterator();

        // Transfer the unique contents over to the array
        while (iterator.hasNext()) {
            unique_bssids.add((String)iterator.next());
        }

        // Return the array of unique BSSIDs
        return unique_bssids;
    }

    // Hamming distance between two codes
    private static int hamming (ArrayList<Integer> a, ArrayList<Integer> b)
    {
        int diff = 0;
        for (int i = 0; i < a.size(); ++i) {
            diff = diff + Math.abs(a.get(i) - b.get(i));
        }
        return diff;
    }
}
