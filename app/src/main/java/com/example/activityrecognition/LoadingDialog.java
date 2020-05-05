package com.example.activityrecognition;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;

public class LoadingDialog {

    private Activity activity;
    private AlertDialog alertDialog;
    private boolean isShowingDialog;

    public LoadingDialog(Activity activity) {
        this.activity = activity;
        this.isShowingDialog = false;
    }

    public void startLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.popup_dialog, null));
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();

        this.isShowingDialog = true;
    }

    public void dismissDialog() {
        alertDialog.dismiss();
        this.isShowingDialog = false;
    }

    public boolean isShowingDialog () {
        return this.isShowingDialog;
    }
}
