package com.example.samir.comunications.bluetooth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.util.Log;

import com.example.samir.comunications.interfaces.Communication;
import com.example.samir.comunications.interfaces.Observer;
import com.example.samir.devicescommunication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samir Sales on 06/07/16.
 */
public abstract class BluetoothUser implements Communication {

    private final String TAG = "BluetoothUser";

    private Activity activity;
    private List<Observer> observers;
    public static final int RECONNECTION_TIME = 3000;
    private byte[] data;
    private Runnable runnable;
    private Handler handler;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private ProgressDialog dialogWaitConnection;
    private WriterBluetooth writerThread;
    private int errorCounter = 0;

    public BluetoothUser(Activity parent) {
        activity = parent;
        observers = new ArrayList<>();
        handler = new Handler();

        setDialogWaitConnection();
    }

    protected void setDialogWaitConnection(){
        dialogWaitConnection = new ProgressDialog(activity);
        dialogWaitConnection.setIcon(R.drawable.ic_launcher);
        dialogWaitConnection.setTitle("Aguarde!");
        dialogWaitConnection.setMessage("conectando...");
        dialogWaitConnection.setCanceledOnTouchOutside(false);
        dialogWaitConnection.setCancelable(false);
        dialogWaitConnection.setIndeterminate(false);
        dialogWaitConnection.setOnCancelListener(null);
    }

    public void showDialogWaitConnection(){
        if (!dialogWaitConnection.isShowing()) {
            activity.runOnUiThread(new Thread() {
                @Override
                public void run() {
                    dialogWaitConnection.show();
                }
            });
        }
    }

    public void closeDialogWaitConnection(){
        if (dialogWaitConnection.isShowing()) {
            dialogWaitConnection.dismiss();
        }
    }

    @Override
    public void open() {
        // different way for server and client...
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
        // different way for server and client...
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
     * It sends data packets.
     * @param data
     */
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
        return false;
    }

    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
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

    protected Activity getActivity(){
        return activity;
    }

    public List<Observer> getObservers(){
        return observers;
    }

    public void resetWriterBluetooth(){
        writerThread = new WriterBluetooth();
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * This method start a timer to try a new connection with the device.
     */
    protected void waitAndReconnect() {
        handler.removeCallbacks(runnable);
        runnable = new Runnable() {
            public void run() {
                reconnect();
            }
        };
        handler.postDelayed(runnable, RECONNECTION_TIME);
    }

    /**
     * This thread reads the bytes from input stream.
     */
    public class ReaderBluetoothThread extends Thread {
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
    public class WriterBluetooth implements Runnable {
        @Override
        public void run() {
            if (data != null) {
                try {
                    outputStream.write(data);
                    errorCounter = 0;
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
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
