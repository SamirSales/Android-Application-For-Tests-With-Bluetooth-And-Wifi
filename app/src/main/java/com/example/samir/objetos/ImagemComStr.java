package com.example.samir.objetos;

import android.util.Log;

import com.example.samir.comunicacaodedispositivos.ConnectedThread;

/**
 * Created by samir on 13/02/15.
 */
public class ImagemComStr {

    public static final byte UPDATE_PAGE = 1;
    public static final byte PRESS_PAGE = 2;

    private int imageResourse;
    private String titulo;
    private int color;

    private boolean sendingMessage;

    public ImagemComStr(int imageResourse, String titulo){
        this.imageResourse = imageResourse;
        this.titulo = titulo;
        this.color = 0xFF000000;
        this.sendingMessage = false;
    }

    public ImagemComStr(int imageResourse, String titulo, int hColor){
        this.imageResourse = imageResourse;
        this.titulo = titulo;
        this.color = hColor;
        this.sendingMessage = false;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getImageResourse() {
        return imageResourse;
    }

    public void setImageResourse(int imageResourse) {
        this.imageResourse = imageResourse;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void actionUpDateImageCS(int position, ConnectedThread connectedThreadServer){
        if(!sendingMessage){
            sendingMessage = true;

            byte[] bytes = {UPDATE_PAGE,(byte)position};
            connectedThreadServer.write(bytes);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendingMessage = false;
                }
            }).start();
        }
    }

    public void actionPressPageImageCS(ConnectedThread connectedThreadServer){
        byte[] bytes = {PRESS_PAGE,0};
        connectedThreadServer.write(bytes);
    }
}
