package com.example.android.bluetoothchat;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.logger.*;

import static android.content.ContentValues.TAG;

public class test extends Activity {

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        System.out.print(BluetoothChat.f_log);

        TextView displayTextView = null;
        displayTextView = (TextView) findViewById(R.id.textView);
        // int myWord = Log.e(TAG, "+ ON RESUME +");//
        String myWord =  BluetoothChat.f_log;
        StringBuffer a = new StringBuffer();
        a.append("hello");

        displayTextView.setText("Server\n" + BluetoothChat.logging);
        //displayTextView.setText("S");

    }


        public void selectFrag(View view) {

        Fragment fr = new LogFragment();


        FragmentManager fm = getFragmentManager();

        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_space, fr);

        fragmentTransaction.commit();

    }

}
