package com.slotmachine.ocr.mic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ToDoListDataAdapter extends RecyclerView.Adapter<ToDoListDataAdapter.ToDoListDataHolder> implements Filterable {

    private Context context;
    private List<ToDoListData> rowDataList;
    private List<ToDoListData> rowDataListAll;

    public ToDoListDataAdapter(Context context, List<ToDoListData> rowDataList) {
        this.context = context;
        this.rowDataList = rowDataList;
        this.rowDataListAll = new ArrayList<>(rowDataList);
    }

    @Override
    @NonNull
    public ToDoListDataAdapter.ToDoListDataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.to_do_list_item_row, parent,false);
        return new ToDoListDataAdapter.ToDoListDataHolder(view);
    }

    @Override
    public Filter getFilter() {
        //this.rowDataListAll = new ArrayList<>(rowDataList);
        return filter;
    }

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            //rowDataListAll =
            List<ToDoListData> filteredList = new ArrayList<>();

            if (charSequence.toString().trim().isEmpty()) {
                filteredList.addAll(rowDataListAll);
                //Toast.makeText(context, rowDataListAll.toString(), Toast.LENGTH_SHORT).show();
            } else {
                for (ToDoListData i : rowDataListAll) {
                    if (i.getMachineId().startsWith(charSequence.toString().trim())) {
                        //Toast.makeText(context, "here", Toast.LENGTH_SHORT).show();
                        filteredList.add(i);
                    }
                }
            }

            Toast.makeText(context, rowDataListAll.toString(), Toast.LENGTH_SHORT).show();

            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            rowDataList.clear();
            rowDataList.addAll((List)filterResults.values);
            notifyDataSetChanged();
        }
    };

    @Override
    public void onBindViewHolder(@NonNull final ToDoListDataAdapter.ToDoListDataHolder holder, final int position) {

        ToDoListData toDoListData = rowDataList.get(position);

        holder.machineIdTextView.setText(String.format(Locale.US,"%s: %s", context.getString(R.string.machine_id_text), toDoListData.getMachineId()));
        holder.descriptionTextView.setText(toDoListData.getDescription());
        holder.locationTextView.setText(String.format(Locale.US, "%s: %s", context.getString(R.string.location_text), toDoListData.getLocation()));

        /*if (toDoListData.getNumberOfProgressives() != null) {
            if (toDoListData.getNumberOfProgressives() == 1) {
                holder.numberOfProgressivesTextView.setText(String.format(Locale.US,"%d %s", toDoListData.getNumberOfProgressives(), context.getString(R.string.progressive_values_row_label_singular)));
            } else {
                holder.numberOfProgressivesTextView.setText(String.format(Locale.US,"%d %s", toDoListData.getNumberOfProgressives(), context.getString(R.string.progressive_values_row_label)));
            }
            holder.numberOfProgressivesTextView.setVisibility(View.VISIBLE);
        } else {
            holder.numberOfProgressivesTextView.setVisibility(View.GONE);
        }*/

        if ((toDoListData.getUser() != null) && (!toDoListData.getUser().trim().isEmpty())) {
            holder.userTextView.setText(String.format(Locale.US, "%s %s", context.getString(R.string.assigned_to_text), toDoListData.getUser()));
            holder.userTextView.setVisibility(View.VISIBLE);
        } else {
            holder.userTextView.setVisibility(View.GONE);
        }

        String progressiveDescriptions = "";
        //int len = toDoListData.getProgressiveDescriptions().length;
        int len = toDoListData.getDescriptionsLength();
        //for (int i = 0; i < 6; i++) {
        //for (int i = 0; i < 10; i++) {
        for (int i = 0; i < len; i++) {
            //if ((toDoListData.getProgressiveDescriptions()[i] != null) && !toDoListData.getProgressiveDescriptions()[i].isEmpty()) {
            if ((toDoListData.getProgressiveDescriptionsList().get(i) != null) && !toDoListData.getProgressiveDescriptionsList().isEmpty()) {
                progressiveDescriptions += toDoListData.getProgressiveDescriptionsList().get(i) + " ";
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
        TextView userTextView;
        TextView progressiveDescriptionTitles;

        public ToDoListDataHolder(View itemView) {
            super(itemView);

            machineIdTextView = itemView.findViewById(R.id.machineIdTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            userTextView = itemView.findViewById(R.id.userTextView);
            progressiveDescriptionTitles = itemView.findViewById(R.id.progressiveDescriptionTitles);
        }
    }

    /*public List<RowData> getRowDataList(){
        return rowDataList;
    }*/
}
