package com.example.samir.comunications.interfaces;

/**
 * Created by Samir Sales on 09/02/15.
 */
public interface Observer {
    void update(byte[] data);
    void connectedCallback();
    void connectedFault();
}

