package com.example.samir.devicescommunication.pingpong;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.samir.devicescommunication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Samir Sales on 07/07/16.
 */
public abstract class PingPongActivity extends Activity{

    private final String TAG = "PingPongActivity";

    private static String startTime;
    private long counter;
    private long counterOfCounter;
    private final int DELAY_TO_SEND = 300;
    private final int MAX_LINES_NUMBER = 10;
    private int batteryInitPercent;
    private int batteryPercent;
    private String batteryChargingStr;
    private Handler handler = new Handler();

    private int lines = 0;

    private ArrayList<String> arrayMessageToSend;
    private ArrayList<String> arrayMessageToRead;

    private Button serverBtn;
    private Button clientBtn;
    private Button searchBtn;
    private TextView textStatus;
    private TextView receivedDataTextView;

    private PowerManager.WakeLock mWakeLock;

    protected void startSettings(){
        resetBatteryStatusCounters();
        keepScreenOn();
        setRegisterReceiverBatteryChanged();
        resetCounters();
        setStartTime();
        setViews();
        setArrayMessageToSend(new ArrayList<String>());
        setArrayMessageToRead(new ArrayList<String>());
    }

    protected void setViews(){
        receivedDataTextView = (TextView)findViewById(R.id.textRecebido);
        textStatus = (TextView)findViewById(R.id.textStatus);
        searchBtn = (Button)findViewById(R.id.search_btn);
        serverBtn = (Button)findViewById(R.id.server_btn);
        clientBtn = (Button)findViewById(R.id.client_btn);
    }

    protected BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            Log.i(TAG, "level: " + level + "; scale: " + scale);
            int percent = (level*100)/scale;

            if(batteryInitPercent==0){
                batteryInitPercent = percent;
            }
            batteryPercent = percent;

            batteryChargingStr = String.valueOf(percent) + "%";
            handler.post( new Runnable() {
                public void run() {
                    Log.i("bateria","bateria: "+ batteryChargingStr);
                }
            });
        }
    };

    protected String getCurrentTime(){
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        return hour+":"+minutes+":"+seconds;
    }

    protected void setStartTime(){
        startTime = getCurrentTime();
    }

    protected void setRegisterReceiverBatteryChanged(){
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    protected void resetBatteryStatusCounters(){
        batteryInitPercent = 0;
        batteryPercent = 0;
    }

    protected void resetCounters(){
        counter = 0;
        counterOfCounter = 0;
    }

    protected void keepScreenOn(){
        // Keep screen on
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }

    protected String getStringInfoFormatted(String user, String endTime){
        return  "I'm "+user+" \n" +
                "horario  inicial: "+ startTime +"\n"+
                "horario    final: "+endTime+"\n"+
                "loop(s) number = "+counterOfCounter+"\n" +
                "last connectionCounter = "+counter+"\n"+
                "bateria inicial = "+batteryInitPercent+"%\n"+
                "bateria agora   = "+batteryPercent+"%";
    }

    protected void setUserAndEndTime(boolean client, String endTime){
        if(client){
            String str = getStringInfoFormatted("CLIENT", endTime);
            setTextStatus(str);
        }else{
            String str = getStringInfoFormatted("SERVER", endTime);
            setTextStatus(str);
        }
    }

    protected void setTextStatusDisconnected(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText("desconectado");
                textStatus.setTextColor(Color.GRAY);

            }
        });
    }

    protected void setTextStatus(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText(str);
                if (clientBtn != null) {
                    clientBtn.setVisibility(View.INVISIBLE);
                    clientBtn.setEnabled(false);
                }
                if (searchBtn != null) {
                    searchBtn.setVisibility(View.INVISIBLE);
                    searchBtn.setEnabled(false);
                }
                if (serverBtn != null) {
                    serverBtn.setVisibility(View.INVISIBLE);
                    serverBtn.setEnabled(false);
                }
            }
        });
    }

    protected void sendSayingTheNextNumber(String numberReceived){
        try {
            long number = Long.parseLong(numberReceived);
            String msg = "0";

            if(number < Long.MAX_VALUE){
                setCounter(number + 1);
                msg = ""+getCounter();
            }else{
                incrementCounterOfCounter();
                setCounter(0);
            }
            updateTextView("Eu: "+ msg);
            arrayMessageToSend.add(msg);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        updateInfo();
    }

    protected void readMessages(InputStream inputStream){
        byte[] buffer = new byte[1024];  // buffer store for the stream

        while (true){
            try {
                // Read from the InputStream
                inputStream.read(buffer);
                // Send the obtained bytes to the UI activity
                String str = new String (buffer);
                str = str.trim();
                arrayMessageToRead.add(str);

                try {
                    Thread.sleep(DELAY_TO_SEND);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(arrayMessageToRead.size()>0){
                    for(String str2 : arrayMessageToRead){
                        updateTextView("Outro usuario: "+str2);
                        sendSayingTheNextNumber(str2);
                    }
                    arrayMessageToRead = new ArrayList<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void printMessages(PrintStream printStream){
        if(getArrayMessageToSend().size()>0){
            for(String str : getArrayMessageToSend()){
                printStream.print(str);
            }
            setArrayMessageToSend(new ArrayList<String>());
        }
    }

    protected void threadWriteMessages(final PrintStream printStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    printMessages(printStream);
                }
            }
        }).start();
    }

    protected void threadReadMessages(final InputStream inputStream){
        new Thread(new Runnable() {
            @Override
            public void run() {
                readMessages(inputStream);
            }
        }).start();
    }

    public void updateTextView(final String text){
        if(lines >= MAX_LINES_NUMBER){
            lines = 1;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setReceivedDataTextView("");
                }
            });
        }else{
            lines++;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setReceivedDataTextView(getReceivedDataText() + text + "\n");
            }
        });
    }

    /**
     * This method should be override
     */
    protected void updateInfo(){}

    protected void incrementCounterOfCounter(){
        counterOfCounter++;
    }

    public void setCounter(long counter){
        this.counter = counter;
    }

    public long getCounter(){
        return counter;
    }

    public void setReceivedDataTextView(String text){
        receivedDataTextView.setText(text);
    }

    public String getReceivedDataText(){
        return receivedDataTextView.getText().toString();
    }

    public void setArrayMessageToSend(ArrayList<String> arrayMessageToSend) {
        this.arrayMessageToSend = arrayMessageToSend;
    }

    public void setArrayMessageToRead(ArrayList<String> arrayMessageToRead){
        this.arrayMessageToRead = arrayMessageToRead;
    }

    public ArrayList<String> getArrayMessageToSend(){
        return arrayMessageToSend;
    }
}
