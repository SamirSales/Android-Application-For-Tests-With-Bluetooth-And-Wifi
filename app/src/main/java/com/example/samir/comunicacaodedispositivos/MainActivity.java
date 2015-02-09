package com.example.samir.comunicacaodedispositivos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.samir.adapter.AdapterListModos;
import com.example.samir.adapter.Item;
import com.example.samir.comm.Communication;
import com.example.samir.comm.CommunicationFactory;
import com.example.samir.comm.Observer;
import com.example.samir.constantes.EnumConexao;


public class MainActivity extends Activity implements Observer {

    private EditText editText;
    private Button btnSend;
    private TextView textRecebido;

    private static boolean connectionStarted;
    private Communication communication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionStarted = false;

        editText = (EditText)findViewById(R.id.editText);
        btnSend = (Button)findViewById(R.id.btnSend);
        textRecebido = (TextView)findViewById(R.id.textRecebido);
    }

    @Override
    public void onStart(){
        super.onStart();
        initConnection();
    }

    public void sendMessageAction(View view){
        String msg = editText.getText().toString();
        Log.i("teste", "mensagem:"+msg);
        editText.setText("");
    }

    public void initConnection() {
        if(!connectionStarted){
            Log.i("teste", "Starting connection...");

//            AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
//            final AdapterListModos adapterListModos = new AdapterListModos(MainActivity.this,
//                    ListaModos.getItens(MainActivity.this), R.id.textView);
//            builderSingle.setTitle(getString(R.string.modos_conexao));
//            builderSingle.setCancelable(false);
//            builderSingle.setPositiveButton(getString(R.string.sair),
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            MainActivity.this.finish();
//                        }
//                    });
//
//            builderSingle.setAdapter(adapterListModos,
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int position) {
//                            Item item = (Item) adapterListModos.getItem(position);
//                            if (item != null) {
//                                iniciarComunicacao(item.getConexao());
//                            }
//                        }
//                    });
//            builderSingle.show();
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

    }

    @Override
    public void connectedCallback() {

    }

    @Override
    public void connectedFault() {

    }
}
