package com.example.samir.comunications;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Created by Samir Sales on 08/07/16.
 * TODO: this class will be used for the improvement of the code reuse concept.
 */
public class StreamManager {

    private ArrayList<String> arrayMessageToRead;
    private ArrayList<String> arrayMessageToSend;

    private Thread reader;
    private Thread writer;

    private OnStreamManager onStreamManager;

    public StreamManager(final InputStream inputStream, final PrintStream printStream, final OnStreamManager onStreamManager){
        arrayMessageToRead = new ArrayList<>();
        arrayMessageToSend = new ArrayList<>();
        this.onStreamManager = onStreamManager;
        setReader(inputStream);
        setWriter(printStream);
    }

    private void setReader(final InputStream inputStream){
        reader = new Thread(new Runnable() {
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
                                onStreamManager.onRead(str2);
                            }
                            arrayMessageToRead = new ArrayList<>();
                            buffer = new byte[1024];
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setWriter(final PrintStream printStream){
        writer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(arrayMessageToSend.size()>0){
                        for(String str : arrayMessageToSend){
                            printStream.print(str);
                            onStreamManager.onWrite(str);
                        }
                        arrayMessageToSend = new ArrayList<>();
                    }
                }
            }
        });
    }

    public void send(String message){
        arrayMessageToSend.add(message);
    }

    public void startThreads(){
        reader.start();
        writer.start();
    }

    public interface OnStreamManager{
        void onRead(String received);
        void onWrite(String message);
    }
}
