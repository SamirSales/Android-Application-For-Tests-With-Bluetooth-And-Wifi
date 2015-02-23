package com.example.samir.comunicacaodedispositivos;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samir on 19/02/15.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ComunicacaoWifiP2P extends Activity implements WifiP2pManager.ConnectionInfoListener{

    private final String TAG = "ComunicacaoWifiP2P";

    EditText editText;
    TextView textRecebido;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;

    private boolean wifiP2pEnabled;

    private WiFiDirectBroadcastReceiver receiver;

    private ArrayList<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private WifiP2pManager.PeerListListener peerListListener;

    private WifiServerThread wifiServerThread;
    private ConnectServerWifi connectServerWifi;

    private WifiClientThread wifiClientThread;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.comm_wifi_p2p);

        wifiServerThread = null;
        wifiClientThread = null;

        editText = (EditText)findViewById(R.id.editText);
        textRecebido = (TextView)findViewById(R.id.textRecebido);

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
        receiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
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

    @Override
    public void onStop(){
        super.onStop();
        if(wifiServerThread != null){
            wifiServerThread.serverRunning = false;
            wifiServerThread = null;
        }

        if(wifiClientThread != null){
            wifiClientThread.running = false;
            wifiClientThread = null;
        }
    }

    public void procurarDispositivosBtn(View view){
        fetchListOfPeers();
        listOfPeersWifiDialog();

        if(wifiServerThread == null){
            wifiServerThread = new WifiServerThread();
            wifiServerThread.start();
        }
    }

    public void clientActionBtn(View view){
        if(wifiClientThread == null){
            wifiClientThread = new WifiClientThread();
            wifiClientThread.start();
        }
    }

    public void sendMessageAction(View view){
        if(wifiClientThread == null && wifiServerThread != null){
            //server
        }else{
            if(wifiClientThread != null && wifiServerThread == null){
                //client
            }
        }
    }

    public void updateTextView(String text){
        textRecebido.setText(textRecebido.getText().toString() + text + "\n");
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
                    //conectar...
                    connectToAPeer(peers.get(position));
                    dialog.dismiss();
                }
            });
            dialog.show();
        }else{
            Toast.makeText(this,"Nenhum par encontrado ainda! Tente de novo, daqui a pouco!",Toast.LENGTH_LONG).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void fetchListOfPeers(){
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i(TAG,"Procurando dispositivos...");

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

                //imprime o nome dos dispositivos
                for(int i=0; i<peers.size(); i++){
                    WifiP2pDevice wDev = peers.get(i);
                    Log.i(TAG,"peer: "+wDev.deviceName);
                }
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(ComunicacaoWifiP2P.this, "Falha na procura de dispositivos!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void connectToAPeer(WifiP2pDevice device){

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        Log.i(TAG, "device.deviceAddress="+device.deviceAddress.toString());
        Log.i(TAG, "device.deviceAddress="+device.deviceAddress);
        Log.i(TAG, "device.wps.setup="+config.wps.setup);
        Log.i(TAG, "device.wps.setup="+config.toString());

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                    Log.i(TAG,"Pedido de conexao realizado com sucesso!!!");
                }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(ComunicacaoWifiP2P.this, "Connect failed. Retry.",
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

    class WiFiDirectBroadcastReceiver extends BroadcastReceiver{

        private WifiP2pManager.Channel mChannel;
        private WifiP2pManager mManager;
        private Context context;

        public WiFiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, Context context){
            super();
            this.mManager = mManager;
            this.mChannel = mChannel;
            this.context = context;
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    wifiP2pEnabled = true;
                    Log.i(TAG, "wifiP2pEnabled = true;");
                } else {
                    wifiP2pEnabled = false;
                    Log.i(TAG, "wifiP2pEnabled = false;");
                }

            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                if (mManager != null) {
                    mManager.requestPeers(mChannel, peerListListener);
                }
                Log.d(TAG, "The peer list has changed!");

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // Connection state changed!  We should probably do something about
                // that.
                Log.i(TAG, "WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION");

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
//                    .findFragmentById(R.id.frag_list);
//            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
//                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
                Log.i(TAG, "WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");

            }
        }
    }

    private class WifiServerThread extends Thread {
        boolean serverRunning;
        boolean connectionEnabled;
        InputStream inputstream;
        OutputStream outputStream;
        Socket client;

        public WifiServerThread(){
            serverRunning = false;
            connectionEnabled = false;
        }

        @Override
        public void run(){
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(8888);
                Log.i(TAG, "server host 8888 started...");
            } catch (IOException e) {
                e.printStackTrace();
            }

            serverRunning = true;
            while(serverRunning){
                try {
                    Log.i(TAG, "server is waiting for connection...");
                    client = serverSocket.accept();
                    inputstream = client.getInputStream();
                    outputStream = client.getOutputStream();
                    connectionEnabled = true;
                    Log.i(TAG, "thread server recebeu conexao!");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                trataPacotesServer(client);


                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void trataPacotesServer(Socket socket){

            final ArrayList<String> arrayMessage = new ArrayList<String>();
            connectServerWifi =  new ConnectServerWifi(socket, arrayMessage);
            connectServerWifi.start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){

                        for(int i=0; i<arrayMessage.size();i++){
                            final String str = arrayMessage.get(i);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateTextView("Recebido:"+str);
                                }
                            });
                            arrayMessage.remove(i);
                            if(i>-1){ i--; }
                        }

                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }




    private class WifiClientThread extends Thread {
        boolean running;
        boolean connectionEnabled;
        Socket socket;
        OutputStream outputStream;
        InputStream inputStream;


        public WifiClientThread(){
            running = false;
            connectionEnabled = false;
        }

        @Override
        public void run(){
            try {
                socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress("36:be:00:9b:bd:c1", 8888)), 500);

                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
}
