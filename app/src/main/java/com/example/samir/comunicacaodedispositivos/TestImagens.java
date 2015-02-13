package com.example.samir.comunicacaodedispositivos;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samir.adapter.MyPagerAdapter;
import com.example.samir.comm.Communication;
import com.example.samir.comm.CommunicationFactory;
import com.example.samir.comm.Observer;
import com.example.samir.constantes.EnumConexao;
import com.example.samir.objetos.ImagemComStr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by samir on 10/02/15.
 */

public class TestImagens extends Activity implements Observer {

    private String TAG = "TestImagens";

    private android.support.v4.view.ViewPager pager;
    private Button serverBtn;
    private Button clientBtn;
    private TextView textConectado;

    private static ConnectedThread connectedThreadServer;
    private static boolean connectionStarted;
    private boolean working_as_server;
    private Communication communication = null;

    private AcceptThread aTh;

    private MyPagerAdapter myPagerAdapter;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_image_test);

        serverBtn = (Button)findViewById(R.id.serverBtn);
        clientBtn = (Button)findViewById(R.id.clientBtn);
        textConectado = (TextView)findViewById(R.id.textConectado);
        pager = (ViewPager)findViewById(R.id.pager);
        pager.setPageTransformer(true, new DepthPageTransformer());

        connectedThreadServer = null;

        ArrayList<ImagemComStr> arrayList = new ArrayList<ImagemComStr>();
        arrayList.add(new ImagemComStr(R.drawable.image1,"Lobo"));
        arrayList.add(new ImagemComStr(R.drawable.image2,"Arara"));
        arrayList.add(new ImagemComStr(R.drawable.image3,"Carro"));
        arrayList.add(new ImagemComStr(R.drawable.image4,"Leão"));

        myPagerAdapter = new MyPagerAdapter(this,arrayList, connectedThreadServer);
        pager.setAdapter(myPagerAdapter);
    }

    public void buttonActionServer(View view){
        working_as_server = true;
        aTh  = new AcceptThread();
        aTh.start();
    }

    public void buttonActionClient(View view){
        working_as_server = false;
        initConnection();
    }

    public void initConnection() {
        if(!connectionStarted){
            iniciarComunicacao(EnumConexao.BLUETOOTH);
            connectionStarted = true;
        }
    }

    public void iniciarComunicacao(EnumConexao con) {
        communication = new CommunicationFactory(this, con).getCommunication();
        communication.addObserver(TestImagens.this);
        communication.open();
    }

    public void sendMessage(){
        if(working_as_server){

            if(connectedThreadServer != null){
                byte[] bytes = {1,1};
                connectedThreadServer.write(bytes);
            }
        }
    }

    @Override
    public void update(byte[] data) {
        Log.i(TAG, "press");
        byte tipo = data[0];

        switch (tipo){
            case ImagemComStr.UPDATE_PAGE:
                final byte pagina = data[1];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pager.setCurrentItem(pagina);
                    }
                });

                break;
            case ImagemComStr.PRESS_PAGE:
                final int position = pager.getCurrentItem();
                final ImagemComStr im = myPagerAdapter.getArrayICS().get(position);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TestImagens.this,im.getTitulo(),Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            default:
                Log.i(TAG, "press "+tipo);
                break;
        }
    }

    @Override
    public void connectedCallback() {
        new Thread() {
            public void run() {
                while (communication != null && communication.isConnected()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textConectado.setVisibility(View.VISIBLE);
                            textConectado.setText("CLIENTE");
                            serverBtn.setVisibility(View.GONE);
                            clientBtn.setVisibility(View.GONE);
                        }
                    });

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textConectado.setVisibility(View.INVISIBLE);
                        serverBtn.setVisibility(View.VISIBLE);
                        clientBtn.setVisibility(View.VISIBLE);
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textConectado.setVisibility(View.VISIBLE);
                            textConectado.setText("SERVIDOR");
                            serverBtn.setVisibility(View.GONE);
                            clientBtn.setVisibility(View.GONE);
                        }
                    });

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    manageConnectedSocket(socket);
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG,"thread terminada!");
        }

        private void manageConnectedSocket(BluetoothSocket socket) {
            final ArrayList<String> arrayMessage = new ArrayList<String>();
            connectedThreadServer =  new ConnectedThread(socket, arrayMessage);
            connectedThreadServer.start();
            myPagerAdapter.setConnectedThreadServer(connectedThreadServer);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){

                        for(int i=0; i<arrayMessage.size();i++){
                            final String str = arrayMessage.get(i);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //newLineTextView("Recebido:"+str); TODO
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
