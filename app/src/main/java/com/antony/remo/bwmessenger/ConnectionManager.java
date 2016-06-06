package com.antony.remo.bwmessenger;

import android.content.Context;
import android.util.Log;

import com.antony.remo.bwmessenger.WiFiDirectApp.PTPLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;


public class ConnectionManager {
	
	private final String TAG = "PTP_ConnMan";
	
	private Context mContext;
	ConnectionService mService;
	WiFiDirectApp mApp;
	
	
	
	private Map<String, SocketChannel> mClientChannels = new HashMap<String, SocketChannel>();
	
	
	private Selector mClientSelector = null;
	private Selector mServerSelector = null;
	private ServerSocketChannel mServerSocketChannel = null;
	private SocketChannel mClientSocketChannel = null;
	String mClientAddr = null;
	String mServerAddr = null;
	
	
	public ConnectionManager(ConnectionService service) {
		mService = service;
		mApp = (WiFiDirectApp)mService.getApplication();
	}
	
	public void configIPV4() {
		 
		java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
		java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
	}
	
	
	public static ServerSocketChannel createServerSocketChannel(int port) throws IOException {
	    
	    ServerSocketChannel ssChannel = ServerSocketChannel.open();
	    ssChannel.configureBlocking(false);
	    ServerSocket serverSocket = ssChannel.socket();
	    serverSocket.bind(new InetSocketAddress(port));  
	    return ssChannel;
	}
	
	
	public static SocketChannel createSocketChannel(String hostName, int port) throws IOException {
	    
	    SocketChannel sChannel = SocketChannel.open();
	    sChannel.configureBlocking(false);

	    
	    sChannel.connect(new InetSocketAddress(hostName, port));
	    return sChannel;
	}

	
	
	public SocketChannel connectTo(String hostname, int port) throws Exception {
		SocketChannel sChannel = null;
		
		sChannel = createSocketChannel(hostname, port);  

		
		while (!sChannel.finishConnect()) {
				
		}
		
		
		return sChannel;
	}
	
	
	public int startClientSelector(String host) {
		closeServer();   
		
		if( mClientSocketChannel != null){
			Log.d(TAG, "startClientSelector : client already connected to server: " + mClientSocketChannel.socket().getLocalAddress().getHostAddress());
			return -1;
		}
		
		try {
			
			SocketChannel sChannel = connectTo(host, 1080);  
			
			mClientSelector = Selector.open();
		    mClientSocketChannel = sChannel;
		    mClientAddr = mClientSocketChannel.socket().getLocalAddress().getHostName();
		    sChannel.register(mClientSelector, SelectionKey.OP_READ );
		    mApp.setMyAddr(mClientAddr);
		    mApp.clearMessages();
		    PTPLog.d(TAG, "startClientSelector : started: " + mClientSocketChannel.socket().getLocalAddress().getHostAddress());
		    
			
			new SelectorAsyncTask(mService, mClientSelector).execute();
			return 0;

		} catch(Exception e) {
			PTPLog.e(TAG, "startClientSelector : exception: " + e.toString());
			mClientSelector = null;
			mClientSocketChannel = null;
			mApp.setMyAddr(null);
			
			return -1;
		}		
	}
	
	
	public int startServerSelector() {
		closeClient();   
		
		try {			
			
		    ServerSocketChannel sServerChannel = createServerSocketChannel(1080); 
		    mServerSocketChannel = sServerChannel;
		    mServerAddr = mServerSocketChannel.socket().getInetAddress().getHostAddress();
		    if( "0.0.0.0".equals(mServerAddr)){
		    	mServerAddr = "Master";
		    }
		    ((WiFiDirectApp)mService.getApplication()).setMyAddr(mServerAddr);
		    
		    mServerSelector = Selector.open();
		    SelectionKey acceptKey = sServerChannel.register(mServerSelector, SelectionKey.OP_ACCEPT);
		    acceptKey.attach("accept_channel");
		    mApp.mIsServer = true;
		    
		    
		    
		    Log.d(TAG, "startServerSelector : started: " + sServerChannel.socket().getLocalSocketAddress().toString());
		    
		    new SelectorAsyncTask(mService, mServerSelector).execute();
			return 0;
			
		} catch (Exception e) {
			Log.e(TAG, "startServerSelector : exception: " + e.toString());
			return -1;
		}
	}
	
	
	public void onSelectorError() {
		Log.e(TAG, " onSelectorError : do nothing for now.");
		
	}
	
	
	public void closeServer() {
		if( mServerSocketChannel != null ){
			try{
				mServerSocketChannel.close();
				mServerSelector.close();
			}catch(Exception e){
				
			}finally{
				mApp.mIsServer = false;
				mServerSocketChannel = null;
				mServerSelector = null;
				mServerAddr = null;
				mClientChannels.clear();
			}
		}
	}
	
