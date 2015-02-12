package com.example.samir.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.samir.comunicacaodedispositivos.R;
import com.example.samir.constantes.EnumConexao;

/**
 * Created by samir on 09/02/15.
 */
public class Bluetooth implements Communication {
    private Activity context;
    private List<Observer> observers;
    private ArrayList<Byte> pacote;
    private boolean flagArmazena;
    private static final int RECONNECTION_TIME = 3000;
    private byte[] data;
    private Runnable runnable;
    private Handler handler;

    private BluetoothSocket socket;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    private ProgressDialog dialogWaitConnection;
    private WriterBluetooth writerThread;
    private int errorCounter = 0;

    public Bluetooth(Activity parent) {
        context = parent;
        observers = new ArrayList<Observer>();
        pacote = new ArrayList<Byte>();
        handler = new Handler();

        dialogWaitConnection = new ProgressDialog(context);
        dialogWaitConnection.setIcon(R.drawable.ic_launcher);
        dialogWaitConnection.setTitle("Aguarde!");
        dialogWaitConnection.setMessage("conectando...");
        dialogWaitConnection.setCanceledOnTouchOutside(false);
        dialogWaitConnection.setCancelable(false);
        dialogWaitConnection.setIndeterminate(false);
        dialogWaitConnection.setOnCancelListener(null);
    }

    /**
     * Abertura de conexao.
     * Caso seja a primeria vez que a conexao e' feita, um dialog com os dispositivos bluetooth
     * sera' mostrado para escolha de conexao.
     * Se uma conexao foi feita anteriormente, uma tentativa de conexao
     * com este dispositivo
     */
    @Override
    public void open() {

//        AppConfigDB db = new AppConfigDB(context);
//        db.open();
//        AppConfig config = db.getConfig();
//        db.close();

//        if (config.getConexao() == EnumConexao.NULL) {
            showBluetoothDialog();
//        } else {
//            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(config.getUuid());
//            new ConnectionTask(device).execute();
//        }
    }

    /**
     * Thread responsavel pela leitura de bytes pelo fluxo de entrada bluetooth
     */
    private class ReaderBluetoothThread extends Thread {
        @Override
        public void run() {
            while (isConnected() && inputStream != null) {
                try {
                    int tamanho = inputStream.available();
                    if (tamanho > 0) {
                        byte[] pacote = new byte[tamanho];
                        for (int i = 0; i < tamanho; i++) {
                            pacote[i] = (byte) inputStream.read();
                        }
                        String str = new String(pacote);
                        notifyObservers(pacote);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Envia um pacote de dados
     * @param data
     */
    @Override
    public void send(byte[] data) {
        if (data != null) {
            this.data = data;
            if (isConnected()) {
                new Thread(writerThread).start();
            }
        }
    }

    /**
     * Fecha a conexao bluetooth
     */
    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

    /**
     * Reconecta com o dispositivo bluetooth
     */
    @Override
    public void reconnect() {
        close();
        open();
    }

    /**
     * Verifica o bluetooth esta com a conexao aberta
     * @return
     */

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * O metodo abaixo e' chamado na primeira vez em que a comunicacao bluetooth e feita.
     * Mostra um dialog com os dispositivos bluetooth pareados para iniciar a comunicacao.
     */
    public void showBluetoothDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.paired_devices_list);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevicesSet = mBluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_list_item_1);

        Button btnBack = (Button) dialog.findViewById(R.id.btnVoltarBluetooth);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                for (Observer o : observers) {
                    o.connectedFault();
                }
                System.exit(0);
            }
        });
        if (pairedDevicesSet.size() > 0) {
            for (BluetoothDevice device : pairedDevicesSet) {
                pairedDevices.add(device);
                pairedDevicesAdapter.add(device.getName());
            }
        } else {
            pairedDevicesAdapter.add("Não há nenhum dispositivo pareado.");
        }

