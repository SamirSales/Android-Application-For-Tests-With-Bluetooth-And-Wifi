package com.example.samir.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.samir.comunicacaodedispositivos.ConnectedThread;
import com.example.samir.objetos.ImagemComStr;

import java.util.ArrayList;

/**
 * Created by samir on 13/02/15.
 */
public class MyPagerAdapter extends PagerAdapter {

    private ArrayList<ImagemComStr> arrayICS;
    private int NumberOfPages;
    private Context context;
    private ConnectedThread connectedThreadServer;

    public MyPagerAdapter(Context context, ArrayList<ImagemComStr> arrayICS, ConnectedThread connectedThreadServer){
        this.arrayICS = arrayICS;
        NumberOfPages = arrayICS.size();
        this.context = context;
        this.connectedThreadServer = connectedThreadServer;
    }

    public void setConnectedThreadServer(ConnectedThread connectedThreadServer){
        this.connectedThreadServer = connectedThreadServer;
    }

    public ArrayList<ImagemComStr> getArrayICS(){
        return arrayICS;
    }

    @Override
    public int getCount() {
        return NumberOfPages;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object){
        ImagemComStr im = arrayICS.get(position);
        if(connectedThreadServer != null){
            im.actionUpDateImageCS(position,connectedThreadServer);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        ImageView imageView = new ImageView(context);

        final ImagemComStr ics = arrayICS.get(position);

        imageView.setImageResource(ics.getImageResourse());


        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(imageParams);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layout.setBackgroundColor(ics.getColor());
        layout.setLayoutParams(layoutParams);
        layout.addView(imageView);

        layout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Toast.makeText(context,
                        ics.getTitulo(), Toast.LENGTH_SHORT).show();
                if(connectedThreadServer != null){
                    ics.actionPressPageImageCS(connectedThreadServer);
                }
            }});

        Log.i("TestImagens", "COMANDO " + position);

        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout)object);
    }

}