package com.example.samir.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.samir.comunications.threads.BluetoothConnectedThread;
import com.example.samir.objects.ImageItem;

import java.util.ArrayList;

/**
 * Created by Samir Sales on 13/02/15.
 */
public class MyPagerAdapter extends PagerAdapter {

    private ArrayList<ImageItem> arrayICS;
    private int NumberOfPages;
    private Context context;
    private BluetoothConnectedThread bluetoothConnectedThreadServer;

    public MyPagerAdapter(Context context, ArrayList<ImageItem> arrayICS, BluetoothConnectedThread bluetoothConnectedThreadServer){
        this.arrayICS = arrayICS;
        NumberOfPages = arrayICS.size();
        this.context = context;
        this.bluetoothConnectedThreadServer = bluetoothConnectedThreadServer;
    }

    public void setBluetoothConnectedThreadServer(BluetoothConnectedThread bluetoothConnectedThreadServer){
        this.bluetoothConnectedThreadServer = bluetoothConnectedThreadServer;
    }

    public ArrayList<ImageItem> getArrayICS(){
        return arrayICS;
    }

    @Override
    public int getCount() {
        return NumberOfPages;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object){
        ImageItem imageItem = arrayICS.get(position);
        if(bluetoothConnectedThreadServer != null){
            imageItem.actionUpDateImageCS(position, bluetoothConnectedThreadServer);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        ImageView imageView = new ImageView(context);
        final ImageItem imageItem = arrayICS.get(position);
        imageView.setImageResource(imageItem.getImageResource());

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(imageParams);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layout.setBackgroundColor(imageItem.getColor());
        layout.setLayoutParams(layoutParams);
        layout.addView(imageView);

        layout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(context,imageItem.getTitle(), Toast.LENGTH_SHORT).show();
                if(bluetoothConnectedThreadServer != null){
                    imageItem.actionPressPageImageCS(bluetoothConnectedThreadServer);
                }
            }});

        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout)object);
    }

}