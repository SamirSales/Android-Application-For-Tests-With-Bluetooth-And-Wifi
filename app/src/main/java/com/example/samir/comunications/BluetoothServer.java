package com.example.samir.comunications;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Build;
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

    private BluetoothServerSocket bluetoothServerSocket;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private boolean connected;

    private ProgressDialog dialogWaitConnection;
    private WriterBluetooth writerThread;
    private int errorCounter = 0;

    public BluetoothServer(Activity parent) {
        context = parent;
        observers = new ArrayList<Observer>();
        pacote = new ArrayList<Byte>();
        handler = new Handler();
        connected = false;

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
        Log.i(TAG, "open() ...");
        new ConnectionTask().execute();
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
    public void reconnect() {
        close();
        open();
    }

    @Override
    public void send(byte[] data) {
        if (data != null) {
            this.data = data;
            if (isConnected()) {
                writerThread = new WriterBluetooth();
                new Thread(writerThread).start();
            }
        }
    }

    @Override
    public boolean isConnected() {
//        Log.i(TAG, "isConnected() bluetoothServerSocket="+bluetoothServerSocket.toString());
//        Log.i(TAG, "isConnected() connected="+connected);
        return bluetoothServerSocket != null && connected;
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
         * Metodo que dispara um timer para tentar uma nova conexao com o dispositivo bluetooth
         */
        private void waitAndReconnect() {
            handler.removeCallbacks(runnable);
            runnable = new Runnable() {
                public void run() {
                    reconnect();
                }
            };
            handler.postDelayed(runnable, RECONNECTION_TIME);
        }

        /**
         * Quanda a tarefa terminar, o metodo testa se a conexao socket foi aberta,
         * obtem os fluxos de transmissoes, fecha o dialogo e notifica aos observadores que
         * a conexao bluetooth foi aberta.
         * @param socket a conexao socket blutooth
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        protected void onPostExecute(BluetoothSocket socket) {
            if (socket != null && connected) {

                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                writerThread = new WriterBluetooth();
                new ReaderBluetoothThread().start();

                for (Observer o : observers) {
                    o.connectedCallback();
                }

                //fechaDialogo();
            } else {
                waitAndReconnect();
            }
        }
    }

    private class AcceptThread extends Thread {

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = bluetoothServerSocket.accept();
                    //TODO
                    Log.i(TAG, "a client connected!");
                    try {
                        inputStream = socket.getInputStream();
                        outputStream = socket.getOutputStream();
                        Log.i(TAG, "inputStream="+inputStream.toString());
                        Log.i(TAG, "outputStream="+outputStream.toString());
                        new ReaderBluetoothThread().start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());
                    }
                    connected = true;
                    for (Observer o : observers) {
                        o.connectedCallback();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    connected = false;
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                bluetoothServerSocket.close();
                connected = false;
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    /**
     * Classe privada para transmissao de dados
     */
    private class WriterBluetooth implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "WriterBluetooth -> data="+new String(data));
            if (data != null) {
                try {
                    outputStream.write(data);
                    errorCounter = 0;
                    Log.i(TAG, "WriterBluetooth -> written... data="+new String(data));
                } catch (Exception e) {
                    Log.e(TAG,e.getMessage());
                    errorCounter++;
                    if (errorCounter > 20) {
                        errorCounter = 0;
                        reconnect();
                    }
                }
            }
        }
    }

    /**
     * Thread responsavel pela leitura de bytes pelo fluxo de entrada bluetooth
     */
    private class ReaderBluetoothThread extends Thread {
        @Override
        public void run() {
            while (isConnected() && inputStream != null) {
                try {
                    int tamanho = inputStream.available();
                    if (tamanho > 0) {
                        byte[] pacote = new byte[tamanho];
                        for (int i = 0; i < tamanho; i++) {
                            pacote[i] = (byte) inputStream.read();
                        }
                        String str = new String(pacote);
                        notifyObservers(pacote);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG,e.getMessage());
                }
            }
        }
    }

}
