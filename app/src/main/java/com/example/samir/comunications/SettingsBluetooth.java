package com.example.samir.comunications;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.samir.activities.R;

/**
 * Created by Samir Sales on 08/07/16.
 */
public class SettingsBluetooth {

    public static ProgressDialog progressDialogWaitForConnection(Context context){
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setIcon(R.drawable.ic_launcher);
        progressDialog.setTitle("Aguarde!");
        progressDialog.setMessage("conectando...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.setOnCancelListener(null);
        return progressDialog;
    }

    public static void dialogBluetoothConnectionMode(Context context, final OnBluetoothConnectionMode onBluetoothConnectionMode){
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Conexão Bluetooth");
        builder.setMessage("Conectar-se como...");
        builder.setPositiveButton("cliente", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                onBluetoothConnectionMode.onClientClick(dialog, id);
            }
        });
        builder.setNegativeButton("servidor", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                onBluetoothConnectionMode.onServerClick(dialog, id);
            }
        });
        builder.show();
    }

    public interface OnBluetoothConnectionMode{
        void onClientClick(DialogInterface dialog, int id);
        void onServerClick(DialogInterface dialog, int id);
    }
}
