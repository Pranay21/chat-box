package com.example.android.instantBluetoothChat.beans;

public class Messages
{
    String message;

    public Messages(final String message, final String device)
    {
        this.message = device+ " : "+message;
    }

     public String getMessage()
     {
        return this.message;
    }

}
