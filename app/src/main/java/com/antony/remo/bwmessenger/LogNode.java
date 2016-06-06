package com.antony.remo.bwmessenger;
public interface LogNode {

    
    public void println(int priority, String tag, String msg, Throwable tr);

}
