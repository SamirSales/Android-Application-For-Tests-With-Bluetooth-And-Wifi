package com.example.samir.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.samir.comunicacaodedispositivos.R;

/**
 * Created by samir on 09/02/15.
 */
public class AdapterListModos extends BaseAdapter{
    private LayoutInflater mInflater;
    private List<Item> item;
    private int modo;

    public AdapterListModos(Context context, List<Item> item, int modo) {
        this.item = item;
        this.modo = modo;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return item.size();
    }

    @Override
    public Object getItem(int position) {
        return item.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    static class ViewHolder {
        public TextView modo;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent){
        ViewHolder itemHolder;

        //Pega o item de acordo com a posicao
        final Item itens = item.get(position);

        if(view==null){
            view= mInflater.inflate(R.layout.item_listview, null);
            itemHolder = new ViewHolder();
            itemHolder.modo = (TextView)view.findViewById(this.modo);
            view.setTag(itemHolder);
        }else{
            // pega a view de volta
            itemHolder = (ViewHolder)view.getTag();
        }
        // Atribui seus dados nas suas views. Usando esse Holder a mais eficiente
        itemHolder.modo.setText(itens.getModo());
        return view;
    }
}
