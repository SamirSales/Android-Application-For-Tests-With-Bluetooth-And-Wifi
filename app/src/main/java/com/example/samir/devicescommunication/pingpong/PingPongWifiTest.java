package com.example.samir.devicescommunication.pingpong;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.samir.comunications.SettingsWifi;
import com.example.samir.devicescommunication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by Samir Sales on 27/02/15.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PingPongWifiTest extends PingPongActivity implements WifiP2pManager.ConnectionInfoListener{

    private static final String TAG = "PingPongWifiTest";

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;

    private MyClientTask myClientTask;
    private ArrayList<String> arrayMessageToRead;

    private SocketServerThread socketServerThread;

    private WiFiDirectBroadcastReceiver receiver;

    private ArrayList<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private WifiP2pManager.PeerListListener peerListListener;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_ping_pong_test);

        resetBatteryStatusCounters();
        keepScreenOn();
        setRegisterReceiverBatteryChanged();
        resetCounters();
        setStartTime();
        setArrayMessageToSend(new ArrayList<String>());

        arrayMessageToRead = new ArrayList<>();
        socketServerThread = null;

        setViews();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(mManager, mChannel);
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void searchDevicesBtn(View view){
        fetchListOfPeers();
        listOfPeersWifiDialog();
    }

    public void clientActionBtn(View view){
        try {
            myClientTask = new MyClientTask(SettingsWifi.IP_SERVER,SettingsWifi.HOST);
            myClientTask.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverActionBtn(View view){
        socketServerThread = new SocketServerThread();
        socketServerThread.start();
    }

    @Override
    public void updateInfo(){
        String endTime = getCurrentTime();
        setUserAndEndTime((myClientTask != null), endTime);
    }

    private void listOfPeersWifiDialog(){
        if(peers.size()>0){
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.paired_devices_list);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);

            String arrayPeers[] = new String[peers.size()];
            for(int i=0; i<arrayPeers.length; i++){
                arrayPeers[i] = peers.get(i).deviceName;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, arrayPeers);

            Button btnBack = (Button) dialog.findViewById(R.id.btnVoltarBluetooth);
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            ListView pairedDevicesLV = (ListView) dialog
                    .findViewById(R.id.pairedDevicesListView);
            pairedDevicesLV.setAdapter(adapter);
            pairedDevicesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // connect...
                    connectToAPeer(peers.get(position));
                    dialog.dismiss();
                }
            });
            dialog.show();
        }else{
            Toast.makeText(this, "Nenhum par encontrado ainda! Tente de novo, daqui a pouco!", Toast.LENGTH_LONG).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void fetchListOfPeers(){
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i(TAG, "Procurando dispositivos...");

                peerListListener = new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peerList) {

                        peers.clear();
                        peers.addAll(peerList.getDeviceList());

                        if (peers.size() == 0) {
                            Log.d(TAG, "Nenhum dispositivo encontrado.");
                            return;
                        }
                    }
                };

                // print devices name...
                for(int i=0; i<peers.size(); i++){
                    WifiP2pDevice wDev = peers.get(i);
                    Log.i(TAG,"peer: "+wDev.deviceName);
                }
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(PingPongWifiTest.this, "Falha na procura de dispositivos!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void connectToAPeer(WifiP2pDevice device){

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Pedido de conexao realizado com sucesso!!!");
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(PingPongWifiTest.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
    will notify you when the state of the connection changes
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        // InetAddress from WifiP2pInfo struct.
        //InetAddress groupOwnerAddress = info.groupOwnerAddress.getHostAddress();

        // After the group negotiation, we can determine the group owner.
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
        }
    }

    class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        private WifiP2pManager.Channel mChannel;
        private WifiP2pManager mManager;

        public WiFiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel){
            super();
            this.mManager = mManager;
            this.mChannel = mChannel;
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.i(TAG, "wifiP2pEnabled = true;");
                } else {
                    Log.i(TAG, "wifiP2pEnabled = false;");
                }

            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                if (mManager != null) {
                    mManager.requestPeers(mChannel, peerListListener);
                }
                Log.d(TAG, "The peer list has changed!");

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // Connection state changed!  We should probably do something about that.
                Log.i(TAG, "WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION");

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");

            }
        }
    }

    private String message = "";
    private ServerSocket serverSocket;

    private class SocketServerThread extends Thread {
        int count = 0;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        boolean serverRunning = false;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SettingsWifi.HOST);
                PingPongWifiTest.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setReceivedDataTextView("I'm waiting here: " + serverSocket.getLocalPort());
                        updateInfo();
                    }
                });

                if(!serverRunning){
                    serverRunning = true;

                    // Server waiting for connections...
                    while (serverRunning) {
                        Socket socket = serverSocket.accept();
                        outputStream = socket.getOutputStream();
                        inputStream = socket.getInputStream();

                        count++;
                        message += "#"+count+" from "+socket.getInetAddress()+":"+socket.getPort() + "\n";

                        PingPongWifiTest.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setReceivedDataTextView(message);
                            }
                        });

                        SocketServerReplyThread socketServerReplyThread =
                                new SocketServerReplyThread(socket, count);
                        socketServerReplyThread.run();
                    }
                }

            } catch (IOException e) {e.printStackTrace();}
        }

        public void write(byte[] bytes){
            if(outputStream != null){
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void printMessages(PrintStream printStream){
        if(getArrayMessageToSend().size()>0){
            for(String str : getArrayMessageToSend()){
                printStream.print(str);
            }
            setArrayMessageToSend(new ArrayList<String>());
        }
    }

    private void readMessages(InputStream inputStream){
        byte[] buffer = new byte[1024];  // buffer store for the stream

        while (true){
            try {
                // Read from the InputStream
                inputStream.read(buffer);
                // Send the obtained bytes to the UI activity
                String str = new String (buffer);
                str = str.trim();
                buffer = new byte[1024];
                arrayMessageToRead.add(str);

                try {
                    Thread.sleep(DELAY_TO_SEND);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(arrayMessageToRead.size()>0){
                    for(String str2 : arrayMessageToRead){
                        updateTextView("Outro usuario: "+str2);
                        sendSayingTheNextNumber(str2);
                    }
                    arrayMessageToRead = new ArrayList<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class is used to make the server mode connection.
     */
    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int counter;

        SocketServerReplyThread(Socket socket, int counter) {
            hostThreadSocket = socket;
            this.counter = counter;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            InputStream inputStream;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                inputStream = hostThreadSocket.getInputStream();
                PrintStream printStream = new PrintStream(outputStream);
                updateInfo();

                final PrintStream printStream2 = printStream;
                final InputStream inputStream2 = inputStream;

                threadWriteMessages(printStream2);
                threadReadMessages(inputStream2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setReceivedDataTextView(message);
                }
            });
        }
    }

    /**
     * This class is used to make the client mode connection.
     */
    public class MyClientTask extends AsyncTask {

        Socket socket;
        InputStream mmInStream;
        OutputStream mmOutStream;

        String ip;
        int host;

        boolean myClientIsRunning;
        PrintStream printStream;

        MyClientTask(String ip, int host) throws IOException {
            this.ip = ip;
            this.host = host;
            myClientIsRunning = false;
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

        @Override
        protected Object doInBackground(Object[] params) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                socket = new Socket(ip, host);
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            if(socketServerThread == null){
                updateInfo();
            }
            printStream = new PrintStream(mmOutStream);
            printStream.print("0");

            threadWriteMessages(printStream);
            threadReadMessages(mmInStream);

            return null;
        }
    }

    private void threadWriteMessages(final PrintStream printStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    printMessages(printStream);
                }
            }
        }).start();
    }

    private void threadReadMessages(final InputStream inputStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                readMessages(inputStream);
            }
        }).start();
    }

}
