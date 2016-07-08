package com.example.samir.comunications.threads;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by Samir Sales on 12/02/15.
 */
public class BluetoothConnectedThread extends Thread {
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private ArrayList<String> arrayMessage;

    public BluetoothConnectedThread(BluetoothSocket socket, ArrayList<String> arrayMessage) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.arrayMessage = arrayMessage;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                String str = new String (buffer);
                arrayMessage.add(str);
            } catch (IOException e) {
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }
}