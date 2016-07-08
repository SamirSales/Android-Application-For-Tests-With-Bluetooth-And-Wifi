package com.example.samir.comunications;

/**
 * Created by Samir Sales on 09/02/15.
 */

import android.app.Activity;

import com.example.samir.comunications.bluetooth.BluetoothClient;
import com.example.samir.comunications.bluetooth.BluetoothServer;
import com.example.samir.comunications.enums.EnumConnection;
import com.example.samir.comunications.interfaces.Communication;

public class CommunicationFactory {
    private EnumConnection mode;
    private Communication communication;

    public CommunicationFactory(Activity parent, EnumConnection mode) {
        this.mode = mode;
        communication = null;
        if (mode == EnumConnection.USB) {
            //communication = new USB(parent);
        } else if (mode == EnumConnection.WIFI) {
            //communication = new WiFi(parent);
        } else if (mode == EnumConnection.BLUETOOTH_CLIENT) {
            communication = new BluetoothClient(parent);
        } else if (mode == EnumConnection.BLUETOOTH_SERVER) {
            communication = new BluetoothServer(parent);
        }
    }

    public Communication getCommunication() {
        return communication;
    }
}
