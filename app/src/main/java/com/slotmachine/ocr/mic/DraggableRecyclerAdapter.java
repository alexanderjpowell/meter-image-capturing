package com.slotmachine.ocr.mic;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import timber.log.Timber;

public class DraggableRecyclerAdapter extends RecyclerView.Adapter<DraggableRecyclerAdapter.MyViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {

    private ArrayList data;

    public DraggableRecyclerAdapter(ArrayList data) {
        this.data = data;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draggable_progressive, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(data, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(data, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onRowSelected(MyViewHolder myViewHolder) {
        //myViewHolder.rowView.setBackgroundColor(Color.GRAY);

    }

    @Override
    public void onRowClear(MyViewHolder myViewHolder) {
        //myViewHolder.rowView.setBackgroundColor(Color.WHITE);

    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        View rowView;

        public MyViewHolder(View itemView) {
            super(itemView);
            rowView = itemView;
        }
    }


}