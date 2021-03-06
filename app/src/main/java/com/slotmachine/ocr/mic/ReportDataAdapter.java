package com.slotmachine.ocr.mic;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.List;

public class ReportDataAdapter extends RecyclerView.Adapter<ReportDataAdapter.ReportDataHolder> {

    private List<RowData> rowDataList;

    public ReportDataAdapter(List<RowData> rowDataList) {
        this.rowDataList = rowDataList;
    }

    @Override
    @NonNull
    public ReportDataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.report_list_row, parent,false);
        return new ReportDataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ReportDataHolder holder, final int position) {

        RowData rowData = rowDataList.get(position);

        String progressive1 = rowData.getProgressive1();
        String progressive2 = rowData.getProgressive2();
        String progressive3 = rowData.getProgressive3();
        String progressive4 = rowData.getProgressive4();
        String progressive5 = rowData.getProgressive5();
        String progressive6 = rowData.getProgressive6();
        String progressive7 = rowData.getProgressive7();
        String progressive8 = rowData.getProgressive8();
        String progressive9 = rowData.getProgressive9();
        String progressive10 = rowData.getProgressive10();

        holder.machineIdTextView.setText("Machine ID: " + rowData.getMachineId());
        holder.dateTextView.setText(rowData.getDate());
        holder.userNameTextView.setText(rowData.getUser());

        if (!progressive1.trim().isEmpty()) {
            holder.progressiveTextView1.setText("$" + rowData.getProgressive1());
        } else {
            holder.progressiveTextView1.setText("");
        }

        if (!progressive2.trim().isEmpty()) {
            holder.progressiveTextView2.setText("$" + rowData.getProgressive2());
        } else {
            holder.progressiveTextView2.setText("");
        }

        if (!progressive3.trim().isEmpty()) {
            holder.progressiveTextView3.setText("$" + rowData.getProgressive3());
        } else {
            holder.progressiveTextView3.setText("");
        }

        if (!progressive4.trim().isEmpty()) {
            holder.progressiveTextView4.setText("$" + rowData.getProgressive4());
        } else {
            holder.progressiveTextView4.setText("");
        }

        if (!progressive5.trim().isEmpty()) {
            holder.progressiveTextView5.setText("$" + rowData.getProgressive5());
        } else {
            holder.progressiveTextView5.setText("");
        }

        if (!progressive6.trim().isEmpty()) {
            holder.progressiveTextView6.setText("$" + rowData.getProgressive6());
        } else {
            holder.progressiveTextView6.setText("");
        }

        if (!progressive7.trim().isEmpty()) {
            holder.progressiveTextView7.setText("$" + rowData.getProgressive7());
        } else {
            holder.progressiveTextView7.setText("");
        }

        if (!progressive8.trim().isEmpty()) {
            holder.progressiveTextView8.setText("$" + rowData.getProgressive8());
        } else {
            holder.progressiveTextView8.setText("");
        }

        if (!progressive9.trim().isEmpty()) {
            holder.progressiveTextView9.setText("$" + rowData.getProgressive9());
        } else {
            holder.progressiveTextView9.setText("");
        }

        if (!progressive10.trim().isEmpty()) {
            holder.progressiveTextView10.setText("$" + rowData.getProgressive10());
        } else {
            holder.progressiveTextView10.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return rowDataList.size();
    }

    public void setItem(int position, RowData item) {
        rowDataList.set(position, item);
        notifyItemChanged(position);
    }

    public static class ReportDataHolder extends RecyclerView.ViewHolder{

        TextView machineIdTextView, dateTextView, userNameTextView;
        TextView progressiveTextView1, progressiveTextView2, progressiveTextView3;
        TextView progressiveTextView4, progressiveTextView5, progressiveTextView6;
        TextView progressiveTextView7, progressiveTextView8, progressiveTextView9, progressiveTextView10;
        TextView notesTextView;
        CheckBox checkBox;

        private ReportDataHolder(View itemView) {
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
            progressiveTextView7 = itemView.findViewById(R.id.progressiveTextView7);
            progressiveTextView8 = itemView.findViewById(R.id.progressiveTextView8);
            progressiveTextView9 = itemView.findViewById(R.id.progressiveTextView9);
            progressiveTextView10 = itemView.findViewById(R.id.progressiveTextView10);
            notesTextView = itemView.findViewById(R.id.notesTextView);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
