package com.antony.remo.bwmessenger;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    Toolbar tb;
    Switch sw;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tb=(Toolbar)findViewById(R.id.toolbarmain);
        setSupportActionBar(tb);
        SharedPreferences pref = getSharedPreferences("type",
                MODE_PRIVATE);
        if(pref.getString("messenger",null)==null)
        getSharedPreferences("type", MODE_PRIVATE).edit()
                .putString("messenger", "bluetooth")
                .commit();
        initializeLogging();
        sw=(Switch) findViewById(R.id.mainswitch);
        if(pref.getString("messenger",null).equals("bluetooth")) {
            ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#6C2DC7"));
            tb.setBackgroundDrawable(colorDrawable);
            sw.setChecked(false);
            bmsg();
        }
        else{
            ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#990012"));
            tb.setBackgroundDrawable(colorDrawable);
            sw.setChecked(true);
            hmsg();
         //   finish();
        }
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!b){
                    ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#6C2DC7"));
                    tb.setBackgroundDrawable(colorDrawable);
                   sw.setText("Bluetooth");
                    getSharedPreferences("type", MODE_PRIVATE).edit()
                            .putString("messenger", "bluetooth")
                            .commit();
                    bmsg();
                 //   finish();
                }else{
                    ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#990012"));
                    tb.setBackgroundDrawable(colorDrawable);
                    sw.setText("WIFI Hotspot");
                    getSharedPreferences("type", MODE_PRIVATE).edit()
                            .putString("messenger", "hotspot")
                            .commit();
                hmsg();
              //      finish();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        
        
        int id = item.getItemId();


        

        return super.onOptionsItemSelected(item);
    }

    public void bmsg()
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Toast.makeText(MainActivity.this,"Bluetooth not supported in this device!",Toast.LENGTH_LONG).show();
        }else{
            if (bluetoothAdapter.isEnabled()){
                if(bluetoothAdapter.isDiscovering()){
                    Toast.makeText(MainActivity.this,"Bluetooth is currently in device discovery process!",Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(MainActivity.this,"Bluetooth is Enabled!",Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(MainActivity.this,"Bluetooth not Enabled,please turn it on!",Toast.LENGTH_LONG).show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        BluetoothChatFragment fragment = new BluetoothChatFragment();
        transaction.replace(R.id.sample_content_fragment, fragment);
        transaction.commit();
    }
    public void hmsg()
    {
        /*
        Intent i=new Intent(MainActivity.this,WiFiDirectActivity.class);
        startActivity(i);*/
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Toast.makeText(MainActivity.this,"Bluetooth not supported in this device!",Toast.LENGTH_LONG).show();
        }else{
            if (bluetoothAdapter.isEnabled()){
                if(bluetoothAdapter.isDiscovering()){
                    Toast.makeText(MainActivity.this,"Bluetooth is currently in device discovery process!",Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(MainActivity.this,"Bluetooth is Enabled!",Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(MainActivity.this,"Bluetooth not Enabled,please turn it on!",Toast.LENGTH_LONG).show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        WifiChatFragment fragment = new WifiChatFragment();
        transaction.replace(R.id.sample_content_fragment, fragment);
        transaction.commit();
    }
    private boolean mLogShown;
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }


    public void initializeLogging() {
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());
    }
}
