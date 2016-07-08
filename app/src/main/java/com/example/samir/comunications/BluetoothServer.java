package com.example.samir.comunications;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.example.samir.comunications.interfaces.Observer;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Samir Sales on 11/03/15.
 */
public class BluetoothServer extends BluetoothUser {

    private final String TAG = "BluetoothServer";

    private BluetoothServerSocket bluetoothServerSocket;
    private boolean connected;

    public BluetoothServer(Activity parent) {
        super(parent);
        connected = false;
    }

    @Override
    public void open() {
        new ConnectionTask().execute();
    }

    @Override
    public void close() {
        super.close();
        if (bluetoothServerSocket != null) {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = null;
        }
    }

    @Override
    public boolean isConnected() {
        return bluetoothServerSocket != null && connected;
    }

    private class ConnectionTask extends AsyncTask<Void, Void, BluetoothServerSocket> {
        AcceptThread acceptThread;

        private BluetoothServerSocket connect(){
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final

            BluetoothServerSocket tmp = null;
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("nome", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }
            bluetoothServerSocket = tmp;

            acceptThread = new AcceptThread();
            acceptThread.start();

            return bluetoothServerSocket;
        }

        @Override
        protected BluetoothServerSocket doInBackground(Void... params) {
            return connect();
        }

        /**
         * When the task finish, the method test if the socket connection has been opened, get the
         * streams, close the dialog and notify the observers about the bluetooth connection opened.
         * @param socket the bluetooth socket connection.
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        protected void onPostExecute(BluetoothSocket socket) {

            if (socket != null && connected) {
                try {
                    setInputStream(socket.getInputStream());
                    setOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                resetWriterBluetooth();
                new ReaderBluetoothThread().start();

                for (Observer o : getObservers()) {
                    o.connectedCallback();
                }
            } else {
                waitAndReconnect();
            }
        }
    }

    private class AcceptThread extends Thread {
        public void run() {
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = bluetoothServerSocket.accept();
                    Log.i(TAG, "one client connected!");
                    try {
                        setInputStream(socket.getInputStream());
                        setOutputStream(socket.getOutputStream());
                        new ReaderBluetoothThread().start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());
                    }
                    connected = true;
                    for (Observer o : getObservers()) {
                        o.connectedCallback();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    connected = false;
                    break;
                }
            }
        }

        /**
         * It will cancel the socket listening and cause the thread finish.
         */
        public void cancel() {
            try {
                bluetoothServerSocket.close();
                connected = false;
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

}
