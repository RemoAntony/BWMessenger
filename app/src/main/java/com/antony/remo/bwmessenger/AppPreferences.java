

package com.antony.remo.bwmessenger;

import android.content.Context;
import android.content.SharedPreferences;


public class AppPreferences {

    private static final String TAG = "PTP_Pref";
    
    public static final String PREF_NAME = Constants.PACKAGE_NAME;
    
    public static final String P2P_ENABLED = "p2pEnabled";

    private WiFiDirectApp mApp;
    private SharedPreferences mPref;

    public AppPreferences(WiFiDirectApp app) {
    	mApp = app;
        mPref = mApp.getSharedPreferences(Constants.PACKAGE_NAME, 0);
    }

    
    public String getString(String key) {
        return mPref.getString(key, null);
    }

    
    public void setString(String key, String value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(key, value);
        editor.commit();
    }
    
    
        
    
    public static String getStringFromPref(Context ctx, String preferenceFileName, String key) {
    	String value = null;
    	SharedPreferences pref = ctx.getSharedPreferences(preferenceFileName, 0);
    	if( pref != null){
    		value = pref.getString(key, null);
    	}
        return value;
    }
    
    public static void setStringToPref(Context ctx, String preferenceFileName, String key, String value) {
    	SharedPreferences pref = ctx.getSharedPreferences(preferenceFileName, 0);
    	if( pref != null){
    		SharedPreferences.Editor editor = pref.edit();
            editor.putString(key, value);
            editor.commit();
    	}
    }
    
    
    public static boolean getBooleanFromPref(Context ctx, String preferenceFileName, String key) {
    	boolean value = false;
    	SharedPreferences pref = ctx.getSharedPreferences(preferenceFileName, 0);
    	if( pref != null){
    		value = pref.getBoolean(key, false);
    	}
        return value;
    }
}
