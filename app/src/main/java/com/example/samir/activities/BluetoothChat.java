package com.example.samir.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.samir.comunications.SettingsBluetooth;
import com.example.samir.comunications.enums.EnumConnection;
import com.example.samir.comunications.interfaces.Communication;
import com.example.samir.comunications.CommunicationFactory;
import com.example.samir.comunications.interfaces.Observer;


public class BluetoothChat extends Activity implements Observer {

    private EditText editText;
    private TextView messageReceivedTextView;
    private TextView textConnected;

    private static boolean connectionStarted;
    private boolean working_as_server;
    private Communication communication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);

        connectionStarted = false;

        editText = (EditText)findViewById(R.id.editText);
        messageReceivedTextView = (TextView)findViewById(R.id.textRecebido);
        textConnected = (TextView)findViewById(R.id.textConnected);
    }

    public void sendMessageAction(View view) {
        String msg = editText.getText().toString();
        editText.setText("");
        newLineTextView("Enviado:" + msg);
        communication.send(msg.getBytes());
    }

    public void connectAction(View view){
        SettingsBluetooth.dialogBluetoothConnectionMode(this, new SettingsBluetooth.OnBluetoothConnectionMode() {
            @Override
            public void onClientClick(DialogInterface dialog, int id) {
                working_as_server = false;
                initClientConnection();
            }

            @Override
            public void onServerClick(DialogInterface dialog, int id) {
                working_as_server = true;
                initServerConnection();
            }
        });
    }

    private void newLineTextView(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageReceivedTextView.setText(messageReceivedTextView.getText().toString() + text + "\n");
            }
        });
    }

    /**
     * This method makes the user connect as client.
     */
    public void initClientConnection() {
        if(!connectionStarted){
            initCommunication(EnumConnection.BLUETOOTH_CLIENT);
            connectionStarted = true;
        }
    }

    /**
     * This method makes the user connect as server.
     */
    public void initServerConnection(){
        if(!connectionStarted){
            initCommunication(EnumConnection.BLUETOOTH_SERVER);
            connectionStarted = true;
        }
    }

    public void initCommunication(EnumConnection con) {
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
