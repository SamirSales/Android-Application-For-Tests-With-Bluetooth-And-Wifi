package com.example.samir.devicescommunication.pingpong;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.example.samir.comunications.SettingsWifi;
import com.example.samir.devicescommunication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Samir Sales on 27/02/15.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PingPongWifiTest extends PingPongActivity {

    private MyClientTask myClientTask;
    private SocketServerThread socketServerThread;
    private String message = "";
    private ServerSocket serverSocket;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_ping_pong_test);

        startSettings();
        socketServerThread = null;
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

    /**
     * This class is used to the server wait for client connection.
     */
    private class SocketServerThread extends Thread {
        int connectionCounter = 0;
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

                        connectionCounter++;
                        message += "#"+ connectionCounter +" from "+socket.getInetAddress()+":"+socket.getPort() + "\n";

                        PingPongWifiTest.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setReceivedDataTextView(message);
                            }
                        });

                        SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket);
                        socketServerReplyThread.run();
                    }
                }
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    /**
     * This class is used to make the server interaction with its clients.
     */
    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;

        public SocketServerReplyThread(Socket socket) {
            hostThreadSocket = socket;
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

        private Socket socket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        private String ip;
        private int host;
        private PrintStream printStream;

        public MyClientTask(String ip, int host) throws IOException {
            this.ip = ip;
            this.host = host;
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

}
