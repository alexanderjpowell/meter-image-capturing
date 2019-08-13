package com.slotmachine.ocr.mic;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.List;

public class ReportDataAdapter extends RecyclerView.Adapter<ReportDataAdapter.ReportDataHolder> {

    private Context context;
    private List<RowData> rowDataList;

    public ReportDataAdapter(Context context, List<RowData> rowDataList) {
        this.context = context;
        this.rowDataList = rowDataList;
    }

    @Override
    @NonNull
    public ReportDataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.report_list_row, parent,false);
        return new ReportDataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ReportDataHolder holder, final int position) {

        RowData rowData = rowDataList.get(position);

        holder.machineIdTextView.setText(rowData.getMachineId());
        holder.dateTextView.setText(rowData.getDate());
        holder.userNameTextView.setText(rowData.getUser());
        holder.progressiveTextView1.setText(rowData.getProgressive1());
        holder.progressiveTextView2.setText(rowData.getProgressive2());
        holder.progressiveTextView3.setText(rowData.getProgressive3());
        holder.progressiveTextView4.setText(rowData.getProgressive4());
        holder.progressiveTextView5.setText(rowData.getProgressive5());
        holder.progressiveTextView6.setText(rowData.getProgressive6());
        holder.notesTextView.setText(rowData.getNotes());

        holder.checkBox.setChecked(rowData.isSelected());
        holder.checkBox.setTag(rowDataList.get(position));

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RowData rowData1 = (RowData)holder.checkBox.getTag();
                rowData1.setSelected(holder.checkBox.isChecked());
                rowDataList.get(position).setSelected(holder.checkBox.isChecked());
            }
        });


    }

    @Override
    public int getItemCount() {
        return rowDataList.size();
    }

    /*public void removeItem(int position) {
        rowDataList.remove(position);
        notifyItemRemoved(position);
    }*/

    public static class ReportDataHolder extends RecyclerView.ViewHolder{

        TextView machineIdTextView, dateTextView, userNameTextView;
        TextView progressiveTextView1, progressiveTextView2, progressiveTextView3;
        TextView progressiveTextView4, progressiveTextView5, progressiveTextView6;
        TextView notesTextView;
        CheckBox checkBox;

        public ReportDataHolder(View itemView) {
            super(itemView);

            machineIdTextView = itemView.findViewById(R.id.machineIdTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            progressiveTextView1 = itemView.findViewById(R.id.progressiveTextView1);
            progressiveTextView2 = itemView.findViewById(R.id.progressiveTextView2);
            progressiveTextView3 = itemView.findViewById(R.id.progressiveTextView3);
            progressiveTextView4 = itemView.findViewById(R.id.progressiveTextView4);
            progressiveTextView5 = itemView.findViewById(R.id.progressiveTextView5);
            progressiveTextView6 = itemView.findViewById(R.id.progressiveTextView6);
            notesTextView = itemView.findViewById(R.id.notesTextView);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    /*public List<RowData> getRowDataList(){
        return rowDataList;
    }*/
}
