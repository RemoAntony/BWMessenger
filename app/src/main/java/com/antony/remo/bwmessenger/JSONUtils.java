

package com.antony.remo.bwmessenger;

import com.antony.remo.bwmessenger.WiFiDirectApp.PTPLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import static com.antony.remo.bwmessenger.Constants.MSG_SENDER;
import static com.antony.remo.bwmessenger.Constants.MSG_SIZE;



public class JSONUtils {
    public static final String TAG = "PTP_UtilsJSON";
    
    
    
    public static JSONObject getJsonObject( String jsonstr ) {
    	JSONObject jsonobj = null;
    	try{
    		jsonobj = new JSONObject(jsonstr);
    	}catch(JSONException e){
    		PTPLog.e(TAG, "getJsonObject : " + e.toString());
    	}
    	return jsonobj;
    }
    
    
    public static JSONArray getJsonArray(String jsonstr) {
        JSONArray curjsons = null;
        if (jsonstr == null) {
            return null;
        }
        try {
            curjsons = new JSONArray(jsonstr);  
        } catch (JSONException e) {
        	PTPLog.e(TAG, "getJSONArray:" + e.toString());
        }
        return curjsons;
    }
    
    
    public static JSONArray truncateJSONArray(JSONArray origarray, int offset){
    	int sz = origarray.length();
    	if( sz > MSG_SIZE){
    		JSONArray newarray = new JSONArray();
    		try{
    			for(int i=offset; i < sz; i++){
    				newarray.put(origarray.getJSONObject(i));
    			}
    		}catch(JSONException e){
    			PTPLog.e(TAG, "truncateJSONArray :" + e.toString());
    		}
    		return newarray;
    	}else{
    		return origarray;  
    	}
    }
    
    
    public static int indexOfJSONObject(JSONArray jsonarray, JSONObject jsonobj, String key) {
        String objstr = null;
        if (key == null) {
            objstr = jsonobj.toString();
        } else {
            try {
                objstr = jsonobj.getString(key);
            } catch (JSONException e) {
                objstr = null;
                PTPLog.e(TAG, "findJSONObject:  get key Exception: " + e.toString());
            }
        }

        
        if (objstr != null) {
            objstr = objstr.trim();
            if (objstr.length() == 0) {
                return -1;
            }
        } else {
        	PTPLog.d(TAG, "findJSONObject:  empty key string! no found. ");
            return -1;
        }

        int size = jsonarray.length();
        JSONObject entry = null;
        String entrystr = null;
        for (int i=0; i<size; i++) {
            try {
                entry = jsonarray.getJSONObject(i);
                if (key == null) {
                    entrystr = entry.toString();
                } else {
                    entrystr = entry.getString(key);
                }
                if (entrystr != null) {
                    entrystr = entrystr.trim();
                }

                if (objstr.equals(entrystr)) {
                	PTPLog.d(TAG, "findJSONObject: match :" + objstr);
                    return i;   
                }
            } catch (JSONException e) {
            	PTPLog.e(TAG, "findJSONObject: getJSONObject Exception: " + e.toString());
                continue;
            }
        }
        return -1;
    }

    

    
    public static JSONArray mergeJsonArrays(JSONArray existingjsons, JSONArray newjsons, boolean updatess) {
        if (existingjsons == null)
            return newjsons;
        if (newjsons == null)
            return existingjsons;

        JSONObject newobj = null;
        for (int i=0; i<newjsons.length(); i++) {
            try {
                newobj = newjsons.getJSONObject(i);
                String sender = newobj.getString(MSG_SENDER);
                if(sender == null) {
                    continue;  
                }
                int idx = indexOfJSONObject(existingjsons, newobj, MSG_SENDER);
                if(idx < 0) { 
                    existingjsons.put(newobj);
                } else if(updatess) { 
                    if(newobj.has(MSG_SENDER)) {
                        String newss = newobj.getString(MSG_SENDER);
                        JSONObject oldobj = existingjsons.getJSONObject(idx);
                        oldobj.put(MSG_SENDER, newss);
                        PTPLog.d(TAG, "mergeJsonArrays: update ss: " + newss + " : " + oldobj.toString());
                    }
                }
            } catch (JSONException e) {
                PTPLog.e(TAG, "mergeJSONArrays: getJSONObject Exception: " + e.toString());
                continue;
            }
        }
        return existingjsons;
    }

    
    public static String mergeJsonArrayStrings(String curstr, String newstr) {
        JSONArray curjsons = null;
        JSONArray newjsons = null;

        PTPLog.d(TAG, "mergeJSONArrays:" + curstr + " =+= " + newstr);

        
        if (curstr == null)
            return newstr;
        if (newstr == null)
            return curstr;

        try {
            curjsons = new JSONArray(curstr);  
            newjsons = new JSONArray(newstr);
        } catch (JSONException e) {
        	PTPLog.e(TAG, "mergeJSONArrays:" + e.toString());
            return curstr;   
        }

        mergeJsonArrays(curjsons, newjsons, true);  

        return curjsons.toString();
    }

    
    @Deprecated
    public static boolean fuzzyMatchJsonArrays(String dbJsonStr, String curJsonStr, String key) {
    	PTPLog.d(TAG, "fuzzyMatchJSONArrays : dbsdbjsonstret : " + dbJsonStr + " : curjsonstr :" +curJsonStr);
        if (dbJsonStr == null || curJsonStr == null) {
            return false;    
        }

        JSONArray dbjsons = null;
        JSONArray curjsons = null;
        try {
            dbjsons = new JSONArray(dbJsonStr);  
            curjsons = new JSONArray(curJsonStr);
        } catch (JSONException e) {
        	PTPLog.e(TAG, "mergeJSONArrays:" + e.toString());
            return false;   
        }

        boolean match = false;
        JSONObject curobj = null;
        for (int i=0; i<curjsons.length(); i++) {
            try {
                curobj = curjsons.getJSONObject(i);
            } catch (JSONException e) {
            	PTPLog.e(TAG, "mergeJSONArrays: getJSONObject Exception: " + e.toString());
                continue;  
            }

            if(indexOfJSONObject(dbjsons, curobj, key) >= 0) {
                match = true;
                break;
            }
        }
        return match;
    }

    
    public static Set<String> getValueSetFromJsonArray(JSONArray jsonarray, String key) {
        Set<String> valset = new HashSet<String>();
        if (jsonarray == null) {
            return valset;
        }

        JSONObject curobj = null;
        String valstr = null;
        for (int i=0; i<jsonarray.length(); i++) {
            try {
                curobj = jsonarray.getJSONObject(i);
                if (key == null) {
                    valstr = curobj.toString();
                } else {
                    valstr = curobj.getString(key);
                }
                valset.add(valstr);
                
            } catch (JSONException e) {
            	PTPLog.e(TAG, "getValueSetFromJSONArray: Exception: " + e.toString());
                continue;  
            }
        }
        return valset;
    }
       
    
    public static int intersectSetJsonArray(Set< ? extends String> set, String wrap, String jsonString) {
        int matchcnt = 0;
        if (jsonString != null && jsonString.length() > 0) {
            for (String s : set) {          
                String wrapstr = s;             
                if (wrap != null) {             
                    wrapstr = wrap + s + wrap;      
                }
                if (jsonString.indexOf(wrapstr) >= 0) {
                    PTPLog.d(TAG, "intersectSetJsonArray: Match: " + wrapstr);
                    matchcnt++;
                }
            }
        }
        return matchcnt;
    }

}
