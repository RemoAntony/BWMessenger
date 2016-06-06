package com.antony.remo.bwmessenger;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import static com.antony.remo.bwmessenger.Constants.MSG_BROKEN_CONN;
import static com.antony.remo.bwmessenger.Constants.MSG_FINISH_CONNECT;
import static com.antony.remo.bwmessenger.Constants.MSG_NEW_CLIENT;
import static com.antony.remo.bwmessenger.Constants.MSG_PULLIN_DATA;
import static com.antony.remo.bwmessenger.Constants.MSG_SELECT_ERROR;





public class SelectorAsyncTask extends AsyncTask<Void, Void, Void>{
	private static final String TAG = "PTP_SEL";
	
	private ConnectionService mConnService;
	private Selector mSelector;
	
	public SelectorAsyncTask(ConnectionService connservice, Selector selector) {
		mConnService = connservice;
		mSelector = selector;
	}
	
	@Override
	protected Void doInBackground(Void... arg0) {
		select();
		return null;
	}
	
	private void select() {
		
		while (true) {
			try {
		    	Log.d(TAG, "select : selector monitoring: ");
		    	mSelector.select();   

		    	Log.d(TAG, "select : selector evented out: ");
			    
			    Iterator<SelectionKey> keys = mSelector.selectedKeys().iterator();
			    while (keys.hasNext()) {
			        
			        SelectionKey selKey = keys.next();
			        keys.remove();
			        Log.d(TAG, "select : selectionkey: " + selKey.attachment());
			        
			        try {
			            processSelectionKey(mSelector, selKey);  
			        } catch (IOException e) {
			            selKey.cancel();
			            Log.e(TAG, "select : io exception in processing selector event: " + e.toString());
			        }
			    }
		    } catch (Exception e) {  
		    	Log.e(TAG, "Exception in selector: " + e.toString());
		    	notifyConnectionService(MSG_SELECT_ERROR, null, null);
		        break;
		    }
		}
	}

	
	public void processSelectionKey(Selector selector, SelectionKey selKey) throws IOException {
        if (selKey.isValid() && selKey.isAcceptable()) {  
            ServerSocketChannel ssChannel = (ServerSocketChannel)selKey.channel();
            SocketChannel sChannel = ssChannel.accept();  
            sChannel.configureBlocking(false);
            
            
            SelectionKey socketKey = sChannel.register(selector, SelectionKey.OP_READ );
            socketKey.attach("accepted_client " + sChannel.socket().getInetAddress().getHostAddress());
            Log.d(TAG, "processSelectionKey : accepted a client connection: " + sChannel.socket().getInetAddress().getHostAddress());
            notifyConnectionService(MSG_NEW_CLIENT, sChannel, null);
        } else if (selKey.isValid() && selKey.isConnectable()) {   
	        SocketChannel sChannel = (SocketChannel)selKey.channel();

	        boolean success = sChannel.finishConnect();
	        if (!success) {
	            
	            selKey.cancel();
	            Log.e(TAG, " processSelectionKey : finish connection not success !");
	        }
	        Log.d(TAG, "processSelectionKey : this client connect to remote success: ");
	        notifyConnectionService(MSG_FINISH_CONNECT, sChannel, null);
	        
	    } else if (selKey.isValid() && selKey.isReadable()) {
	        
	        SocketChannel sChannel = (SocketChannel)selKey.channel();
	        Log.d(TAG, "processSelectionKey : remote client is readable, read data: " + selKey.attachment());
	        
	        
	        
	        doReadable(sChannel);
	    } else if (selKey.isValid() && selKey.isWritable()) {
	    	
	        SocketChannel sChannel = (SocketChannel)selKey.channel();
	        Log.d(TAG, "processSelectionKey : remote client is writable, write data: ");
	    }
	}

	
	public void doReadable(SocketChannel schannel){
		String data = readData(schannel);
		if( data != null ){
			Bundle b = new Bundle();
			b.putString("DATA", data);
			notifyConnectionService(MSG_PULLIN_DATA, schannel, b);
		}
	}
	
	
	public String readData(SocketChannel sChannel) {
		ByteBuffer buf = ByteBuffer.allocate(1024*4);   
		byte[] bytes = null;
		String jsonString = null;
		
		try {
		    buf.clear();  
		    int numBytesRead = sChannel.read(buf);
		    if (numBytesRead == -1) {
		        
		    	Log.e(TAG, "readData : channel closed due to read -1: ");
		    	sChannel.close();  
		    	notifyConnectionService(MSG_BROKEN_CONN, sChannel, null);
		    	
		    } else {
		    	Log.d(TAG, "readData: bufpos: limit : " + buf.position() + ":" + buf.limit() + " : " + buf.capacity());
		        buf.flip();  
		        Log.d(TAG, "readData: bufpos: limit : " + buf.position() + ":" + buf.limit() + " : " + buf.capacity());
		        bytes = new byte[buf.limit()];  
		        buf.get(bytes);
		        
		        jsonString = new String(bytes);  
		    }
		}catch(Exception e){
			Log.e(TAG, "readData : exception: " + e.toString());
			notifyConnectionService(MSG_BROKEN_CONN, sChannel, null);
		}
		
		Log.d(TAG, "readData: content: " + jsonString);
		return jsonString; 
	}
	
	
	private void notifyConnectionService(int what, Object obj, Bundle data){
		Handler hdl = mConnService.getHandler();
		Message msg = hdl.obtainMessage();
		msg.what = what;
		
		if( obj != null ){
			msg.obj = obj;	
		}
		if( data != null ){
			msg.setData(data);
		}
		hdl.sendMessage(msg);		
	}

}
