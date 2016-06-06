package com.antony.remo.bwmessenger;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.antony.remo.bwmessenger.WiFiDirectApp.PTPLog;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


public class ChatFragment extends ListFragment {
	private static final String TAG = "PTP_ChatFrag";
	
	WiFiDirectApp mApp = null; 
	private static MainActivityDirect mActivity = null;
	
	private ArrayList<MessageRow> mMessageList = null;   
    private ArrayAdapter<MessageRow> mAdapter= null;
    
    
    
	
    public static ChatFragment newInstance(Activity activity, String groupOwnerAddr, String msg) {
    	ChatFragment f = new ChatFragment();
    	mActivity = (MainActivityDirect)activity;
    	
        Bundle args = new Bundle();
        args.putString("groupOwnerAddr", groupOwnerAddr);
        args.putString("initMsg", msg);
        f.setArguments(args);
        Log.d(TAG, "newInstance :" + groupOwnerAddr + " : " + msg);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        mApp = (WiFiDirectApp)mActivity.getApplication();
        
        setRetainInstance(true);   
    }
    
    
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	outState.putParcelableArrayList("MSG_LIST", mMessageList);
    	Log.d(TAG, "onSaveInstanceState. " + mMessageList.get(0).mMsg);
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
        View contentView = inflater.inflate(R.layout.chat_frag, container, false);  
        final EditText inputEditText = (EditText)contentView.findViewById(R.id.edit_input);
        final Button sendBtn = (Button)contentView.findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				String inputMsg = inputEditText.getText().toString();
				inputEditText.setText("");
				InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
				MessageRow row = new MessageRow(mApp.mDeviceName, inputMsg, null);
				appendChatMessage(row);
				String jsonMsg = mApp.shiftInsertMessage(row);
				PTPLog.d(TAG, "sendButton clicked: sendOut data : " + jsonMsg);
				mActivity.pushOutMessage(jsonMsg);
			}
        });
        
        String groupOwnerAddr = getArguments().getString("groupOwnerAddr");
        String msg = getArguments().getString("initMsg");
        PTPLog.d(TAG, "onCreateView : fragment view created: msg :" + msg);
        
    	if( savedInstanceState != null ){
            mMessageList = savedInstanceState.getParcelableArrayList("MSG_LIST");
            Log.d(TAG, "onCreate : savedInstanceState: " + mMessageList.get(0).mMsg);
        }else if( mMessageList == null ){
        	
            mMessageList = new ArrayList<MessageRow>();
            jsonArrayToList(mApp.mMessageArray, mMessageList);
            Log.d(TAG, "onCreate : jsonArrayToList : " + mMessageList.size() );
        }else {
        	Log.d(TAG, "onCreate : setRetainInstance good : ");
        }
        
        mAdapter = new ChatMessageAdapter(mActivity, mMessageList);
        
        setListAdapter(mAdapter);  
        
        PTPLog.d(TAG, "onCreate chat msg fragment: devicename : " + mApp.mDeviceName + " : " + getArguments().getString("initMsg"));
        return contentView;
    }
    
    @Override 
    public void onDestroyView(){ 
    	super.onDestroyView(); 
    	Log.d(TAG, "onDestroyView: ");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {  
        super.onActivityCreated(savedInstanceState);
        
        if( mMessageList.size() > 0){
        	getListView().smoothScrollToPosition(mMessageList.size()-1);
        }
        
        setHasOptionsMenu(true);
        Log.d(TAG, "onActivityCreated: chat fragment displayed ");
    }
    
    
    public void appendChatMessage(MessageRow row) {
    	Log.d(TAG, "appendChatMessage: chat fragment append msg: " + row.mSender + " ; " + row.mMsg);
    	mMessageList.add(row);
    	getListView().smoothScrollToPosition(mMessageList.size()-1);
    	mAdapter.notifyDataSetChanged();  
    	return;
    }
    
    private void jsonArrayToList(JSONArray jsonarray, List<MessageRow> list) {
    	try{
    		for(int i=0;i<jsonarray.length();i++){
    			MessageRow row = MessageRow.parseMesssageRow(jsonarray.getJSONObject(i));
    			PTPLog.d(TAG, "jsonArrayToList: row : " + row.mMsg);
    			list.add(row);
    		}
    	}catch(JSONException e){
    		PTPLog.e(TAG, "jsonArrayToList: " + e.toString());
    	}
    }
    
    
    final class ChatMessageAdapter extends ArrayAdapter<MessageRow> {

    	public static final int VIEW_TYPE_MYMSG = 0;
    	public static final int VIEW_TYPE_INMSG = 1;
    	public static final int VIEW_TYPE_COUNT = 2;    
    	private LayoutInflater mInflater;
    	
		public ChatMessageAdapter(Context context, List<MessageRow> objects){
			super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
		
		@Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }
		
		@Override
        public int getItemViewType(int position) {
			MessageRow item = this.getItem(position);
			if ( item.mSender.equals(mApp.mDeviceName )){
				return VIEW_TYPE_MYMSG;
			}
			return VIEW_TYPE_INMSG;			
		}
		
		
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;  
			MessageRow item = this.getItem(position);
			boolean mymsg = false;
			
			if ( getItemViewType(position) == VIEW_TYPE_MYMSG){
				if( view == null ){
	            	view = mInflater.inflate(R.layout.chat_row_mymsg, null);  
	            }
				mymsg = true;
				
			} else {
				if( view == null ){
	            	view = mInflater.inflate(R.layout.chat_row_inmsg, null);  
	            }
				
			}
			
            TextView sender = (TextView)view.findViewById(R.id.sender);
            sender.setText(item.mSender);
            
            TextView msgRow = (TextView)view.findViewById(R.id.msg_row);
            msgRow.setText(item.mMsg);
            if( mymsg ){
            	msgRow.setBackgroundResource(R.color.my_msg_background);	
            }else{
            	msgRow.setBackgroundResource(R.color.in_msg_background);
            }
            
            TextView time = (TextView)view.findViewById(R.id.time);
            time.setText(item.mTime);
            
            Log.d(TAG, "getView : " + item.mSender + " " + item.mMsg + " " + item.mTime);
            return view;
		}
    }
}
