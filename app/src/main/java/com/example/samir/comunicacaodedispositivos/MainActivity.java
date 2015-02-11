package com.example.samir.comunicacaodedispositivos;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samir.adapter.AdapterListModos;
import com.example.samir.adapter.Item;
import com.example.samir.comm.Communication;
import com.example.samir.comm.CommunicationFactory;
import com.example.samir.comm.Observer;
import com.example.samir.constantes.EnumConexao;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;


public class MainActivity extends Activity implements Observer {

    private EditText editText;
    private Button btnSend;
    private TextView textRecebido;
    private TextView textConnected;

    private static boolean connectionStarted;
    private Communication communication = null;

    private AcceptThread aTh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        teste();
        connectionStarted = false;

        editText = (EditText)findViewById(R.id.editText);
        btnSend = (Button)findViewById(R.id.btnSend);
        textRecebido = (TextView)findViewById(R.id.textRecebido);
        textConnected = (TextView)findViewById(R.id.textConnected);
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    public void sendMessageAction(View view){
        String msg = editText.getText().toString();
        editText.setText("");
        newLineTextView("Enviado:"+msg);

        communication.send(msg.getBytes());
    }

    public void conectarAction(View view){
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
               aTh  = new AcceptThread();
                aTh.start();
            }
        });
        builder.show();
    }

    public void irParaTestesImagens(View view){
        Intent intent = new Intent(this, TestImagens.class);
        startActivity(intent);
    }

    private void newLineTextView(String text){
        textRecebido.setText(textRecebido.getText().toString()+text+"\n");
    }

    public void initConnection() {
        if(!connectionStarted){
            Log.i("teste", "Starting bluetooth connection...");

            iniciarComunicacao(EnumConexao.BLUETOOTH);

            connectionStarted = true;
        }
    }

    public void iniciarComunicacao(EnumConexao con) {
        communication = new CommunicationFactory(this, con).getCommunication();
        communication.addObserver(MainActivity.this);
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
        //TODO
        newLineTextView("Recebido:"+data);
    }

    @Override
    public void connectedCallback() {
        new Thread() {
            public void run() {
                while (communication != null && communication.isConnected()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textConnected.setText("conectado");
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
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            Log.i("bluetooth server","thread iniciada");
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    Log.i("bluetooth server","Uma conexão estabelecida");
                    Toast.makeText(MainActivity.this,"Uma conexão estabelecida", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.i("bluetooth server",e.getMessage());
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    //manageConnectedSocket(socket);
                    try {
                        Log.i("bluetooth server","socket != null -> close();");
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                Log.i("bluetooth server","procurando conexão...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if(aTh != null){
            aTh.cancel();
        }
    }

    public void teste(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        Method getUuidsMethod = null;
        try {
            getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        /*
        samsung
        0000111f-0000-1000-8000-00805f9b34fb
        00001112-0000-1000-8000-00805f9b34fb
        0000110a-0000-1000-8000-00805f9b34fb

        motorola
        00001112-0000-1000-8000-00805f9b34fb
         */

        ParcelUuid[] uuids = new ParcelUuid[0];
        try {
            uuids = (ParcelUuid[]) getUuidsMethod.invoke(adapter, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        for (ParcelUuid uuid: uuids) {
            Log.d("teste", "UUID: " + uuid.getUuid().toString());
        }
    }

}
