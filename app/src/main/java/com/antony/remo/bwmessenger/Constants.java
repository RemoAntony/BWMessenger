package com.antony.remo.bwmessenger;
public interface Constants {

    
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String PACKAGE_NAME = Constants.class.getPackage().getName();

    public static final int MSG_NULL = 0;
    public static final int MSG_STARTSERVER = 1001;
    public static final int MSG_STARTCLIENT = 1002;
    public static final int MSG_CONNECT = 1003;
    public static final int MSG_DISCONNECT = 1004;   
    public static final int MSG_PUSHOUT_DATA = 1005;
    public static final int MSG_NEW_CLIENT = 1006;
    public static final int MSG_FINISH_CONNECT = 1007;
    public static final int MSG_PULLIN_DATA = 1008;
    public static final int MSG_REGISTER_ACTIVITY = 1009;

    public static final int MSG_SELECT_ERROR = 2001;
    public static final int MSG_BROKEN_CONN = 2002;  

    public static final int MSG_SIZE = 50;    
    public static final String MSG_SENDER = "sender";
    public static final String MSG_TIME = "time";
    public static final String MSG_CONTENT = "msg";

    
    public static final String CAT_LOCATION = "LocationRule";
    public static final String ACT_CREATE = "Create";
    public static final String ACT_DELETE = "Delete";
    public static final String LAB_HOME = "HomeRule";
    public static final String LAB_WORK = "WorkRule";
    public static final String LAB_OTHER = "OtherRule";
}