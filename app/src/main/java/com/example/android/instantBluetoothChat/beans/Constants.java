package com.example.android.instantBluetoothChat.beans;


public class Constants
{
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_DISCONNECT = 6;
    // Layout Views
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent request codes
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_CONNECT_DEVICE = 1;
    // Name of the connected device
    public static String mConnectedServerName = null;
    public static String mConnectedDeviceName1 = null;
    public static String mConnectedDeviceName2 = null;
    public static String mConnectedDeviceName3 = null;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; 		// connecting to the server device
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int ALL_CONNECTED = 4;	// all the devices connected to the server
    public static final int CONNECTED_SERVER = 5;


}
