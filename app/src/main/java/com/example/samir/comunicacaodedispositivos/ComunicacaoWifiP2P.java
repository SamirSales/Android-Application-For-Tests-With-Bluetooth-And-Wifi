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
import android.os.AsyncTask;
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

import com.example.samir.comm.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by samir on 19/02/15.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ComunicacaoWifiP2P extends Activity implements WifiP2pManager.ConnectionInfoListener{

    private static final String TAG = "ComunicacaoWifiP2P";

    final private static String IP_SERVER = "192.168.49.1";
    final private static int HOST = 8080;

    EditText editText;
    TextView textRecebido;
    TextView textStatus;
    Button server_btn;
    Button client_btn;
    Button search_btn;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;

    private boolean wifiP2pEnabled;

    private WiFiDirectBroadcastReceiver receiver;

    private ArrayList<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private WifiP2pManager.PeerListListener peerListListener;
    private ConnectServerWifi connectServerWifi;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.comm_wifi_p2p);

        editText = (EditText)findViewById(R.id.editText);
        textRecebido = (TextView)findViewById(R.id.textRecebido);
        textStatus = (TextView)findViewById(R.id.textStatus);
        search_btn = (Button)findViewById(R.id.search_btn);
        server_btn = (Button)findViewById(R.id.server_btn);
        client_btn = (Button)findViewById(R.id.client_btn);

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
    }

    public void procurarDispositivosBtn(View view){
        fetchListOfPeers();
        listOfPeersWifiDialog();
    }

    public void clientActionBtn(View view){
        MyClientTask myClientTask = new MyClientTask(IP_SERVER,HOST);
        myClientTask.execute();
    }

    public void serverActionBtn(View view){
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    public void updateStatusOfConnection(){
        final String ip = Utils.getIPAddress(true);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText("server:"+IP_SERVER+" porta:"+HOST);
                client_btn.setVisibility(View.INVISIBLE);
                search_btn.setVisibility(View.INVISIBLE);
                server_btn.setVisibility(View.INVISIBLE);
                client_btn.setEnabled(false);
                search_btn.setEnabled(false);
                server_btn.setEnabled(false);
            }
        });
    }

    public void sendMessageAction(View view){
        //TODO
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

                // Connection state changed!  We should probably do something about that.
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


    String message = "";
    ServerSocket serverSocket;

    private class SocketServerThread extends Thread {
        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(HOST);
                ComunicacaoWifiP2P.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textRecebido.setText("I'm waiting here: "+serverSocket.getLocalPort());
                        updateStatusOfConnection();
                    }
                });

                while (true) {
                    Socket socket = serverSocket.accept();
                    count++;
                    message += "#"+count+" from "+socket.getInetAddress()+":"+socket.getPort() + "\n";

                    ComunicacaoWifiP2P.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textRecebido.setText(message);
                        }
                    });

                    SocketServerReplyThread socketServerReplyThread =
                            new SocketServerReplyThread(socket, count);
                    socketServerReplyThread.run();

                }
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = "Hello from Android, you are #" + cnt;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
                printStream.close();
                updateStatusOfConnection();

                message += "replayed: " + msgReply + "\n";

                ComunicacaoWifiP2P.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textRecebido.setText(message);
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                message += "Something wrong! " + e.toString() + "\n";
            }

            ComunicacaoWifiP2P.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textRecebido.setText(message);
                }
            });
        }

    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = null;
            enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: " + inetAddress.getHostAddress() + "\n";
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        String dstAddress;
        int dstPort;
        String response = "";

        MyClientTask(String addr, int port){
            dstAddress = addr;
            dstPort = port;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;

            try {
                socket = new Socket(dstAddress, dstPort);

                ByteArrayOutputStream byteArrayOutputStream =
                        new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];

                int bytesRead;
                InputStream inputStream = socket.getInputStream();

    /*
     * notice:
     * inputStream.read() will block if no data return
     */
                while ((bytesRead = inputStream.read(buffer)) != -1){
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            textRecebido.setText(response);
            super.onPostExecute(result);
        }

    }












//    public static String getMACAddress(String interfaceName) {
//        try {
//            List<NetworkInterface> interfaces = Collections
//                    .list(NetworkInterface.getNetworkInterfaces());
//
//            for (NetworkInterface intf : interfaces) {
//                if (interfaceName != null) {
//                    if (!intf.getName().equalsIgnoreCase(interfaceName))
//                        continue;
//                }
//                byte[] mac = intf.getHardwareAddress();
//                if (mac == null)
//                    return "";
//                StringBuilder buf = new StringBuilder();
//                for (int idx = 0; idx < mac.length; idx++)
//                    buf.append(String.format("%02X:", mac[idx]));
//                if (buf.length() > 0)
//                    buf.deleteCharAt(buf.length() - 1);
//                return buf.toString();
//            }
//        } catch (Exception ex) {
//        } // for now eat exceptions
//        return "";
//        /*
//         * try { // this is so Linux hack return
//         * loadFileAsString("/sys/class/net/" +interfaceName +
//         * "/address").toUpperCase().trim(); } catch (IOException ex) { return
//         * null; }
//         */
//    }
}
