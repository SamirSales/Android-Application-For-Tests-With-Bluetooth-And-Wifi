package com.example.samir.adapter;

import com.example.samir.comunications.EnumConexao;

/**
 * Created by samir on 09/02/15.
 */
public class Item {
    private EnumConexao conexao;
    private String modo;
    private int valor;


    public Item(EnumConexao conecao, String modo, int valor) {
        this.conexao = conecao;
        this.modo = modo;
        this.valor = valor;
    }


    public String getModo() {
        return modo;
    }

    public EnumConexao getConexao() {
        return conexao;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }


    public int getValor() {
        return valor;
    }


    public void setValor(int valor) {
        this.valor = valor;
    }

}
