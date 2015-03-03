package com.example.samir.constantes;

/**
 * Created by samir on 09/02/15.
 */
public enum EnumConexao {
    NULL(0),
    BLUETOOTH(1),
    WIFI(2),
    USB(3),
    BLUETOOTH_PING_TEST(4),;

    private int valor;

    EnumConexao(int valor){
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }
}