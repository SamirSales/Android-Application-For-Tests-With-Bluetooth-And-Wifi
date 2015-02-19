package com.example.samir.comunicacaodedispositivos;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.samir.comm.Communication;
import com.example.samir.comm.CommunicationFactory;
import com.example.samir.comm.Observer;
import com.example.samir.constantes.EnumConexao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;


public class BluetoothChat extends Activity implements Observer {

    private String TAG = "MainActivity";

    private EditText editText;
    private Button btnSend;
    private TextView textRecebido;
    private TextView textConnected;

    private static ConnectedThread connectedThreadServer;
    private static boolean connectionStarted;
    private boolean working_as_server;
    private Communication communication = null;

    private AcceptThread aTh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectedThreadServer = null;
        connectionStarted = false;

        editText = (EditText)findViewById(R.id.editText);
        btnSend = (Button)findViewById(R.id.btnSend);
        textRecebido = (TextView)findViewById(R.id.textRecebido);
        textConnected = (TextView)findViewById(R.id.textConnected);
    }

    public void sendMessageAction(View view){
        String msg = editText.getText().toString();
        if(working_as_server){

            if(connectedThreadServer != null){
                byte[] bytes = msg.getBytes();
                connectedThreadServer.write(bytes);
                editText.setText("");
                newLineTextView("Enviado:"+msg);
            }
        }else{
            editText.setText("");
            newLineTextView("Enviado:"+msg);
            communication.send(msg.getBytes());
        }
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

    private void newLineTextView(String text){
        textRecebido.setText(textRecebido.getText().toString()+text+"\n");
    }

    public void initConnection() {
        if(!connectionStarted){
            iniciarComunicacao(EnumConexao.BLUETOOTH);
            connectionStarted = true;
        }
    }

    public void iniciarComunicacao(EnumConexao con) {
        communication = new CommunicationFactory(this, con).getCommunication();
        communication.addObserver(BluetoothChat.this);
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

    @Override
    public void update(byte[] data) {
        newLineTextView("Recebido:"+new String(data));
    }

    @Override
    public void connectedCallback() {
        new Thread() {
            public void run() {
                while (communication != null && communication.isConnected()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textConnected.setText("cliente conectado");
                            textConnected.setTextColor(Color.GREEN);
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
                        textConnected.setText("desconectado");
                        textConnected.setTextColor(Color.GRAY);
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
                            textConnected.setText("servidor conectado");
                            textConnected.setTextColor(Color.GREEN);
                        }
                    });

                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
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

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){

                        for(int i=0; i<arrayMessage.size();i++){
                            final String str = arrayMessage.get(i);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    newLineTextView("Recebido:"+str);
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
