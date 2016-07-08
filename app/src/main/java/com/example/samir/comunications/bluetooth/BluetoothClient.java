package com.example.samir.comunications.bluetooth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.samir.comunications.interfaces.Observer;
import com.example.samir.activities.R;

/**
 * Created by Samir Sales on 09/02/15.
 */
public class BluetoothClient extends BluetoothUser {

    final private String TAG = "BluetoothClient";

    private BluetoothSocket socket;

    public BluetoothClient(Activity parent) {
        super(parent);
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

    @Override
    public void close() {
        super.close();
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
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.paired_devices_list);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevicesSet = mBluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);

        Button btnBack = (Button) dialog.findViewById(R.id.btnVoltarBluetooth);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                for (Observer o : getObservers()) {
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
            ListView pairedDevicesLV = (ListView) dialog.findViewById(R.id.pairedDevicesListView);
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

    /**
     * Class for bluetooth connection. It start an async task by the use of a BluetoothDevice object
     * as a parameter.
     */
    private class ConnectionTask extends AsyncTask<Void, Void, BluetoothSocket> {
        private BluetoothDevice device;

        public ConnectionTask(BluetoothDevice device) {
            this.device = device;
        }

        protected BluetoothSocket doInBackground(Void... method) {
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

        /**
         * Before the task execution, the dialog is shown.
         */
        protected void onPreExecute() {
            super.onPreExecute();
            showDialogWaitConnection();
        }

        /**
         * When the task finish, the method test if the socket connection has been opened, get the
         * streams, close the dialog and notify the observers about the bluetooth connection opened.
         * @param socket the bluetooth socket connection.
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        protected void onPostExecute(BluetoothSocket socket) {

            if (socket != null && socket.isConnected()) {
                BluetoothClient.this.socket = socket;

                try {
                    setInputStream(BluetoothClient.this.socket.getInputStream());
                    setOutputStream(BluetoothClient.this.socket.getOutputStream());
                    new ReaderBluetoothThread().start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                resetWriterBluetooth();

                for (Observer o : getObservers()) {
                    o.connectedCallback();
                }
                closeDialogWaitConnection();
            } else {
                waitAndReconnect();
            }
        }
    }

}