package com.example.samir.devicescommunication.pingpong;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.samir.comunications.CommunicationFactory;
import com.example.samir.comunications.enums.EnumConnection;
import com.example.samir.comunications.interfaces.Communication;
import com.example.samir.comunications.interfaces.Observer;
import com.example.samir.devicescommunication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.UUID;

/**
 * Created by Samir Sales on 02/03/15.
 */
public class PingPongBlueTest extends PingPongActivity implements Observer {

    private String TAG = "PingPongBlueTest";

    private static ConnectThreadBluePingTest connectedThreadServer;
    private static boolean connectionStarted;
    private Communication communication = null;
    private AcceptThread acceptThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping_pong_bleutooth);

        startSettings();
        connectedThreadServer = null;
        connectionStarted = false;
    }

    public void connectAction(View view){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conexão Bluetooth");
        builder.setMessage("Conectar-se como...");
        builder.setPositiveButton("cliente", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                initConnection();
            }
        });
        builder.setNegativeButton("servidor", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                acceptThread = new AcceptThread();
                acceptThread.start();
            }
        });
        builder.show();
    }

    public void initConnection() {
        if(!connectionStarted){
            initCommunication(EnumConnection.BLUETOOTH_CLIENT);
            connectionStarted = true;
        }
    }

    public void initCommunication(EnumConnection con) {
        communication = new CommunicationFactory(this, con).getCommunication();
        communication.addObserver(this);
        communication.open();
    }

    @Override
    public void updateInfo(){
        String endTime = getCurrentTime();
        setUserAndEndTime((connectedThreadServer == null), endTime);
    }

    @Override
    public void update(byte[] data) {
        // bluetooth client reading...
        String dataStr =  new String(data);
        updateTextView("Recebido:" + dataStr);
        sendSayingTheNextNumber(dataStr);
        String counterStr = getCounter()+"";
        communication.send(counterStr.getBytes());
    }

    @Override
    public void connectedCallback() {
        new Thread() {
            public void run() {
                if(communication != null && communication.isConnected()){
                    communication.send("0".getBytes());
                }
                while (communication != null && communication.isConnected()) {
                    updateInfo();

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                setTextStatusDisconnected();
            }
        }.start();
    }

    @Override
    public void connectedFault() {
        initConnection();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

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
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    updateInfo();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    manageConnectedSocket(socket);
                }
            }
        }

        private void manageConnectedSocket(BluetoothSocket socket) {
            connectedThreadServer =  new ConnectThreadBluePingTest(socket);
            connectedThreadServer.start();
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

    @Override
    public void onStop(){
        super.onStop();
        if(acceptThread != null){
            acceptThread.cancel();
        }
    }

    /**
     * This class does the bluetooth server communication interaction.
     */
    public class ConnectThreadBluePingTest extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private PrintStream printStream;

        public ConnectThreadBluePingTest(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            printStream = new PrintStream(mmOutStream);
        }

        public void run() {
            threadWriteMessages(printStream);
            threadReadMessages(mmInStream);
        }
    }

}
