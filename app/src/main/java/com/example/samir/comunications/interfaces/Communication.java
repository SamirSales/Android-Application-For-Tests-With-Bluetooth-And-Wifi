package com.example.samir.comunications.interfaces;

/**
 * Created by Samir Sales on 09/02/15.
 */
public interface Communication {

    void open();
    void close();
    void reconnect();
    void send(byte data[]);
    boolean isConnected();
    void addObserver(Observer o);
    void remObserver(Observer o);
    void notifyObservers(byte[] data);
}