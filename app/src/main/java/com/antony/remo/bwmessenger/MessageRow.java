package com.antony.remo.bwmessenger;

import android.os.Parcel;
import android.os.Parcelable;

import com.antony.remo.bwmessenger.WiFiDirectApp.PTPLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.antony.remo.bwmessenger.Constants.MSG_CONTENT;
import static com.antony.remo.bwmessenger.Constants.MSG_SENDER;
import static com.antony.remo.bwmessenger.Constants.MSG_TIME;


public class MessageRow implements Parcelable {
	private final static String TAG = "PTP_MSG";
	
	public String mSender;
	public String mMsg;
	public String mTime;
	public static final String mDel = "^&^";
	
	private MessageRow() { 
	    this.mSender = null;
		this.mTime = null;
		this.mMsg = null;
	}
	
	public MessageRow(String sender, String msg, String time){
		mTime = time;
		if( time == null ){
			Date now = new Date();
			
			
			mTime = new SimpleDateFormat("h:mm a").format(now);
		} 
		mSender = sender;
		mMsg = msg;
	}
	
	public MessageRow(Parcel in) {
        readFromParcel(in);
    }
	
	public String toString() {
		return mSender + mDel + mMsg + mDel + mTime;
	}
	
	
	public static JSONObject getAsJSONObject(MessageRow msgrow) {
		JSONObject jsonobj = new JSONObject();
		try{
			jsonobj.put(MSG_SENDER, msgrow.mSender);
			jsonobj.put(MSG_TIME, msgrow.mTime);
			jsonobj.put(MSG_CONTENT, msgrow.mMsg);
		}catch(JSONException e){
			PTPLog.e(TAG, "getAsJSONObject : " + e.toString());
		}
		return jsonobj;
	}
	
	
	public static MessageRow parseMesssageRow(JSONObject jsonobj) {
		MessageRow row = null;
		if( jsonobj != null ){
			try{
				row = new MessageRow(jsonobj.getString(MSG_SENDER), jsonobj.getString(MSG_CONTENT), jsonobj.getString(MSG_TIME)); 
			}catch(JSONException e){
				PTPLog.e(TAG, "parseMessageRow: " + e.toString());
			}
		}
		return row;
	}
	
	
	public static MessageRow parseMessageRow(String jsonMsg){
		JSONObject jsonobj = JSONUtils.getJsonObject(jsonMsg);
		PTPLog.d(TAG, "parseMessageRow : " + jsonobj.toString());
		return parseMesssageRow(jsonobj);
	}

	public static final Parcelable.Creator<MessageRow> CREATOR = new Parcelable.Creator<MessageRow>() {
        public MessageRow createFromParcel(Parcel in) {
            return new MessageRow(in);
        }
 
        public MessageRow[] newArray(int size) {
            return new MessageRow[size];
        }
    };
    
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mSender);
		dest.writeString(mMsg);
		dest.writeString(mTime);
	}
	
	public void readFromParcel(Parcel in) {
		mSender = in.readString();
		mMsg = in.readString();
		mTime = in.readString();
    }
}