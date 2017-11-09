package com.example.android.bluetoothchat;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.example.android.instantBluetoothChat.beans.Adapter;
import com.example.android.instantBluetoothChat.beans.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import static com.example.android.instantBluetoothChat.beans.Constants.*;

public class BluetoothChat extends AppCompatActivity {
    // Debugging
    private static final boolean D = true;
    private static final String TAG = "BluetoothChat";
    public static String f_log;
    public static StringBuilder logging = new StringBuilder();
    int client_count = 1;



  //private ActionBar ab;
    private ListView mConversationView;
    private EditText mOutEditText;
    private ImageButton mSendButton;
    private GridView mUsersListView;
    // Array adapter for the conversation thread
    private Adapter mConversationArrayAdapter;
    // Array adapter for the devices
    private ArrayAdapter<String> mDevicesArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    // ArrayList of connected users
    private HashSet<String> mConnectedDevices;

    ArrayList<Messages> messages;

    private String HMAP_DEVICE_MESSAGE="device_message";
    private String HMAP_LIST = "sendlist";
    private String HMAP_RETRIEVE = "retrieve";

    private String destinationName="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        logging.append("+++ ON CREATE +++\n");
        // Set up the window layout
        setContentView(R.layout.activity_main);


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth unavailable", Toast.LENGTH_LONG).show();
            f_log= f_log+" "+"Bluetooth unavailable\n";
            logging.append("Bluetooth unavailable\n");
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        logging.append("++ ON START ++\n");

        // If Bluetooth is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }


