package com.example.samir.comunicacaodedispositivos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

/**
 * Created by samir on 24/06/15.
 */
public class BluetoothLowEnergy extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendMessageAction(View view) {
        //TODO
    }

    public void conectarAction(View view){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conexão Bluetooth L.E.");
        builder.setMessage("Conectar-se como...");
        builder.setPositiveButton("cliente", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //TODO
                bluetoothLESettingAndChecking();
            }
        });
        builder.setNegativeButton("servidor", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //TODO
            }
        });
        builder.show();
    }

    private void bluetoothLESettingAndChecking(){

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "This device does not suport the BLE", Toast.LENGTH_SHORT).show();
            return;
        }
        
    }
}
