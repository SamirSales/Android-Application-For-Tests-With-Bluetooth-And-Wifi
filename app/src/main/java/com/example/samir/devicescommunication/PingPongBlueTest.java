package com.example.samir.devicescommunication;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.samir.comunications.enums.EnumConnection;
import com.example.samir.comunications.interfaces.Communication;
import com.example.samir.comunications.CommunicationFactory;
import com.example.samir.comunications.interfaces.Observer;
import com.example.samir.testOfComunication.Utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by Samir Sales on 02/03/15.
 */
public class PingPongBlueTest extends Activity implements Observer {

    private String TAG = "PingPongBlueTest";

    private final int DELAY_TO_SEND = 300;

    private long counter;
    private long counterOfCounter;

    private TextView textStatus;
    private TextView textRecebido;

    private static ConnectThreadBluePingTest connectedThreadServer;
    private static boolean connectionStarted;
    private boolean working_as_server;
    private Communication communication = null;

    private AcceptThread acceptThread;

    private String startTime;

    private int batteryInitPercent;
    private int batteryPercent;

    private Handler handler = new Handler();
    String batteryChargingStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping_pong_bleutooth);

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        startTime = hour+":"+minutes+":"+seconds;

        //bateria
        //this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        counter = 0;
        counterOfCounter = 0;

        textStatus = (TextView)findViewById(R.id.textStatus);
        textRecebido = (TextView)findViewById(R.id.textRecebido);

        connectedThreadServer = null;
        connectionStarted = false;
    }

//    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(final Context context, Intent intent) {
//            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
//            Log.i(TAG, "level: " + level + "; scale: " + scale);
//            int percent = (level*100)/scale;
//
//            if(batteryInitPercent==0){
//                batteryInitPercent = percent;
//            }
//            batteryPercent = percent;
//
//            batteryChargingStr = String.valueOf(percent) + "%";
//            handler.post( new Runnable() {
//                public void run() {
//                    Log.i("bateria","bateria: "+ batteryChargingStr);
//                }
//            });
//        }
//    };

    public void connectAction(View view){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conexão Bluetooth");
        builder.setMessage("Conectar-se como...");
        builder.setPositiveButton("cliente", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                working_as_server = false;
                initConnection();
            }
        });
        builder.setNegativeButton("servidor", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                working_as_server = true;
                acceptThread = new AcceptThread();
                acceptThread.start();
            }
        });
        builder.show();
    }

    private void sendSayingTheNextNumber(String numberReceived){
        try {
            long number = Long.parseLong(numberReceived);
            String msg = "0";

            if(number < Long.MAX_VALUE){
                counter = number+1;
                msg = ""+counter;
            }else{
                counterOfCounter++;
            }
            updateReceivedText("Eu: " + msg);
        }catch (Exception ex){
            Log.e(TAG, ex.getMessage());
        }
        updateInformations();
    }

    int count_update = 0;
    private void updateReceivedText(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(count_update>=6){
                    count_update = 0;
                    textRecebido.setText("");
                }
                count_update++;
                textRecebido.setText(textRecebido.getText().toString()+text+"\n");
            }
        });

    }

    public void initConnection() {
        if(!connectionStarted){
            initCommunication(EnumConnection.BLUETOOTH_PING_TEST);
            connectionStarted = true;
        }
    }

    public void initCommunication(EnumConnection con) {
        communication = new CommunicationFactory(this, con).getCommunication();
        communication.addObserver(this);
        communication.open();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void textNotifyConnection(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText(str);
            }
        });
    }

    public void updateInformations(){
        final String ip = Utils.getIPAddress(true);
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        String endTime = hour+":"+minutes+":"+seconds;

        if(connectedThreadServer == null){
            String str = getStringInfoFormated("CLIENT", endTime);
            textNotifyConnection(str);
        }else{
            String str = getStringInfoFormated("SERVER", endTime);
            textNotifyConnection(str);
        }
    }

    private String getStringInfoFormated(String user, String endTime){
        return  "I'm "+user+" \n" +
                "horario  inicial: "+ startTime +"\n"+
                "horario    final: "+endTime+"\n"+
                "loop(s) number = "+counterOfCounter+"\n" +
                "last count = "+counter+"\n"+
                "bateria inicial = "+batteryInitPercent+"%\n"+
                "bateria agora   = "+batteryPercent+"%";
    }

    @Override
    public void update(byte[] data) {
        String str =  new String(data);
        updateReceivedText("Recebido:" + str);
        sendSayingTheNextNumber(str);
        String str2 = counter+"";
        communication.send(str2.getBytes(Charset.forName("UTF-8")));
    }

    @Override
    public void connectedCallback() {
        new Thread() {
            public void run() {
                if(communication != null && communication.isConnected()){
                    communication.send("0".getBytes());
                }
                while (communication != null && communication.isConnected()) {
                    updateInformations();

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textStatus.setText("desconectado");
                        textStatus.setTextColor(Color.GRAY);
                    }
                });
            }
        }.start();
    }

    @Override
    public void connectedFault() {
        initConnection();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        private boolean thread_ativa;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            thread_ativa = false;
            BluetoothServerSocket tmp = null;
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("nome", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    thread_ativa = true;
                    updateInformations();

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    manageConnectedSocket(socket);
                }
            }
            Log.i(TAG,"thread terminada!");
        }

        private void manageConnectedSocket(BluetoothSocket socket) {
            final ArrayList<String> arrayMessage = new ArrayList<String>();
            connectedThreadServer =  new ConnectThreadBluePingTest(socket, arrayMessage);
            connectedThreadServer.start();

            // Reading...
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){

                        for(int i=0; i<arrayMessage.size();i++){
                            final String str = arrayMessage.get(i);

                            updateReceivedText("Recebido:" + str);
                            sendSayingTheNextNumber(str);
                            arrayMessage.remove(i);
                            String str2 = counter+"";
                            connectedThreadServer.write(str2.getBytes(Charset.forName("UTF-8")));
                            if(i>-1){ i--; }
                        }

                        try {
                            Thread.sleep(DELAY_TO_SEND);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
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

}
