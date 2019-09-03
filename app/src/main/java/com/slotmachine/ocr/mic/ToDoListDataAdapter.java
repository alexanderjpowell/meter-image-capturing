package com.slotmachine.ocr.mic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

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

        holder.machineIdTextView.setText(String.format(Locale.US,"%s: %s", context.getString(R.string.machine_id_text), toDoListData.getMachineId()));
        holder.descriptionTextView.setText(toDoListData.getDescription());
        holder.locationTextView.setText(String.format(Locale.US, "%s: %s", context.getString(R.string.location_text), toDoListData.getLocation()));

        if (toDoListData.getNumberOfProgressives() != null) {
            if (toDoListData.getNumberOfProgressives() == 1) {
                holder.numberOfProgressivesTextView.setText(String.format(Locale.US,"%d %s", toDoListData.getNumberOfProgressives(), context.getString(R.string.progressive_values_row_label_singular)));
            } else {
                holder.numberOfProgressivesTextView.setText(String.format(Locale.US,"%d %s", toDoListData.getNumberOfProgressives(), context.getString(R.string.progressive_values_row_label)));
            }
            holder.numberOfProgressivesTextView.setVisibility(View.VISIBLE);
        } else {
            holder.numberOfProgressivesTextView.setVisibility(View.GONE);
        }

        if (toDoListData.getUser() != null) {
            holder.userTextView.setText(String.format(Locale.US, "%s %s", context.getString(R.string.assigned_to_text), toDoListData.getUser()));
            holder.userTextView.setVisibility(View.VISIBLE);
        } else {
            holder.userTextView.setVisibility(View.GONE);
        }

        String progressiveDescriptions = "";
        for (int i = 0; i < 4; i++) {
            if ((toDoListData.getProgressiveDescriptions()[i] != null) && !toDoListData.getProgressiveDescriptions()[i].isEmpty()) {
                progressiveDescriptions += toDoListData.getProgressiveDescriptions()[i] + " ";
            }
        }
        holder.progressiveDescriptionTitles.setText(progressiveDescriptions.trim());
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

        TextView machineIdTextView;
        TextView descriptionTextView, locationTextView;
        TextView numberOfProgressivesTextView;
        TextView userTextView;
        TextView progressiveDescriptionTitles;

        public ToDoListDataHolder(View itemView) {
            super(itemView);

            machineIdTextView = itemView.findViewById(R.id.machineIdTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            numberOfProgressivesTextView = itemView.findViewById(R.id.numberOfProgressivesTextView);
            userTextView = itemView.findViewById(R.id.userTextView);
            progressiveDescriptionTitles = itemView.findViewById(R.id.progressiveDescriptionTitles);
        }
    }

    /*public List<RowData> getRowDataList(){
        return rowDataList;
    }*/
}
