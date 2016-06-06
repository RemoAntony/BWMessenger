

package com.antony.remo.bwmessenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.antony.remo.bwmessenger.DeviceListFragment.DeviceActionListener;
import com.antony.remo.bwmessenger.WiFiDirectApp.PTPLog;


public class WiFiDirectActivity extends Activity implements DeviceActionListener {

    public static final String TAG = "PTP_Activity";
    
    WiFiDirectApp mApp = null;

    boolean mHasFocus = false;
    private boolean retryChannel = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);   

        mApp = (WiFiDirectApp)getApplication();
        
        mApp.mHomeActivity = this;
        
        
        Intent serviceIntent = new Intent(this, ConnectionService.class);
        startService(serviceIntent);  
        
        PTPLog.d(TAG, "onCreate : home activity launched, start service anyway.");
    }

    
    @Override
    public void onResume() {
        super.onResume();
        mHasFocus = true;
        if( mApp.mThisDevice != null ){
        	PTPLog.d(TAG, "onResume : redraw this device details");
        	updateThisDevice(mApp.mThisDevice);
        	
        	
            if( mApp.mP2pInfo != null && mApp.mThisDevice.status == WifiP2pDevice.CONNECTED){
            	PTPLog.d(TAG, "onResume : redraw detail fragment");
            	onConnectionInfoAvailable(mApp.mP2pInfo);
            } else {
            	
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
	@Override
	public void onStop() {  
		super.onStop();
		mHasFocus = false;
	}

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	mApp.mHomeActivity = null;
    	PTPLog.d(TAG, "onDestroy: reset app home activity.");
    }

    
    public void resetData() {
    	runOnUiThread(new Runnable() {
    		@Override public void run() {
    			DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
    			DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
    			if (fragmentList != null) {
    				fragmentList.clearPeers();
    			}
    			if (fragmentDetails != null) {
    				fragmentDetails.resetViews();
    			}
    		}
    	});
    }
    
    
    public void updateThisDevice(final WifiP2pDevice device){
    	runOnUiThread(new Runnable() {
    		@Override public void run() {
    			DeviceListFragment fragment = (DeviceListFragment)getFragmentManager().findFragmentById(R.id.frag_list);
    			fragment.updateThisDevice(device);
    		}
    	});
    }
    
    
    public void onPeersAvailable(final WifiP2pDeviceList peerList){
    	runOnUiThread(new Runnable() {
    		@Override public void run() {
    			DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
    	    	fragmentList.onPeersAvailable(mApp.mPeers);  
    	    	DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
    	    	
    	    	for(WifiP2pDevice d : peerList.getDeviceList()){
    	    		if( d.status == WifiP2pDevice.FAILED ){
    	    			PTPLog.d(TAG, "onPeersAvailable: Peer status is failed " + d.deviceName );
    	    	    	fragmentDetails.resetViews();
    	    		}
    	    	}
    		}
    	});
    }
    
    
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
    	runOnUiThread(new Runnable() {
    		@Override public void run() {
    			DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
    			fragmentDetails.onConnectionInfoAvailable(info);
    		}
    	});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case android.R.id.home:   
        		Log.d(TAG, " navigating up or home clicked.");
        		
        		return true;
        		
            case R.id.atn_direct_enable:
            	if( !mApp.isP2pEnabled() ){
                    
                    
                    
            	
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, " WiFi direct support : already enabled. ");
                }
                return true;
            case R.id.atn_direct_discover:
                if( !mApp.isP2pEnabled() ){
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning, Toast.LENGTH_LONG).show();
                    return true;
                }

                
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();  
                
                PTPLog.d(TAG, "onOptionsItemSelected : start discoverying ");

                mApp.mP2pMan.discoverPeers(mApp.mP2pChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                        PTPLog.d(TAG, "onOptionsItemSelected : discovery succeed... " );
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                    	PTPLog.d(TAG, "onOptionsItemSelected : discovery failed !!! " + reasonCode);
                    	fragment.clearPeers();
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed, try again... ", Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
                
            case R.id.disconnect:
            	PTPLog.d(TAG, "onOptionsItemSelected : disconnect all connections and stop server ");
            	ConnectionService.getInstance().mConnMan.closeClient();
            	ConnectionService.getInstance().mConnMan.closeServer();
            	return true;
            	
            case R.id.about:
            	PTPLog.d(TAG, "onOptionsItemSelected : about ");
            	Toast.makeText(this, "Free China using Peer-Peer", Toast.LENGTH_LONG).show();
            	return true;
            	
            case R.id.help:
            	PTPLog.d(TAG, "onOptionsItemSelected : help ");
            	Toast.makeText(this, "learn to use Peer-Peer to fight against censorship", Toast.LENGTH_LONG).show();
            	return true;
            	
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }

    
    public void connect(WifiP2pConfig config) {
    	PTPLog.d(TAG, "connect : connect to server : " + config.deviceAddress);
    	
        mApp.mP2pMan.connect(mApp.mP2pChannel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                
            	Toast.makeText(WiFiDirectActivity.this, "Connect success..", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        PTPLog.d(TAG, "disconnect : removeGroup " );
        mApp.mP2pMan.removeGroup(mApp.mP2pChannel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                PTPLog.d(TAG, "Disconnect failed. Reason : 1=error, 2=busy; " + reasonCode);
                Toast.makeText(WiFiDirectActivity.this, "disconnect failed.." + reasonCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess() {
            	PTPLog.d(TAG, "Disconnect succeed. ");
                fragment.getView().setVisibility(View.GONE);
            }
        });
    }

    
    public void onChannelDisconnected() {  
        Toast.makeText(this, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("WiFi Direct down, please re-enable WiFi Direct")
	       .setCancelable(true)
	       .setPositiveButton("Re-enable WiFi Direct", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
	           }
	       })
	       .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                finish();
	           }
	       });
	
        AlertDialog info = builder.create();
        info.show();
    }

    @Override
    public void cancelDisconnect() {
        
        if (mApp.mP2pMan != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE || fragment.getDevice().status == WifiP2pDevice.INVITED) {
                mApp.mP2pMan.cancelConnect(mApp.mP2pChannel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection", Toast.LENGTH_SHORT).show();
                        PTPLog.d(TAG, "cancelConnect : success canceled...");
                    }
                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "cancelConnect: request failed. Please try again.. ", Toast.LENGTH_SHORT).show();
                        PTPLog.d(TAG, "cancelConnect : cancel connect request failed..." + reasonCode);
                    }
                });
            }
        }
    }
    
    
    public void startChatActivity(final String initMsg) {
    	if( ! mApp.mP2pConnected ){
    		Log.d(TAG, "startChatActivity : p2p connection is missing, do nothng...");
    		return;
    	}
    	
    	PTPLog.d(TAG, "startChatActivity : start chat activity fragment..." + initMsg);
    	runOnUiThread(new Runnable() {
    		@Override public void run() {
    			Intent i = mApp.getLauchActivityIntent(MainActivity.class, initMsg);
    	    	startActivity(i);
    		}
    	});
    }
}
