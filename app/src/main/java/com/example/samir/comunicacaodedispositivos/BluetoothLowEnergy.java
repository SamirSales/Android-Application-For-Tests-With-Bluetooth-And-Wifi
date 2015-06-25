package com.example.samir.comunicacaodedispositivos;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by samir on 24/06/15.
 */
public class BluetoothLowEnergy extends Activity {

    private final String TAG = "BluetoothLowEnergy";

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
                inicializeBluetoothAdaper();
                scanLeDevice(true);
            }
        });
        builder.setNegativeButton("servidor", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if(isThereSuportToBLE()){
                    // TODO
                    //CRIAR SERVICO
                    //ACEITAR CONXAO
                }
            }
        });
        builder.show();
    }

    private BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_ENABLE_BT = 3456;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void inicializeBluetoothAdaper(){
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean isThereSuportToBLE(){

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "This device does not suport the BLE", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean mScanning;
    private Handler mHandler;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler = new Handler();
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.i(TAG,"onLeScan implementation");
                }
            };

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        showBluetoothDialog();
    }


    public void showBluetoothDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.paired_devices_list);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle("Devices");

        Set<BluetoothDevice> pairedDevicesSet = mBluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1);

        Button btnBack = (Button) dialog.findViewById(R.id.btnVoltarBluetooth);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        if (pairedDevicesSet.size() > 0) {
            for (BluetoothDevice device : pairedDevicesSet) {
                pairedDevices.add(device);
                pairedDevicesAdapter.add(device.getName());
            }
        } else {
            pairedDevicesAdapter.add("Não há nenhum dispositivo pareado.");
        }

        if (!pairedDevices.isEmpty()) {
            ListView pairedDevicesLV = (ListView) dialog
                    .findViewById(R.id.pairedDevicesListView);
            pairedDevicesLV.setAdapter(pairedDevicesAdapter);
            pairedDevicesLV.setBackgroundColor(Color.LTGRAY);
            pairedDevicesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BluetoothDevice device = pairedDevices.get(position);
                    //new ConnectionTask(device).execute();
                    Log.i(TAG,device.getName()+" selecionado");
                    dialog.dismiss();
                }
            });
        }
        dialog.show();
    }


    
}