    public String getLocalBluetoothName(){
        if(mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String name = mBluetoothAdapter.getName();
        return name;
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        logging.append("+ ON RESUME +\n");

        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == Constants.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");
        logging.append("setupChat\n");

        this.messages = new ArrayList<Messages>();

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new Adapter(this, this.messages);
        mConversationView = (ListView) findViewById(R.id.messagepanel);
        mConversationView.setAdapter(mConversationArrayAdapter);


        mDevicesArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mUsersListView = (GridView)findViewById(R.id.devicesList);
        mUsersListView.setAdapter(mDevicesArrayAdapter);
        //Initialize the ArrayList of connected Devices
        mConnectedDevices = new HashSet<String>();

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.insertmessage);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (ImageButton) findViewById(R.id.sendmessage);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.insertmessage);
                String message = view.getText().toString();
                sendMessage(message,getLocalBluetoothName());

            }
        });


        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");


    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
        logging.append("- ON PAUSE -\n");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
        logging.append("- ON STOP -\n");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
        logging.append("- ON DESTROY -\n");
    }

    private void discoverable() {
        if(D) Log.d(TAG, "discoverable");
        logging.append("discoverable\n");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 500);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message,String devicename) {

           if (message.length() > 0) {
            ArrayList<String> list = new ArrayList<>();
            list.add(devicename);
            list.add(message);
            // Get the message bytes and tell the BluetoothChatService to write
            HashMap<String,ArrayList<String>> hmap = new HashMap<String, ArrayList<String>>();
            hmap.put(HMAP_DEVICE_MESSAGE,list);


            try {
                // Convert Map to byte array
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(byteOut);
                out.writeObject(hmap);
                out.flush();
                byte[] send = byteOut.toByteArray();
                mChatService.write(send);

            }catch (Exception e){

            }

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message,getLocalBluetoothName());
                    }
                    if(D) Log.i(TAG, "END onEditorAction");
                    logging.append("END onEditorAction\n");
                    return true;
                }
            };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ArrayList<String>  list;
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg);
                  //  logging.append("MESSAGE_STATE_CHANGE: \n" + msg);
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    try {
                        // Parse byte array to Map
                        ByteArrayInputStream byteIn = new ByteArrayInputStream(writeBuf);
                        ObjectInputStream in = new ObjectInputStream(byteIn);
                        Map<String, ArrayList<String>> data = (Map<String, ArrayList<String>>) in.readObject();
                        if(data.containsKey(HMAP_LIST)){

                        }else if(data.containsKey(HMAP_DEVICE_MESSAGE)){
                            if(!mChatService.serverDevice) {
                                list = data.get(HMAP_DEVICE_MESSAGE);
                                //if(!list.get(0).equals(getLocalBluetoothName()))
                                Messages message = new Messages(list.get(1).toString(),"Me");
                                messages.add(message);
                                mConversationArrayAdapter.notifyDataSetChanged();
                            }
                        }
                    }catch (Exception e){

                    }

                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                   // construct a string from the buffer
                    try {
                        // Parse byte array to Map

                        ByteArrayInputStream byteIn = new ByteArrayInputStream(readBuf);
                        ObjectInputStream in = new ObjectInputStream(byteIn);
                        Map<String, ArrayList<String>> data = (Map<String, ArrayList<String>>) in.readObject();
                        if(data.containsKey(HMAP_DEVICE_MESSAGE)) {
                            list = data.get(HMAP_DEVICE_MESSAGE);
                            if(!mChatService.serverDevice) {
                                if(!list.get(0).equals(getLocalBluetoothName())) {
                                    if(client_count>3)
                                        client_count=1;
                                    Messages message = new Messages(list.get(1).toString(),"Remote Client" +client_count);
                                    client_count++;
                                    messages.add(message);
                                    mConversationArrayAdapter.notifyDataSetChanged();
                                }
                            }else{
                                BluetoothChat.this.sendMessage(list.get(1),list.get(0));
                            }
                            Log.e("value",list.get(0) + " : " + list.get(1));

                        }else if(data.containsKey(HMAP_LIST)){
                            if(mChatService.serverDevice) {
                                list = data.get(HMAP_LIST);
                                sendList(list.get(0));
                            }
                        }else if(data.containsKey(HMAP_RETRIEVE)){
                            if(!mChatService.serverDevice) {
                                list = data.get(HMAP_RETRIEVE);
                                displayDevices(list);
                            }
                        }

                    }catch (Exception e){

                    }

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    if (mChatService.getState() == Constants.STATE_CONNECTED) {
                        if (mChatService.connectedDevices == 1) {
                            mConnectedDeviceName1 = msg.getData().getString(DEVICE_NAME);
                            Toast.makeText(getApplicationContext(), "Connected to "
                                    + mConnectedDeviceName1, Toast.LENGTH_SHORT).show();
                            f_log=f_log+" "+"connected to "+mConnectedDeviceName1+"\n";
                            logging.append("connected to "+mConnectedDeviceName1+"\n");
                        } else if ( mChatService.connectedDevices == 2) {
                            mConnectedDeviceName2 = msg.getData().getString(DEVICE_NAME);
                            Toast.makeText(getApplicationContext(), "Connected to "
                                    + mConnectedDeviceName2, Toast.LENGTH_SHORT).show();
                            f_log=f_log+" "+"connected to "+ mConnectedDeviceName2+"\n";
                            logging.append("connected to "+ mConnectedDeviceName2+"\n");
                        } else if ( mChatService.connectedDevices == 3) {
                            mConnectedDeviceName3 = msg.getData().getString(DEVICE_NAME);
                            Toast.makeText(getApplicationContext(), "Connected to "
                                    + mConnectedDeviceName3, Toast.LENGTH_SHORT).show();
                        }
                    }

                    if(mChatService.serverDevice){
                        mConnectedDevices.add(msg.getData().getString(DEVICE_NAME));
                        Log.e("Size",":"+mConnectedDevices.size());
                        mDevicesArrayAdapter.clear();
                        mDevicesArrayAdapter.addAll(mConnectedDevices);
                        mDevicesArrayAdapter.notifyDataSetChanged();
                    }

                    if (mChatService.getState() == Constants.CONNECTED_SERVER) {
                        mConnectedServerName = msg.getData().getString(DEVICE_NAME);
                        Toast.makeText(getApplicationContext(), "Connected to server "
                                + mConnectedServerName, Toast.LENGTH_SHORT).show();
                        f_log=f_log+" "+"Connected to server "
                                + mConnectedServerName+"\n";
                        logging.append("Connected to server "
                                + mConnectedServerName+"\n");
                    }

                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();

                    break;
                case MESSAGE_DISCONNECT:

                    if (mChatService.serverDevice) {
                        mDevicesArrayAdapter.clear();
                        mConnectedDevices.clear();
                        mDevicesArrayAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        logging.append("onActivityResult " + resultCode+"\n");
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();

                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    logging.append("BT not enabled\n");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.clientserver_menu, menu);
        return true;
    }

    private void displayDevices(ArrayList<String> list){
        MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(new MaterialSimpleListAdapter.Callback() {
            @Override
            public void onMaterialListItemSelected(MaterialDialog dialog, int index, MaterialSimpleListItem item) {
                // TODO
                destinationName = item.toString();
                dialog.dismiss();
            }
        });
        if(!list.isEmpty()) {

                    for (String name:list){
                        adapter.add(new MaterialSimpleListItem.Builder(this)
                                .content(name.toString())
                                .icon(android.R.drawable.btn_star_big_on)
                                .backgroundColor(Color.WHITE)
                                .build());
                    }


                }

    }

    private void sendList(String devicename){

        ArrayList<String> list = new ArrayList<String>();
        Iterator iterator = mConnectedDevices.iterator();

        // check values
        while (iterator.hasNext()){
            String value = iterator.next().toString();
            if(!value.equals(devicename)) {
                list.add(value.toString());
            }
        }

        // Get the message bytes and tell the BluetoothChatService to write
        HashMap<String,ArrayList<String>> hmap = new HashMap<String,ArrayList<String>>();
        hmap.put(HMAP_RETRIEVE,list);

        try {
            // Convert Map to byte array
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(hmap);
            out.flush();
            byte[] send = byteOut.toByteArray();
            mChatService.write(send);

        }catch (Exception e){
            Log.e("",""+e);
        }

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                discoverable();
                return true;
            //PRIYANKA
//            case R.id.logs:
//                // Launch the DeviceListActivity to see devices and do scan
//                Intent log = new Intent(this, test.class);
//                startActivity(log);
//                return true;

        }
        return false;
    }

}