	public void closeClient() {
		if( mClientSocketChannel != null ){
			try{
				mClientSocketChannel.close();
				mClientSelector.close();
			}catch(Exception e){
				
			}finally{
				mClientSocketChannel = null;
				mClientSelector = null;
				mClientAddr = null;
			}
		}
	}
	
	
	public void onBrokenConn(SocketChannel schannel){
		try{
			String peeraddr = schannel.socket().getInetAddress().getHostAddress();
			if( mApp.mIsServer ){
				mClientChannels.remove(peeraddr);
				Log.d(TAG, "onBrokenConn : client down: " + peeraddr);
			}else{
				Log.d(TAG, "onBrokenConn : set null client channel after server down: " + peeraddr);
				closeClient();
			}
			schannel.close();
		}catch(Exception e){
			PTPLog.e(TAG, "onBrokenConn: close channel: " + e.toString());
		}
	}
	
	
	public void onNewClient(SocketChannel schannel){
		String ipaddr = schannel.socket().getInetAddress().getHostAddress();
		Log.d(TAG, "onNewClient : server added remote client: " + ipaddr);
		mClientChannels.put(ipaddr, schannel);
	}
	
	
	public void onFinishConnect(SocketChannel schannel){
		String clientaddr = schannel.socket().getLocalAddress().getHostAddress();
		String serveraddr = schannel.socket().getInetAddress().getHostAddress();
		Log.d(TAG, "onFinishConnect : client connect to server succeed : " + clientaddr + " -> " + serveraddr);
		mClientSocketChannel = schannel;
		mClientAddr = clientaddr;
		((WiFiDirectApp)mService.getApplication()).setMyAddr(mClientAddr);
	}
	
	
	public void onDataIn(SocketChannel schannel, String data){
		Log.d(TAG, "connection onDataIn : " + data);
		if( mApp.mIsServer ){  
			pubDataToAllClients(data, schannel);
		}
	}

	
	private int writeData(SocketChannel sChannel, String jsonString){
		byte[] buf = jsonString.getBytes();
		ByteBuffer bytebuf = ByteBuffer.wrap(buf);  
		int nwritten = 0;
		try {
		    
		    Log.d(TAG, "writeData: start:limit = " + bytebuf.position() + " : " + bytebuf.limit());
		    nwritten = sChannel.write(bytebuf);
		} catch (Exception e) {
		    
			Log.e(TAG, "writeData: exception : " + e.toString());
			onBrokenConn(sChannel);
		}
		Log.d(TAG, "writeData: content: " + new String(buf) + "  : len: " + nwritten);
		return nwritten;
	}
	
	
	private void pubDataToAllClients(String msg, SocketChannel incomingChannel){
		Log.d(TAG, "pubDataToAllClients : isServer ? " + mApp.mIsServer + " msg: " + msg );
		if( !mApp.mIsServer ){
			return;
		}
		
		for( SocketChannel s: mClientChannels.values()) {
			if ( s != incomingChannel){
				String peeraddr = s.socket().getInetAddress().getHostAddress();
				Log.d(TAG, "pubDataToAllClients : Server pub data to:  " + peeraddr);
				writeData(s, msg);
			}
		}
	}
	
	
	public int pushOutData(String jsonString){
		if( !mApp.mIsServer ){   
			sendDataToServer(jsonString);
		}else{
			
			pubDataToAllClients(jsonString, null);
		}
		return 0;
	}
	
	
	private int sendDataToServer(String jsonString) {
		if(mClientSocketChannel == null) {
			Log.d(TAG, "sendDataToServer: channel not connected ! waiting...");
			return 0;
		}
		Log.d(TAG, "sendDataToServer: " + mClientAddr + " -> " + mClientSocketChannel.socket().getInetAddress().getHostAddress() + " : " +  jsonString);
		return writeData(mClientSocketChannel, jsonString);
	}
}
