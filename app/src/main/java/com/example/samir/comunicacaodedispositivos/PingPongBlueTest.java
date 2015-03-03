package com.example.samir.comunicacaodedispositivos;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.samir.comm.Communication;
import com.example.samir.comm.CommunicationFactory;
import com.example.samir.comm.Observer;
import com.example.samir.comm.Utils;
import com.example.samir.constantes.EnumConexao;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by samir on 02/03/15.
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

    private AcceptThread aTh;

    private String horarioInicial;

    private int batteryInitPercent;
    private int batteryPercent;

    private Handler handler = new Handler();
    String valorDaBatteria;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping_pong_bleutooth);

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        horarioInicial = hour+":"+minutes+":"+seconds;

        //bateria
        //this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        counter = 0;
        counterOfCounter = 0;

        textStatus = (TextView)findViewById(R.id.textStatus);
        textRecebido = (TextView)findViewById(R.id.textRecebido);

        connectedThreadServer = null;
        connectionStarted = false;

    }

    public void conectarAction(View view){
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
                aTh  = new AcceptThread();
                aTh.start();
            }
        });
        builder.show();
    }

    private void sendSayingTheNextNumber(String numberRecived){
        //TODO

        try {
            long number = Long.parseLong(numberRecived);
            String msg = "0";

            if(number < Long.MAX_VALUE){
                counter = number+1;
                msg = ""+counter;
                upDateTextRecebido("Eu: "+msg);
            }else{
                counterOfCounter++;
                upDateTextRecebido("Eu: " + msg);
            }
        }catch (Exception ex){
            Log.e(TAG, ex.getMessage());
        }

        updateInformations();
    }

    int count_update = 0;
    private void upDateTextRecebido(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(count_update<6){
                    textRecebido.setText(textRecebido.getText().toString()+text+"\n");
                    count_update++;
                }else{
                    count_update = 0;
                    textRecebido.setText("");
                    textRecebido.setText(textRecebido.getText().toString()+text+"\n");
                    count_update++;
                }
            }
        });

    }

    public void initConnection() {
        if(!connectionStarted){
            iniciarComunicacao(EnumConexao.BLUETOOTH_PING_TEST);
            connectionStarted = true;
        }
    }

    public void iniciarComunicacao(EnumConexao con) {
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

    /*
    Metodos para a implementacao da Interface Observer
     */

    private void avisoConexao(final String str){
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
        String horarioFinal = hour+":"+minutes+":"+seconds;

        if(connectedThreadServer == null){
            String str = "I'm CLIENT \n" +
                    "horario  inicial: "+horarioInicial+"\n"+
                    "horario    final: "+horarioFinal+"\n"+
                    "loop(s) number = "+counterOfCounter+"\n"+
                    "last count = "+counter+"\n"+
                    "bateria inicial = "+batteryInitPercent+"%"+"\n"+
                    "bateria agora   = "+batteryPercent+"%";
            avisoConexao(str);
        }else{
             String str = "I'm SERVER \n" +
                    "horario  inicial: "+horarioInicial+"\n"+
                    "horario    final: "+horarioFinal+"\n"+
                    "loop(s) number = "+counterOfCounter+"\n" +
                    "last count = "+counter+"\n"+
                    "bateria inicial = "+batteryInitPercent+"%\n"+
                    "bateria agora   = "+batteryPercent+"%";
            avisoConexao(str);
        }
    }

    @Override
    public void update(byte[] data) {
        //TODO
        String str =  new String(data);
        upDateTextRecebido("Recebido:" + str);
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

            /*
            Lendo...
             */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){

                        for(int i=0; i<arrayMessage.size();i++){
                            final String str = arrayMessage.get(i);

                            //TODO
                            upDateTextRecebido("Recebido:" + str);
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
        if(aTh != null){
            aTh.cancel();
        }
    }

}
