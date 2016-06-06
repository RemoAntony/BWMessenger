package com.antony.remo.bwmessenger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.antony.remo.bwmessenger.WiFiDirectApp.PTPLog;

import java.nio.channels.SocketChannel;

import static com.antony.remo.bwmessenger.Constants.MSG_BROKEN_CONN;
import static com.antony.remo.bwmessenger.Constants.MSG_FINISH_CONNECT;
import static com.antony.remo.bwmessenger.Constants.MSG_NEW_CLIENT;
import static com.antony.remo.bwmessenger.Constants.MSG_NULL;
import static com.antony.remo.bwmessenger.Constants.MSG_PULLIN_DATA;
import static com.antony.remo.bwmessenger.Constants.MSG_PUSHOUT_DATA;
import static com.antony.remo.bwmessenger.Constants.MSG_REGISTER_ACTIVITY;
import static com.antony.remo.bwmessenger.Constants.MSG_SELECT_ERROR;
import static com.antony.remo.bwmessenger.Constants.MSG_STARTCLIENT;
import static com.antony.remo.bwmessenger.Constants.MSG_STARTSERVER;

public class ConnectionService extends Service implements ChannelListener, PeerListListener, ConnectionInfoListener {  
	
	private static final String TAG = "PTP_Serv";
	
	private static ConnectionService _sinstance = null;

	private  WorkHandler mWorkHandler;
    private  MessageHandler mHandler;
    
    boolean retryChannel = false;
    
    WiFiDirectApp mApp;
    MainActivityDirect mActivity;    
	ConnectionManager mConnMan;
	
	
    private void _initialize() {
    	if (_sinstance != null) {
    		PTPLog.d(TAG, "_initialize, already initialized, do nothing.");
            return;
        }

    	_sinstance = this;
    	mWorkHandler = new WorkHandler(TAG);
        mHandler = new MessageHandler(mWorkHandler.getLooper());
        
        mApp = (WiFiDirectApp)getApplication();
        mApp.mP2pMan = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
        PTPLog.d(TAG, "_initialize, get p2p service and init channel !!!");
        
        mConnMan = new ConnectionManager(this);
    }
    
    public static ConnectionService getInstance(){
    	return _sinstance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        _initialize();
        PTPLog.d(TAG, "onCreate : done");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	_initialize();
    	processIntent(intent);
    	return START_STICKY;
    }
    
    
    private void processIntent(Intent intent){
    	if( intent == null){
    		return;
    	}
    	
    	String action = intent.getAction();
    	PTPLog.d(TAG, "processIntent: " + intent.toString());
    	
    	if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {  
              int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
              if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                  
            	  mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
            	  AppPreferences.setStringToPref(mApp, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED, "1");
            	  PTPLog.d(TAG, "processIntent : WIFI_P2P_STATE_CHANGED_ACTION : enabled, re-init p2p channel to framework ");
              } else {
            	  mApp.mThisDevice = null;  	
            	  mApp.mP2pChannel = null;
            	  mApp.mPeers.clear();
            	  PTPLog.d(TAG, "processIntent : WIFI_P2P_STATE_CHANGED_ACTION : disabled, null p2p channel to framework ");
            	  if( mApp.mHomeActivity != null ){
            		  mApp.mHomeActivity.updateThisDevice(null);
            		  mApp.mHomeActivity.resetData();
            	  }
            	  AppPreferences.setStringToPref(mApp, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED, "0");
              }
          } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {  
        	  
              
              
              
        	  PTPLog.d(TAG, "processIntent: WIFI_P2P_PEERS_CHANGED_ACTION: call requestPeers() to get list of peers");
              if (mApp.mP2pMan != null) {
            	  mApp.mP2pMan.requestPeers(mApp.mP2pChannel, (PeerListListener) this);
              }
          } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
              if (mApp.mP2pMan == null) {
                  return;
              }

              NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
              PTPLog.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION : " + networkInfo.getReason() + " : " + networkInfo.toString());
              if (networkInfo.isConnected()) {
            	  Log.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION: p2p connected ");
                  
                  mApp.mP2pMan.requestConnectionInfo(mApp.mP2pChannel, this);  
              } else {
            	  Log.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION: p2p disconnected, mP2pConnected = false..closeClient.."); 
            	  mApp.mP2pConnected = false;
            	  mApp.mP2pInfo = null;   
            	  mConnMan.closeClient();
            	  
            	  if( mApp.mHomeActivity != null ){
            		  mApp.mHomeActivity.resetData();
            	  }
              }
          } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {  
        	  
        	  mApp.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        	  mApp.mDeviceName = mApp.mThisDevice.deviceName;
        	  PTPLog.d(TAG, "processIntent: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION " + mApp.mThisDevice.deviceName);
        	  if( mApp.mHomeActivity != null ){
        		  mApp.mHomeActivity.updateThisDevice(mApp.mThisDevice);
        	  }
          }
    }
    
    
    @Override
    public void onChannelDisconnected() {
    	if( !retryChannel ){
    		PTPLog.d(TAG, "onChannelDisconnected : retry initialize() ");
    		mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
    		if( mApp.mHomeActivity != null) {
    			mApp.mHomeActivity.resetData();
    		}
    		retryChannel = true;
    	}else{
    		PTPLog.d(TAG, "onChannelDisconnected : stop self, ask user to re-enable.");
    		if( mApp.mHomeActivity != null) {
    			mApp.mHomeActivity.onChannelDisconnected();
    		}
    		stopSelf();
    	}
    }
    
    
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
    	mApp.mPeers.clear();
    	mApp.mPeers.addAll(peerList.getDeviceList());
		PTPLog.d(TAG, "onPeersAvailable : update peer list...");
		
    	WifiP2pDevice connectedPeer = mApp.getConnectedPeer();
    	if( connectedPeer != null ){
    		PTPLog.d(TAG, "onPeersAvailable : exist connected peer : " + connectedPeer.deviceName);
    	} else {
    		
    	}
    	
    	if(mApp.mP2pInfo != null && connectedPeer != null ){
    		if( mApp.mP2pInfo.groupFormed && mApp.mP2pInfo.isGroupOwner ){
    			PTPLog.d(TAG, "onPeersAvailable : device is groupOwner: startSocketServer");
    			mApp.startSocketServer();
    		}else if( mApp.mP2pInfo.groupFormed && connectedPeer != null ){
    			
    			
    			
    		}
    	}
    	
    	if( mApp.mHomeActivity != null){
    		mApp.mHomeActivity.onPeersAvailable(peerList);
    	}
    }
    
    
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
    	Log.d(TAG, "onConnectionInfoAvailable: " + info.groupOwnerAddress.getHostAddress());
        if (info.groupFormed && info.isGroupOwner ) {
			
            
        	
        	
        } else if (info.groupFormed) {
        	PTPLog.d(TAG, "onConnectionInfoAvailable: device is client, connect to group owner: startSocketClient ");
        	mApp.startSocketClient(info.groupOwnerAddress.getHostAddress());
        }
        mApp.mP2pConnected = true;
        mApp.mP2pInfo = info;   
    }
    
    private void enableStartChatActivity() {
    	if( mApp.mHomeActivity != null ){
			PTPLog.d(TAG, "enableStartChatActivity :  nio channel ready, enable start chat !");
			mApp.mHomeActivity.onConnectionInfoAvailable(mApp.mP2pInfo);
    	}
    }
    
	@Override
	public IBinder onBind(Intent arg0) { return null; }
	
	public Handler getHandler() {
        return mHandler;
    }

	
    final class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            processMessage(msg);
        }
    }
    
    
    private void processMessage(android.os.Message msg) {
    	
        switch (msg.what) {
        case MSG_NULL:
        	break;
        case MSG_REGISTER_ACTIVITY:
        	PTPLog.d(TAG, "processMessage: onActivityRegister to chat fragment...");
        	onActivityRegister((MainActivityDirect)msg.obj, msg.arg1);
        	break;
        case MSG_STARTSERVER:
        	PTPLog.d(TAG, "processMessage: startServerSelector...");
        	if( mConnMan.startServerSelector() >= 0){
        		enableStartChatActivity();
        	}
        	break;
        case MSG_STARTCLIENT:
        	PTPLog.d(TAG, "processMessage: startClientSelector...");
        	if( mConnMan.startClientSelector((String)msg.obj) >= 0){
        		enableStartChatActivity();
        	}
        	break;
        case MSG_NEW_CLIENT:
        	PTPLog.d(TAG, "processMessage:  onNewClient...");
        	mConnMan.onNewClient((SocketChannel)msg.obj);
        	break;
        case MSG_FINISH_CONNECT:
        	PTPLog.d(TAG, "processMessage:  onFinishConnect...");
        	mConnMan.onFinishConnect((SocketChannel)msg.obj);
        	break;
        case MSG_PULLIN_DATA:
        	PTPLog.d(TAG, "processMessage:  onPullIndata ...");
        	onPullInData((SocketChannel)msg.obj, msg.getData());
        	break;
        case MSG_PUSHOUT_DATA:
        	PTPLog.d(TAG, "processMessage: onPushOutData...");
        	onPushOutData((String)msg.obj);
        	break;
        case MSG_SELECT_ERROR:
        	PTPLog.d(TAG, "processMessage: onSelectorError...");
        	mConnMan.onSelectorError();
        	break;
        case MSG_BROKEN_CONN:
        	PTPLog.d(TAG, "processMessage: onBrokenConn...");
        	mConnMan.onBrokenConn((SocketChannel)msg.obj);
        	break;
        default:
        	break;
        }
    }
    
    
    private void onActivityRegister(MainActivityDirect activity, int register){
    	Log.d(TAG, "onActivityRegister : activity register itself to service : " + register);
    	if( register == 1){
    		mActivity = activity;
    	}else{
    		mActivity = null;    
    	}
    }
    
    
    private String onPullInData(SocketChannel schannel, Bundle b){
    	String data = b.getString("DATA");
    	Log.d(TAG, "onDataIn : recvd msg : " + data);
    	mConnMan.onDataIn(schannel, data);  
    	MessageRow row = MessageRow.parseMessageRow(data);
    	
    	mApp.shiftInsertMessage(row);
    	showNotification(row);
    	
    	showInActivity(row);
    	return data;
    }
    
    
    private void onPushOutData(String data){
    	Log.d(TAG, "onPushOutData : " + data);
    	mConnMan.pushOutData(data);
    }
    
    
    public int connectionSendData(String jsonstring) {
    	Log.d(TAG, "connectionSendData : " + jsonstring);
    	new SendDataAsyncTask(mConnMan, jsonstring).execute();
    	return 0;
    }
    
    
    public class SendDataAsyncTask extends AsyncTask<Void, Void, Integer> {
    	private String data;
    	private ConnectionManager connman;
    	
    	public SendDataAsyncTask(ConnectionManager conn, String jsonstring) {
    		connman = conn;
    		data = jsonstring;
    	}
    	
		@Override
		protected Integer doInBackground(Void... params) {
			return connman.pushOutData(data);
		}
		 
		@Override
		protected void onPostExecute(Integer result) {
			Log.d(TAG, "SendDataAsyncTask : onPostExecute:  " + data + " len: " + result);
		}
    }
    
    
    public void showNotification(MessageRow row) {
    	
    	NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    	Notification notification = new Notification(R.drawable.ic_action_discover, row.mMsg, System.currentTimeMillis());
    	notification.defaults |= Notification.DEFAULT_VIBRATE;
    	CharSequence title = row.mSender;
    	CharSequence text = row.mMsg;

    	
    	Intent notificationIntent = mApp.getLauchActivityIntent(MainActivityDirect.class, row.mMsg);
    	
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

    	
    	notificationManager.notify(1, notification);
    	PTPLog.d(TAG, "showNotification: " + row.mMsg);
    }
    
    
    private void showInActivity(final MessageRow row){
    	PTPLog.d(TAG, "showInActivity : " + row.mMsg);
    	if( mActivity != null ){
    		mActivity.showMessage(row);
    	} else {
    		if( mApp.mHomeActivity != null && mApp.mHomeActivity.mHasFocus == true ){
    			PTPLog.d(TAG, "showInActivity :  chat activity down, force start only when home activity has focus !");
    			mApp.mHomeActivity.startChatActivity(row.mMsg);
    		}else{
    			PTPLog.d(TAG, "showInActivity :  Home activity down, do nothing, notification will launch it...");
    		}
    	}
    }
    
    public static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
            	
                return "Available";
            case WifiP2pDevice.INVITED:
            	
                return "Invited";
            case WifiP2pDevice.CONNECTED:
            	
                return "Connected";
            case WifiP2pDevice.FAILED:
            	
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
            	
                return "Unavailable";
            default:
                return "Unknown = " + deviceStatus;
        }
    }
 }
