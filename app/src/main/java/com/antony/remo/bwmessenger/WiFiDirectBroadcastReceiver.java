

package com.antony.remo.bwmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "PTP_Recv";

    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent serviceIntent = new Intent(context, ConnectionService.class);  
        serviceIntent.setAction(action);   
        serviceIntent.putExtras(intent);
    	context.startService(serviceIntent);  
    }
}
