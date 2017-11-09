package com.example.android.instantBluetoothChat.beans;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.android.bluetoothchat.R;
import com.example.android.instantBluetoothChat.beans.Messages;

import java.util.ArrayList;

public class Adapter extends BaseAdapter
{
    Context context;
    ArrayList<Messages> messages;
    
   public Adapter(final Context context, final ArrayList<Messages> messages) {
        super();
        this.context = context;
        this.messages = messages;
    }
    
    public int getCount() {
        return this.messages.size();
    }
    
    public Object getItem(final int n) {
        return this.messages.get(n);
    }
    
    public long getItemId(final int n) {
        return 0L;
    }


    public class ViewHolder
    {
        TextView t;
    }

    public View getView(final int n, final View view, final ViewGroup viewGroup) {
        View inflate = view;
        ViewHolder tag;
        final Messages message = (Messages)this.getItem(n);
        if (inflate == null) {
            final LayoutInflater layoutInflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflate = LayoutInflater.from(this.context).inflate(R.layout.message, viewGroup, false);
            tag = new ViewHolder();
            tag.t = (TextView)inflate.findViewById(R.id.text1);
            inflate.setTag((Object)tag);
        }
        else {
            tag = (ViewHolder)inflate.getTag();
        }
        tag.t.setText(message.getMessage());
    return inflate;
    }

}
