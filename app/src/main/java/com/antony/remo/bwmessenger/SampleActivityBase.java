package com.antony.remo.bwmessenger;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;



public class SampleActivityBase extends FragmentActivity {

    public static final String TAG = "SampleActivityBase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected  void onStart() {
        super.onStart();
        initializeLogging();
    }

    
    public void initializeLogging() {
        
        
        LogWrapper logWrapper = new LogWrapper();
        com.antony.remo.bwmessenger.Log.setLogNode(logWrapper);
        Log.i(TAG, "Ready");
    }
}