package com.example.samir.devicescommunication.pingpong;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.samir.comunications.SettingsWifi;
import com.example.samir.devicescommunication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by Samir Sales on 27/02/15.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PingPongWifiTest extends PingPongActivity {

    private WifiP2pManager mManager;

    private MyClientTask myClientTask;
    private ArrayList<String> arrayMessageToRead;

    private SocketServerThread socketServerThread;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_ping_pong_test);

        resetBatteryStatusCounters();
        keepScreenOn();
        setRegisterReceiverBatteryChanged();
        resetCounters();
        setStartTime();
        setArrayMessageToSend(new ArrayList<String>());

        arrayMessageToRead = new ArrayList<>();
        socketServerThread = null;

        setViews();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
    }

    public void searchDevicesBtn(View view){
        //doing nothing...
    }

    public void clientActionBtn(View view){
        try {
            myClientTask = new MyClientTask(SettingsWifi.IP_SERVER,SettingsWifi.HOST);
            myClientTask.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverActionBtn(View view){
        socketServerThread = new SocketServerThread();
        socketServerThread.start();
    }

    @Override
    public void updateInfo(){
        String endTime = getCurrentTime();
        setUserAndEndTime((myClientTask != null), endTime);
    }

    private String message = "";
    private ServerSocket serverSocket;

    /**
     * This class is used to the server wait for client connection.
     */
    private class SocketServerThread extends Thread {
        int count = 0;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        boolean serverRunning = false;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SettingsWifi.HOST);
                PingPongWifiTest.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setReceivedDataTextView("I'm waiting here: " + serverSocket.getLocalPort());
                        updateInfo();
                    }
                });

                if(!serverRunning){
                    serverRunning = true;

                    // Server waiting for connections...
                    while (serverRunning) {
                        Socket socket = serverSocket.accept();
                        outputStream = socket.getOutputStream();
                        inputStream = socket.getInputStream();

                        count++;
                        message += "#"+count+" from "+socket.getInetAddress()+":"+socket.getPort() + "\n";

                        PingPongWifiTest.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setReceivedDataTextView(message);
                            }
                        });

                        SocketServerReplyThread socketServerReplyThread =
                                new SocketServerReplyThread(socket, count);
                        socketServerReplyThread.run();
                    }
                }

            } catch (IOException e) {e.printStackTrace();}
        }

        public void write(byte[] bytes){
            if(outputStream != null){
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void printMessages(PrintStream printStream){
        if(getArrayMessageToSend().size()>0){
            for(String str : getArrayMessageToSend()){
                printStream.print(str);
            }
            setArrayMessageToSend(new ArrayList<String>());
        }
    }

    private void readMessages(InputStream inputStream){
        byte[] buffer = new byte[1024];  // buffer store for the stream

        while (true){
            try {
                // Read from the InputStream
                inputStream.read(buffer);
                // Send the obtained bytes to the UI activity
                String str = new String (buffer);
                str = str.trim();
                buffer = new byte[1024];
                arrayMessageToRead.add(str);

                try {
                    Thread.sleep(DELAY_TO_SEND);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(arrayMessageToRead.size()>0){
                    for(String str2 : arrayMessageToRead){
                        updateTextView("Outro usuario: "+str2);
                        sendSayingTheNextNumber(str2);
                    }
                    arrayMessageToRead = new ArrayList<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class is used to make the server interaction with its clients.
     */
    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int counter;

        SocketServerReplyThread(Socket socket, int counter) {
            hostThreadSocket = socket;
            this.counter = counter;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            InputStream inputStream;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                inputStream = hostThreadSocket.getInputStream();
                PrintStream printStream = new PrintStream(outputStream);
                updateInfo();

                final PrintStream printStream2 = printStream;
                final InputStream inputStream2 = inputStream;

                threadWriteMessages(printStream2);
                threadReadMessages(inputStream2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setReceivedDataTextView(message);
                }
            });
        }
    }

    /**
     * This class is used to make the client mode connection.
     */
    public class MyClientTask extends AsyncTask {

        Socket socket;
        InputStream mmInStream;
        OutputStream mmOutStream;

        String ip;
        int host;

        boolean myClientIsRunning;
        PrintStream printStream;

        MyClientTask(String ip, int host) throws IOException {
            this.ip = ip;
            this.host = host;
            myClientIsRunning = false;
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        @Override
        protected Object doInBackground(Object[] params) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                socket = new Socket(ip, host);
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            if(socketServerThread == null){
                updateInfo();
            }
            printStream = new PrintStream(mmOutStream);
            printStream.print("0");

            threadWriteMessages(printStream);
            threadReadMessages(mmInStream);

            return null;
        }
    }

    private void threadWriteMessages(final PrintStream printStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    printMessages(printStream);
                }
            }
        }).start();
    }

    private void threadReadMessages(final InputStream inputStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                readMessages(inputStream);
            }
        }).start();
    }

}
