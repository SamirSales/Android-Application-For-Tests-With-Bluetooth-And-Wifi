package com.example.samir.comm;

/**
 * Created by samir on 09/02/15.
 */
public interface Observer {
    public void update(byte[] data);
    public void connectedCallback();

    public void connectedFault();
}

