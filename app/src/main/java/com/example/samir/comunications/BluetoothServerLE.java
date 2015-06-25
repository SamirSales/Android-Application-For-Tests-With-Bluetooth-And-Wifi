package com.example.samir.comunications;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;

import com.example.samir.comunicacaodedispositivos.R;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samir on 25/06/15.
 */
public class BluetoothServerLE implements Communication{

    private Activity context;
    private List<Observer> observers;
    private ArrayList<Byte> pacote;
    private boolean flagArmazena;
    private static final int RECONNECTION_TIME = 3000;
    private byte[] data;
    private Runnable runnable;
    private Handler handler;

    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private boolean connected;

    private ProgressDialog dialogWaitConnection;
    //private WriterBluetooth writerThread;
    private int errorCounter = 0;

    public BluetoothServerLE(Activity parent) {
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

    }

    @Override
    public void close() {

    }

    @Override
    public void reconnect() {

    }

    @Override
    public void send(byte[] data) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void addObserver(Observer o) {

    }

    @Override
    public void remObserver(Observer o) {

    }

    @Override
    public void notifyObservers(byte[] data) {

    }
}
