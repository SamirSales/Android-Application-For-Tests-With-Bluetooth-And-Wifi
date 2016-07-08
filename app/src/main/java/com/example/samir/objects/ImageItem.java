package com.example.samir.objects;

import com.example.samir.comunications.threads.BluetoothConnectedThread;

/**
 * Created by Samir Sales on 13/02/15.
 */
public class ImageItem {

    public static final byte UPDATE_PAGE = 1;
    public static final byte PRESS_PAGE = 2;

    private int imageResource;
    private String title;
    private int color;

    private boolean sendingMessage;

    public ImageItem(int imageResource, String title){
        this.imageResource = imageResource;
        this.title = title;
        this.color = 0xFF000000;
        this.sendingMessage = false;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResource() {
        return imageResource;
    }

    public int getColor() {
        return color;
    }

    public void actionUpDateImageCS(int position, BluetoothConnectedThread bluetoothConnectedThreadServer){
        if(!sendingMessage){
            sendingMessage = true;

            byte[] bytes = {UPDATE_PAGE,(byte)position};
            bluetoothConnectedThreadServer.write(bytes);

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

    public void actionPressPageImageCS(BluetoothConnectedThread bluetoothConnectedThreadServer){
        byte[] bytes = {PRESS_PAGE,0};
        bluetoothConnectedThreadServer.write(bytes);
    }
}
