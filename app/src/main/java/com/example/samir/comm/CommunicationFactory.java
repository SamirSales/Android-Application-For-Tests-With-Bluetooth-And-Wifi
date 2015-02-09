package com.example.samir.comm;

/**
 * Created by samir on 09/02/15.
 */
import android.app.Activity;

import com.example.samir.constantes.EnumConexao;

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
            communication = new Bluetooth(parent);
        }
    }

    public Communication getCommunication() {
        return communication;
    }
}
