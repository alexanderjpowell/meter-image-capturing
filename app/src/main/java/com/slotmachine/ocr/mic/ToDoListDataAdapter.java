package com.slotmachine.ocr.mic;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ToDoListDataAdapter extends RecyclerView.Adapter<ToDoListDataAdapter.ToDoListDataHolder> {

    private Context context;
    private List<ToDoListData> rowDataList;

    public ToDoListDataAdapter(Context context, List<ToDoListData> rowDataList) {
        this.context = context;
        this.rowDataList = rowDataList;
    }

    @Override
    @NonNull
    public ToDoListDataAdapter.ToDoListDataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.to_do_list_item_row, parent,false);
        return new ToDoListDataAdapter.ToDoListDataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ToDoListDataAdapter.ToDoListDataHolder holder, final int position) {

        ToDoListData toDoListData = rowDataList.get(position);

        holder.machineIdTextView.setText(toDoListData.getMachineId());
        holder.descriptionTextView.setText(toDoListData.getDescription());
        holder.locationTextView.setText(toDoListData.getLocation());

        if (toDoListData.isCompleted()) {
            holder.locationTextView.setTextColor(Color.GREEN);
            holder.lastScannedTextView.setText(toDoListData.getDate());
        } else {
            holder.lastScannedTextView.setVisibility(View.INVISIBLE);
        }

        /*holder.progressiveTextView1.setText(rowData.getProgressive1());
        holder.progressiveTextView2.setText(rowData.getProgressive2());
        holder.progressiveTextView3.setText(rowData.getProgressive3());
        holder.progressiveTextView4.setText(rowData.getProgressive4());
        holder.progressiveTextView5.setText(rowData.getProgressive5());
        holder.progressiveTextView6.setText(rowData.getProgressive6());
        holder.notesTextView.setText(rowData.getNotes());

        holder.checkBox.setChecked(rowData.isSelected());*/
        /*holder.checkBox.setTag(rowDataList.get(position));

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RowData rowData1 = (RowData)holder.checkBox.getTag();
                rowData1.setSelected(holder.checkBox.isChecked());
                rowDataList.get(position).setSelected(holder.checkBox.isChecked());
            }
        });*/
    }

    @Override
    public int getItemCount() {
        return rowDataList.size();
    }

    /*public void removeItem(int position) {
        rowDataList.remove(position);
        notifyItemRemoved(position);
    }*/

    public static class ToDoListDataHolder extends RecyclerView.ViewHolder{

        TextView machineIdTextView, lastScannedTextView;//, userNameTextView;
        TextView descriptionTextView, locationTextView;
        //CheckBox checkBox;

        public ToDoListDataHolder(View itemView) {
            super(itemView);

            machineIdTextView = itemView.findViewById(R.id.machineIdTextView);
            lastScannedTextView = itemView.findViewById(R.id.lastScannedTextView);
            //userNameTextView = itemView.findViewById(R.id.userNameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
        }
    }

    /*public List<RowData> getRowDataList(){
        return rowDataList;
    }*/
}
