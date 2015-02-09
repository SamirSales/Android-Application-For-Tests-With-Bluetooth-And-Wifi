package com.example.samir.comm;

/**
 * Created by samir on 09/02/15.
 */
public interface Communication {

    public void open();
    public void close();
    public void reconnect();
    public void send(byte data[]);
    public boolean isConnected();
    public void addObserver(Observer o);
    public void remObserver(Observer o);
    public void notifyObservers(byte[] data);
}