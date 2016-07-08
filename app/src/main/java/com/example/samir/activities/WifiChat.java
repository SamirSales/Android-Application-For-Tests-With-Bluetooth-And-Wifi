package com.example.samir.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.samir.comunications.SettingsWifi;
import com.example.samir.comunications.threads.WifiSocketServerThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by Samir Sales on 19/02/15.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class WifiChat extends Activity{

    private static final String TAG = "WifiChat";

    private EditText editText;
    private TextView receivedDataTextView;
    private TextView textStatus;
    private Button server_btn;
    private Button client_btn;

    private MyClientTask myClientTask;
    private ArrayList<String> arrayMessageToRead;
    private ArrayList<String> arrayMessageToSend;

//    private SocketServerThread socketServerThread;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.comm_wifi_p2p);

        arrayMessageToRead = new ArrayList<>();
        arrayMessageToSend = new ArrayList<>();
//        socketServerThread = null;
        myClientTask = null;

        editText = (EditText)findViewById(R.id.editText);
        receivedDataTextView = (TextView)findViewById(R.id.textRecebido);
        textStatus = (TextView)findViewById(R.id.textStatus);
        server_btn = (Button)findViewById(R.id.server_btn);
        client_btn = (Button)findViewById(R.id.client_btn);
    }

    public void clientActionBtn(View view){
        try {
            myClientTask = new MyClientTask(SettingsWifi.IP_SERVER, SettingsWifi.HOST);
            myClientTask.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private WifiSocketServerThread wifiSocketServerThread;

    public void serverActionBtn(View view){
        wifiSocketServerThread = new WifiSocketServerThread(new WifiSocketServerThread.OnWifiServerThread() {
            @Override
            public void onStartWaitConnection(final ServerSocket serverSocket) {
                WifiChat.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        receivedDataTextView.setText("I'm waiting here: " + serverSocket.getLocalPort());
                        updateStatusOfConnection();
                    }
                });
            }
            @Override
            public void onNewConnection(int connectionCounter, final String message, Socket socket) {
                WifiChat.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        receivedDataTextView.setText(message);
                    }
                });
                SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket, connectionCounter);
                socketServerReplyThread.run();
            }
        });
        wifiSocketServerThread.start();
    }

    public void updateStatusOfConnection(){
        connectionNotification("server:" + SettingsWifi.IP_SERVER + " host:" + SettingsWifi.HOST);
    }

    public void connectionNotification(final String mgs){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText(mgs);
                client_btn.setVisibility(View.INVISIBLE);
                server_btn.setVisibility(View.INVISIBLE);
                client_btn.setEnabled(false);
                server_btn.setEnabled(false);
            }
        });
    }

    public void sendMessageAction(View view){
        String msg = editText.getText().toString();
        msg = msg.trim();
        if(!msg.equals("")){
            arrayMessageToSend.add(msg);
            editText.setText("");
        }
    }

    public void updateTextView(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                receivedDataTextView.setText(receivedDataTextView.getText().toString() + text + "\n");
            }
        });
    }

    private String message = "";

    private void threadReader(final InputStream inputStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];

                while (true){
                    try {
                        inputStream.read(buffer);
                        String str = new String(buffer).trim();
                        arrayMessageToRead.add(str);
                        if(arrayMessageToRead.size()>0){
                            for(String str2 : arrayMessageToRead){
                                updateTextView("Outro usuario: "+str2);
                            }
                            arrayMessageToRead = new ArrayList<>();
                            buffer = new byte[1024];
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void threadWriter(final PrintStream printStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(arrayMessageToSend.size()>0){
                        for(String str : arrayMessageToSend){
                            printStream.print(str);
                            updateTextView("Eu: "+str);
                        }
                        arrayMessageToSend = new ArrayList<String>();
                    }
                }
            }
        }).start();
    }

    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        private int connectionCounter = 0;
        private OutputStream outputStream;
        private InputStream inputStream;
        private PrintStream printStream;

        public SocketServerReplyThread(Socket socket, int connectionCounter) {
            hostThreadSocket = socket;
            this.connectionCounter = connectionCounter;
        }

        @Override
        public void run() {

            String msgReply = "Hello from Android, you are #" + connectionCounter;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                inputStream = hostThreadSocket.getInputStream();
                printStream = new PrintStream(outputStream);
//                printStream.print(msgReply);
                arrayMessageToSend.add(msgReply);
                updateStatusOfConnection();

                message += "replayed: " + msgReply + "\n";

                WifiChat.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        receivedDataTextView.setText(message);
                    }
                });

                threadWriter(printStream);
                threadReader(inputStream);

            } catch (IOException e) {
                e.printStackTrace();
                message += "Something wrong! " + e.toString() + "\n";
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    receivedDataTextView.setText(message);
                }
            });
        }

    }

    public class MyClientTask extends AsyncTask {

        private Socket socket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        private String ip;
        private int host;
        private PrintStream printStream;

        private MyClientTask(String ip, int host) throws IOException {
            this.ip = ip;
            this.host = host;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                socket = new Socket(ip, host);//TODO NO HOST FOUND
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            if(wifiSocketServerThread == null){
                connectionNotification("client host:" + SettingsWifi.HOST);
            }
            printStream = new PrintStream(mmOutStream);
            threadWriter(printStream);
            threadReader(mmInStream);

            return null;
        }
    }

}
