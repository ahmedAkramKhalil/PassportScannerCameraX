package com.foo.ocr.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.foo.ocr.R;
import com.foo.ocr.mrzscanner.mrzdecoder.MRZItem;

import java.util.List;

public class MRZRecyclerAdapter extends RecyclerView.Adapter<MRZRecyclerAdapter.ViewHolder>{

    private List<MRZItem> listData;
    public MRZRecyclerAdapter(List<MRZItem> dataMap) {
        this.listData = dataMap;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final MRZItem myListData = listData.get(position);
        holder.title.setText(myListData.getName());
        holder.data.setText(myListData.getData());

    }


    @Override
    public int getItemCount() {
        return listData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView data;
        public ViewHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.title);
            this.data = (TextView)itemView.findViewById(R.id.content);
        }
    }

}
