package com.example.samir.comunications.enums;

/**
 * Created by Samir Sales on 09/02/15.
 */
public enum EnumConnection {
    NULL(0),
    BLUETOOTH_CLIENT(1),
    WIFI(2),
    USB(3),
    BLUETOOTH_PING_TEST(4),
    BLUETOOTH_SERVER(5);

    private int valor;

    EnumConnection(int valor){
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }
}