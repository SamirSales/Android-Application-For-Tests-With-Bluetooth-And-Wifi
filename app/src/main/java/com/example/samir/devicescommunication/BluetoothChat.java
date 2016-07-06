package com.example.samir.devicescommunication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.samir.comunications.enums.EnumConnection;
import com.example.samir.comunications.interfaces.Communication;
import com.example.samir.comunications.CommunicationFactory;
import com.example.samir.comunications.interfaces.Observer;


public class BluetoothChat extends Activity implements Observer {

    private String TAG = "BluetoothChat";

    private EditText editText;
    private Button btnSend;
    private TextView textRecebido;
    private TextView textConnected;

    private static boolean connectionStarted;
    private boolean working_as_server;
    private Communication communication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionStarted = false;

        editText = (EditText)findViewById(R.id.editText);
        btnSend = (Button)findViewById(R.id.btnSend);
        textRecebido = (TextView)findViewById(R.id.textRecebido);
        textConnected = (TextView)findViewById(R.id.textConnected);
    }

    public void sendMessageAction(View view) {
        String msg = editText.getText().toString();
        editText.setText("");
        newLineTextView("Enviado:" + msg);
        communication.send(msg.getBytes());

    }

    public void conectarAction(View view){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conexão Bluetooth");
        builder.setMessage("Conectar-se como...");
        builder.setPositiveButton("cliente", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                working_as_server = false;
                initClientConnection();
            }
        });
        builder.setNegativeButton("servidor", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                working_as_server = true;
                initServerConnection();
            }
        });
        builder.show();
    }

    private void newLineTextView(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textRecebido.setText(textRecebido.getText().toString()+text+"\n");
            }
        });
    }

    public void initClientConnection() {
        if(!connectionStarted){
            iniciarComunicacaoModoCliente(EnumConnection.BLUETOOTH_CLIENT);
            connectionStarted = true;
        }
    }

    public void initServerConnection(){
        if(!connectionStarted){
            iniciarComunicacaoModoCliente(EnumConnection.BLUETOOTH_SERVER);
            connectionStarted = true;
        }
    }

    public void iniciarComunicacaoModoServidor(EnumConnection con){
        communication = new CommunicationFactory(this, con).getCommunication();
        communication.addObserver(BluetoothChat.this);
        communication.open();
    }

    public void iniciarComunicacaoModoCliente(EnumConnection con) {
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
                            if(working_as_server){
                                textConnected.setText("SERVER");
                                textConnected.setTextColor(Color.BLUE);
                            }else{
                                textConnected.setText("CLIENT");
                                textConnected.setTextColor(Color.GREEN);
                            }
                        }
                    });

                    try {
                        sleep(3000);
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
        if(working_as_server){
            initServerConnection();
        }else{
            initClientConnection();
        }
    }

}
