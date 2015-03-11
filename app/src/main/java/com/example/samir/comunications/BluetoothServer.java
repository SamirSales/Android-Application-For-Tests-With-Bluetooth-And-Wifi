package com.example.samir.comunications;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.example.samir.comunicacaodedispositivos.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by samir on 11/03/15.
 */
public class BluetoothServer implements Communication {

    private final String TAG = "BluetoothServer";

    private Activity context;
    private List<Observer> observers;
    private ArrayList<Byte> pacote;
    private boolean flagArmazena;
    private static final int RECONNECTION_TIME = 3000;
    private byte[] data;
    private Runnable runnable;
    private Handler handler;

    private BluetoothServerSocket mmServerSocket;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    private ProgressDialog dialogWaitConnection;
    private WriterBluetooth writerThread;
    private int errorCounter = 0;

    public BluetoothServer(Activity parent) {
        context = parent;
        observers = new ArrayList<Observer>();
        pacote = new ArrayList<Byte>();
        handler = new Handler();

        dialogWaitConnection = new ProgressDialog(context);
        dialogWaitConnection.setIcon(R.drawable.ic_launcher);
        dialogWaitConnection.setTitle("Aguarde!");
        dialogWaitConnection.setMessage("conectando...");
        dialogWaitConnection.setCanceledOnTouchOutside(false);
        dialogWaitConnection.setCancelable(false);
        dialogWaitConnection.setIndeterminate(false);
        dialogWaitConnection.setOnCancelListener(null);
    }

    @Override
    public void open() {
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
        if (mmServerSocket != null) {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = null;
        }
    }

    @Override
    public void reconnect() {
        close();
        open();
    }

    @Override
    public void send(byte[] data) {
        if (data != null) {
            this.data = data;
            if (isConnected()) {
                new Thread(writerThread).start();
            }
        }
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void remObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(byte[] data) {
        if (data != null) {
            for (Observer o : observers) {
                o.update(data);
            }
        }
    }

    private class AcceptThread extends Thread {

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final

            BluetoothServerSocket tmp = null;
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("nome", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    for (Observer o : observers) {
                        o.connectedCallback();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    public class ConnectedThread  extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private Handler mHandler;
        private ArrayList<String> arrayMessage;

        public ConnectedThread(BluetoothSocket socket, ArrayList<String> arrayMessage) {
            mmSocket = socket;
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
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /**
     * Classe privada para transmissao de dados
     */
    private class WriterBluetooth implements Runnable {
        @Override
        public void run() {
            if (data != null) {
                try {
                    outputStream.write(data);
                    errorCounter = 0;
                } catch (Exception e) {
                    errorCounter++;
                    if (errorCounter > 20) {
                        errorCounter = 0;
                        reconnect();
                    }
                }
            }
        }
    }

}
