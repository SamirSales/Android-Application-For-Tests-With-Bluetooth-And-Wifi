package com.example.samir.comunicacaodedispositivos;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by samir on 10/02/15.
 */

public class TestImagens extends Activity {

    private android.support.v4.view.ViewPager pager;
    private PagerAdapter mPagerAdapter;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_image_test);

        pager = (ViewPager)findViewById(R.id.pager);
        pager.setPageTransformer(true, new DepthPageTransformer());

        ArrayList<ImagemComStr> arrayList = new ArrayList<ImagemComStr>();
        arrayList.add(new ImagemComStr(R.drawable.image1,"Lobo"));
        arrayList.add(new ImagemComStr(R.drawable.image2,"Arara"));
        arrayList.add(new ImagemComStr(R.drawable.image3,"Carro"));
        arrayList.add(new ImagemComStr(R.drawable.image4,"Leão"));

        MyPagerAdapter myPagerAdapter = new MyPagerAdapter(this,arrayList);
        pager.setAdapter(myPagerAdapter);
    }

    private class ImagemComStr{
        private int imageResourse;
        private String titulo;
        private int color;

        public ImagemComStr(int imageResourse, String titulo){
            this.imageResourse = imageResourse;
            this.titulo = titulo;
            this.color = 0xFF000000;
        }

        public ImagemComStr(int imageResourse, String titulo, int hColor){
            this.imageResourse = imageResourse;
            this.titulo = titulo;
            this.color = hColor;
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
    }

    private class MyPagerAdapter extends PagerAdapter{

        private ArrayList<ImagemComStr> arrayICS;
        private int NumberOfPages;
        private Context context;

        public MyPagerAdapter(Context context, ArrayList<ImagemComStr> arrayICS){
            this.arrayICS = arrayICS;
            NumberOfPages = arrayICS.size();
            this.context = context;
        }

        @Override
        public int getCount() {
            return NumberOfPages;
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
                    String message = "";
                    Toast.makeText(context,
                            ics.getTitulo(),Toast.LENGTH_LONG).show();
                }});

            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout)object);
        }

    }

}
