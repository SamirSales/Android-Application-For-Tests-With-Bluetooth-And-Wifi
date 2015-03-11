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
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samir.testOfComunication.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by samir on 27/02/15.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PingPongTest extends Activity implements WifiP2pManager.ConnectionInfoListener{

    private static final String TAG = "PingPongTest";

    final private static String IP_SERVER = "192.168.49.1";
    final private static int HOST = 8080;

    private static String horarioInicial;

    final private long DELAY_TO_SEND = 300;

    TextView textRecebido;
    TextView textStatus;
    Button server_btn;
    Button client_btn;
    Button search_btn;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;

    private boolean wifiP2pEnabled;

    long counter;
    long counterOfCounter;

    MyClientTask myClientTask;
    ArrayList<String> arrayMessageToRead;
    ArrayList<String> arrayMessageToSend;

    SocketServerThread socketServerThread;

    private WiFiDirectBroadcastReceiver receiver;

    private ArrayList<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private WifiP2pManager.PeerListListener peerListListener;
    private ConnectServerWifi connectServerWifi;

    protected PowerManager.WakeLock mWakeLock;

    private int batteryInitPercent;
    private int batteryPercent;

    String valorDaBatteria;

    private Handler handler = new Handler();

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            Log.i(TAG, "level: " + level + "; scale: " + scale);
            int percent = (level*100)/scale;

            if(batteryInitPercent==0){
                batteryInitPercent = percent;
            }
            batteryPercent = percent;

            valorDaBatteria = String.valueOf(percent) + "%";
            handler.post( new Runnable() {

                public void run() {
                    //Toast.makeText(context, "bateria: "+valorDaBatteria, Toast.LENGTH_SHORT).show();
                    Log.i("bateria","bateria: "+valorDaBatteria);
                }
            });

        }
    };

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_ping_pong_test);

        batteryInitPercent = 0;
        batteryPercent = 0;

        //keep screen on
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

        //bateria
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        counter = 0;
        counterOfCounter = 0;

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        horarioInicial = hour+":"+minutes+":"+seconds;

        arrayMessageToRead = new ArrayList<>();
        arrayMessageToSend = new ArrayList<>();
        socketServerThread = null;

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

    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }

    public void procurarDispositivosBtn(View view){
        fetchListOfPeers();
        listOfPeersWifiDialog();
    }

    private void sendSayingTheNextNumber(String numberRecived){
        //TODO

        try {
            long number = Long.parseLong(numberRecived);
            String msg = "0";

            if(number < Long.MAX_VALUE){
                counter = number+1;
                msg = ""+counter;
                updateTextView("Eu: "+msg);
                arrayMessageToSend.add(msg);
            }else{
                counterOfCounter++;
                updateTextView("Eu: "+msg);
                arrayMessageToSend.add(msg);
            }
        }catch (Exception ex){
            Log.e(TAG, ex.getMessage());
        }

        updateInformations();
    }

    public void clientActionBtn(View view){
        if(myClientTask != null && socketServerThread == null){

        }
        try {
            myClientTask = new MyClientTask(IP_SERVER,HOST);
            myClientTask.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverActionBtn(View view){
        socketServerThread = new SocketServerThread();
        socketServerThread.start();
    }

    public void updateInformations(){
        final String ip = Utils.getIPAddress(true);
        //TODO
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        String horarioFinal = hour+":"+minutes+":"+seconds;

        if(myClientTask != null || socketServerThread != null){
            if(myClientTask != null){
                String str = "I'm CLIENT \n" +
                        "ip server" + IP_SERVER + " host:" + HOST+"\n" +
                        "horario  inicial: "+horarioInicial+"\n"+
                        "horario    final: "+horarioFinal+"\n"+
                        "loop(s) number = "+counterOfCounter+"\n"+
                        "last count = "+counter+"\n"+
                        "bateria inicial = "+batteryInitPercent+"%"+"\n"+
                        "bateria agora   = "+batteryPercent+"%";
                avisoConexao(str);
            }else{
                String str = "I'm SERVER \n" +
                        "host:" + HOST+"\n" +
                        "horario  inicial: "+horarioInicial+"\n"+
                        "horario    final: "+horarioFinal+"\n"+
                        "loop(s) number = "+counterOfCounter+"\n" +
                        "last count = "+counter+"\n"+
                        "bateria inicial = "+batteryInitPercent+"%\n"+
                        "bateria agora   = "+batteryPercent+"%";
                avisoConexao(str);
            }
        }


    }

    public void avisoConexao(final String mgs){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText(mgs);
                client_btn.setVisibility(View.INVISIBLE);
                search_btn.setVisibility(View.INVISIBLE);
                server_btn.setVisibility(View.INVISIBLE);
                client_btn.setEnabled(false);
                search_btn.setEnabled(false);
                server_btn.setEnabled(false);
            }
        });
    }

    int linhas = 0;
    public void updateTextView(final String text){
        if(linhas<6){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textRecebido.setText(textRecebido.getText().toString() + text + "\n");
                }
            });
            linhas++;
        }else{
            linhas = 0;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textRecebido.setText("");
                    textRecebido.setText(textRecebido.getText().toString() + text + "\n");
                }
            });
            linhas++;
        }

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

                //imprime o nome dos dispositivos
                for(int i=0; i<peers.size(); i++){
                    WifiP2pDevice wDev = peers.get(i);
                    Log.i(TAG,"peer: "+wDev.deviceName);
                }
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(PingPongTest.this, "Falha na procura de dispositivos!", Toast.LENGTH_LONG).show();
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
                Toast.makeText(PingPongTest.this, "Connect failed. Retry.",
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
                Log.i(TAG, "WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");

            }
        }
    }


    String message = "";
    ServerSocket serverSocket;

    private class SocketServerThread extends Thread {
        int count = 0;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        boolean serverRunnig = false;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(HOST);
                PingPongTest.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textRecebido.setText("I'm waiting here: "+serverSocket.getLocalPort());
                        //updateStatusOfConnection();
                        updateInformations();
                    }
                });

                if(!serverRunnig){
                    serverRunnig = true;

                    /*
                    Essa eh pra ser a parte onde o servidor fica esperando conexoes
                     */
                    while (serverRunnig) {
                        Socket socket = serverSocket.accept();
                        outputStream = socket.getOutputStream();
                        inputStream = socket.getInputStream();

                        count++;
                        message += "#"+count+" from "+socket.getInetAddress()+":"+socket.getPort() + "\n";

                        PingPongTest.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textRecebido.setText(message);
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

    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        public void myPrint(String msg){
            OutputStream outputStream;
            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msg);
                printStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            OutputStream outputStream;
            InputStream inputStream;
            String msgReply = "Hello from Android, you are #" + cnt;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                inputStream = hostThreadSocket.getInputStream();
                PrintStream printStream = new PrintStream(outputStream);
                //printStream.print(msgReply);
                //printStream.close();
                //updateStatusOfConnection();
                updateInformations();

//                message += "replayed: " + msgReply + "\n";
//
//                PingPongTest.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        textRecebido.setText(message);
//                    }
//                });

                final PrintStream printStream2 = printStream;
                final InputStream inputStream2 = inputStream;

                //NEWS HERE... TODO
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true){

                            if(arrayMessageToSend.size()>0){
                                for(String str : arrayMessageToSend){
                                    Log.i(TAG, "arrayMessage.get(i)="+str);
                                    printStream2.print(str);
                                    //updateTextView("Eu: "+str);
                                }
                                arrayMessageToSend = new ArrayList<String>();
                            }

                        }
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] buffer = new byte[1024];  // buffer store for the stream
                        int bytes; // bytes returned from read()
                        //se tiver algo no read(), imprime
                        while (true){
                            try {
                                // Read from the InputStream
                                bytes = inputStream2.read(buffer);
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
                                        //Log.i(TAG, "arrayMessage.get(i)="+str2);
                                        //printStream2.print(str2);
                                        updateTextView("Outro usuario: " + str2);
                                        sendSayingTheNextNumber(str2);

                                    }
                                    arrayMessageToRead = new ArrayList<String>();
                                }

                            } catch (IOException e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    }
                }).start();


            } catch (IOException e) {
                e.printStackTrace();
                message += "Something wrong! " + e.toString() + "\n";
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textRecebido.setText(message);
                }
            });
        }

    }

    public class MyClientTask extends AsyncTask {

        Socket socket;
        InputStream mmInStream;
        OutputStream mmOutStream;

        String ip;
        int porta;

        String response = "";

        //alteracoes...
        boolean myClientIsRunning;
        ByteArrayOutputStream byteArrayOutputStream;
        PrintStream printStream;

        MyClientTask(String ip, int porta) throws IOException {
            this.ip = ip;
            this.porta = porta;
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
                socket = new Socket(ip,porta);
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (UnknownHostException e) {
                Log.e(TAG,e.getMessage());
            } catch(IOException e) {
                Log.e(TAG,e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            if(socketServerThread == null){
                //avisoConexao("client porta:"+HOST);
                updateInformations();
            }

            printStream = new PrintStream(mmOutStream);
            printStream.print("0");

            //ESCRITA
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){

                        if(arrayMessageToSend.size()>0){
                            for(String str : arrayMessageToSend){
                                //updateTextView("Eu: "+str);
                                Log.i(TAG, "cliente envia = " + str);
                                printStream.print(str);
                            }
                            arrayMessageToSend = new ArrayList<String>();
                        }
                    }
                }
            }).start();

            //LEITURA
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];  // buffer store for the stream
                    int bytes; // bytes returned from read()

                    while (true) {
                        try {
                            // Read from the InputStream
                            bytes = mmInStream.read(buffer);
                            Log.i(TAG,"bytes = "+bytes);
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
                                    Log.i(TAG, "arrayMessage.get(i)="+str2);
                                    updateTextView("Outro usuario: "+str2);
                                    sendSayingTheNextNumber(str2);
                                }
                                arrayMessageToRead = new ArrayList<String>();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
            }).start();

            return null;
        }
    }

}