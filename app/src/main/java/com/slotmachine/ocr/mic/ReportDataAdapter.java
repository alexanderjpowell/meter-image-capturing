package com.slotmachine.ocr.mic;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ReportDataAdapter extends RecyclerView.Adapter<ReportDataAdapter.ReportDataHolder> {

    Context context;
    List<RowData> rowDataList = new ArrayList<>();

    public ReportDataAdapter(Context context, List<RowData> rowDataList) {
        this.context = context;
        this.rowDataList = rowDataList;
    }

    @Override
    public ReportDataHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.report_list_row,parent,false);
        return new ReportDataHolder(view);
    }

    @Override
    public void onBindViewHolder(final ReportDataHolder holder, final int position) {

        RowData rowData = rowDataList.get(position);

        holder.machineIdTextView.setText(rowData.getMachineId());
        holder.dateTextView.setText(rowData.getDate());
        holder.progressiveTextView1.setText(rowData.getProgressive1());
        holder.progressiveTextView2.setText(rowData.getProgressive2());
        holder.progressiveTextView3.setText(rowData.getProgressive3());
        holder.progressiveTextView4.setText(rowData.getProgressive4());
        holder.progressiveTextView5.setText(rowData.getProgressive5());
        holder.progressiveTextView6.setText(rowData.getProgressive6());

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

    public void removeItem(int position) {
        rowDataList.remove(position);
        notifyItemRemoved(position);
    }

    public static class ReportDataHolder extends RecyclerView.ViewHolder{

        TextView machineIdTextView, dateTextView;
        TextView progressiveTextView1, progressiveTextView2, progressiveTextView3;
        TextView progressiveTextView4, progressiveTextView5, progressiveTextView6;
        CheckBox checkBox;

        public ReportDataHolder(View itemView) {
            super(itemView);

            machineIdTextView = itemView.findViewById(R.id.machineIdTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            progressiveTextView1 = itemView.findViewById(R.id.progressiveTextView1);
            progressiveTextView2 = itemView.findViewById(R.id.progressiveTextView2);
            progressiveTextView3 = itemView.findViewById(R.id.progressiveTextView3);
            progressiveTextView4 = itemView.findViewById(R.id.progressiveTextView4);
            progressiveTextView5 = itemView.findViewById(R.id.progressiveTextView5);
            progressiveTextView6 = itemView.findViewById(R.id.progressiveTextView6);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    public List<RowData> getRowDataList(){
        return rowDataList;
    }
}
