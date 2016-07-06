package com.example.samir.comunications;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.samir.comunications.interfaces.Communication;
import com.example.samir.comunications.interfaces.Observer;
import com.example.samir.devicescommunication.R;

/**
 * Created by Samir Sales on 09/02/15.
 */
public class BluetoothClient implements Communication {

    final private String TAG = "BluetoothClient";

    private Activity context;
    private List<Observer> observers;
    private static final int RECONNECTION_TIME = 3000;
    private byte[] data;
    private Runnable runnable;
    private Handler handler;

    private BluetoothSocket socket;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    private ProgressDialog dialogWaitConnection;
    private WriterBluetooth writerThread;
    private int errorCounter = 0;

    public BluetoothClient(Activity parent) {
        context = parent;
        observers = new ArrayList<>();
        handler = new Handler();

        setDialogWaitConnection();
    }

    private void setDialogWaitConnection(){
        dialogWaitConnection = new ProgressDialog(context);
        dialogWaitConnection.setIcon(R.drawable.ic_launcher);
        dialogWaitConnection.setTitle("Aguarde!");
        dialogWaitConnection.setMessage("conectando...");
        dialogWaitConnection.setCanceledOnTouchOutside(false);
        dialogWaitConnection.setCancelable(false);
        dialogWaitConnection.setIndeterminate(false);
        dialogWaitConnection.setOnCancelListener(null);
    }

    /**
     * Opening connection.
     * If it is the first time the connection has been done, a dialog with the bluetooth devices
     * list will be shown as connection options. If the connection has been made previously,
     * an attempted connection with this device.
     */
    @Override
    public void open() {
        showBluetoothDialog();
    }

    /**
     * It sends data packets.
     * @param data
     */
    @Override
    public void send(byte[] data) {
        if (data != null) {
            this.data = data;
            if (isConnected()) {
                new Thread(writerThread).start();
            }
        }
    }

    /**
     * Close the bluetooth connection.
     */
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
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

    /**
     * Reconnect the bluetooth device.
     */
    @Override
    public void reconnect() {
        close();
        open();
    }

    /**
     * It checks the bluetooth connection.
     * @return true if it is connected.
     */

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * This method calls a dialog that shows a list of paired bluetooth devices. Clicking on one
     * of the items from the list, the user can choose his device connection option.
     */
    public void showBluetoothDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.paired_devices_list);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevicesSet = mBluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);

        Button btnBack = (Button) dialog.findViewById(R.id.btnVoltarBluetooth);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                for (Observer o : observers) {
                    o.connectedFault();
                }
                System.exit(0);
            }
        });

        if (pairedDevicesSet.size() > 0) {
            for (BluetoothDevice device : pairedDevicesSet) {
                pairedDevices.add(device);
                pairedDevicesAdapter.add(device.getName());
            }
        } else {
            pairedDevicesAdapter.add("Não há nenhum dispositivo pareado.");
        }

        if (!pairedDevices.isEmpty()) {
            ListView pairedDevicesLV = (ListView) dialog
                    .findViewById(R.id.pairedDevicesListView);
            pairedDevicesLV.setAdapter(pairedDevicesAdapter);
            pairedDevicesLV.setBackgroundColor(Color.LTGRAY);
            pairedDevicesLV.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BluetoothDevice device = pairedDevices.get(position);
                    new ConnectionTask(device).execute();
                    dialog.dismiss();
                }
            });
        }
        dialog.show();
    }

    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    /**
     * Notify the observer about a new data package.
     * @param data um array de bytes
     */
    @Override
    public void notifyObservers(byte[] data) {
        if (data != null) {
            for (Observer o : observers) {
                o.update(data);
            }
        }
    }

    /**
     * This method start a timer to try a new connection with the device.
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
     * Class for bluetooth connection. It start an async task by the use of a BluetoothDevice object
     * as a parameter.
     */
    private class ConnectionTask extends AsyncTask<Void, Void, BluetoothSocket> {
        private BluetoothDevice device;

        public ConnectionTask(BluetoothDevice device) {
            this.device = device;
        }

        protected BluetoothSocket doInBackground(Void... metodo) {
            return connect();
        }

        /**
         * Try to connect to the bluetooth device.
         * @return a bluetooth socket connection or null, if it's fail.
         */
        private BluetoothSocket connect() {
            BluetoothSocket socket = null;
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                socket.connect();
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }
            return socket;
        }

        private void closeDialog() {
            if (dialogWaitConnection.isShowing()) {
                dialogWaitConnection.dismiss();
            }
        }

        /**
         * Before the task execution, the dialog is shown.
         */
        protected void onPreExecute() {
            super.onPreExecute();
            if (!dialogWaitConnection.isShowing()) {
                context.runOnUiThread(new Thread() {
                    @Override
                    public void run() {
                        dialogWaitConnection.show();
                    }
                });
            }
        }

        /**
         * When the task finish, the method test if the socket connection has been opened, get the
         * streams, close the dialog and notify the observers about the bluetooth connection opened.
         * @param result the bluetooth socket connection.
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        protected void onPostExecute(BluetoothSocket result) {
            if (result != null && result.isConnected()) {
                socket = result;

                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    new ReaderBluetoothThread().start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                writerThread = new WriterBluetooth();

                for (Observer o : observers) {
                    o.connectedCallback();
                }

                closeDialog();
            } else {
                waitAndReconnect();
            }
        }
    }

    /**
     * This thread reads the bytes from input stream.
     */
    private class ReaderBluetoothThread extends Thread {
        @Override
        public void run() {
            while (isConnected() && inputStream != null) {
                try {
                    int length = inputStream.available();
                    if (length > 0) {
                        byte[] bytes = new byte[length];
                        for (int i = 0; i < length; i++) {
                            bytes[i] = (byte) inputStream.read();
                        }
                        notifyObservers(bytes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This class makes the sending of data packets.
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