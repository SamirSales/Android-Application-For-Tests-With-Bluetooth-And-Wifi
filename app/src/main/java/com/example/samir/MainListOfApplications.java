package com.example.samir;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.samir.devicescommunication.BluetoothChat;
import com.example.samir.devicescommunication.P2PWifiCommunication;
import com.example.samir.devicescommunication.PingPongBlueTest;
import com.example.samir.devicescommunication.PingPongWifiTest;
import com.example.samir.devicescommunication.R;
import com.example.samir.devicescommunication.TestImagens;

/**
 * Created by Samir Sales on 19/02/15.
 */
public class MainListOfApplications extends Activity implements AdapterView.OnItemClickListener{

    private ListView listView;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.main_list_of_applications);

        String[] arrayTestes = new String[] {
                "Chat Bluetooth",
                "Imagens + Bluetooth",
                "Chat Wifi P2P",
                "Teste Ping-Pong Wifi P2P",
                "Teste Ping-Pong Bluetooth"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, arrayTestes);

        listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        switch (position){
            case 0:
                //Chat Bluetooth
                Intent intent0 = new Intent(this, BluetoothChat.class);
                startActivity(intent0);
                break;
            case 1:
                Intent intent1 = new Intent(this, TestImagens.class);
                startActivity(intent1);
                break;
            case 2:
                //Testes P2P Wifi
                Intent intent2 = new Intent(this, P2PWifiCommunication.class);
                startActivity(intent2);
                break;
            case 3:
                //Testes P2P Wifi
                Intent intent3 = new Intent(this, PingPongWifiTest.class);
                startActivity(intent3);
                break;
            case 4:
                //Testes P2P Bluetooth
                Intent intent4 = new Intent(this, PingPongBlueTest.class);
                startActivity(intent4);
                break;
            default:
                break;
        }
    }
}
