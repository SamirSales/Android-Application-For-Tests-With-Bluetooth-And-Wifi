package com.example.samir.comunicacaodedispositivos;

import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by samir on 23/02/15.
 */
public class ConnectServerWifi extends Thread {
    private final Socket socket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private ArrayList<String> arrayMessage;

    public ConnectServerWifi(Socket socket1, ArrayList<String> arrayMessage) {
        socket = socket1;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.arrayMessage = arrayMessage;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket1.getInputStream();
            tmpOut = socket1.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
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

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) { }
    }
}