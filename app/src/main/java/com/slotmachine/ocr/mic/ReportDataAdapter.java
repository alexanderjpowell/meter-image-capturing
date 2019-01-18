package com.slotmachine.ocr.mic;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ReportDataAdapter extends RecyclerView.Adapter<ReportDataAdapter.MyViewHolder> {

    private List<RowData> rowDataList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView machineId, date, numberOfProgressives, numberOfProgressives1, numberOfProgressives2, numberOfProgressives3;

        public MyViewHolder(View view) {
            super(view);
            machineId = (TextView)view.findViewById(R.id.machineIdTextView);
            date = (TextView)view.findViewById(R.id.dateTextView);
            numberOfProgressives = (TextView)view.findViewById(R.id.numberOfProgressivesTextView);
            numberOfProgressives1 = (TextView)view.findViewById(R.id.numberOfProgressivesTextView1);
            numberOfProgressives2 = (TextView)view.findViewById(R.id.numberOfProgressivesTextView2);
            numberOfProgressives3 = (TextView)view.findViewById(R.id.numberOfProgressivesTextView3);
        }
    }

    public ReportDataAdapter(List<RowData> rowDataList) {
        this.rowDataList = rowDataList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.report_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        RowData row = rowDataList.get(position);
        holder.machineId.setText(row.getMachineId());
        holder.date.setText(row.getDate());
        holder.numberOfProgressives.setText(row.getNumberOfProgressives());
        holder.numberOfProgressives1.setText(row.getNumberOfProgressives1());
        holder.numberOfProgressives2.setText(row.getNumberOfProgressives2());
        holder.numberOfProgressives3.setText(row.getNumberOfProgressives3());
    }

    @Override
    public int getItemCount() {
        return rowDataList.size();
    }

}
