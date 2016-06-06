

package com.antony.remo.bwmessenger;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.antony.remo.bwmessenger.WiFiDirectApp.PTPLog;

import java.util.ArrayList;
import java.util.List;


public class DeviceListFragment extends ListFragment {  

	private static final String TAG = "PTP_ListFrag";
	
	WiFiDirectApp mApp = null;
	
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));
        mApp = (WiFiDirectApp)getActivity().getApplication();
        onPeersAvailable(mApp.mPeers);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    
    public WifiP2pDevice getDevice() {
        return device;
    }

    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener) getActivity()).showDetails(device);
    }

    
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(ConnectionService.getDeviceStatus(device.status));
                }
                PTPLog.d(TAG, "WiFiPeerListAdapter : getView : " + device.deviceName);
            }
            return v;
        }
    }

    
    public void updateThisDevice(WifiP2pDevice device) { 
    	TextView nameview = (TextView) mContentView.findViewById(R.id.my_name);
    	TextView statusview = (TextView) mContentView.findViewById(R.id.my_status);
    	if ( device != null) {
	    	PTPLog.d(TAG, "updateThisDevice: " + device.deviceName + " = " + ConnectionService.getDeviceStatus(device.status));
	    	this.device = device;
	        nameview.setText(device.deviceName);
	        statusview.setText(ConnectionService.getDeviceStatus(device.status));
    	} else if (this.device != null ){
    		nameview.setText(this.device.deviceName);
	        statusview.setText("WiFi Direct Disabled, please re-enable.");
    	}
    }

    
    public void onPeersAvailable(List<WifiP2pDevice> peerList) {   
        if (progressDialog != null && progressDialog.isShowing()) {  
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList);
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            PTPLog.d(WiFiDirectActivity.TAG, "onPeersAvailable : No devices found");
            return;
        }
    }

    public void clearPeers() {
    	getActivity().runOnUiThread(new Runnable() {
    		@Override public void run() {
    			if (progressDialog != null && progressDialog.isShowing()) {
    	            progressDialog.dismiss();
    	        }
    	        peers.clear();
    	        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    	        Toast.makeText(getActivity(), "p2p connection broken...please try again...", Toast.LENGTH_LONG).show();
    		}
    	});
    	
    }

   
    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                    }
        });
    }

    
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();
    }

}