        if (!pairedDevices.isEmpty()) {
            ListView pairedDevicesLV = (ListView) dialog
                    .findViewById(R.id.pairedDevicesListView);
            pairedDevicesLV.setAdapter(pairedDevicesAdapter);
            pairedDevicesLV.setBackgroundColor(Color.LTGRAY);
            pairedDevicesLV.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BluetoothDevice device = pairedDevices.get(position);
                    new ConnectionTask(device).execute();
                    dialog.dismiss();
                }
            });
        }
        dialog.show();
    }

    /**
     * Adiciona um observador a lista. As classes de comunicacao usam o observador para:
     *   - Notificar que a conexao foi aberta
     *   - Notificar que ocorreu uma falha na conexao
     *   - Notificar que um pacote foi recebido
     * @param o um objeto que implementa Observer
     */
    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    /**
     * Remove um observador da lista
     * @param o um observador
     */
    @Override
    public void remObserver(Observer o) {
        observers.remove(o);
    }

    /**
     * Notifica aos observadores que um pacote Infolev chegou pela transmissao
     * @param data um array de bytes
     */
    @Override
    public void notifyObservers(byte[] data) {
        if (data != null) {
            for (Observer o : observers) {
                o.update(data);
            }
        }
    }

    /**
     * Identifica quando o pacote do protocolo Infolev e' formado
     *
     * @param pacoteBuffer
     */
    public void verificaPacote(byte[] pacoteBuffer) {
        // Verifica o pacote
        for (int i = 0; i < pacoteBuffer.length; i++) {
            // start byte 02

            if (pacoteBuffer[i] == 0x02) {
                pacote = new ArrayList<Byte>();
                flagArmazena = true;
                pacote.add(pacoteBuffer[i]);
            } else {
                if (flagArmazena) {
                    pacote.add(pacoteBuffer[i]);
                    // end byte 03

                    if (pacoteBuffer[i] == 0x03) {
                        flagArmazena = false;
                    }
                }
            }
        }
    }

    /**
     * Metodo que dispara um timer para tentar uma nova conexao com o dispositivo bluetooth
     */
    private void waitAndReconnect() {
        handler.removeCallbacks(runnable);
        runnable = new Runnable() {
            public void run() {
                reconnect();
            }
        };
        handler.postDelayed(runnable, RECONNECTION_TIME);
    }

    /**
     * Classe para conexao bluetooth. Ocorre o disparo de uma tarefa assincrona,
     * dado um dispositivo bluetooth(classe BluetoothDevice) como parametro.
     */
    private class ConnectionTask extends AsyncTask<Void, Void, BluetoothSocket> {
        private BluetoothDevice device;

        /**
         *
         * @param device Um dispositivo de bluetooth recebido a partir da lista de dispositivos pareados
         *
         */
        public ConnectionTask(BluetoothDevice device) {
            this.device = device;
        }

        protected BluetoothSocket doInBackground(Void... metodo) {
            return connect();
        }

        /**
         * Tanta se conectar ao dispositivo bluetooth.
         * @return A conexao socket bluetooth. null em caso de falha
         */
        private BluetoothSocket connect() {
            BluetoothSocket socket = null;
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                socket.connect();
            } catch (IOException e) {
                Log.e("Bluetooth",e.getMessage());
            }
            return socket;
        }

        private void fechaDialogo() {
            if (dialogWaitConnection.isShowing()) {
                dialogWaitConnection.dismiss();
            }
        }

        /**
         * Antes da tarefa iniciar, a janela de dialogo e' exibida
         */
        protected void onPreExecute() {
            super.onPreExecute();
            if (!dialogWaitConnection.isShowing()) {
                context.runOnUiThread(new Thread() {
                    @Override
                    public void run() {
                        dialogWaitConnection.show();
                    }
                });
            }
        }

        /**
         * Quanda a tarefa terminar, o metodo testa se a conexao socket foi aberta,
         * obtem os fluxos de transmissoes, fecha o dialogo e notifica aos observadores que
         * a conexao bluetooth foi aberta.
         * @param result a conexao socket blutooth
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        protected void onPostExecute(BluetoothSocket result) {
            if (result != null && result.isConnected()) {
                socket = result;

                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                writerThread = new WriterBluetooth();
                new ReaderBluetoothThread().start();

                for (Observer o : observers) {
                    o.connectedCallback();
                }

                fechaDialogo();
            } else {
                waitAndReconnect();
            }
        }
    }

    /**
     * Classe privada para transmissao de dados
     */
    private class WriterBluetooth implements Runnable {
        @Override
        public void run() {
            if (data != null) {
                try {
                    outputStream.write(data);
                    errorCounter = 0;
                } catch (Exception e) {
                    errorCounter++;
                    if (errorCounter > 20) {
                        errorCounter = 0;
                        reconnect();
                    }
                }
            }
        }
    }

}