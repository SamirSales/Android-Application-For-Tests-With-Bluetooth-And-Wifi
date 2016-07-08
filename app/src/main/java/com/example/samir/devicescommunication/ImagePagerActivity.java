package com.example.samir.devicescommunication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samir.adapter.MyPagerAdapter;
import com.example.samir.comunications.enums.EnumConnection;
import com.example.samir.comunications.interfaces.Communication;
import com.example.samir.comunications.CommunicationFactory;
import com.example.samir.comunications.interfaces.Observer;
import com.example.samir.comunications.threads.BluetoothConnectedThread;
import com.example.samir.objects.ImageItem;
import com.example.samir.utils.DepthPageTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Samir Sales on 10/02/15.
 */

public class ImagePagerActivity extends Activity implements Observer {

    private String TAG = "ImagePagerActivity";

    private android.support.v4.view.ViewPager pager;
    private Button serverBtn;
    private Button clientBtn;
    private TextView connectionTextView;

    private static BluetoothConnectedThread bluetoothConnectedThreadServer;
    private static boolean connectionStarted;
    private Communication communication = null;

    private AcceptThread aTh;

    private MyPagerAdapter myPagerAdapter;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_image_test);

        serverBtn = (Button)findViewById(R.id.serverBtn);
        clientBtn = (Button)findViewById(R.id.clientBtn);
        connectionTextView = (TextView)findViewById(R.id.textConectado);
        pager = (ViewPager)findViewById(R.id.pager);
        pager.setPageTransformer(true, new DepthPageTransformer());

        bluetoothConnectedThreadServer = null;

        ArrayList<ImageItem> arrayList = new ArrayList<ImageItem>();
        arrayList.add(new ImageItem(R.drawable.image1,"Lobo"));
        arrayList.add(new ImageItem(R.drawable.image2,"Arara"));
        arrayList.add(new ImageItem(R.drawable.image3,"Carro"));
        arrayList.add(new ImageItem(R.drawable.image4,"Leão"));

        myPagerAdapter = new MyPagerAdapter(this,arrayList, bluetoothConnectedThreadServer);
        pager.setAdapter(myPagerAdapter);
    }

    public void buttonActionServer(View view){
        aTh  = new AcceptThread();
        aTh.start();
    }

    public void buttonActionClient(View view){
        initConnection();
    }

    public void initConnection() {
        if(!connectionStarted){
            initCommunication(EnumConnection.BLUETOOTH_CLIENT);
            connectionStarted = true;
        }
    }

    public void initCommunication(EnumConnection con) {
        communication = new CommunicationFactory(this, con).getCommunication();
        communication.addObserver(ImagePagerActivity.this);
        communication.open();
    }

    @Override
    public void update(byte[] data) {
        byte type = data[0];

        switch (type){
            case ImageItem.UPDATE_PAGE:
                final byte pagina = data[1];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pager.setCurrentItem(pagina);
                    }
                });

                break;
            case ImageItem.PRESS_PAGE:
                final int position = pager.getCurrentItem();
                final ImageItem imageItem = myPagerAdapter.getArrayICS().get(position);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ImagePagerActivity.this,imageItem.getTitle(),Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            default:
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
                            connectionTextView.setVisibility(View.VISIBLE);
                            connectionTextView.setText("CLIENTE");
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
                        connectionTextView.setVisibility(View.INVISIBLE);
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
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionTextView.setVisibility(View.VISIBLE);
                            connectionTextView.setText("SERVIDOR");
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
            bluetoothConnectedThreadServer =  new BluetoothConnectedThread(socket, arrayMessage);
            bluetoothConnectedThreadServer.start();
            myPagerAdapter.setBluetoothConnectedThreadServer(bluetoothConnectedThreadServer);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        for(int i=0; i<arrayMessage.size();i++){
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
