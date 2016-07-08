package com.example.samir.comunications.threads;

import com.example.samir.comunications.SettingsWifi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Samir Sales on 08/07/16.
 */
public class WifiSocketServerThread extends Thread {

    private int connectionCounter = 0;
    private String message;
    private boolean serverRunning = false;

    private ServerSocket serverSocket;

    private OnWifiServerThread onWifiServerThread;

    public WifiSocketServerThread(OnWifiServerThread onWifiServerThread){
        this.onWifiServerThread = onWifiServerThread;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(SettingsWifi.HOST);

            // Ready to connect...
            onWifiServerThread.onStartWaitConnection(serverSocket);

            if(!serverRunning){
                serverRunning = true;

                // Server waiting for connections...
                while (serverRunning) {
                    Socket socket = serverSocket.accept();
                    connectionCounter++;
                    message += "#"+ connectionCounter +" from "+socket.getInetAddress()+":"+socket.getPort() + "\n";

                    // New connection...
                    onWifiServerThread.onNewConnection(connectionCounter, message, socket);
                }
            }
        } catch (IOException e) {e.printStackTrace();}
    }

    public interface OnWifiServerThread{
        void onStartWaitConnection(ServerSocket serverSocket);
        void onNewConnection(int connectionCounter, String message, Socket socket);
    }
}