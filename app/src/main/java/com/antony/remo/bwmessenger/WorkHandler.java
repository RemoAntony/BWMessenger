
package com.antony.remo.bwmessenger;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;


public final class WorkHandler {

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    
    public WorkHandler(String threadName) {
        
        mHandlerThread = new HandlerThread(threadName, Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    
    public Handler getHandler() {
        return mHandler;
    }

    
    public Looper getLooper() {
        Handler h = mHandler;
        return h != null ? h.getLooper() : null;
    }

    
    public void close() {
        if (mHandler != null) {
            mHandlerThread.getLooper().quit();
            mHandlerThread.quit();
            mHandler = null;
        }
    }

    
    @Override
    protected void finalize() {
        if (mHandler != null) {
            close();
        }
    }
}
