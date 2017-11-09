package com.example.android.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.android.instantBluetoothChat.beans.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import com.example.android.*;


//import static com.example.android.bluetoothchat.BluetoothChat.getF_log;
import static com.example.android.instantBluetoothChat.beans.Constants.ALL_CONNECTED;
import static com.example.android.instantBluetoothChat.beans.Constants.STATE_CONNECTED;
import static com.example.android.instantBluetoothChat.beans.Constants.CONNECTED_SERVER;
import static com.example.android.instantBluetoothChat.beans.Constants.STATE_CONNECTING;
import static com.example.android.instantBluetoothChat.beans.Constants.STATE_LISTEN;
import static com.example.android.instantBluetoothChat.beans.Constants.STATE_NONE;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("40537b38-c7a5-476c-9ea6-28dd106c5d97");
    String f = BluetoothChat.f_log;

    // Member fields
    public final BluetoothAdapter bt_Adapter;
    private final Handler bt_Handler;
    private AcceptThread bt_AcceptThread;
    private ConnectThread bt_ConnectThread;
    private ConnectedThread bt_ConnectedClientThread;
    private ConnectedThread bt_ConnectedThread1;
    private ConnectedThread bt_ConnectedThread2;
    private ConnectedThread bt_ConnectedThread3;
    private int mState;


    //public static final int STATE_DISCONNECT = 6;


    public int connectedDevices = 0;
    public boolean serverDevice = false;
    public static final int MAX_DEVICE = 3;  //
    private static final int SERVER_ID = -1;


    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        bt_Adapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        bt_Handler = handler;

    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        bt_Handler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");
        BluetoothChat.logging.append("start\n");
        // Cancel any thread attempting to make a connection
        if (bt_ConnectThread != null) {bt_ConnectThread.cancel(); bt_ConnectThread = null;}

        // Start the thread to listen on a BluetoothServerSocket
        if (bt_AcceptThread == null) {
            bt_AcceptThread = new AcceptThread();
            bt_AcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to Server
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (bt_ConnectThread != null) {bt_ConnectThread.cancel(); bt_ConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (bt_ConnectedThread1 != null) {bt_ConnectedThread1.cancel(); bt_ConnectedThread1 = null;}
        if (bt_ConnectedThread2 != null) {bt_ConnectedThread2.cancel(); bt_ConnectedThread2 = null;}
        if (bt_ConnectedThread3 != null) {bt_ConnectedThread3.cancel(); bt_ConnectedThread3 = null;}
        if (bt_ConnectedClientThread != null) {bt_ConnectedClientThread.cancel(); bt_ConnectedClientThread = null;}

        // Start the thread to connect with the given device
        bt_ConnectThread = new ConnectThread(device);
        bt_ConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connectedClient(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");
        BluetoothChat.logging.append("connected\n");

        // Cancel the thread that doing client connection
        if (bt_ConnectThread != null) {bt_ConnectThread.cancel(); bt_ConnectThread = null;}

        // Cancel any thread currently running a connection
        if (bt_ConnectedThread1 != null) {bt_ConnectedThread1.cancel(); bt_ConnectedThread1 = null;}
        if (bt_ConnectedThread2 != null) {bt_ConnectedThread2.cancel(); bt_ConnectedThread2 = null;}
        if (bt_ConnectedThread3 != null) {bt_ConnectedThread3.cancel(); bt_ConnectedThread3 = null;}
        if (bt_ConnectedClientThread != null) {bt_ConnectedClientThread.cancel(); bt_ConnectedClientThread = null;}

        // This is client device so cancel the AcceptThread
        if (bt_AcceptThread != null) {bt_AcceptThread.cancel();bt_AcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        bt_ConnectedClientThread = new ConnectedThread(socket, SERVER_ID,device.getName()); // TODO -1 is the server that we connected, socket also server's socket
        bt_ConnectedClientThread.start();

        // Send the name of the connected Server back to the UI Activity
        Message msg = bt_Handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        bt_Handler.sendMessage(msg);

        serverDevice = false;
        setState(CONNECTED_SERVER);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread for client connection
        if (bt_ConnectThread != null) {bt_ConnectThread.cancel(); bt_ConnectThread = null;}


        bt_AcceptThread.cancel();
        bt_AcceptThread = null;
        BluetoothChatService.this.start();



        if (connectedDevices < MAX_DEVICE) connectedDevices++;
        setState(STATE_CONNECTED); // TODO state for each device
        if (!serverDevice) serverDevice = true; // TODO put it under connectedDevices == 1

        // Start the thread to manage the connection and perform transmissions
        if (connectedDevices == 1) {
            bt_ConnectedThread1 = new ConnectedThread(socket, connectedDevices,device.getName());
            bt_ConnectedThread1.start();

            // Send the name of the connected device back to the UI Activity
            Message msg = bt_Handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            bt_Handler.sendMessage(msg);

        } else if (connectedDevices == 2) {
            bt_ConnectedThread2 = new ConnectedThread(socket, connectedDevices,device.getName());
            bt_ConnectedThread2.start();

            // Send the name of the connected device back to the UI Activity
            Message msg = bt_Handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            bt_Handler.sendMessage(msg);
        }else if (connectedDevices == 3) {
            bt_ConnectedThread3 = new ConnectedThread(socket, connectedDevices,device.getName());
            bt_ConnectedThread3.start();

            // Send the name of the connected device back to the UI Activity
            Message msg = bt_Handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            bt_Handler.sendMessage(msg);
        }

        // Cancel the accept thread for more than MAX_DEVICE
        if (connectedDevices == MAX_DEVICE)
            if (bt_AcceptThread != null) {
                bt_AcceptThread.cancel();
                bt_AcceptThread = null;
                setState(ALL_CONNECTED);
            }

    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        BluetoothChat.logging.append("stop\n");
        if (bt_ConnectThread != null) {bt_ConnectThread.cancel(); bt_ConnectThread = null;}
        if (bt_ConnectedThread1 != null) {bt_ConnectedThread1.cancel(); bt_ConnectedThread1 = null;}
        if (bt_ConnectedThread2 != null) {bt_ConnectedThread2.cancel(); bt_ConnectedThread2 = null;}
        if (bt_ConnectedThread3 != null) {bt_ConnectedThread3.cancel(); bt_ConnectedThread3 = null;}
        if (bt_ConnectedClientThread != null) {bt_ConnectedClientThread.cancel(); bt_ConnectedClientThread = null;}
        if (bt_AcceptThread != null) {bt_AcceptThread.cancel(); bt_AcceptThread = null;}
        connectedDevices = 0;
        serverDevice = false;
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {


        // Synchronize a copy of the ConnectedThread
        if (serverDevice) {
            // Create temporary object
            ConnectedThread r1 = null;
            ConnectedThread r2 = null;
            ConnectedThread r3 = null;

            synchronized (this) {
                if (mState != ALL_CONNECTED) return;
                r1 = bt_ConnectedThread1;
                r2 = bt_ConnectedThread2;
                r3 = bt_ConnectedThread3;
            }
            // Perform the write unsynchronized
            // TODO if are debugging
            if (r1 != null) r1.write(out);
            if (r2 != null) r2.write(out);
            if (r3 != null) r3.write(out);
        } else {

            ConnectedThread client = null;

            synchronized (this) {
                if (mState != CONNECTED_SERVER) return;
                client = bt_ConnectedClientThread;
            }
            // Perform the write unsynchronized
            client.write(out);
        }

    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = bt_Handler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        BluetoothChat.f_log= BluetoothChat.f_log+ " Unable to connect device\n";
        BluetoothChat.logging.append("Unable to connect device");
        msg.setData(bundle);
        bt_Handler.sendMessage(msg);
        // Make it possible to be server again
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = bt_Adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);

            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN bt_AcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();

                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    Log.i(TAG, "socket != null");
                    connected(socket, socket.getRemoteDevice());
                }
            }
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket BluetoothSocket;
        private final BluetoothDevice DeviceSocket;

        public ConnectThread(BluetoothDevice device) {
            DeviceSocket = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            BluetoothSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN bt_ConnectThread");
            setName("ConnectThread");

            // Cancel discovery in case it's still running
            bt_Adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                BluetoothSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    BluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Exception occurred so start the service again
                BluetoothChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                bt_ConnectThread = null;
            }

            // Start the connected thread
            connectedClient(BluetoothSocket, DeviceSocket);
        }

        public void cancel() {
            try {
                BluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device either for server or client
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket ServerSocket;
        private final InputStream InputStream;
        private final OutputStream OutputStream;
        private final int DeviceNumber;

        public ConnectedThread(BluetoothSocket socket, int DeviceNo, String devicename) {
            Log.d(TAG, "create ConnectedThread");
            ServerSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            this.DeviceNumber = DeviceNo; // SERVER_ID
             // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            InputStream = tmpIn;
            OutputStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream for Server's messages
                    bytes = InputStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    bt_Handler.obtainMessage(Constants.MESSAGE_READ, bytes, DeviceNumber, buffer).sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    BluetoothChat.logging.append("disconnected\n");
                         break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                OutputStream.write(buffer);
                // Share the sent message back to the UI Activity
                bt_Handler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                ServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
