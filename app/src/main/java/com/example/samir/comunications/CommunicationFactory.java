package com.example.samir.comunications;

/**
 * Created by samir on 09/02/15.
 */
import android.app.Activity;

import com.example.samir.comunications.BluetoothClient;
import com.example.samir.comunications.Communication;
import com.example.samir.comunications.EnumConexao;
import com.example.samir.testOfComunication.BluetoothPingTest;

public class CommunicationFactory {
    private EnumConexao mode;
    private Communication communication;

    public CommunicationFactory(Activity parent, EnumConexao mode) {
        this.mode = mode;
        communication = null;
        if (mode == EnumConexao.USB) {
            //communication = new USB(parent);
        } else if (mode == EnumConexao.WIFI) {
            //communication = new WiFi(parent);
        } else if (mode == EnumConexao.BLUETOOTH) {
            communication = new BluetoothClient(parent);
        } else if (mode == EnumConexao.BLUETOOTH_PING_TEST) {
            communication = new BluetoothPingTest(parent);
        }
    }

    public Communication getCommunication() {
        return communication;
    }
}
