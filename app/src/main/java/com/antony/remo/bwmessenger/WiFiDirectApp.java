package com.antony.remo.bwmessenger;

import android.app.Application;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.antony.remo.bwmessenger.Constants.MSG_STARTCLIENT;
import static com.antony.remo.bwmessenger.Constants.MSG_STARTSERVER;

public class WiFiDirectApp extends Application {

	private static final String TAG = "PTP_APP";
	
	WifiP2pManager mP2pMan = null;;
	Channel mP2pChannel = null;
	boolean mP2pConnected = false;
	String mMyAddr = null;
	String mDeviceName = null;   
	
	WifiP2pDevice mThisDevice = null;
	WifiP2pInfo mP2pInfo = null;  
	
	boolean mIsServer = false;
	
	WiFiDirectActivity mHomeActivity = null;
	List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();  
	JSONArray mMessageArray = new JSONArray();		
	
	@Override
    public void onCreate() {
        super.onCreate();
    }
	
	
	public boolean isP2pEnabled() {
		String state = AppPreferences.getStringFromPref(this, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED);
		if ( state != null && "1".equals(state.trim())){
			return true;
		}
		return false;
	}
	
	
    public void startSocketServer() {
    	Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
    	msg.what = MSG_STARTSERVER;
    	ConnectionService.getInstance().getHandler().sendMessage(msg);
    }
    
    
    public void startSocketClient(String hostname) {
    	Log.d(TAG, "startSocketClient : client connect to group owner : " + hostname);
    	Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
    	msg.what = MSG_STARTCLIENT;
    	msg.obj = hostname;
    	ConnectionService.getInstance().getHandler().sendMessage(msg);
    }
    
    
    public WifiP2pDevice getConnectedPeer(){
    	WifiP2pDevice peer = null;
		for(WifiP2pDevice d : mPeers ){ 
    		PTPLog.d(TAG, "getConnectedPeer : device : " + d.deviceName + " status: " + ConnectionService.getDeviceStatus(d.status));
    		if( d.status == WifiP2pDevice.CONNECTED){
    			peer = d;
    		}
    	}
    	return peer;
    }
    
    
    public void shiftInsertMessage(String jsonmsg){
    	JSONObject jsonobj = JSONUtils.getJsonObject(jsonmsg);
    	mMessageArray.put(jsonobj);
    	mMessageArray = JSONUtils.truncateJSONArray(mMessageArray, 10);  
    }
    
    public String shiftInsertMessage(MessageRow row) {
    	JSONObject jsonobj = MessageRow.getAsJSONObject(row);
    	if( jsonobj != null ){
    		mMessageArray.put(jsonobj);
    	}
    	mMessageArray = JSONUtils.truncateJSONArray(mMessageArray, 10);  
    	return jsonobj.toString();
    }
    
    public void clearMessages() {
    	mMessageArray = new JSONArray();
    }
    
    
    public Intent getLauchActivityIntent(Class<?> cls, String initmsg){
    	Intent i = new Intent(this, cls);
    	i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	i.putExtra("FIRST_MSG", initmsg);
    	return i;
    }
    
    public void setMyAddr(String addr){
    	mMyAddr = addr;
    }
    
	public static class PTPLog {
		public static void i(String tag, String msg) {
            Log.i(tag, msg);
        }
		public static void d(String tag, String msg) {
            Log.d(tag, msg);
        }
		public static void e(String tag, String msg) {
            Log.e(tag, msg);
        }
	}

}
